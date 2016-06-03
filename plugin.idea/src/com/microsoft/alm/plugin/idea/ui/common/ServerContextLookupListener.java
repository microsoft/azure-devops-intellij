// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.TeamServicesException;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ServerContextLookupListener implements Operation.Listener {
    private static final Logger logger = LoggerFactory.getLogger(ServerContextLookupListener.class);

    private final ServerContextLookupPageModel pageModel;
    private ServerContextLookupOperation activeOperation;

    public ServerContextLookupListener(final ServerContextLookupPageModel pageModel) {
        ArgumentHelper.checkNotNull(pageModel, "pageModel");
        this.pageModel = pageModel;
    }

    public void loadContexts(final List<ServerContext> contexts, final ServerContextLookupOperation.ContextScope resultScope) {
        logger.info(String.format("loadContexts activeOperation with %s contexts and a scope of %s",
                contexts != null ? contexts.size() : "n/a", resultScope != null ? resultScope.name() : "n/a"));
        ServerContextLookupOperation activeOperation = new ServerContextLookupOperation(contexts, resultScope);
        loadContexts(activeOperation);
    }

    public void loadContexts(final ServerContextLookupOperation activeOperation) {
        logger.info(String.format("loadContexts terminateActiveOperation in state %s", activeOperation.getState().name()));
        ArgumentHelper.checkNotNull(activeOperation, "activeOperation");

        terminateActiveOperation();
        this.activeOperation = activeOperation;
        this.activeOperation.addListener(this);
        this.activeOperation.doWorkAsync(Operation.EMPTY_INPUTS);
    }

    @Override
    public void notifyLookupStarted() {
        logger.info("ServerContext lookup started");
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
        logger.info("ServerContext lookup completed");
        operationDone();
        IdeaHelper.runOnUIThread(new Runnable() {
            public void run() {
                pageModel.setLoading(false);
            }
        });
    }

    @Override
    public void notifyLookupResults(final Operation.Results results) {
        logger.info("ServerContext lookup results has error: " + results.hasError());
        final ServerContextLookupOperation.ServerContextLookupResults lookupResults = (ServerContextLookupOperation.ServerContextLookupResults) results;
        if (lookupResults.isCancelled()) {
            operationDone();
            IdeaHelper.runOnUIThread(new Runnable() {
                public void run() {
                    pageModel.addError(ModelValidationInfo.createWithResource(TfPluginBundle.KEY_OPERATION_LOOKUP_CANCELED));
                    pageModel.setLoading(false);
                }
            });
        } else {
            IdeaHelper.runOnUIThread(new Runnable() {
                public void run() {
                    pageModel.appendContexts(lookupResults.getServerContexts());

                    if (lookupResults.hasError()) {
                        if (lookupResults.getError() instanceof TeamServicesException) {
                            pageModel.addError(ModelValidationInfo.createWithMessage(
                                    LocalizationServiceImpl.getInstance().getExceptionMessage(results.getError())));
                        } else {
                            pageModel.addError(ModelValidationInfo.createWithResource(TfPluginBundle.KEY_OPERATION_LOOKUP_ERRORS));
                        }
                    }
                }
            });
        }
    }

    private void operationDone() {
        logger.info("ServerContext operation done");
        if (activeOperation != null) {
            activeOperation.removeListener(this);
            activeOperation = null;
        }
    }

    public void terminateActiveOperation() {
        logger.info("ServerContext operation terminated");
        if (activeOperation != null) {
            activeOperation.removeListener(this);
            activeOperation.cancel();
            activeOperation = null;
        }
    }
}
