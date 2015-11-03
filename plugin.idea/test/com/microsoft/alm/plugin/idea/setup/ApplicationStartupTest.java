// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.sun.jna.Platform;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WindowsStartup.class, Platform.class, MacStartup.class})
public class ApplicationStartupTest extends IdeaAbstractTest {

    @Before
    public void localSetup() {
        PowerMockito.mockStatic(Platform.class);
        PowerMockito.mockStatic(WindowsStartup.class);
        PowerMockito.mockStatic(MacStartup.class);
    }

    @Test
    @Ignore("TODO: ignoring test until creating keys is added back in")
    public void testWindowsOS() {
        setOsResponses(true, false, false);
        osSetup();

        PowerMockito.verifyStatic(Mockito.times(1));
        WindowsStartup.startup();

        PowerMockito.verifyStatic(Mockito.times(0));
        MacStartup.startup();
    }

    @Test
    public void testMacOS() {
        setOsResponses(false, true, false);
        osSetup();

        PowerMockito.verifyStatic(Mockito.times(0));
        WindowsStartup.startup();

        PowerMockito.verifyStatic(Mockito.times(1));
        MacStartup.startup();
    }

    public void osSetup() {
        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.doOsSetup();
    }

    public void setOsResponses(boolean windowsResponse, boolean macResponse, boolean linuxResponse){
        Mockito.when(Platform.isWindows()).thenReturn(windowsResponse);
        Mockito.when(Platform.isMac()).thenReturn(macResponse);
        Mockito.when(Platform.isLinux()).thenReturn(linuxResponse);
    }
}
