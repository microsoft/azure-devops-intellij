// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.setup;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LinuxStartupTest extends IdeaAbstractTest {

    @Test
    public void testCreateDesktopFile() throws IOException, InterruptedException {
        File mockScriptFile = Mockito.mock(File.class);
        Mockito.when(mockScriptFile.getAbsolutePath()).thenReturn("/test/path/to/script/vsts.sh");
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File desktopFile = LinuxStartup.createDesktopFileAndUpdateDatabase(mockScriptFile, new File(tmpDir, "vsts-test.desktop"));
        Assert.assertTrue(desktopFile.exists());

        BufferedReader br = new BufferedReader(new FileReader(desktopFile));
        Assert.assertEquals("[Desktop Entry]", br.readLine());
        Assert.assertEquals("Name=VSTS Protocol Handler", br.readLine());
        Assert.assertEquals("Comment=Custom protocol handler for the IntelliJ VSTS plugin", br.readLine());
        Assert.assertEquals("Exec=" + mockScriptFile.getAbsolutePath() + " %u", br.readLine());
        Assert.assertEquals("Icon=", br.readLine());
        Assert.assertEquals("Terminal=False", br.readLine());
        Assert.assertEquals("Type=Application", br.readLine());
        Assert.assertEquals("X-MultipleArgs=True", br.readLine());
        Assert.assertEquals("MimeType=x-scheme-handler/vsoi", br.readLine());
        Assert.assertEquals("Encoding=UTF-8", br.readLine());
        Assert.assertEquals("Categories=Network;Application;", br.readLine());

        desktopFile.delete();
    }
}
