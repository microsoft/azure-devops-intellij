// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;

import java.util.List;

public class ServerContextLookupListener implements ServerContextLookupOperation.Listener {

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
        this.activeOperation.lookupContextsAsync();
    }

    @Override
    public void notifyLookupStarted() {
        runOnUIThread(new Runnable() {
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
        runOnUIThread(new Runnable() {
            public void run() {
                pageModel.setLoading(false);
            }
        });
    }

    @Override
    public void notifyLookupCanceled() {
        operationDone();
        runOnUIThread(new Runnable() {
            public void run() {
                pageModel.addError(ModelValidationInfo.createWithResource(TfPluginBundle.KEY_OPERATION_ERRORS_LOOKUP_CANCELED));
                pageModel.setLoading(false);
            }
        });
    }

    @Override
    public void notifyLookupResults(final List<ServerContext> serverContexts) {
        runOnUIThread(new Runnable() {
            public void run() {
                pageModel.appendContexts(serverContexts);
            }
        });
    }

    private void runOnUIThread(final Runnable runnable) {
        if (ApplicationManager.getApplication() != null) {
            ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
        } else {
            // If we don't have an application then we are testing, just run the runnable here
            runnable.run();
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
