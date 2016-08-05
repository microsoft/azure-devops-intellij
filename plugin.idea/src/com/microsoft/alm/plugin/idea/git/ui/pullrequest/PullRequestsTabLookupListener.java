// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.pullrequest;

import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.idea.common.ui.common.tabs.TabLookupListenerImpl;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for pull request lookup operations
 */
public class PullRequestsTabLookupListener extends TabLookupListenerImpl {

    public PullRequestsTabLookupListener(@NotNull final VcsPullRequestsModel model) {
        super(model);
    }

    /**
     * Load PR data based on the git url
     *
     * @param repositoryContext
     */
    public void loadData(final RepositoryContext repositoryContext, Operation.Inputs inputs) {
        this.repositoryContext = repositoryContext;
        final PullRequestLookupOperation activeOperation = new PullRequestLookupOperation(repositoryContext.getUrl());
        loadData(activeOperation, inputs);
    }
}