// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.setup;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthTypes;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationStartupTest extends IdeaAbstractTest {
    public File VSTS_DIR = new File(System.getProperty("java.io.tmpdir"), ".vsts");
    public String TEST_IDE_LOCATION = "/test/idea/location/";
    public String LOCATIONS_CSV_PATH = VSTS_DIR + "/locations.csv";
    public String TMP_DIR = System.getProperty("java.io.tmpdir");
    public String IDEA_NAME = "idea";

    @Mock
    public ApplicationNamesInfo mockApplicationNamesInfo;

    @Mock
    private MockedStatic<ApplicationNamesInfo> applicationNamesInfo;

    @Before
    public void localSetup() {
        when(mockApplicationNamesInfo.getProductName()).thenReturn(IDEA_NAME);
        applicationNamesInfo.when(ApplicationNamesInfo::getInstance).thenReturn(mockApplicationNamesInfo);
        var ignored = VSTS_DIR.mkdir();
    }

    @After
    public void localCleanup() throws Exception {
        FileUtils.deleteDirectory(VSTS_DIR);
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
        try (var authHelper = Mockito.mockStatic(AuthHelper.class)) {
            authHelper.when(AuthHelper::isAuthTypeFromSettingsFileSet).thenReturn(true);

            ApplicationStartup appStartup = new ApplicationStartup();
            appStartup.configureAuthType();

            authHelper.verify(AuthHelper::setDeviceFlowEnvFromSettingsFile, times(1));
            authHelper.verify(AuthHelper::isDeviceFlowEnvSetTrue, times(0));
        }
    }

    @Test
    public void testConfigureAuthType_EnvVariable() {
        try (var authHelper = Mockito.mockStatic(AuthHelper.class)) {
            authHelper.when(AuthHelper::isAuthTypeFromSettingsFileSet).thenReturn(false);
            authHelper.when(AuthHelper::isDeviceFlowEnvSetTrue).thenReturn(true);

            ApplicationStartup appStartup = new ApplicationStartup();
            appStartup.configureAuthType();

            authHelper.verify(AuthHelper::setDeviceFlowEnvFromSettingsFile, times(0));
            authHelper.verify(AuthHelper::isDeviceFlowEnvSetTrue, times(1));
            authHelper.verify(() -> AuthHelper.setAuthTypeInSettingsFile(AuthTypes.DEVICE_FLOW), times(1));
        }
    }

    @Test
    public void testConfigureAuthType_None() {
        try (var authHelper = Mockito.mockStatic(AuthHelper.class)) {
            authHelper.when(AuthHelper::isAuthTypeFromSettingsFileSet).thenReturn(false);
            authHelper.when(AuthHelper::isDeviceFlowEnvSetTrue).thenReturn(false);

            ApplicationStartup appStartup = new ApplicationStartup();
            appStartup.configureAuthType();

            authHelper.verify(AuthHelper::setDeviceFlowEnvFromSettingsFile, times(0));
            authHelper.verify(() -> AuthHelper.setAuthTypeInSettingsFile(AuthTypes.DEVICE_FLOW), times(0));
        }
    }

    public void writeToFile(File file, String content) throws Exception {
        FileWriter fileWriter = new FileWriter(file.getPath());
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(content);
        bufferedWriter.close();
    }
}
