// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.intellij.openapi.components.ApplicationComponent;
import com.microsoft.alm.plugin.idea.services.CredentialsPromptImpl;
import com.microsoft.alm.plugin.idea.services.ServerContextStoreImpl;
import com.microsoft.alm.plugin.idea.services.TelemetryContextInitializer;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.sun.jna.Platform;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes and configures plugin at startup
 */
public class ApplicationStartup implements ApplicationComponent {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartup.class);

    public ApplicationStartup() {
    }

    public void initComponent() {
        // Setup the services that the core plugin components need
        PluginServiceProvider.getInstance().initialize(
                new ServerContextStoreImpl(),
                new CredentialsPromptImpl(),
                new TelemetryContextInitializer(),
                true);

        doOsSetup();
    }

    public void disposeComponent() {
    }

    @NotNull
    public String getComponentName() {
        return "ApplicationStartup";
    }

    /**
     * Finds the OS type the plugin is running on and calls the setup for it
     */
    protected void doOsSetup() {
        if (Platform.isWindows()) {
            logger.debug("Windows operating system detected");
            // WindowsStartup.startup(); TODO: comment back in once arguments are being passed
        } else if (Platform.isMac()) {
            logger.debug("Mac operating system detected");
            MacStartup.startup();
        } else {
            //TODO: Add Linux logic
        }
    }
}