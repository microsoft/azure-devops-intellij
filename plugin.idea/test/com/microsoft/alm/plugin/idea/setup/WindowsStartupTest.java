// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Advapi32Util.class})
public class WindowsStartupTest extends IdeaAbstractTest {
    private static final String TEST_EXE_PATH = "exe/path/idea.exe";
    private static final String DIFFERENT_EXE_PATH = "different/path/to/exe/idea.exe";
    private static final String TEST_EXE_NAME = "IntellijPluginTest_" + new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
    private static File TEST_EXE;

    @After
    public void testCleanup() {
        if (TEST_EXE != null) {
            TEST_EXE.delete();
        }
    }

    @Test
    public void testCheckIfKeysExistAndMatchHappyCase() {
        PowerMockito.mockStatic(Advapi32Util.class);
        Mockito.when(Advapi32Util.registryKeyExists(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY)).thenReturn(true);
        Mockito.when(Advapi32Util.registryValueExists(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY, TEST_EXE_PATH)).thenReturn(true);
        Assert.assertTrue(WindowsStartup.checkIfKeysExistAndMatch(TEST_EXE_PATH));
    }

    @Test
    public void testCheckIfKeysExistAndMatchMismatchingExePaths()  {
        PowerMockito.mockStatic(Advapi32Util.class);
        Mockito.when(Advapi32Util.registryKeyExists(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY)).thenReturn(true);
        Mockito.when(Advapi32Util.registryValueExists(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY, TEST_EXE_PATH)).thenReturn(false);
        Assert.assertFalse(WindowsStartup.checkIfKeysExistAndMatch(DIFFERENT_EXE_PATH));
    }

    @Test
    public void testCheckIfKeysExistAndMatchNoVsoiKeyFound() {
        PowerMockito.mockStatic(Advapi32Util.class);
        Mockito.when(Advapi32Util.registryKeyExists(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY)).thenReturn(false);
        Assert.assertFalse(WindowsStartup.checkIfKeysExistAndMatch(DIFFERENT_EXE_PATH));
    }

    @Test
    public void testCreateRegeditFile() throws IOException {
        TEST_EXE = File.createTempFile(TEST_EXE_NAME, ".exe");
        File regeditFile = WindowsStartup.createRegeditFile(TEST_EXE.getPath());
        Assert.assertTrue(regeditFile.exists());

        BufferedReader br = new BufferedReader(new FileReader(regeditFile));
        Assert.assertEquals("Windows Registry Editor Version 5.00", br.readLine());
        Assert.assertEquals("", br.readLine());
        Assert.assertEquals("[-HKEY_CLASSES_ROOT\\vsoi]", br.readLine());
        Assert.assertEquals("", br.readLine());
        Assert.assertEquals("[HKEY_CLASSES_ROOT\\vsoi]", br.readLine());
        Assert.assertEquals("\"URL Protocol\"=\"\"", br.readLine());
        Assert.assertEquals("", br.readLine());
        Assert.assertEquals("[HKEY_CLASSES_ROOT\\vsoi\\Shell\\Open\\Command]", br.readLine());
        Assert.assertEquals("\"\"=\"" + TEST_EXE.getPath().replace("\\", "\\\\") + "\"", br.readLine());
    }

    @Test
    public void getValidExeHappyCase() throws IOException {
        TEST_EXE = File.createTempFile(TEST_EXE_NAME, ".exe");
        Assert.assertEquals(TEST_EXE.getPath(), WindowsStartup.getValidExe(TEST_EXE.getPath()));
    }

    @Test
    public void getValidExeNoFile() {
        Assert.assertEquals("", WindowsStartup.getValidExe("fake/path/to/idea.exe"));
    }

    @Test
    public void getValidExeNoExeInPath() {
        Assert.assertEquals("", WindowsStartup.getValidExe("fake/path/to/idea no exe"));
    }
}
