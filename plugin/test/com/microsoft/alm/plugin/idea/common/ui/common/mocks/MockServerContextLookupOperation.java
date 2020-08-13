// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common.mocks;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MockServerContextLookupOperation extends ServerContextLookupOperation {

    public class MockServerContextLookupResults extends ServerContextLookupResults {
        private final List<ServerContext> serverContexts = new ArrayList<ServerContext>();

        @Override
        public List<ServerContext> getServerContexts() {
            return Collections.unmodifiableList(serverContexts);
        }

        public MockServerContextLookupResults(List<ServerContext> contexts) {
            serverContexts.addAll(contexts);
        }
    }

    public MockServerContextLookupOperation(List<ServerContext> contextList, ContextScope resultScope) {
        super(contextList, resultScope);
    }

    @Override
    public void onLookupStarted() {
        super.onLookupStarted();
    }

    public void onLookupResults(List<ServerContext> results) {
        MockServerContextLookupResults lookupResults = new MockServerContextLookupResults(results);
        super.onLookupResults(lookupResults);
    }

    @Override
    public void doWork(Inputs inputs) {
        // Do nothing (all the results are supposed to be provided by the test, no need to run actual work).
    }

    @Override
    protected void onLookupResults(Results results) {
        super.onLookupResults(results);
    }

    @Override
    public void onLookupCompleted() {
        super.onLookupCompleted();
    }
}
