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

/**
 * This class assures the the plugin service provider is initialized for all tests.
 */
public class IdeaAbstractTest extends AbstractTest {
    public IdeaAbstractTest() {
        PluginServiceProvider.getInstance().initialize(
                new ServerContextStoreImpl(),
                new CredentialsPromptImpl(),
                new DeviceFlowResponsePromptImpl(),
                new TelemetryContextInitializer(),
                PropertyServiceImpl.getInstance(),
                LocalizationServiceImpl.getInstance(),
                false);
    }
}
