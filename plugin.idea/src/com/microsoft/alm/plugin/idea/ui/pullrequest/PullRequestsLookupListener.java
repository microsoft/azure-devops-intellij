// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.microsoft.alm.plugin.idea.ui.common.LookupListenerImpl;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for pull request lookup operations
 */
public class PullRequestsLookupListener extends LookupListenerImpl {

    public PullRequestsLookupListener(@NotNull final VcsPullRequestsModel model) {
        super(model, Operation.EMPTY_INPUTS);
    }

    /**
     * Load PR data based on the git url
     *
     * @param gitRemoteUrl
     */
    public void loadData(final String gitRemoteUrl) {
        this.gitRemoteUrl = gitRemoteUrl;
        final PullRequestLookupOperation activeOperation = new PullRequestLookupOperation(gitRemoteUrl);
        loadData(activeOperation);
    }
}