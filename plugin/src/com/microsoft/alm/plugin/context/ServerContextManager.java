// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.ServerContextStore;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.WinHttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

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

    private ServerContextManager() {
        try {
            if (PluginServiceProvider.getInstance().isInsideIDE()) {
                restoreFromSavedState();
            }
        } catch (Throwable t) {
            // being careful here
            logger.error("constructor", t);
        }
    }

    public static ServerContextManager getInstance() {
        return ServerContextManagerHolder.INSTANCE;
    }

    public synchronized ServerContext getActiveContext() {
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

    public synchronized ServerContext getActiveTfsContext() {
        final ServerContext currentContext = getActiveContext();
        if (currentContext != ServerContext.NO_CONTEXT &&
                currentContext.getType() == ServerContext.Type.TFS) {
            return currentContext;
        }
        return null;
    }

    public synchronized ServerContext getActiveVsoContext() {
        final ServerContext currentContext = getActiveContext();
        if (ServerContext.NO_CONTEXT != currentContext &&
                (ServerContext.Type.VSO_DEPLOYMENT == currentContext.getType() || ServerContext.Type.VSO == currentContext.getType())) {
            return currentContext;
        }
        return null;
    }

    public synchronized void setActiveContext(final ServerContext context) {
        activeContext = context;
        getStore().saveServerContext(context);
    }

    public synchronized void clearServerContext(final URI serverUri) {
        final ServerContextStore.Key key = ServerContextStore.Key.create(serverUri);
        getStore().forgetServerContext(key);
        //TODO -- only one for now
        if (activeContext != ServerContext.NO_CONTEXT) {
            final ServerContextStore.Key activeKey = ServerContextStore.Key.create(activeContext);
            if (activeKey.stringValue().equals(key.stringValue())) {
                activeContext = null;
            }
        }
    }

    public synchronized void clearServerContext(final ServerContext context) {
        getStore().forgetServerContext(ServerContextStore.Key.create(context));
        activeContext = ServerContext.NO_CONTEXT;
    }


    public synchronized ServerContext getServerContextByHostURI(final URI uri) {
        //TODO -- only one for now
        if (activeContext != ServerContext.NO_CONTEXT &&
                activeContext.getUri().getHost().equalsIgnoreCase(uri.getHost())) {
            return activeContext;
        }
        return ServerContext.NO_CONTEXT;
    }


    private ServerContextStore getStore() {
        return PluginServiceProvider.getInstance().getServerContextStore();
    }

    private void restoreFromSavedState() {
        //TODO -- only one for now
        List<ServerContext> loaded = getStore().restoreServerContexts();
        activeContext = loaded.size() > 0 ? loaded.get(0) : ServerContext.NO_CONTEXT;
    }

}
