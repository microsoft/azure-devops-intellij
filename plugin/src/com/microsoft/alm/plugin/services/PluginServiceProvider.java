// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

import com.microsoft.applicationinsights.extensibility.ContextInitializer;

/**
 * This class is a singleton that holds all of the services that must be provided by the plugin that uses this module.
 * When the plugin is loaded for the first time, this class must be initialized. It may only be initialized once.
 * <p/>
 * Thread-safety: Not Thread Safe
 */
public class PluginServiceProvider {

    private boolean initialized = false;
    private boolean insideIDE = false;
    private ServerContextStore contextStore;
    private CredentialsPrompt credentialsPrompt;
    private ContextInitializer telemetryContextInitializer;

    private static class ProviderHolder {
        private static PluginServiceProvider INSTANCE = new PluginServiceProvider();
    }

    public static PluginServiceProvider getInstance() {
        return ProviderHolder.INSTANCE;
    }

    public void initialize(final ServerContextStore contextStore,
                           final CredentialsPrompt credentialsPrompt,
                           final ContextInitializer telemetryContextInitializer,
                           final boolean insideIDE) {
        if (!initialized) {
            this.contextStore = contextStore;
            this.credentialsPrompt = credentialsPrompt;
            this.telemetryContextInitializer = telemetryContextInitializer;
            this.insideIDE = insideIDE;
            initialized = true;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isInsideIDE() {
        return insideIDE;
    }

    public ServerContextStore getServerContextStore() {
        assert initialized;
        assert contextStore != null;

        return contextStore;
    }

    public CredentialsPrompt getCredentialsPrompt() {
        assert initialized;
        assert credentialsPrompt != null;

        return credentialsPrompt;
    }

    public ContextInitializer getTelemetryContextInitializer() {
        assert initialized;
        assert telemetryContextInitializer != null;

        return telemetryContextInitializer;
    }
}
