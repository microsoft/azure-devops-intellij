// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.WorkItemLookupOperation;

public class WorkItemsLookupListener implements Operation.Listener {

    private final VcsWorkItemsModel model;
    private final WorkItemsTableModel tableModel;
    private WorkItemLookupOperation activeOperation;
    private String gitRemoteUrl;

    public WorkItemsLookupListener(final VcsWorkItemsModel model, final WorkItemsTableModel tableModel) {
        assert model != null;
        this.model = model;
        this.tableModel = tableModel;
    }

    public void loadWorkItems(final String gitRemoteUrl) {
        this.gitRemoteUrl = gitRemoteUrl;
        WorkItemLookupOperation activeOperation = new WorkItemLookupOperation(gitRemoteUrl);
        loadWorkItems(activeOperation);
    }

    private void loadWorkItems(final WorkItemLookupOperation activeOperation) {
        assert activeOperation != null;
        this.activeOperation = activeOperation;
        this.activeOperation.addListener(this);
        this.activeOperation.doWorkAsync(new WorkItemLookupOperation.WitInputs(
                WorkItemHelper.getAssignedToMeQuery()));
    }

    @Override
    public void notifyLookupStarted() {
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                model.setTabStatus(VcsTabStatus.LOADING_IN_PROGRESS);
            }
        });
    }

    @Override
    public void notifyLookupCompleted() {
        operationDone();
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                //set status to complete if it is still in-progress and not updated by notifyLookupResults
                if (model.getTabStatus() == VcsTabStatus.LOADING_IN_PROGRESS) {
                    model.setTabStatus(VcsTabStatus.LOADING_COMPLETED);
                }
            }
        });
    }

    @Override
    public void notifyLookupResults(final Operation.Results results) {
        final WorkItemLookupOperation.WitResults witResults = (WorkItemLookupOperation.WitResults) results;

        if (witResults.isCancelled()) {
            operationDone();
            IdeaHelper.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    model.setTabStatus(VcsTabStatus.LOADING_COMPLETED);
                }
            });
        } else if (witResults.hasError()) {
            IdeaHelper.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if (AuthHelper.isNotAuthorizedError(witResults.getError())) {
                        final ServerContext newContext = ServerContextManager.getInstance().updateAuthenticationInfo(gitRemoteUrl);
                        if (newContext != null) {
                            //try reloading the work items with new context and authentication info
                            model.loadWorkItems();
                        } else {
                            //user cancelled login, don't retry
                            model.setTabStatus(VcsTabStatus.NO_AUTH_INFO);
                        }
                    } else {
                        model.setTabStatus(VcsTabStatus.LOADING_COMPLETED_ERRORS);
                    }
                }
            });
        } else {
            // Update table model on UI thread
            IdeaHelper.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    tableModel.addWorkItems(witResults.getWorkItems());
                }
            });
        }
    }

    private void operationDone() {
        if (activeOperation != null) {
            activeOperation.removeListener(this);
            activeOperation = null;
        }
    }

    public void terminateActiveOperation() {
        if (activeOperation != null) {
            activeOperation.removeListener(this);
            activeOperation.cancel();
            activeOperation = null;
        }
    }
}
