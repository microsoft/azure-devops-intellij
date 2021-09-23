// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.events.ServerPollingManager;

public abstract class IdeaLightweightTest extends BasePlatformTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AbstractTest.setup();
    }

    @Override
    protected void tearDown() throws Exception {
        ServerPollingManager.getInstance().stopPolling();
        AbstractTest.cleanup();
        super.tearDown();
    }
}
