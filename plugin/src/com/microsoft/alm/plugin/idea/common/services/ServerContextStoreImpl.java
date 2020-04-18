// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.services;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.settings.TeamServicesSecrets;
import com.microsoft.alm.plugin.idea.common.settings.TeamServicesSettingsService;
import com.microsoft.alm.plugin.services.ServerContextStore;

import java.util.List;

/**
 * This implementation of ServerContextStore simply wraps calls to the IntelliJ plugin class implementation that
 * saves our Server information.
 */
public class ServerContextStoreImpl implements ServerContextStore {
    @Override
    public void forgetServerContext(final String key) {
        TeamServicesSecrets.forget(key);
    }

    @Override
    public List<ServerContext> restoreServerContexts() {
        return TeamServicesSettingsService.getInstance().restoreServerContexts();
    }

    @Override
    public void saveServerContext(final ServerContext context) {
        TeamServicesSecrets.getInstance().save(context);
    }
}
