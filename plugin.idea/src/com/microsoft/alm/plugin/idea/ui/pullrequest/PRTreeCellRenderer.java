// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;


import com.microsoft.alm.plugin.idea.resources.Icons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.IdentityRefWithVote;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.PullRequestAsyncStatus;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;

/**
 * Custom rendering of Pull Requests
 */
public class PRTreeCellRenderer extends DefaultTreeCellRenderer {
    private final static String GIT_REFS_HEADS = "refs/heads/";
    private final static short REVIEWER_VOTE_NO_RESPONSE = 0;
    private final static short REVIEWER_VOTE_APPROVED_WITH_SUGGESTIONS = 5;
    private final static short REVIEWER_VOTE_APPROVED = 10;
    private final static short REVIEWER_VOTE_WAITING_FOR_AUTHOR = -5;
    private final static short REVIEWER_VOTE_REJECTED = -10;

    private PRTreeNodeForm prViewForm;

    public PRTreeCellRenderer() {

    }

    @Override
    public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected,
                                                  final boolean expanded, final boolean leaf,
                                                  final int row, final boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        setIcon(null);
        if (value instanceof PRTreeNode && ((PRTreeNode) value).getGitPullRequest() != null) {
            final GitPullRequest pullRequest = ((PRTreeNode) value).getGitPullRequest();
            prViewForm = new PRTreeNodeForm(selected, hasFocus);
            prViewForm.setSummary(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_SUMMARY,
                    pullRequest.getCreatedBy().getDisplayName(),
                    pullRequest.getPullRequestId(),
                    pullRequest.getCreationDate().toString(),
                    pullRequest.getSourceRefName().replace(GIT_REFS_HEADS, ""),
                    pullRequest.getTargetRefName().replace(GIT_REFS_HEADS, "")));
            setStatus(prViewForm, pullRequest);

            final Component component = prViewForm.getPanel();
            return component;
        }
        return this;
    }

    //TODO: create a model for the rendering view logic
    private void setStatus(final PRTreeNodeForm prView, final GitPullRequest pullRequest) {
        //first check merge status, if there are conflicts, show that to the user
        final PullRequestAsyncStatus mergeStatus = pullRequest.getMergeStatus();
        if (mergeStatus == PullRequestAsyncStatus.CONFLICTS ||
                mergeStatus == PullRequestAsyncStatus.FAILURE ||
                mergeStatus == PullRequestAsyncStatus.REJECTED_BY_POLICY) {
            //merge failed
            prView.setStatus(pullRequest.getTitle(),
                    TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_MERGE_FAILED),
                    Icons.PR_STATUS_FAILED);
            return;
        }

        //no merge errors, so look at reviewer response
        short lowestVote = 0;
        short highestVote = 0;

        if (pullRequest.getReviewers() != null && pullRequest.getReviewers().length > 0) {
            for (final IdentityRefWithVote reviewer : pullRequest.getReviewers()) {
                final short vote = reviewer.getVote();
                if (vote < lowestVote) {
                    lowestVote = vote;
                }
                if (vote > highestVote) {
                    highestVote = vote;
                }
            }
        }

        short finalVote = REVIEWER_VOTE_NO_RESPONSE;
        if (lowestVote < REVIEWER_VOTE_NO_RESPONSE) {
            //use negative comments if any
            finalVote = lowestVote;
        } else if (highestVote > REVIEWER_VOTE_NO_RESPONSE) {
            //use positive comments
            finalVote = highestVote;
        }

        //set message and icon based on aggregate reviewers response
        String voteString = TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REVIEWER_NO_RESPONSE);
        Icon voteIcon = Icons.PR_STATUS_NO_RESPONSE;
        if (finalVote == REVIEWER_VOTE_APPROVED_WITH_SUGGESTIONS) {
            voteString = TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REVIEWER_APPROVED_SUGGESTIONS);
            voteIcon = Icons.PR_STATUS_SUCCEEDED;
        }
        if (finalVote == REVIEWER_VOTE_APPROVED) {
            voteString = TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REVIEWER_APPROVED);
            voteIcon = Icons.PR_STATUS_SUCCEEDED;
        }
        if (finalVote == REVIEWER_VOTE_WAITING_FOR_AUTHOR) {
            voteString = TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REVIEWER_WAITING);
            voteIcon = Icons.PR_STATUS_WAITING;
        }
        if (finalVote == REVIEWER_VOTE_REJECTED) {
            voteString = TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REVIEWER_REJECTED);
            voteIcon = Icons.PR_STATUS_FAILED;
        }

        prView.setStatus(pullRequest.getTitle(), voteString, voteIcon);
    }
}
