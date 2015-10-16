// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WindowsStartup.class})
public class ApplicationStartupTest extends IdeaAbstractTest {
    private static final String OS_NAME = "os.name";

    @Test
    @Ignore("TODO: ignoring test until creating keys is added back in")
    public void testWindowsOS() {
        osSetup("windows");
        PowerMockito.verifyStatic();
        WindowsStartup.startup();
    }

    @Test
    public void testMacOS() {
        osSetup("mac");
        PowerMockito.verifyStatic(Mockito.times(0));
        WindowsStartup.startup();
    }

    public static void osSetup(String osName) {
        PowerMockito.mockStatic(WindowsStartup.class);
        System.setProperty(OS_NAME, osName);
        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.doOsSetup();
    }
}
