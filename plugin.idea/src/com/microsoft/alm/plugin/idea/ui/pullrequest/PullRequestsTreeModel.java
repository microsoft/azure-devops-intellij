// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.icons.AllIcons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;

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
        if (scope == PullRequestLookupOperation.PullRequestScope.REQUESTED_BY_ME) {
            for (final GitPullRequest pullRequest : pullRequests) {
                requestedByMeRoot.add(new PRTreeNode(pullRequest));
            }
            reload(requestedByMeRoot);
        }

        if (scope == PullRequestLookupOperation.PullRequestScope.ASSIGNED_TO_ME) {
            for (final GitPullRequest pullRequest : pullRequests) {
                assignedToMeRoot.add(new PRTreeNode(pullRequest));
            }
            reload(assignedToMeRoot);
        }
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
                    setIcon(AllIcons.General.Bullet);
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
    }
}
