// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;

@RunWith(PowerMockRunner.class)
public class MacStartupTest extends IdeaAbstractTest {
    public final String PROTOCOL = "http://";
    public final String APP_URL_PLUGIN_INSTALLED = "/Users/test/Library/Application%20Support/IdeaIC14/com.microsoft.alm/lib/platform/";
    public final String APP_URL_INSIDE_IDEA = "/Users/test/Library/Caches/IdeaIC14/plugins-sandbox/plugins/com.microsoft.alm.plugin.idea/classes";

    @Test
    public void testGetAppPath_PluginInstalled() throws Exception {
        URL url = new URL(PROTOCOL + APP_URL_PLUGIN_INSTALLED);
        Assert.assertEquals(APP_URL_PLUGIN_INSTALLED.replaceAll("%20", " ") + MacStartup.OS_X_DIR + "/" + MacStartup.APP_NAME, IdeaHelper.getResourcePath(url, MacStartup.APP_NAME, MacStartup.OS_X_DIR));
    }

    @Test
    public void testGetAppPath_RunningInIdea() throws Exception {
        URL url = new URL(PROTOCOL + APP_URL_INSIDE_IDEA);
         Assert.assertEquals(APP_URL_INSIDE_IDEA + IdeaHelper.TEST_RESOURCES_SUB_PATH + MacStartup.OS_X_DIR + "/" + MacStartup.APP_NAME, IdeaHelper.getResourcePath(url, MacStartup.APP_NAME, MacStartup.OS_X_DIR));
    }
}
