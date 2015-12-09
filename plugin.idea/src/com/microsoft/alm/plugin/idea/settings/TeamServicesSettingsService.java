// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Stores a SettingsState object to file and handles writing and reading the objects
 */
@State(
        name = "VSTSSettings",
        storages = {@Storage(
                file = StoragePathMacros.APP_CONFIG + "/vsts_settings.xml")}
)
public class TeamServicesSettingsService implements PersistentStateComponent<SettingsState> {
    private static final Logger logger = LoggerFactory.getLogger(TeamServicesSettingsService.class);
    private SettingsState state = null;
    private boolean serverContextsRestored = false;

    // This default instance is only returned in the case of tests or we are somehow running outside of IntelliJ
    private static TeamServicesSettingsService DEFAULT_INSTANCE = new TeamServicesSettingsService();

    public static TeamServicesSettingsService getInstance() {
        TeamServicesSettingsService service = null;
        if (ApplicationManager.getApplication() != null) {
            service = ServiceManager.getService(TeamServicesSettingsService.class);
        }

        if (service == null) {
            service = DEFAULT_INSTANCE;
        }

        return service;
    }

    @Nullable
    @Override
    public SettingsState getState() {
        if (!serverContextsRestored) {
            // return the same state that we loaded
            return state;
        } else {
            final SettingsState saveState = new SettingsState();
            final Collection<ServerContext> serverContexts = ServerContextManager.getInstance().getAllServerContexts();
            final List<ServerContextState> contextStates = new ArrayList<ServerContextState>();
            for (ServerContext context : serverContexts) {
                contextStates.add(new ServerContextState(context));
            }
            saveState.serverContexts = contextStates.toArray(new ServerContextState[contextStates.size()]);
            return saveState;
        }
    }

    @Override
    public void loadState(SettingsState state) {
        this.state = state;
    }

    public List<ServerContext> restoreServerContexts() {
        final List<ServerContext> serverContexts = new ArrayList<ServerContext>();
        if (state != null && state.serverContexts != null) {
            for (final ServerContextState contextState : state.serverContexts) {
                String key = null;
                try {
                    key = ServerContext.getKey(contextState.uri);
                    final AuthenticationInfo authenticationInfo = ServerContextSecrets.load(key);
                    if (authenticationInfo != null) {
                        serverContexts.add(contextState.createBuilder()
                                .uri(contextState.uri)
                                .authentication(authenticationInfo)
                                .build());
                    }
                } catch (final Throwable restoreThrowable) {
                    logger.warn("Failed to restore server context", restoreThrowable);
                    // attempt to clean up left over data
                    if (key != null) {
                        try {
                            ServerContextSecrets.forget(key);
                        } catch (final Throwable cleanupThrowable) {
                            logger.warn("Failed to cleanup invalid server context");
                        }
                    }
                }
            }
        }

        // Remember that this method was called, so that when we persist, we read the new values
        serverContextsRestored = true;
        return serverContexts;
    }

}

