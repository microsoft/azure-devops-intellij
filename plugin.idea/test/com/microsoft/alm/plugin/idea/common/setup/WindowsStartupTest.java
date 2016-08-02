// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.setup;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
@PrepareForTest({Advapi32Util.class, FileUtils.class})
public class WindowsStartupTest extends IdeaAbstractTest {
    private static final String TEST_EXE_NAME = "IntellijPluginTest_" + new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
    private static final String TEST_CMD_NAME = "vsts_" + new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
    private File mockCmdFile = Mockito.mock(File.class);
    private File TEST_EXE;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Advapi32Util.class);
        PowerMockito.mockStatic(FileUtils.class);
    }

    @After
    public void testCleanup() {
        if (TEST_EXE != null) {
            TEST_EXE.delete();
        }
    }

    @Test
    public void testDoesKeyNeedUpdated_NoKeyExists() throws Exception {
        Mockito.when(Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY, "")).thenThrow(Win32Exception.class);
        Assert.assertTrue(WindowsStartup.doesKeyNeedUpdated(mockCmdFile));
    }

    @Test
    public void testDoesKeyNeedUpdated_ExistingCmdGone() throws Exception {
        Mockito.when(mockCmdFile.getPath()).thenReturn(TEST_CMD_NAME);
        Mockito.when(Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY, "")).thenReturn(TEST_CMD_NAME + " \"%1\"");
        Assert.assertTrue(WindowsStartup.doesKeyNeedUpdated(mockCmdFile));
    }

    @Test
    public void testDoesKeyNeedUpdated_ExistingCmdOlder() throws Exception {
        TEST_EXE = File.createTempFile(TEST_CMD_NAME, ".cmd");
        Mockito.when(FileUtils.contentEquals(TEST_EXE, mockCmdFile)).thenReturn(false);
        Mockito.when(mockCmdFile.getPath()).thenReturn(TEST_CMD_NAME);
        Mockito.when(mockCmdFile.lastModified()).thenReturn(TEST_EXE.lastModified() + 1000);
        Mockito.when(Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY, "")).thenReturn(TEST_EXE.getPath() + " \"%1\"");
        Assert.assertTrue(WindowsStartup.doesKeyNeedUpdated(mockCmdFile));
    }

    @Test
    public void testDoesKeyNeedUpdated_ExistingCmdNewer() throws Exception {
        TEST_EXE = File.createTempFile(TEST_CMD_NAME, ".cmd");
        Mockito.when(FileUtils.contentEquals(TEST_EXE, mockCmdFile)).thenReturn(false);
        Mockito.when(mockCmdFile.getPath()).thenReturn(TEST_CMD_NAME);
        Mockito.when(mockCmdFile.lastModified()).thenReturn(TEST_EXE.lastModified() - 1000);
        Mockito.when(Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY, "")).thenReturn(TEST_EXE.getPath() + " \"%1\"");
        Assert.assertFalse(WindowsStartup.doesKeyNeedUpdated(mockCmdFile));
    }

    @Test
    public void testDoesKeyNeedUpdated_SameFile() throws Exception {
        TEST_EXE = File.createTempFile(TEST_CMD_NAME, ".cmd");
        Mockito.when(Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY, "")).thenReturn(TEST_EXE.getPath() + " \"%1\"");
        Assert.assertFalse(WindowsStartup.doesKeyNeedUpdated(TEST_EXE));
    }

    @Test
    public void testDoesKeyNeedUpdated_DiffFilesSameContents() throws Exception {
        TEST_EXE = File.createTempFile(TEST_CMD_NAME, ".cmd");
        Mockito.when(FileUtils.contentEquals(TEST_EXE, mockCmdFile)).thenReturn(true);
        Mockito.when(mockCmdFile.getPath()).thenReturn(TEST_CMD_NAME);
        Mockito.when(Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY, "")).thenReturn(TEST_EXE.getPath() + " \"%1\"");
        Assert.assertFalse(WindowsStartup.doesKeyNeedUpdated(mockCmdFile));
    }

    @Test
    public void testCreateRegeditFile() throws IOException {
        TEST_EXE = File.createTempFile(TEST_EXE_NAME, ".exe");
        File regeditFile = WindowsStartup.createRegeditFile(TEST_EXE);
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
        Assert.assertEquals("\"\"=\"" + TEST_EXE.getPath().replace("\\", "\\\\") + " \\\"%1\\\" \"", br.readLine());
    }
}
