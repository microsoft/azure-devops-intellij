// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.sun.jna.Platform;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WindowsStartup.class, Platform.class})
public class ApplicationStartupTest extends IdeaAbstractTest {

    @Test
    @Ignore("TODO: ignoring test until creating keys is added back in")
    public void testWindowsOS() {
        PowerMockito.mockStatic(Platform.class);
        Mockito.when(Platform.isWindows()).thenReturn(true);
        osSetup();
        PowerMockito.verifyStatic();
        WindowsStartup.startup();
    }

    @Test
    public void testMacOS() {
        PowerMockito.mockStatic(Platform.class);
        Mockito.when(Platform.isWindows()).thenReturn(false);
        osSetup();
        PowerMockito.verifyStatic(Mockito.times(0));
        WindowsStartup.startup();
    }

    public static void osSetup() {
        PowerMockito.mockStatic(WindowsStartup.class);
        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.doOsSetup();
    }
}
