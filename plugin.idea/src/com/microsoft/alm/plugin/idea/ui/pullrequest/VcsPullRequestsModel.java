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
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.ui.vcsimport.ImportController;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
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

    private VcsTabStatus tabStatus = VcsTabStatus.NOT_TF_GIT_REPO;

    public static final String PROP_PR_TAB_STATUS = "prTabStatus";


    public VcsPullRequestsModel(@NotNull Project project) {
        this.project = project;

        treeModel = new PullRequestsTreeModel();
        treeDataProvider = new PullRequestsLookupListener(this);
    }

    public VcsTabStatus getTabStatus() {
        return tabStatus;
    }

    public void setTabStatus(final VcsTabStatus status) {
        if (this.tabStatus != status) {
            this.tabStatus = status;
            setChangedAndNotify(PROP_PR_TAB_STATUS);
        }
    }

    public PullRequestsTreeModel getPullRequestsTreeModel() {
        return treeModel;
    }

    private boolean isTfGitRepository() {
        gitRepository = TfGitHelper.getTfGitRepository(project);
        if (gitRepository == null) {
            setTabStatus(VcsTabStatus.NOT_TF_GIT_REPO);
            logger.debug("isTfGitRepository: Failed to get Git repo for current project");
            return false;
        } else {
            return true;
        }
    }

    public void loadPullRequests() {
        if (isTfGitRepository()) {
            clearPullRequests();
            treeDataProvider.loadPullRequests(TfGitHelper.getTfGitRemote(gitRepository).getFirstUrl());
        }
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
        if (!isTfGitRepository()) {
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
