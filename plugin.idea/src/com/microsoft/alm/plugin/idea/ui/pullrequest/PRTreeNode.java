// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequest;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Tree Node to represent a GitPullRequest, or one of the parent nodes
 */
public class PRTreeNode extends DefaultMutableTreeNode {
    private final String name;
    private final GitPullRequest gitPullRequest;

    public PRTreeNode(final GitPullRequest gitPullRequest) {
        super(gitPullRequest, false);
        this.gitPullRequest = gitPullRequest;
        this.name = gitPullRequest.getTitle();
    }

    public PRTreeNode(final String name) {
        super(name, true);
        this.name = name;
        this.gitPullRequest = null;
    }

    public String toString() {
        return name;
    }

    public GitPullRequest getGitPullRequest() {
        return gitPullRequest;
    }
}
