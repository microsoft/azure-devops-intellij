// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.microsoft.alm.plugin.idea.ui.common.LookupListenerImpl;
import com.microsoft.alm.plugin.operations.WorkItemLookupOperation;

/**
 * Listener for work item lookup operations
 */
public class WorkItemsLookupListener extends LookupListenerImpl {

    public WorkItemsLookupListener(final VcsWorkItemsModel model) {
        super(model, new WorkItemLookupOperation.WitInputs(
                WorkItemHelper.getAssignedToMeQuery()));
    }

    /**
     * Load work item data based on the git url
     *
     * @param gitRemoteUrl
     */
    public void loadData(final String gitRemoteUrl) {
        this.gitRemoteUrl = gitRemoteUrl;
        WorkItemLookupOperation activeOperation = new WorkItemLookupOperation(gitRemoteUrl);
        loadData(activeOperation);
    }
}
