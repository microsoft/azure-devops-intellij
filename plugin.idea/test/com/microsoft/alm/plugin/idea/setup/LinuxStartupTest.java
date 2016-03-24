// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

//package com.microsoft.alm.plugin.idea.setup;
//
//import org.junit.After;
//import org.junit.Assert;
//import org.junit.Test;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//
//public class LinuxStartupTest {
//    private File TEST_DESKTOP_FILE;
//
//    @After
//    public void testCleanup() {
//        if (TEST_DESKTOP_FILE != null) {
//            TEST_DESKTOP_FILE.delete();
//        }
//    }
//
//    @Test
//    public void testCreateDesktopFile() throws IOException {
//        TEST_DESKTOP_FILE = File.createTempFile("vsoi-test", ".desktop");
//        File desktopFile = LinuxStartup.createDesktopFile(TEST_DESKTOP_FILE);
//        Assert.assertTrue(desktopFile.exists());
//
//        BufferedReader br = new BufferedReader(new FileReader(desktopFile));
//        Assert.assertEquals("[Desktop Entry]", br.readLine());
//        Assert.assertEquals("Name=VSTS Protocol Handler", br.readLine());
//        Assert.assertEquals("Comment=Custom protocol handler for the IntelliJ VSTS plugin", br.readLine());
//        Assert.assertEquals("Exec=" + TEST_DESKTOP_FILE.getAbsolutePath() + " %u", br.readLine());
//        Assert.assertEquals("Icon=", br.readLine());
//        Assert.assertEquals("Terminal=0", br.readLine());
//        Assert.assertEquals("Type=Application", br.readLine());
//        Assert.assertEquals("X-MultipleArgs=True", br.readLine());
//        Assert.assertEquals("MimeType=x-scheme-handler/vsoi", br.readLine());
//        Assert.assertEquals("Encoding=UTF-8", br.readLine());
//        Assert.assertEquals("Categories=Network;Application;", br.readLine());
//    }
//}
