// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.workitem;

import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.idea.common.ui.common.tabs.TabLookupListenerImpl;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.WorkItemLookupOperation;

/**
 * Listener for work item lookup operations
 */
public class WorkItemsTabLookupListener extends TabLookupListenerImpl {

    public WorkItemsTabLookupListener(final VcsWorkItemsModel model) {
        super(model);
    }

    /**
     * Load work item data based on the repository context
     *
     * @param repositoryContext
     */
    public void loadData(final RepositoryContext repositoryContext, final Operation.Inputs inputs) {
        this.repositoryContext = repositoryContext;
        WorkItemLookupOperation activeOperation = new WorkItemLookupOperation(repositoryContext);
        loadData(activeOperation, inputs);
    }
}
