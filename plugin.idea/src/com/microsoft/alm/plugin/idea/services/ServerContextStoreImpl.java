// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.services;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.utils.ServerContextSettings;
import com.microsoft.alm.plugin.services.ServerContextStore;

/**
 * This implementation of ServerContextStore simply wraps calls to the IntelliJ plugin class implemenation that
 * saves our Server information.
 */
public class ServerContextStoreImpl implements ServerContextStore {
    @Override
    public void forgetServerContext(final String key) {
        ServerContextSettings.getInstance().forgetServerContext(key);
    }

    @Override
    public String getKey(final ServerContext context) {
        return ServerContextSettings.getKey(context);
    }

    @Override
    public ServerContext loadServerContext(final String key) {
        return ServerContextSettings.getInstance().getServerContext(key);
    }

    @Override
    public void saveServerContext(final String key, final ServerContext context) {
        ServerContextSettings.getInstance().saveServerContext(key, context);
    }
}
