// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.microsoft.alm.plugin.idea.ui.common.tabs.TabLookupListenerImpl;
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
     * Load work item data based on the git url
     *
     * @param gitRemoteUrl
     */
    public void loadData(final String gitRemoteUrl, final Operation.Inputs inputs) {
        this.gitRemoteUrl = gitRemoteUrl;
        WorkItemLookupOperation activeOperation = new WorkItemLookupOperation(gitRemoteUrl);
        loadData(activeOperation, inputs);
    }
}
