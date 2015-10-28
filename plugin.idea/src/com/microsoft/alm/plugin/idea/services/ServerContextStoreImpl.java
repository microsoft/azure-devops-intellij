// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.services;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.utils.ServerContextSettings;
import com.microsoft.alm.plugin.services.ServerContextStore;

import java.util.List;

/**
 * This implementation of ServerContextStore simply wraps calls to the IntelliJ plugin class implementation that
 * saves our Server information.
 */
public class ServerContextStoreImpl implements ServerContextStore {
    @Override
    public void forgetServerContext(final Key key) {
        ServerContextSettings.getInstance().forgetServerContextSecrets(key);
    }

    @Override
    public List<ServerContext> restoreServerContexts() {
        return ServerContextSettings.getInstance().restoreServerContexts();
    }

    @Override
    public void saveServerContext(final ServerContext context) {
        ServerContextSettings.getInstance().saveServerContextSecrets(context);
    }
}
