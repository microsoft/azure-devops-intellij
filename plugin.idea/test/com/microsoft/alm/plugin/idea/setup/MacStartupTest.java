// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

@RunWith(PowerMockRunner.class)
public class MacStartupTest extends IdeaAbstractTest {
    public final String PROTOCOL = "http://";
    public final String APP_URL_PLUGIN_INSTALLED = "/Users/test/Library/Application%20Support/IdeaIC14/com.microsoft.alm/lib/vsoi.app/";
    public final String APP_URL_INSIDE_IDEA = "/Users/test/Library/Caches/IdeaIC14/plugins-sandbox/plugins/com.microsoft.alm.plugin.idea/classes/";

    public File appletMock = Mockito.mock(File.class);

    @Test
    public void testGetAppPath_PluginInstalled() throws Exception {
        URL url = new URL(PROTOCOL + APP_URL_PLUGIN_INSTALLED);
        Assert.assertEquals(APP_URL_PLUGIN_INSTALLED.replaceAll("%20", " "), MacStartup.getAppPath(url));
    }

    @Test
    public void testGetAppPath_RunningInIdea() throws Exception {
        URL url = new URL(PROTOCOL + APP_URL_INSIDE_IDEA);
        Assert.assertEquals(APP_URL_INSIDE_IDEA + MacStartup.OS_X_DIR + MacStartup.APP_NAME, MacStartup.getAppPath(url));
    }

    @Test
    public void testSetAppletPermission_PermissionsSet() throws Exception {
        Mockito.when(appletMock.exists()).thenReturn(true);
        Mockito.when(appletMock.canExecute()).thenReturn(true);
        MacStartup.setAppletPermissions(appletMock);
        Mockito.verify(appletMock, Mockito.never()).setExecutable(Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void testSetAppletPermission_PermissionsNotSet() throws Exception {
        Mockito.when(appletMock.exists()).thenReturn(true);
        Mockito.when(appletMock.canExecute()).thenReturn(false);
        MacStartup.setAppletPermissions(appletMock);
        Mockito.verify(appletMock, Mockito.times(1)).setExecutable(true, false);
    }

    @Test(expected = FileNotFoundException.class)
    public void testSetAppletPermission_FileNotFound() throws Exception {
        Mockito.when(appletMock.exists()).thenReturn(false);
        MacStartup.setAppletPermissions(appletMock);
    }
}
