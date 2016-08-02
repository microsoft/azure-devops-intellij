// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.services.PropertyServiceImpl;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private boolean propertiesRestored = false;

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
        final SettingsState saveState = new SettingsState();
        saveState.serverContexts = getServerContextStates();
        saveState.properties = getPropertyStates();
        return saveState;
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
                    final AuthenticationInfo authenticationInfo = TeamServicesSecrets.load(key);
                    if (authenticationInfo != null) {
                        serverContexts.add(contextState.createBuilder()
                                .authentication(authenticationInfo)
                                .build());
                    }

                } catch (final Throwable restoreThrowable) {
                    logger.warn("Failed to restore server context", restoreThrowable);
                    // attempt to clean up left over data
                    if (key != null) {
                        try {
                            TeamServicesSecrets.forget(key);
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

    public Map<String, String> restoreProperties() {
        Map<String, String> map = new HashMap<String, String>();
        if (state != null && state.properties != null) {
            for (final PropertyState ps : state.properties) {
                map.put(ps.name, ps.value);
            }
        }

        propertiesRestored = true;
        return map;
    }

    private ServerContextState[] getServerContextStates() {
        try {
            if (!serverContextsRestored && state != null) {
                // return the same state that we loaded
                return state.serverContexts;
            } else {
                final Collection<ServerContext> serverContexts = ServerContextManager.getInstance().getAllServerContexts();
                final List<ServerContextState> contextStates = new ArrayList<ServerContextState>();
                for (ServerContext context : serverContexts) {
                    contextStates.add(new ServerContextState(context));
                }
                return contextStates.toArray(new ServerContextState[contextStates.size()]);
            }
        } catch (Throwable t) {
            logger.warn("getServerContextStates: Unexpected exception", t);
        }
        return null;
    }

    private PropertyState[] getPropertyStates() {
        try {
            if (!propertiesRestored && state != null) {
                // return the same state that we loaded
                return state.properties;
            } else {
                final Map<String, String> map = PropertyServiceImpl.getInstance().getProperties();
                final PropertyState[] states = new PropertyState[map.size()];

                int index = 0;
                for (final Map.Entry<String, String> entry : map.entrySet()) {
                    states[index++] = new PropertyState(entry.getKey(), entry.getValue());
                }
                return states;
            }
        } catch (Throwable t) {
            logger.warn("getPropertyStates: Unexpected exception", t);
        }
        return null;
    }
}

