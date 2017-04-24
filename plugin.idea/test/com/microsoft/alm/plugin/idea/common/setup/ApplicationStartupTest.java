// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.setup;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthTypes;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.sun.jna.Platform;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WindowsStartup.class, Platform.class, MacStartup.class, ApplicationNamesInfo.class, AuthHelper.class})
public class ApplicationStartupTest extends IdeaAbstractTest {
    public File VSTS_DIR = new File(System.getProperty("java.io.tmpdir"), ".vsts");
    public String TEST_IDE_LOCATION = "/test/idea/location/";
    public String LOCATIONS_CSV_PATH = VSTS_DIR + "/locations.csv";
    public String TMP_DIR = System.getProperty("java.io.tmpdir");
    public String IDEA_NAME = "idea";

    @Mock
    public ApplicationNamesInfo mockApplicationNamesInfo;

    @Before
    public void localSetup() {
        when(mockApplicationNamesInfo.getProductName()).thenReturn(IDEA_NAME);
        PowerMockito.mockStatic(Platform.class, WindowsStartup.class, MacStartup.class, ApplicationNamesInfo.class, AuthHelper.class);
        when(ApplicationNamesInfo.getInstance()).thenReturn(mockApplicationNamesInfo);
        VSTS_DIR.mkdir();
    }

    @After
    public void localCleanup() throws Exception {
        FileUtils.deleteDirectory(VSTS_DIR);
    }

    @Test
    public void testWindowsOS() {
        setOsResponses(true, false, false);
        osSetup();
        verifyStatics(1, 0, 0);
    }

    @Test
    public void testMacOS() {
        setOsResponses(false, true, false);
        osSetup();
        verifyStatics(0, 1, 0);
    }

    @Test
    public void testLinuxOS() {
        setOsResponses(false, false, true);
        osSetup();
        verifyStatics(0, 0, 1);
    }

    @Test
    public void testSetupPreferenceDirExists() {
        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.setupPreferenceDir(TMP_DIR);
        Assert.assertTrue(VSTS_DIR.exists());
    }

    @Test
    public void testSetupPreferenceDirCreated() throws Exception {
        FileUtils.deleteDirectory(VSTS_DIR);
        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.setupPreferenceDir(TMP_DIR);
        Assert.assertTrue(VSTS_DIR.exists());
    }

    @Test
    public void testCacheIdeLocationFirstTime() throws Exception {
        File expectedFile = File.createTempFile("expectedLocation", ".csv");
        writeToFile(expectedFile, IDEA_NAME + "," + TEST_IDE_LOCATION + "\n");

        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.cacheIdeLocation(VSTS_DIR, TEST_IDE_LOCATION);
        Assert.assertTrue(FileUtils.contentEquals(expectedFile, new File(LOCATIONS_CSV_PATH)));
    }

    @Test
    public void testCacheIdeLocationOtherEntries() throws Exception {
        File expectedFile = File.createTempFile("expectedLocation", ".csv");
        writeToFile(expectedFile, "pycharm,/current/dir/for/pycharm\n" + IDEA_NAME + ",/current/location/of/ide\n");
        writeToFile(new File(LOCATIONS_CSV_PATH), "pycharm,/current/dir/for/pycharm\n");

        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.cacheIdeLocation(VSTS_DIR, "/current/location/of/ide");
        Assert.assertTrue(FileUtils.contentEquals(expectedFile, new File(LOCATIONS_CSV_PATH)));
    }

    @Test
    public void testCacheIdeLocationOldEntry() throws Exception {
        File expectedFile = File.createTempFile("expectedLocation", ".csv");
        writeToFile(expectedFile, IDEA_NAME + ",/current/location/of/ide\n");
        writeToFile(new File(LOCATIONS_CSV_PATH), IDEA_NAME + ",/old/dir/for/ide\n");

        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.cacheIdeLocation(VSTS_DIR, "/current/location/of/ide");
        Assert.assertTrue(FileUtils.contentEquals(expectedFile, new File(LOCATIONS_CSV_PATH)));
    }

    @Test
    public void testConfigureAuthType_SettingsFile() {
        when(AuthHelper.isAuthTypeFromSettingsFileSet()).thenReturn(true);

        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.configureAuthType();
        PowerMockito.verifyStatic(Mockito.times(1));
        AuthHelper.setDeviceFlowEnvFromSettingsFile();
        PowerMockito.verifyStatic(Mockito.times(0));
        AuthHelper.isDeviceFlowEnvSetTrue();
    }

    @Test
    public void testConfigureAuthType_EnvVariable() {
        when(AuthHelper.isAuthTypeFromSettingsFileSet()).thenReturn(false);
        when(AuthHelper.isDeviceFlowEnvSetTrue()).thenReturn(true);

        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.configureAuthType();
        PowerMockito.verifyStatic(Mockito.times(0));
        AuthHelper.setDeviceFlowEnvFromSettingsFile();
        PowerMockito.verifyStatic(Mockito.times(1));
        AuthHelper.isDeviceFlowEnvSetTrue();
        AuthHelper.setAuthTypeInSettingsFile(AuthTypes.DEVICE_FLOW);
    }

    @Test
    public void testConfigureAuthType_None() {
        when(AuthHelper.isAuthTypeFromSettingsFileSet()).thenReturn(false);
        when(AuthHelper.isDeviceFlowEnvSetTrue()).thenReturn(false);

        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.configureAuthType();
        PowerMockito.verifyStatic(Mockito.times(0));
        AuthHelper.setDeviceFlowEnvFromSettingsFile();
        AuthHelper.isDeviceFlowEnvSetTrue();
        AuthHelper.setAuthTypeInSettingsFile(AuthTypes.DEVICE_FLOW);
    }

    public void osSetup() {
        ApplicationStartup appStartup = new ApplicationStartup();
        appStartup.doOsSetup(VSTS_DIR, TEST_IDE_LOCATION);
    }

    public void setOsResponses(boolean windowsResponse, boolean macResponse, boolean linuxResponse) {
        when(Platform.isWindows()).thenReturn(windowsResponse);
        when(Platform.isMac()).thenReturn(macResponse);
        when(Platform.isLinux()).thenReturn(linuxResponse);
    }

    public void verifyStatics(int winRuns, int macRuns, int linuxRuns) {
        PowerMockito.verifyStatic(Mockito.times(winRuns));
        WindowsStartup.startup();

        PowerMockito.verifyStatic(Mockito.times(macRuns));
        MacStartup.startup();

        PowerMockito.verifyStatic(Mockito.times(linuxRuns));
        LinuxStartup.startup();
    }

    public void writeToFile(File file, String content) throws Exception {
        FileWriter fileWriter = new FileWriter(file.getPath());
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(content);
        bufferedWriter.close();
    }
}
