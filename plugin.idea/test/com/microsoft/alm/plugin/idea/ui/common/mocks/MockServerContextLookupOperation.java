// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.mocks;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;

import java.util.List;

public class MockServerContextLookupOperation extends ServerContextLookupOperation {

    public MockServerContextLookupOperation(List<ServerContext> contextList, ContextScope resultScope) {
        super(contextList, resultScope);
    }

    @Override
    public void lookupContextsAsync() {
        // Do nothing
    }

    @Override
    public void onLookupStarted() {
        super.onLookupStarted();
    }

    @Override
    public void onLookupResults(List<ServerContext> results) {
        super.onLookupResults(results);
    }

    @Override
    public void onLookupCompleted() {
        super.onLookupCompleted();
    }

    @Override
    public void onLookupCanceled() {
        super.onLookupCanceled();
    }
}
