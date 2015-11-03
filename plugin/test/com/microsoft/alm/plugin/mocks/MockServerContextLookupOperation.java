// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.mocks;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;

import java.util.List;

public class MockServerContextLookupOperation extends ServerContextLookupOperation {
    public MockServerContextLookupOperation(List<ServerContext> contextList, ContextScope resultScope) {
        super(contextList, resultScope);
    }

    //@Override
    protected void doLookup(ServerContext context, List<TeamProjectCollectionReference> collections) {
        // create server contexts
        // trigger events and return results
        //lookup.addResults();

    }
}
