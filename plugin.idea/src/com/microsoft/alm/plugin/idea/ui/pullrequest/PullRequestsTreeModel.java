// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;

import javax.swing.tree.DefaultTreeModel;
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
        root.insert(requestedByMeRoot, 0);
        this.assignedToMeRoot = new PRTreeNode(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_ASSIGNED_TO_ME));
        root.insert(assignedToMeRoot, 1);
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
}
