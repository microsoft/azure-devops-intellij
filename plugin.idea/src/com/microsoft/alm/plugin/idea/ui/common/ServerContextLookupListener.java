// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;

import java.util.List;

public class ServerContextLookupListener implements Operation.Listener {

    private final ServerContextLookupPageModel pageModel;
    private ServerContextLookupOperation activeOperation;

    public ServerContextLookupListener(final ServerContextLookupPageModel pageModel) {
        assert pageModel != null;
        this.pageModel = pageModel;
    }

    public void loadContexts(final List<ServerContext> contexts, final ServerContextLookupOperation.ContextScope resultScope) {
        ServerContextLookupOperation activeOperation = new ServerContextLookupOperation(contexts, resultScope);
        loadContexts(activeOperation);
    }

    public void loadContexts(final ServerContextLookupOperation activeOperation) {
        assert activeOperation != null;

        terminateActiveOperation();
        this.activeOperation = activeOperation;
        this.activeOperation.addListener(this);
        this.activeOperation.doWorkAsync(Operation.EMPTY_INPUTS);
    }

    @Override
    public void notifyLookupStarted() {
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                pageModel.setLoading(true);
                pageModel.clearContexts();
            }
        });
    }

    @Override
    public void notifyLookupCompleted() {
        operationDone();
        IdeaHelper.runOnUIThread(new Runnable() {
            public void run() {
                pageModel.setLoading(false);
            }
        });
    }

    @Override
    public void notifyLookupResults(final Operation.Results results) {
        final ServerContextLookupOperation.ServerContextLookupResults lookupResults = (ServerContextLookupOperation.ServerContextLookupResults) results;
        if (lookupResults.isCancelled()) {
            operationDone();
            IdeaHelper.runOnUIThread(new Runnable() {
                public void run() {
                    pageModel.addError(ModelValidationInfo.createWithResource(TfPluginBundle.KEY_OPERATION_ERRORS_LOOKUP_CANCELED));
                    pageModel.setLoading(false);
                }
            });
        } else {
            IdeaHelper.runOnUIThread(new Runnable() {
                public void run() {
                    if (lookupResults.hasError()) {
                        pageModel.addError(ModelValidationInfo.createWithMessage(
                                LocalizationServiceImpl.getInstance().getServerExceptionMessage(lookupResults.getError().getMessage())));
                    }
                    pageModel.appendContexts(lookupResults.getServerContexts());
                }
            });
        }
    }

    private void operationDone() {
        activeOperation.removeListener(this);
        activeOperation = null;
    }

    public void terminateActiveOperation() {
        if (activeOperation != null) {
            activeOperation.removeListener(this);
            activeOperation.cancel();
            activeOperation = null;
        }
    }
}
