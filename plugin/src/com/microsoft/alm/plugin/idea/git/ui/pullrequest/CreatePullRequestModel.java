// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.pullrequest;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.SortedComboBoxModel;
import com.microsoft.alm.common.artifact.ArtifactID;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.context.rest.GitHttpClientEx;
import com.microsoft.alm.plugin.context.rest.GitPullRequestEx;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.utils.EventContextHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.git.utils.GeneralGitHelper;
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.alm.workitemtracking.webapi.models.Link;
import com.microsoft.visualstudio.services.webapi.patch.json.JsonPatchDocument;
import com.microsoft.visualstudio.services.webapi.patch.json.JsonPatchOperation;
import git4idea.GitBranch;
import git4idea.GitCommit;
import git4idea.GitExecutionException;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepoInfo;
import git4idea.repo.GitRepository;
import git4idea.util.GitCommitCompareInfo;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ComboBoxModel;
import javax.swing.event.HyperlinkEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

public class CreatePullRequestModel extends AbstractModel {

    private static final Logger logger = LoggerFactory.getLogger(CreatePullRequestModel.class);


    /* size limit */
    public static final int MAX_SIZE_TITLE = 400;
    public static final int MAX_SIZE_DESCRIPTION = 4000;

    /**
     * Property names
     */
    public static final String PROP_TARGET_BRANCH = "targetBranch";
    public static final String PROP_TARGET_BRANCH_COMBO_MODEL = "targetBranchComboModel";
    public static final String PROP_SOURCE_BRANCH = "sourceBranch";
    public static final String PROP_TITLE = "title";
    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_LOADING = "loading";
    public static final String PROP_DIFF_MODEL = "diffModel";
    private static final String LINK_NAME_KEY = "name";
    private static final String LINK_NAME_VALUE = "Pull Request";
    private static final String ARTIFACT_LINK_RELATION = "ArtifactLink";
    private static final String RELATIONS_PATH = "/relations/-";

    private Project project;
    private GitRepository gitRepository;
    private GitRemoteBranch targetBranch;
    private String title;
    private String description;
    private final Collection<GitRemote> tfGitRemotes;
    private final ComboBoxModel remoteBranchComboModel;
    private final PullRequestHelper pullRequestHelper;
    private final Set<Integer> workItems;

    /* Branch diff provider, non-final for unit test */
    private DiffCompareInfoProvider diffCompareInfoProvider;

    /* UI properties */
    private boolean loading = false;
    private GitChangesContainer localBranchChanges;

    private final LoadingCache<Pair<String, String>, GitCommitCompareInfo> diffCache;

    /* Executor service for running diff calculating Futures */
    private final ListeningExecutorService executorService;

    private ApplicationProvider applicationProvider;


    public CreatePullRequestModel(@NotNull final Project project,
                                  @NotNull final GitRepository gitRepository) {
        this.project = project;
        this.gitRepository = gitRepository;

        this.tfGitRemotes = TfGitHelper.getTfGitRemotes(gitRepository);

        this.remoteBranchComboModel = createRemoteBranchDropdownModel();
        this.targetBranch = (GitRemoteBranch) this.remoteBranchComboModel.getSelectedItem();

        this.applicationProvider = new ApplicationProvider();
        this.pullRequestHelper = new PullRequestHelper();

        this.diffCompareInfoProvider = new DiffCompareInfoProvider();
        this.diffCache = CacheBuilder.newBuilder().maximumSize(20)
                .build(
                        new CacheLoader<Pair<String, String>, GitCommitCompareInfo>() {
                            @Override
                            public GitCommitCompareInfo load(Pair<String, String> key) throws Exception {
                                // if we missed the cache, then show the loading spinner, otherwise
                                // just switch to the diff we have to avoid flickering the screen
                                applicationProvider.invokeAndWaitWithAnyModality(new Runnable() {
                                    @Override
                                    public void run() {
                                        // set the view to show loading
                                        setLoading(true);
                                    }
                                });

                                return getDiffCompareInfoProvider()
                                        .getBranchCompareInfo(project, gitRepository, key.getFirst(), key.getSecond());
                            }
                        }
                );

        this.executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        this.workItems = new HashSet<Integer>();
    }

    public boolean dispose(long timeout, TimeUnit timeUnit) throws InterruptedException {
        executorService.shutdown();
        return executorService.awaitTermination(timeout, timeUnit);
    }

    public Project getProject() {
        return project;
    }

    public void setProject(final Project project) {
        this.project = project;
    }

    public synchronized String getTitle() {
        return StringUtils.isNotBlank(title) ? title : StringUtils.EMPTY;
    }

    public void setTitle(final String title) {
        synchronized (this) {
            this.title = StringUtils.trim(title);
        }
        setChangedAndNotify(PROP_TITLE);
    }

    public synchronized String getDescription() {
        return StringUtils.isNotBlank(description) ? description : StringUtils.EMPTY;
    }

    public void setDescription(final String description) {
        synchronized (this) {
            this.description = StringUtils.trim(description);
        }
        setChangedAndNotify(PROP_DESCRIPTION);
    }

    public DiffCompareInfoProvider getDiffCompareInfoProvider() {
        return diffCompareInfoProvider;
    }

    public void setDiffCompareInfoProvider(final DiffCompareInfoProvider diffCompareInfoProvider) {
        this.diffCompareInfoProvider = diffCompareInfoProvider;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(final boolean loading) {
        if (this.loading != loading) {
            this.loading = loading;
            setChangedAndNotify(PROP_LOADING);
        }
    }

    public GitChangesContainer getLocalBranchChanges() {
        return localBranchChanges;
    }

    public void setLocalBranchChanges(final GitChangesContainer localBranchChanges) {
        if (this.localBranchChanges != localBranchChanges) {
            this.localBranchChanges = localBranchChanges;
            setChangedAndNotify(PROP_DIFF_MODEL);
        }
    }

    /**
     * Get current branch
     *
     * @return current branch
     */
    @Nullable
    public GitLocalBranch getSourceBranch() {
        return getInfo() != null ? getInfo().getCurrentBranch() : null;
    }

    /**
     * Get target branch
     *
     * @return the target branch
     */
    public synchronized GitRemoteBranch getTargetBranch() {
        return this.targetBranch;
    }

    /**
     * Set the target branch
     *
     * @param targetBranch
     */
    public synchronized void setTargetBranch(final GitRemoteBranch targetBranch) {
        if (this.targetBranch != targetBranch) {
            this.targetBranch = targetBranch;
            setChangedAndNotify(PROP_TARGET_BRANCH);
        }
    }

    /**
     * Get all remote branches from all alm remotes
     *
     * @return ComboBoxModel with all alm remote branches
     */
    @NotNull
    public ComboBoxModel getRemoteBranchDropdownModel() {
        return this.remoteBranchComboModel;
    }

    private ComboBoxModel createRemoteBranchDropdownModel() {
        final SortedComboBoxModel<GitRemoteBranch> sortedRemoteBranches
                = new SortedComboBoxModel<GitRemoteBranch>(new TfGitHelper.BranchComparator());
        final GitRemoteBranch remoteTrackingBranch = this.getRemoteTrackingBranch();

        // only show valid remote branches
        sortedRemoteBranches.addAll(Collections2.filter(getInfo().getRemoteBranches(),
                remoteBranch -> {
                    /* two conditions:
                     *   1. remote must be a vso/tfs remote
                     *   2. this isn't the remote tracking branch of current local branch
                     */
                    return tfGitRemotes.contains(remoteBranch.getRemote())
                            && !remoteBranch.equals(remoteTrackingBranch);
                })
        );
        sortedRemoteBranches.setSelectedItem(TfGitHelper.getDefaultBranch(sortedRemoteBranches.getItems(), tfGitRemotes));

        return sortedRemoteBranches;
    }

    @Nullable
    private GitRemoteBranch getRemoteTrackingBranch() {
        final GitLocalBranch localBranch = this.getSourceBranch();

        return localBranch != null && this.gitRepository != null
                ? localBranch.findTrackedBranch(this.gitRepository) : null;
    }

    /**
     * This method calculates the commits and diff information against the tip of current branch and
     * the common ancestor of source branch (current branch) and target branch (selected remote branch).
     * <p/>
     * If there is no common parent (two branches are parallel), return an empty GitCommitCompareInfo
     * <p/>
     * This is potentially an expensive calculation, probably should do it on a background thread.
     * We will also attempt to cache the result
     * <p/>
     * default access for testing so we bypass UI code,
     * TODO: reevaluate the testing to properly shutoff the access level
     *
     * @return gitChangesContainer on what has changed on source branch
     */
    GitChangesContainer getMyChangesCompareInfo() throws VcsException {

        final GitBranch currBranch = this.getSourceBranch();

        final GitRemoteBranch selectedRemoteBranch = this.getTargetBranch();

        // if source branch or currentBranch isn't set, just return empty diff
        if (selectedRemoteBranch == null || currBranch == null) {
            return GitChangesContainer.createChangesContainer(null, null, null, null,
                    getDiffCompareInfoProvider().getEmptyDiff(this.gitRepository), this.gitRepository);
        }

        // get hash of last commit for each branch
        final String remoteBranchHash = GeneralGitHelper.getLastCommitHash(project, gitRepository, selectedRemoteBranch);
        final String currBranchHash = GeneralGitHelper.getLastCommitHash(project, gitRepository, currBranch);

        try {
            GitCommitCompareInfo changes
                    = this.diffCache.get(new Pair<String, String>(currBranchHash, remoteBranchHash));

            return GitChangesContainer.createChangesContainer(currBranch.getName(), selectedRemoteBranch.getName(),
                    currBranchHash, remoteBranchHash, changes, this.gitRepository);
        } catch (ExecutionException e) {
            throw new VcsException(e.getCause());
        }
    }

    /**
     * This method spawns a background thread to calculate the diff
     * <p/>
     * TODO: refactor the onSuccess/onFailure callback so we can test this method
     */
    public void loadDiff() {
        if (this.getSourceBranch() != null && this.getTargetBranch() != null) {
            ListenableFuture<GitChangesContainer> diffFuture = this.executorService.submit(new Callable<GitChangesContainer>() {
                @Override
                public GitChangesContainer call() throws Exception {
                    // calculate the diffs
                    return getMyChangesCompareInfo();
                }
            });

            Futures.addCallback(diffFuture, new FutureCallback<GitChangesContainer>() {
                public void onSuccess(final GitChangesContainer changesContainer) {
                    applicationProvider.invokeAndWaitWithAnyModality(new Runnable() {
                        @Override
                        public void run() {
                            // try to update the view to show diff, but only if the calculated diff
                            // is still upto date -- make sure user didn't select another branch
                            // while we were busy calculating the diffs
                            if (changesContainer != null && isChangesUpToDate(changesContainer)) {
                                setLoading(false);

                                final GitCommitCompareInfo compareInfo = changesContainer.getGitCommitCompareInfo();
                                if (compareInfo != null) {
                                    List<GitCommit> commits
                                            = compareInfo.getBranchToHeadCommits(changesContainer.getGitRepository());

                                    final GitLocalBranch sourceBranch = getSourceBranch();
                                    final GitRemoteBranch targetBranch = getTargetBranch();

                                    if (StringUtils.isEmpty(getTitle()) && commits != null && sourceBranch.getName() != null
                                            && targetBranch.getNameForRemoteOperations() != null) {
                                        final String defaultTitle = pullRequestHelper.createDefaultTitle(commits,
                                                sourceBranch.getName(),
                                                targetBranch.getNameForRemoteOperations());
                                        setTitle(defaultTitle);

                                        final String defaultDescription
                                                = pullRequestHelper.createDefaultDescription(commits);
                                        setDescription(defaultDescription);
                                    }

                                    // find workitems from commits
                                    workItems.clear();
                                    for (final GitCommit commit : commits) {
                                        final String commitMsg = commit.getFullMessage();
                                        workItems.addAll(VcsHelper.getWorkItemIdsFromMessage(commitMsg));
                                    }
                                }

                                setLocalBranchChanges(changesContainer);
                            }
                        }
                    });
                }

                public void onFailure(final Throwable thrown) {
                    logger.warn("onFailure in loadDiff", thrown);

                    applicationProvider.invokeAndWaitWithAnyModality(new Runnable() {
                        public void run() {
                            final GitLocalBranch sourceBranch = getSourceBranch();
                            final GitRemoteBranch targetBranch = getTargetBranch();
                            final String sourceBranchName = sourceBranch != null ? sourceBranch.getName() : "";
                            final String targetBranchName = targetBranch != null ? targetBranch.getName() : "";
                            notifyDiffFailedError(getProject(),
                                    TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_ERRORS_DIFF_FAILED_MSG,
                                            sourceBranchName, targetBranchName));

                            final GitChangesContainer changesContainer = GitChangesContainer.createChangesContainer(sourceBranchName, targetBranchName, null, null,
                                    getDiffCompareInfoProvider().getEmptyDiff(gitRepository), gitRepository);
                            if (isChangesUpToDate(changesContainer)) {
                                setLoading(false);
                                setLocalBranchChanges(changesContainer);
                            }

                        }
                    });
                }
            }, directExecutor());
        }
    }

    /**
     * Create pull request on a background thread
     * <p/>
     * This method will first check to see if the local branch has a tracking branch:
     * yes:
     * push the commits to the remote tracking branch
     * no:
     * try create a remote branch matching the local branch name exactly, with the remote set to the GitRemote of
     * the target branch
     * <p/>
     * If push fails for whatever reason, stop and show an error message
     * <p/>
     * After we push the local branch, then create the pull request.  Pull request link should be returned
     * in a notification bubble
     */
    public void createPullRequest() {
        /* verifying branch selections */
        final GitLocalBranch sourceBranch = this.getSourceBranch();
        final GitRemoteBranch targetBranch = this.getTargetBranch();

        if (sourceBranch == null) {
            // how did we get here? validation failed?
            notifyCreateFailedError(project,
                    TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_ERRORS_SOURCE_EMPTY));
            return;
        }

        if (targetBranch == null) {
            // how did we get here? validation failed?
            notifyCreateFailedError(project,
                    TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_ERRORS_TARGET_NOT_SELECTED));
            return;
        }

        if (targetBranch.equals(this.getRemoteTrackingBranch())) {
            // how did we get here? Didn't we filter you out?
            notifyCreateFailedError(project,
                    TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_ERRORS_TARGET_IS_LOCAL_TRACKING));
            return;
        }

        //TODO Determine the correct/best way to get the remote url
        final String gitRemoteUrl = TfGitHelper.getTfGitRemote(gitRepository).getFirstUrl();
        final CreatePullRequestModel createModel = this;
        /* Let's keep all server interactions to a background thread */
        final Task.Backgroundable createPullRequestTask = new Task.Backgroundable(project, TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_DIALOG_TITLE),
                true, PerformInBackgroundOption.DEAF) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                ListenableFuture<Pair<String, GitCommandResult>> pushResult
                        = doPushCommits(gitRepository, sourceBranch, targetBranch.getRemote(), progressIndicator);

                Futures.addCallback(pushResult, new FutureCallback<Pair<String, GitCommandResult>>() {
                    @Override
                    public void onSuccess(@Nullable Pair<String, GitCommandResult> result) {
                        if (result != null && StringUtils.isNotEmpty(result.getFirst())) {
                            final String title = createModel.getTitle();
                            final String description = createModel.getDescription();
                            final String branchNameOnRemoteServer = result.getFirst();

                            // get context from manager, we want to do this after push completes since credentials could have changed during the Git push
                            final ServerContext context = ServerContextManager.getInstance().getUpdatedContext(
                                    gitRemoteUrl, true);

                            if (context == null) {
                                notifyCreateFailedError(project, TfPluginBundle.message(TfPluginBundle.KEY_ERRORS_AUTH_NOT_SUCCESSFUL, gitRemoteUrl));
                                return;
                            }

                            doCreatePullRequest(project, context, title, description, branchNameOnRemoteServer, targetBranch);
                        } else {
                            // I really don't have anything else to say, push failed, the title says it all
                            // I have no error message to be more specific
                            notifyPushFailedError(createModel.getProject(), StringUtils.EMPTY);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        notifyPushFailedError(createModel.getProject(), t.getLocalizedMessage());
                    }
                }, directExecutor());
            }
        };

        createPullRequestTask.queue();
    }

    private ListenableFuture<Pair<String, GitCommandResult>> doPushCommits(@NotNull final GitRepository gitRepository,
                                                                           @NotNull final GitLocalBranch localBranch,
                                                                           @NotNull final GitRemote gitRemote,
                                                                           @NotNull final ProgressIndicator indicator) {
        // just set the result without going off to another thread, we should already be in a background task
        SettableFuture<Pair<String, GitCommandResult>> pushResult
                = SettableFuture.<Pair<String, GitCommandResult>>create();

        indicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_PUSH_TITLE));
        final Git git = ServiceManager.getService(Git.class);

        final GitRemoteBranch trackingBranch = localBranch.findTrackedBranch(gitRepository);

        final String createdBranchNameOnServer;
        final StringBuilder pushSpec = new StringBuilder(localBranch.getName());
        if (trackingBranch != null && trackingBranch.getRemote().equals(gitRemote)) {
            // if the tracking branch is on the same remote, we should update that
            pushSpec.append(":").append(trackingBranch.getNameForRemoteOperations());
            createdBranchNameOnServer = trackingBranch.getNameForRemoteOperations();
        } else {
            createdBranchNameOnServer = localBranch.getName();
        }

        final String fetchUrl = getFetchUrl(gitRemote);
        final String pushSpecStr = pushSpec.toString();
        final String gitRemoteName = gitRemote.getName();
        logger.debug("Pushing {} to {}: {}", pushSpecStr, gitRemoteName, fetchUrl);
        final GitCommandResult result
                = git.push(gitRepository, gitRemoteName, fetchUrl, pushSpecStr, true);

        if (result.success()) {
            pushResult.set(Pair.create(createdBranchNameOnServer, result));
        } else {
            final String errMsg = result.getErrorOutputAsJoinedString();
            pushResult.setException(new GitExecutionException(errMsg, null));
        }

        return pushResult;
    }

    private void doCreatePullRequest(@NotNull final Project project,
                                     @NotNull final ServerContext context,
                                     @NotNull final String title,
                                     @NotNull final String description,
                                     @NotNull final String branchNameOnRemoteServer,
                                     @NotNull final GitRemoteBranch targetBranch) {
        final GitHttpClientEx gitClient = context.getGitHttpClient();

        try {
            final UUID repositoryId = context.getGitRepository().getId();
            final UUID projectId = context.getTeamProjectReference().getId();

            final GitPullRequestEx pullRequestToBeCreated
                    = pullRequestHelper.generateGitPullRequest(title, description, branchNameOnRemoteServer, targetBranch, workItems, context);

            // creating pull request and associating work items with it (if that API is available)
            final GitPullRequest gitPullRequest
                    = gitClient.createPullRequest(pullRequestToBeCreated, projectId, repositoryId, true, true);

            final String repositoryRemoteUrl = context.getGitRepository().getRemoteUrl();
            notifySuccess(project, TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_CREATED_TITLE),
                    pullRequestHelper.getHtmlMsg(repositoryRemoteUrl, gitPullRequest.getPullRequestId()));

        } catch (Throwable t) {
            if (AuthHelper.isNotAuthorizedError(t)) {
                final ServerContext newContext = ServerContextManager.getInstance().updateAuthenticationInfo(context.getGitRepository().getRemoteUrl());
                if (newContext != null) {
                    //retry creating the pull request with new context and authentication info
                    doCreatePullRequest(project, newContext, title, description, branchNameOnRemoteServer, targetBranch);
                } else {
                    //user cancelled login, don't retry
                }
            } else {
                // catch everything so we don't bubble up to IntelliJ
                final Pair<PullRequestHelper.PRCreateStatus, String> parsed
                        = pullRequestHelper.parseException(t, branchNameOnRemoteServer, targetBranch, context, gitClient);

                if (parsed.getFirst() == PullRequestHelper.PRCreateStatus.DUPLICATE) {
                    notifySuccess(project,
                            TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_ALREADY_EXISTS_TITLE), parsed.getSecond());
                } else {
                    notifyCreateFailedError(project, parsed.getSecond());
                    logger.warn("Create pull request failed", t);
                }
            }
        }
    }

    private void createWorkItemLinks(final int workItemId, final ServerContext context, final int prNumber) {
        final String toolSpecificId = context.getTeamProjectReference().getId().toString() + "/" + context.getGitRepository().getId().toString() + "/" + prNumber;
        final ArtifactID artifactID = new ArtifactID("Git", "PullRequestId", toolSpecificId);

        // attributes specify the link type
        final HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(LINK_NAME_KEY, LINK_NAME_VALUE);

        // create link object to add to the work item
        final Link link = new Link();
        link.setUrl(artifactID.encodeURI());
        link.setTitle(StringUtils.EMPTY);
        link.setRel(ARTIFACT_LINK_RELATION);
        link.setAttributes(attributes);

        // create the operation that will add the link to the work item
        final JsonPatchOperation operation = new JsonPatchOperation();
        operation.setOp(com.microsoft.visualstudio.services.webapi.patch.Operation.ADD);
        operation.setPath(RELATIONS_PATH);
        operation.setValue(link);

        final JsonPatchDocument doc = new JsonPatchDocument();
        doc.add(operation);

        try {
            context.getWitHttpClient().updateWorkItem(doc, workItemId, false, false);
        } catch (Throwable t) {
            // if we fail then just swallow it
            logger.warn("createWorkItemLinks experienced an exception while associating a work item and pull request", t);
        }
    }

    private String getFetchUrl(@NotNull final GitRemote gitRemote) {
        return gitRemote.getFirstUrl();
    }

    /* if user has changed the dropdown while we calculate the diff, this diff is out of date */
    private boolean isChangesUpToDate(final GitChangesContainer changesContainer) {

        // target branches must match
        final GitRemoteBranch targetBranch = this.getTargetBranch();
        if (changesContainer.getTargetBranchName() != null && targetBranch != null) {
            if (!changesContainer.getTargetBranchName().equals(targetBranch.getName())) {
                return false;
            }
        }

        // source branches must match
        final GitLocalBranch sourceBranch = this.getSourceBranch();
        if (changesContainer.getSourceBranchName() != null && sourceBranch != null) {
            if (!changesContainer.getSourceBranchName().equals(sourceBranch.getName())) {
                return false;
            }
        }

        return true;
    }

    public ModelValidationInfo validate() {
        if (StringUtils.isEmpty(this.getTitle())) {
            return ModelValidationInfo.createWithResource(PROP_TITLE,
                    TfPluginBundle.KEY_CREATE_PR_ERRORS_TITLE_EMPTY);
        }

        if (this.getTitle().length() > MAX_SIZE_TITLE) {
            return ModelValidationInfo.createWithResource(PROP_TITLE,
                    TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_ERRORS_TITLE_TOO_LONG, MAX_SIZE_TITLE));
        }

        if (StringUtils.isEmpty(this.getDescription())) {
            return ModelValidationInfo.createWithResource(PROP_DESCRIPTION,
                    TfPluginBundle.KEY_CREATE_PR_ERRORS_DESCRIPTION_EMPTY);
        }

        if (this.getDescription().length() > MAX_SIZE_DESCRIPTION) {
            return ModelValidationInfo.createWithResource(PROP_DESCRIPTION,
                    TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_ERRORS_DESCRIPTION_TOO_LONG, MAX_SIZE_DESCRIPTION));
        }

        if (this.getSourceBranch() == null) {
            return ModelValidationInfo.createWithResource(PROP_SOURCE_BRANCH,
                    TfPluginBundle.KEY_CREATE_PR_ERRORS_SOURCE_EMPTY);
        }

        if (this.getTargetBranch() == null) {
            return ModelValidationInfo.createWithResource(PROP_TARGET_BRANCH,
                    TfPluginBundle.KEY_CREATE_PR_ERRORS_TARGET_NOT_SELECTED);
        }

        return ModelValidationInfo.NO_ERRORS;
    }

    private GitRepoInfo getInfo() {
        return this.gitRepository.getInfo();
    }

    private void notifyDiffFailedError(final Project project, final String message) {
        notifyError(project, TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_ERRORS_DIFF_FAILED_TITLE), message);
    }

    private void notifyPushFailedError(final Project project, final String message) {
        notifyError(project, TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_ERRORS_PUSH_FAILED_TITLE), message);
    }

    private void notifyCreateFailedError(final Project project, final String message) {
        notifyError(project, TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_ERRORS_CREATE_FAILED_TITLE), message);
    }

    private void notifyError(final Project project, final String title, final String message) {
        if (message != null) {
            VcsNotifier.getInstance(project).notifyError(title, message);
        } else {
            VcsNotifier.getInstance(project).notifyError(title, "");
        }
    }

    private void notifySuccess(final Project project, final String title, final String message) {
        VcsNotifier.getInstance(project).notifyImportantInfo(title, message, new NotificationListener() {
            @Override
            public void hyperlinkUpdate(@NotNull final Notification n, @NotNull final HyperlinkEvent e) {
                BrowserUtil.browse(e.getURL());
            }
        });

        // Update the PR tab and any other UI that is listening for PR Changed events
        EventContextHelper.triggerPullRequestChanged(EventContextHelper.SENDER_CREATE_PULL_REQUEST, project);
    }

    static class ApplicationProvider {
        public Application getApplication() {
            return ApplicationManager.getApplication();
        }

        /* must mock the ModalityState.any() call as it also goes out to ApplicationManager */
        public void invokeAndWaitWithAnyModality(final Runnable r) {
            getApplication().invokeAndWait(r, ModalityState.any());
        }
    }

    /* default */
    void setApplicationProvider(final ApplicationProvider applicationProvider) {
        this.applicationProvider = applicationProvider;
    }
}
