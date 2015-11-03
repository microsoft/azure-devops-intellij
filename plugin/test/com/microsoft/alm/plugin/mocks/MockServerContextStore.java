// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.mocks;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.services.ServerContextStore;

import java.util.Collections;
import java.util.List;

public class MockServerContextStore implements ServerContextStore {
    private ServerContext context = new ServerContextBuilder().type(ServerContext.Type.VSO_DEPLOYMENT).build();

    @Override
    public void forgetServerContext(Key key) {
        context = null;
    }

    @Override
    public List<ServerContext> restoreServerContexts() {
        return Collections.singletonList(context);
    }

    @Override
    public void saveServerContext(ServerContext context) {
        this.context = context;
    }
}
