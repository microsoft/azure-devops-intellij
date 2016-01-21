// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.microsoft.alm.plugin.idea.resources.Icons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.IdentityRefWithVote;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.PullRequestAsyncStatus;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.Component;
import java.util.List;

public class PullRequestsTreeModel extends DefaultTreeModel {
    private final PRTreeNode root;
    private final PRTreeNode requestedByMeRoot;
    private final PRTreeNode assignedToMeRoot;

    public PullRequestsTreeModel() {
        super(null);

        this.root = new PRTreeNode(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_TITLE));
        setRoot(root);
        this.requestedByMeRoot = new PRTreeNode(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REQUESTED_BY_ME));
        root.add(requestedByMeRoot);
        this.assignedToMeRoot = new PRTreeNode(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_ASSIGNED_TO_ME));
        root.add(assignedToMeRoot);
    }

    public PRTreeNode getRequestedByMeRoot() {
        return requestedByMeRoot;
    }

    public PRTreeNode getAssignedToMeRoot() {
        return assignedToMeRoot;
    }

    public void appendPullRequests(final List<GitPullRequest> pullRequests, final PullRequestLookupOperation.PullRequestScope scope) {
        final PRTreeNode rootNode = scope == PullRequestLookupOperation.PullRequestScope.REQUESTED_BY_ME ? requestedByMeRoot : assignedToMeRoot;
        for (final GitPullRequest pullRequest : pullRequests) {
            rootNode.add(new PRTreeNode(pullRequest));
        }
        reload(rootNode);
    }

    public void clearPullRequests() {
        requestedByMeRoot.removeAllChildren();
        reload(requestedByMeRoot);
        assignedToMeRoot.removeAllChildren();
        reload(assignedToMeRoot);
    }

    /**
     * Tree Node to represent a GitPullRequest, or one of the parent nodes
     */
    public class PRTreeNode extends DefaultMutableTreeNode {
        private final String name;
        private final GitPullRequest gitPullRequest;

        public PRTreeNode(final GitPullRequest gitPullRequest) {
            super(gitPullRequest, false);
            this.gitPullRequest = gitPullRequest;
            this.name = gitPullRequest.getPullRequestId() + ": " + gitPullRequest.getTitle();
        }

        public PRTreeNode(final String name) {
            super(name, true);
            this.name = name;
            this.gitPullRequest = null;
        }

        public String toString() {
            return name;
        }
    }

    /**
     * Custom rendering of Pull Requests
     */
    public static class PRTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof PRTreeNode) {
                final GitPullRequest pullRequest = ((PRTreeNode) value).gitPullRequest;
                if (pullRequest != null) {
                    final PRTreeNodeForm prView = new PRTreeNodeForm();
                    prView.setPRTitle("#" + pullRequest.getPullRequestId() + " : " + pullRequest.getTitle());
                    prView.setPRBranches(pullRequest.getSourceRefName().replace(GIT_REFS_HEADS, ""),
                            pullRequest.getTargetRefName().replace(GIT_REFS_HEADS, ""));
                    setStatus(prView, pullRequest);
                    final Component component = prView.getPanel();
                    component.setBackground(JBColor.WHITE);
                    return component;
                } else {
                    if (expanded) {
                        setIcon(AllIcons.General.ComboArrowDown);
                    } else {
                        setIcon(AllIcons.General.ComboArrowRight);
                    }
                }
            }
            return this;
        }

        private void setStatus(final PRTreeNodeForm prView, final GitPullRequest pullRequest) {
            //first check merge status, if there are conflicts, show that to the user
            final PullRequestAsyncStatus mergeStatus = pullRequest.getMergeStatus();
            if (mergeStatus == PullRequestAsyncStatus.CONFLICTS ||
                    mergeStatus == PullRequestAsyncStatus.FAILURE ||
                    mergeStatus == PullRequestAsyncStatus.REJECTED_BY_POLICY) {
                //merge failed
                prView.setStatus(
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

            prView.setStatus(voteString, voteIcon);
        }

        private final static String GIT_REFS_HEADS = "refs/heads/";
        private final static short REVIEWER_VOTE_NO_RESPONSE = 0;
        private final static short REVIEWER_VOTE_APPROVED_WITH_SUGGESTIONS = 5;
        private final static short REVIEWER_VOTE_APPROVED = 10;
        private final static short REVIEWER_VOTE_WAITING_FOR_AUTHOR = -5;
        private final static short REVIEWER_VOTE_REJECTED = -10;
        //using values greater that current largest and lowest
        private final static short REVIEWER_VOTE_MOST_NEGATIVE = -10;
        private final static short REVIEWER_VOTE_MOST_POSITIVE = 10;

    }
}
