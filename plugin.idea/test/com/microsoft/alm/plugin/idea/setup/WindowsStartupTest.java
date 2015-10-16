// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.ice.jni.registry.NoSuchKeyException;
import com.ice.jni.registry.NoSuchValueException;
import com.ice.jni.registry.Registry;
import com.ice.jni.registry.RegistryException;
import com.ice.jni.registry.RegistryKey;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
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
@PrepareForTest({Registry.class})
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
    @Ignore("TODO: ignoring test until creating keys is added back in")
    public void testDllSet() throws Exception {
        boolean dllFound = false;
        File testDll = File.createTempFile("ICE_JNIRegistry", "dll");
        WindowsStartup.addDllPath(testDll.getParent());

        String prop = System.getProperty("java.library.path");
        String pathSeparator = System.getProperty("path.separator");
        String[] paths = prop.split(pathSeparator);
        for (String path : paths) {
            File dll = new File(path + "/" + testDll.getName());
            if (dll.exists()) {
                dllFound = true;
            }
        }
        Assert.assertTrue(dllFound);
    }

    @Test
    public void testCheckIfKeysExistAndMatchHappyCase() throws RegistryException {
        RegistryKey mockVsoiKey = Mockito.mock(RegistryKey.class);
        Mockito.when(mockVsoiKey.getDefaultValue()).thenReturn(TEST_EXE_PATH);
        RegistryKey mockIntellijKey = PowerMockito.mock(RegistryKey.class);
        Mockito.when(mockIntellijKey.openSubKey(WindowsStartup.VSOI_KEY)).thenReturn(mockVsoiKey);
        Assert.assertTrue(WindowsStartup.checkIfKeysExistAndMatch(TEST_EXE_PATH, mockIntellijKey));
    }

    @Test
    public void testCheckIfKeysExistAndMatchMismatchingExePaths() throws RegistryException {
        RegistryKey mockVsoiKey = Mockito.mock(RegistryKey.class);
        Mockito.when(mockVsoiKey.getDefaultValue()).thenReturn(TEST_EXE_PATH);
        RegistryKey mockIntellijKey = PowerMockito.mock(RegistryKey.class);
        Mockito.when(mockIntellijKey.openSubKey(WindowsStartup.VSOI_KEY)).thenReturn(mockVsoiKey);
        Assert.assertFalse(WindowsStartup.checkIfKeysExistAndMatch(DIFFERENT_EXE_PATH, mockIntellijKey));
    }

    @Test
    public void testCheckIfKeysExistAndMatchNoVsoiKeyFound() throws RegistryException {
        RegistryKey mockIntellijKey = PowerMockito.mock(RegistryKey.class);
        Mockito.when(mockIntellijKey.openSubKey(WindowsStartup.VSOI_KEY)).thenThrow(new NoSuchKeyException());
        Assert.assertFalse(WindowsStartup.checkIfKeysExistAndMatch(DIFFERENT_EXE_PATH, mockIntellijKey));
    }

    @Test
    public void testCheckIfKeysExistAndMatchNoVsoiDefaultValueFound() throws RegistryException {
        RegistryKey mockVsoiKey = Mockito.mock(RegistryKey.class);
        Mockito.when(mockVsoiKey.getDefaultValue()).thenThrow(new NoSuchValueException());
        RegistryKey mockIntellijKey = PowerMockito.mock(RegistryKey.class);
        Mockito.when(mockIntellijKey.openSubKey(WindowsStartup.VSOI_KEY)).thenReturn(mockVsoiKey);
        Assert.assertFalse(WindowsStartup.checkIfKeysExistAndMatch(DIFFERENT_EXE_PATH, mockIntellijKey));
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
