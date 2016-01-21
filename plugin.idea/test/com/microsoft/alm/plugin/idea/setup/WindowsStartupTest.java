// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.setup;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.apache.commons.lang.StringUtils;
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
@PrepareForTest({Advapi32Util.class, ApplicationNamesInfo.class})
public class WindowsStartupTest extends IdeaAbstractTest {
    private static final String TEST_INTELLIJ_ENTRY = "C:\\Program Files\\JetBrains\\IntelliJ IDEA 14.1.4\\bin\\idea.exe \"%1\"";
    private static final String TEST_COMMAND = "C:\\Program Files\\JetBrains\\IntelliJ IDEA 14.1.4\\bin\\idea.exe vsts \"%1\"";
    private static final String DIFFERENT_TEST_COMMAND = "C:\\Program Files\\JetBrains\\IntelliJ IDEA 15.1.4\\bin\\idea.exe vsts \"%1\"";
    private static final String TEST_NO_EXE_STRING = "C:\\Program Files\\JetBrains\\IntelliJ IDEA 15.1.4\\bin\\exe.idea \"%1\"";
    private static final String TEST_EXE_NAME = "IntellijPluginTest_" + new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
    private static File TEST_EXE;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Advapi32Util.class);
        PowerMockito.mockStatic(ApplicationNamesInfo.class);
    }

    @After
    public void testCleanup() {
        if (TEST_EXE != null) {
            TEST_EXE.delete();
        }
    }

    @Test
    public void testFindApplicationExeAndroidStudioExe() {
        String studioExe = System.getProperty("os.arch").contains("64") ? "studio64.exe" : "studio32.exe";
        Mockito.when(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE,
                WindowsStartup.ANDROID_STUDIO_REGISTRY_KEY, WindowsStartup.ANDROID_STUDIO_REGISTRY_VALUE)).thenReturn("C:\\Program Files\\Android\\Android Studio");
        Assert.assertEquals("C:\\Program Files\\Android\\Android Studio\\bin\\" + studioExe + " \"%1\"", WindowsStartup.findApplicationExe("Studio"));
    }

    @Test
    public void testFindApplicationExePhpStormExe() {
        ApplicationNamesInfo mockApplicationNamesInfo = Mockito.mock(ApplicationNamesInfo.class);
        Mockito.when(mockApplicationNamesInfo.getScriptName()).thenReturn("PhpStorm");
        Mockito.when(ApplicationNamesInfo.getInstance()).thenReturn(mockApplicationNamesInfo);
        Mockito.when(Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, "Applications\\PhpStorm.exe\\shell\\open\\command", StringUtils.EMPTY)).
                thenReturn("C:\\Program Files (x86)\\JetBrains\\PhpStorm 10.0.3\\bin\\PhpStorm.exe \"%1\"");
        Assert.assertEquals("C:\\Program Files (x86)\\JetBrains\\PhpStorm 10.0.3\\bin\\PhpStorm.exe \"%1\"", WindowsStartup.findApplicationExe("PhpStorm"));
    }

    @Test
    public void testCheckIfKeysExistAndMatchHappyCase() {
        Mockito.when(Advapi32Util.registryKeyExists(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY)).thenReturn(true);
        Mockito.when(Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY, "")).thenReturn(TEST_COMMAND);
        Assert.assertTrue(WindowsStartup.checkIfKeysExistAndMatch(TEST_COMMAND));
    }

    @Test
    public void testCheckIfKeysExistAndMatchMismatchingExePaths() {
        Mockito.when(Advapi32Util.registryKeyExists(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY)).thenReturn(true);
        Mockito.when(Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY, "")).thenReturn(DIFFERENT_TEST_COMMAND);
        Assert.assertFalse(WindowsStartup.checkIfKeysExistAndMatch(TEST_COMMAND));
    }

    @Test
    public void testCheckIfKeysExistAndMatchNoVsoiKeyFound() {
        Mockito.when(Advapi32Util.registryKeyExists(WinReg.HKEY_CLASSES_ROOT, WindowsStartup.VSOI_KEY)).thenReturn(false);
        Assert.assertFalse(WindowsStartup.checkIfKeysExistAndMatch(TEST_COMMAND));
    }

    @Test
    public void testCreateRegeditFile() throws IOException {
        TEST_EXE = File.createTempFile(TEST_EXE_NAME, ".exe");
        String command = TEST_EXE.getPath() + " vsts \"%1\"";
        File regeditFile = WindowsStartup.createRegeditFile(command);
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
        Assert.assertEquals("\"\"=\"" + command.replace("\\", "\\\\").replace("\"", "\\\"") + "\"", br.readLine());
    }

    @Test
    public void isValidExeHappyCase() throws IOException {
        TEST_EXE = File.createTempFile(TEST_EXE_NAME, ".exe");
        Assert.assertTrue(WindowsStartup.isValidExe(TEST_EXE.getPath()));
    }

    @Test
    public void isValidExeNoFile() {
        Assert.assertFalse(WindowsStartup.isValidExe("fake/path/to/idea.exe"));
    }

    @Test
    public void isValidExeNoExeInPath() {
        Assert.assertFalse(WindowsStartup.isValidExe(TEST_NO_EXE_STRING));
    }

    @Test
    public void getCreateCommandHappy() {
        Assert.assertEquals(TEST_COMMAND, WindowsStartup.createCommand(TEST_INTELLIJ_ENTRY));
    }

    @Test
    public void getCreateCommandNoExe() {
        Assert.assertEquals(StringUtils.EMPTY, WindowsStartup.createCommand(TEST_NO_EXE_STRING));
    }
}
