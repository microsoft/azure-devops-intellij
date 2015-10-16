// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.plugin.authentication.TfsAuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationInfo;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.ServerContextStore;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.WinHttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class used to manage ServerContext objects.
 */
public class ServerContextManager {

    private static final Logger logger = LoggerFactory.getLogger(ServerContextManager.class);

    private static final boolean NTLM_ENABLED;
    private ServerContext activeContext = null;

    static {
        final String propertyNtlmEnabled = System.getProperty("ntlmEnabled");
        NTLM_ENABLED = propertyNtlmEnabled == null || Boolean.parseBoolean(propertyNtlmEnabled);
    }

    public static boolean isNtlmEnabled() {
        return NTLM_ENABLED && WinHttpClients.isWinAuthAvailable();
    }

    private static class ServerContextManagerHolder {
        private static final ServerContextManager INSTANCE = new ServerContextManager();
    }

    public static ServerContextManager getInstance() {
        return ServerContextManagerHolder.INSTANCE;
    }

    public synchronized ServerContext getActiveContext() {
        if (activeContext == ServerContext.NO_CONTEXT && PluginServiceProvider.getInstance().isInsideIDE()) {
            //no context and we are running inside the IDE
            activeContext = getStore().loadServerContext(ServerContextStore.DEFAULT_CONTEXT);
        }
        return activeContext;
    }

    public synchronized ServerContext getActiveGitRepoContext(final String gitRepoUrl) {
        final ServerContext currentContext = getActiveContext();
        if (currentContext.getGitRepository() != null &&
                StringUtils.equalsIgnoreCase(currentContext.getGitRepository().getRemoteUrl(), gitRepoUrl)) {
            return currentContext;
        }
        return null;
    }

    public synchronized ServerContext<TfsAuthenticationInfo> getActiveTfsContext() {
        final ServerContext currentContext = getActiveContext();
        if (currentContext != ServerContext.NO_CONTEXT &&
                currentContext.getType() == ServerContext.Type.TFS &&
                currentContext.getAuthenticationInfo() instanceof TfsAuthenticationInfo) {
            return currentContext;
        }
        return null;
    }

    public synchronized ServerContext<VsoAuthenticationInfo> getActiveVsoContext() {
        final ServerContext currentContext = getActiveContext();
        if (ServerContext.NO_CONTEXT != currentContext && currentContext.getAuthenticationInfo() instanceof VsoAuthenticationInfo &&
                (ServerContext.Type.VSO_DEPLOYMENT == currentContext.getType() || ServerContext.Type.VSO == currentContext.getType())) {
            return currentContext;
        }
        return null;
    }

    public synchronized void setActiveContext(final ServerContext context) {
        activeContext = context;
        String key = getStore().getKey(context);
        getStore().saveServerContext(key, context);
    }

    public synchronized void clearServerContext(final ServerContext context) {
        String key = getStore().getKey(context);
        getStore().forgetServerContext(key);
        activeContext = ServerContext.NO_CONTEXT;
    }

    private ServerContextStore getStore() {
        return PluginServiceProvider.getInstance().getServerContextStore();
    }

}
