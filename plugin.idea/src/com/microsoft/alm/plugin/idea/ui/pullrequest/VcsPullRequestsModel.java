// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.ui.common.FeedbackAction;
import com.microsoft.alm.plugin.idea.ui.vcsimport.ImportController;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.PullRequestAsyncStatus;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.PullRequestStatus;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VcsPullRequestsModel extends AbstractModel {
    private static final Logger logger = LoggerFactory.getLogger(VcsPullRequestsModel.class);

    private final Project project;

    private final PullRequestsTreeModel treeModel;
    private final PullRequestsLookupListener treeDataProvider;

    private GitRepositoryProvider gitRepositoryProvider;
    private GitRepository gitRepository;

    private ServerContextProvider serverContextProvider;
    private ServerContext context;

    private boolean connected = false;
    private boolean authenticated = false;
    private boolean authenticating = false;
    private boolean loading = false;
    private boolean loadingErrors = false;
    private Date lastRefreshed;

    public static final String PROP_CONNECTED = "connected";
    public static final String PROP_AUTHENTICATED = "authenticated";
    public static final String PROP_AUTHENTICATING = "authenticating";
    public static final String PROP_LOADING = "loading";
    public static final String PROP_LOADING_ERRORS = "loadingErrors";
    public static final String PROP_LAST_REFRESHED = "lastRefreshed";


    public VcsPullRequestsModel(@NotNull Project project) {
        this.project = project;

        gitRepositoryProvider = new GitRepositoryProvider();
        serverContextProvider = new ServerContextProvider();

        treeModel = new PullRequestsTreeModel();
        treeDataProvider = new PullRequestsLookupListener(this);
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

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(final boolean connected) {
        if (this.connected != connected) {
            this.connected = connected;
            setChangedAndNotify(PROP_CONNECTED);
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(final boolean authenticated) {
        if (this.authenticated != authenticated) {
            this.authenticated = authenticated;
            setChangedAndNotify(PROP_AUTHENTICATED);
        }
    }

    public boolean isAuthenticating() {
        return authenticating;
    }

    public void setAuthenticating(final boolean authenticating) {
        if (this.authenticating != authenticating) {
            this.authenticating = authenticating;
            setChangedAndNotify(PROP_AUTHENTICATING);
        }
    }

    public boolean hasLoadingErrors() {
        return loadingErrors;
    }

    public void setLoadingErrors(final boolean loadingErrors) {
        if (this.loadingErrors != loadingErrors) {
            this.loadingErrors = loadingErrors;
            setChangedAndNotify(PROP_LOADING_ERRORS);
        }
    }

    public void setLastRefreshed(final Date lastRefreshed) {
        if (this.lastRefreshed != lastRefreshed) {
            this.lastRefreshed = lastRefreshed;
            setChangedAndNotify(PROP_LAST_REFRESHED);
        }
    }

    public Date getLastRefreshed() {
        return lastRefreshed;
    }

    public PullRequestsTreeModel getPullRequestsTreeModel() {
        return treeModel;
    }

    private boolean connectionSetup() {
        if (connected && gitRepository != null && authenticated && context != null) {
            logger.debug("connectionSetup: connection is good");
            return true;
        }

        gitRepository = gitRepositoryProvider.getGitRepository(project);
        if (gitRepository == null) {
            setConnected(false);
            logger.debug("connectionSetup: Failed to get Git repo for current project");
            return false;
        }
        setConnected(true);

        setAuthenticating(true);
        context = serverContextProvider.getAuthenticatedServerContext(project, gitRepository);
        setAuthenticating(false);

        if (context == null) {
            setAuthenticated(false);
            logger.debug("connectionSetup: failed to get authenticated context for current repo");
            return false;
        }
        setAuthenticated(true);

        //connection setup successfully
        return true;
    }

    public void loadPullRequests() {
        if (!connectionSetup()) {
            return;
        }

        clearPullRequests();
        treeDataProvider.loadPullRequests(context);
    }

    public void importIntoTeamServicesGit() {
        final ImportController controller = new ImportController(project);
        controller.showModalDialog();

        //TODO: how do we know this is done to load pull requests?
    }

    public void openGitRepoLink() {
        if (context != null && context.getGitRepository() != null) {
            if (StringUtils.isNotEmpty(context.getGitRepository().getRemoteUrl())) {
                BrowserUtil.browse(context.getGitRepository().getRemoteUrl()
                        .concat(UrlHelper.URL_SEPARATOR).concat("pullrequests"));
            }
        }
    }

    public void openSelectedPullRequestLink() {
        final GitPullRequest pullRequest = getSelectedPullRequest();
        if (pullRequest != null) {
            BrowserUtil.browse(getPullRequestWebLink(context.getGitRepository().getRemoteUrl(),
                    pullRequest.getPullRequestId()));
        }
    }

    private String getPullRequestWebLink(final String gitRemoteUrl, final int pullRequestId) {
        return gitRemoteUrl.concat(UrlHelper.URL_SEPARATOR)
                .concat("pullrequest").concat(UrlHelper.URL_SEPARATOR + pullRequestId);
    }

    /**
     * Gets the selected pull request and tries to set its state to ABANDONED
     * Runs on a background thread and notifies user upon completion
     */
    public void abandonSelectedPullRequest() {
        final Task.Backgroundable abandonPullRequestTask = new Task.Backgroundable(project, TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_TITLE)) {
            @Override
            public void run(final ProgressIndicator indicator) {
                final GitPullRequest pullRequest = getSelectedPullRequest();
                if (pullRequest != null) {
                    final String prLink = getPullRequestWebLink(context.getGitRepository().getRemoteUrl(),
                            pullRequest.getPullRequestId());
                    final int prId = pullRequest.getPullRequestId();

                    try {
                        final GitPullRequest pullRequestToUpdate = new GitPullRequest();
                        pullRequestToUpdate.setStatus(PullRequestStatus.ABANDONED);
                        final GitPullRequest pr = context.getGitHttpClient().updatePullRequest(pullRequestToUpdate,
                                pullRequest.getRepository().getId(), pullRequest.getPullRequestId());

                        if (pr != null && pr.getStatus() == PullRequestStatus.ABANDONED) {
                            //success
                            notifyOperationStatus(true,
                                    TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_ABANDON_SUCCEEDED, prLink, prId));
                        } else {
                            logger.warn("abandonSelectedPullRequest: pull request status not ABANDONED as expected. Actual status = {}",
                                    pr != null ? pr.getStatus() : "null");
                            notifyOperationStatus(false,
                                    TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_ABANDON_FAILED, prLink, prId));
                        }
                    } catch (Throwable t) {
                        logger.warn("abandonSelectedPullRequest: Unexpected exception", t);
                        notifyOperationStatus(false,
                                TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_ABANDON_FAILED, prLink, prId));
                    }
                } else {
                    //couldn't find selected pull request
                    notifyOperationStatus(false, TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_ABANDON_FAILED_UNEXPECTED));
                }
            }
        };
        abandonPullRequestTask.queue();
    }

    /**
     * Gets the selected pull request and tries to set its state to COMPLETED
     * Runs on a background thread and notifies user upon completion or status of merge
     */
    public void completeSelectedPullRequest() {
        final Task.Backgroundable completePullRequestTask = new Task.Backgroundable(project,
                TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_TITLE)) {
            @Override
            public void run(final ProgressIndicator indicator) {
                final GitPullRequest pullRequest = getSelectedPullRequest();
                if (pullRequest != null) {
                    final String prLink = getPullRequestWebLink(context.getGitRepository().getRemoteUrl(),
                            pullRequest.getPullRequestId());
                    final int prId = pullRequest.getPullRequestId();

                    try {
                        GitPullRequest pullRequestToUpdate = new GitPullRequest();
                        pullRequestToUpdate.setStatus(PullRequestStatus.COMPLETED);
                        pullRequestToUpdate.setLastMergeSourceCommit(pullRequest.getLastMergeSourceCommit());
                        final GitPullRequest pr = context.getGitHttpClient().updatePullRequest(pullRequestToUpdate,
                                pullRequest.getRepository().getId(), pullRequest.getPullRequestId());
                        if (pr != null) {
                            if (pr.getStatus() == PullRequestStatus.COMPLETED) {
                                //success
                                notifyOperationStatus(true,
                                        TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_COMPLETE_SUCCEEDED, prLink, prId));
                                return;
                            } else if (pr.getMergeStatus() == PullRequestAsyncStatus.QUEUED) {
                                // in progress
                                //success
                                notifyOperationStatus(true,
                                        TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_COMPLETE_MERGE_IN_PROGRESS, prLink, prId));
                                return;
                            } else if (pr.getMergeStatus() == PullRequestAsyncStatus.CONFLICTS ||
                                    pr.getMergeStatus() == PullRequestAsyncStatus.FAILURE ||
                                    pr.getMergeStatus() == PullRequestAsyncStatus.REJECTED_BY_POLICY) {
                                //merge failed
                                notifyOperationStatus(false,
                                        TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_COMPLETE_MERGE_FAILED, prLink, prId));
                                return;
                            }
                        }
                        //failed
                        notifyOperationStatus(false,
                                TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_COMPLETE_FAILED, prLink, prId));
                    } catch (Throwable t) {
                        logger.warn("abandonSelectedPullRequest: Unexpected exception", t);
                        notifyOperationStatus(false, TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_COMPLETE_FAILED, prLink, prId));
                    }
                } else {
                    //couldn't find selected pull request
                    notifyOperationStatus(false, TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_COMPLETE_FAILED_UNEXPECTED));
                }
            }
        };
        completePullRequestTask.queue();
    }

    private GitPullRequest getSelectedPullRequest() {
        if (context != null && context.getGitRepository() != null) {
            if (StringUtils.isNotEmpty(context.getGitRepository().getRemoteUrl())) {
                return treeModel.getSelectedPullRequest();
            }
        }
        return null;
    }

    private void notifyOperationStatus(final boolean success, final String message) {
        if (success) {
            VcsNotifier.getInstance(project).notifySuccess(
                    TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_TITLE), message, NotificationListener.URL_OPENING_LISTENER);
        } else {
            VcsNotifier.getInstance(project).notifyError(
                    TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_TITLE), message, NotificationListener.URL_OPENING_LISTENER);
        }
    }

    public void sendFeedback(final boolean smile) {
        final FeedbackAction action = new FeedbackAction(project, TfPluginBundle.KEY_VCS_PR_TITLE);
        action.sendFeedback(smile);
    }

    public void appendPullRequests(final List<GitPullRequest> pullRequests, final PullRequestLookupOperation.PullRequestScope scope) {
        treeModel.appendPullRequests(pullRequests, scope);
    }

    public void clearPullRequests() {
        treeModel.clearPullRequests();
    }

    public void createNewPullRequest() {
        if (!connectionSetup()) {
            return;
        }

        final CreatePullRequestController createPullRequestController = new CreatePullRequestController(project, gitRepository);
        createPullRequestController.showModalDialog();

        //TODO: how do we know this is done to refresh the tree?
    }

    public void dispose() {
        treeDataProvider.terminateActiveOperation();
    }

    static class GitRepositoryProvider {
        public GitRepository getGitRepository(@NotNull final Project project) {
            return TfGitHelper.getTfGitRepository(project);
        }
    }

    @VisibleForTesting
    void setGitRepositoryProvider(final GitRepositoryProvider gitRepositoryProvider) {
        this.gitRepositoryProvider = gitRepositoryProvider;
    }

    static class ServerContextProvider {
        public ServerContext getAuthenticatedServerContext(@Nullable final Project project, @NotNull final GitRepository gitRepository) {
            final String gitRemoteUrl = TfGitHelper.getTfGitRemote(gitRepository).getFirstUrl();
            final SettableFuture<ServerContext> future = SettableFuture.create();

            final Task.Backgroundable authenticationTask = new Task.Backgroundable(project,
                    TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_AUTHENTICATING),
                    false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    final ServerContext context = ServerContextManager.getInstance().getAuthenticatedContext(gitRemoteUrl, true);
                    future.set(context);
                }
            };
            authenticationTask.queue();

            // Don't wait any longer than 15 minutes for the user to authenticate
            try {
                return future.get(15, TimeUnit.MINUTES);
            } catch (Throwable t) {
                logger.warn("getAuthenticatedServerContext: Authentication not complete after waiting for 15 minutes", t);
            }
            return null;
        }
    }

    @VisibleForTesting
    void setServerContextProvider(final ServerContextProvider serverContextProvider) {
        this.serverContextProvider = serverContextProvider;
    }
}
