// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.pullrequest;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.resources.Icons;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.tabs.TabModelImpl;
import com.microsoft.alm.plugin.idea.common.utils.EventContextHelper;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.alm.sourcecontrol.webapi.model.PullRequestStatus;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VcsPullRequestsModel extends TabModelImpl<PullRequestsTreeModel> {
    private static final Logger logger = LoggerFactory.getLogger(VcsPullRequestsModel.class);
    private final GitRepository gitRepository;

    public VcsPullRequestsModel(@NotNull Project project) {
        super(project, new PullRequestsTreeModel(), "PullRequestsTab.");
        operationInputs = new Operation.CredInputsImpl();
        gitRepository = VcsHelper.getGitRepository(project);
    }

    protected void createDataProvider() {
        dataProvider = new PullRequestsTabLookupListener(this);
    }

    private boolean isTfGitRepository() {
        return isTeamServicesRepository() && gitRepository != null;
    }
    
    public void openGitRepoLink() {
        if (isTfGitRepository()) {
            final ServerContext context = TfGitHelper.getSavedServerContext(gitRepository);
            if (context != null && context.getGitRepository() != null) {
                if (StringUtils.isNotEmpty(context.getGitRepository().getRemoteUrl())) {
                    BrowserUtil.browse(context.getGitRepository().getRemoteUrl()
                            .concat(UrlHelper.URL_SEPARATOR).concat("pullrequests"));
                }
            }
        }
    }

    public void openSelectedItemsLink() {
        if (isTfGitRepository()) {
            final ServerContext context = TfGitHelper.getSavedServerContext(gitRepository);
            final GitPullRequest pullRequest = getSelectedPullRequest();
            if (context != null && pullRequest != null) {
                BrowserUtil.browse(getPullRequestWebLink(context.getGitRepository().getRemoteUrl(),
                        pullRequest.getPullRequestId()));
            }
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
        if (isTfGitRepository()) {
            final ServerContext context = TfGitHelper.getSavedServerContext(gitRepository);

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
                        //no pull request selected
                        notifyOperationStatus(false, TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_ABANDON_FAILED_NO_SELECTION));
                    }
                }
            };

            if (context != null) {
                abandonPullRequestTask.queue();
            }
        }
    }

    private GitPullRequest getSelectedPullRequest() {
        if (isTfGitRepository()) {
            final ServerContext context = TfGitHelper.getSavedServerContext(gitRepository);

            if (context != null && context.getGitRepository() != null) {
                if (StringUtils.isNotEmpty(context.getGitRepository().getRemoteUrl())) {
                    return viewForModel.getSelectedPullRequest();
                }
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

        // Update the PR tab and any other UI that is listening for PR Changed events (even on failure updating the tab is a good idea)
        EventContextHelper.triggerPullRequestChanged(EventContextHelper.SENDER_ABANDON_PULL_REQUEST, project);
    }

    public void appendData(final Operation.Results results) {
        final PullRequestLookupOperation.PullRequestLookupResults lookupResults = (PullRequestLookupOperation.PullRequestLookupResults) results;
        viewForModel.appendPullRequests(lookupResults.getPullRequests(), lookupResults.getScope());
    }

    public void clearData() {
        viewForModel.clearPullRequests();
    }

    public void createNewItem() {
        if (!isTeamServicesRepository() || gitRepository == null) {
            return;
        }

        final CreatePullRequestController createPullRequestController = new CreatePullRequestController(project, gitRepository);
        createPullRequestController.showModalDialog();

        //TODO: how do we know this is done to refresh the tree?
    }
}
