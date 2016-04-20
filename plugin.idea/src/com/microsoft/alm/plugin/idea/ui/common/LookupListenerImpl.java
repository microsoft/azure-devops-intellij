// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabModel;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.operations.Operation;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for lookup operations
 */
public abstract class LookupListenerImpl implements Operation.Listener {

    private final TabModel model;
    private final Operation.Inputs inputs;
    private Operation activeOperation;
    protected String gitRemoteUrl;

    public LookupListenerImpl(@NotNull final TabModel model, final Operation.Inputs inputs) {
        assert model != null;
        this.model = model;
        this.inputs = inputs;
    }

    /**
     * Load data based on the git url
     *
     * @param gitRemoteUrl
     */
    public abstract void loadData(final String gitRemoteUrl);

    /**
     * Load data asynchronously based on the given operation
     *
     * @param activeOperation
     */
    protected void loadData(final Operation activeOperation) {
        assert activeOperation != null;
        this.activeOperation = activeOperation;
        this.activeOperation.addListener(this);
        this.activeOperation.doWorkAsync(inputs);
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
        if (results.isCancelled()) {
            operationDone();
            IdeaHelper.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    model.setTabStatus(VcsTabStatus.LOADING_COMPLETED);
                }
            });
        } else if (results.hasError()) {
            final ServerContext newContext;
            if (AuthHelper.isNotAuthorizedError(results.getError())) {
                newContext = ServerContextManager.getInstance().updateAuthenticationInfo(gitRemoteUrl); //call this on a background thread, will hang UI thread if not
            } else {
                newContext = null;
            }
            IdeaHelper.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if (AuthHelper.isNotAuthorizedError(results.getError())) {
                        if (newContext != null) {
                            //try reloading the data with new context and authentication info
                            model.loadData();
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
            IdeaHelper.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    model.appendData(results);
                }
            });
        }
    }

    protected void operationDone() {
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