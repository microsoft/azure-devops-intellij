// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.resources.Icons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.ui.vcsimport.ImportController;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.utils.Providers;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.PullRequestStatus;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VcsPullRequestsModel extends AbstractModel {
    private static final Logger logger = LoggerFactory.getLogger(VcsPullRequestsModel.class);

    private final Project project;

    private final PullRequestsTreeModel treeModel;
    private final PullRequestsLookupListener treeDataProvider;

    private GitRepository gitRepository;
    private ServerContext context;

    private boolean connected = false;
    private boolean authenticated = false;
    private boolean authenticating = false;
    private boolean loading = false;
    private boolean loadingErrors = false;

    public static final String PROP_CONNECTED = "connected";
    public static final String PROP_AUTHENTICATED = "authenticated";
    public static final String PROP_AUTHENTICATING = "authenticating";
    public static final String PROP_LOADING = "loading";
    public static final String PROP_LOADING_ERRORS = "loadingErrors";


    public VcsPullRequestsModel(@NotNull Project project) {
        this.project = project;

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

    public PullRequestsTreeModel getPullRequestsTreeModel() {
        return treeModel;
    }

    private boolean connectionSetup() {
        //always load latest saved context and repo information since it might be changed outside of pull requests tab
        gitRepository = new Providers.GitRepositoryProvider().getGitRepository(project);

        setAuthenticating(true);
        context = new Providers.ServerContextProvider().getAuthenticatedServerContext(project, gitRepository);
        setAuthenticating(false);

        if (connected && gitRepository != null && authenticated && context != null) {
            logger.debug("connectionSetup: connection is good");
            return true;
        }

        if (gitRepository == null) {
            setConnected(false);
            logger.debug("connectionSetup: Failed to get Git repo for current project");
            return false;
        }
        setConnected(true);

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

    public void loadPullRequests(final ServerContext context) {
        this.context = context;
        this.authenticated = true;
        loadPullRequests();
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
            boolean abandonPR;

            @Override
            public void run(final ProgressIndicator indicator) {
                final GitPullRequest pullRequest = getSelectedPullRequest();
                if (pullRequest != null) {
                    final String prLink = getPullRequestWebLink(context.getGitRepository().getRemoteUrl(),
                            pullRequest.getPullRequestId());
                    final int prId = pullRequest.getPullRequestId();

                    //prompt user for confirmation
                    IdeaHelper.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            abandonPR = IdeaHelper.showConfirmationDialog(project,
                                    TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_ABANDON_CONFIRMATION, prId),
                                    TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_TITLE),
                                    Icons.VSLogo, Messages.YES_BUTTON, Messages.NO_BUTTON);
                        }
                    }, true, indicator.getModalityState());

                    if (abandonPR) {
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
                    }
                } else {
                    //couldn't find selected pull request
                    notifyOperationStatus(false, TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_ABANDON_FAILED_UNEXPECTED));
                }
            }
        };

        abandonPullRequestTask.queue();
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
}
