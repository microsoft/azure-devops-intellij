// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea;

import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.idea.common.services.CredentialsPromptImpl;
import com.microsoft.alm.plugin.idea.common.services.DeviceFlowResponsePromptImpl;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.common.services.PropertyServiceImpl;
import com.microsoft.alm.plugin.idea.common.services.ServerContextStoreImpl;
import com.microsoft.alm.plugin.idea.common.services.TelemetryContextInitializer;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import org.junit.BeforeClass;

/**
 * This class assures the the plugin service provider is initialized for all tests.
 */
public class IdeaAbstractTest extends AbstractTest {

    @BeforeClass
    public static void setup() {
        // Make sure we skip client initialization so telemetry is not sent to azure
        System.setProperty("com.microsoft.alm.plugin.telemetry.skipClientInitialization", "true");

        PluginServiceProvider.getInstance().initialize(
                new ServerContextStoreImpl(),
                new CredentialsPromptImpl(),
                new DeviceFlowResponsePromptImpl(),
                new TelemetryContextInitializer(),
                PropertyServiceImpl.getInstance(),
                LocalizationServiceImpl.getInstance(),
                false);

        // ensure the AbstractTest's setup method is called as well.
        // We need to do the lines above first since the plugin service provider can only be inited once
        AbstractTest.setup();
    }

    public IdeaAbstractTest() {
    }
}
