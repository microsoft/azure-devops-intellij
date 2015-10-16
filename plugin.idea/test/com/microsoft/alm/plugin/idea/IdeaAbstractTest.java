// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea;

import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.idea.services.CredentialsPromptImpl;
import com.microsoft.alm.plugin.idea.services.ServerContextStoreImpl;
import com.microsoft.alm.plugin.idea.services.TelemetryContextInitializer;
import com.microsoft.alm.plugin.services.PluginServiceProvider;

/**
 * This class assures the the plugin service provider is initialized for all tests.
 */
public class IdeaAbstractTest extends AbstractTest {
    public IdeaAbstractTest() {
        PluginServiceProvider.getInstance().initialize(
                new ServerContextStoreImpl(),
                new CredentialsPromptImpl(),
                new TelemetryContextInitializer(),
                false);
    }
}
