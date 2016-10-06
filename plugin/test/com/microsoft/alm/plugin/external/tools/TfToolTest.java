// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.tools;

import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.external.commands.TfVersionCommand;
import com.microsoft.alm.plugin.external.exceptions.ToolBadExitCodeException;
import com.microsoft.alm.plugin.external.exceptions.ToolException;
import com.microsoft.alm.plugin.external.exceptions.ToolVersionException;
import com.microsoft.alm.plugin.external.models.ToolVersion;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import com.sun.jna.Platform;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemHelper.class, TfTool.class, PluginServiceProvider.class, Platform.class})
public class TfToolTest extends AbstractTest {
    private static File exeDirectory;
    private static File exeFile;

    @Mock
    public PluginServiceProvider pluginServiceProvider;

    @Mock
    public PropertyService propertyService;

    @BeforeClass
    public static void setOnce() throws Exception {
        String path = System.getProperty("java.io.tmpdir") + File.separator + TfTool.TF_DIRECTORY_PREFIX + "-" + TfTool.TF_MIN_VERSION;
        exeDirectory = new File(path);
        exeDirectory.mkdir();
        exeFile = new File(path, "tf.cmd");
        exeFile.createNewFile();
    }

    @AfterClass
    public static void cleanUp() {
        exeFile.delete();
        exeDirectory.delete();
    }

    @Before
    public void setUp() {
        PowerMockito.mockStatic(SystemHelper.class);
        PowerMockito.mockStatic(PluginServiceProvider.class);
        when(PluginServiceProvider.getInstance()).thenReturn(pluginServiceProvider);
        when(pluginServiceProvider.getPropertyService()).thenReturn(propertyService);

        PowerMockito.mockStatic(Platform.class);
        when(Platform.isWindows()).thenReturn(true);
    }

    @Test(expected = ToolException.class)
    public void testGetValidLocation_noTfProperty() {
        when(propertyService.getProperty(PropertyService.PROP_TF_HOME)).thenReturn(null);
        TfTool.getValidLocation();
    }

    @Test
    public void testGetValidLocation_TfPropertySet() throws Exception {
        when(propertyService.getProperty(PropertyService.PROP_TF_HOME)).thenReturn(exeFile.getPath());
        assertEquals(exeFile.getPath(), TfTool.getValidLocation());
    }

    @Test(expected = ToolException.class)
    public void testGetValidLocation_noTfCommandFound() {
        when(propertyService.getProperty(PropertyService.PROP_TF_HOME)).thenReturn("/path/clc");
        TfTool.getValidLocation();
    }

    @Test(expected = ToolVersionException.class)
    public void testCheckVersion_tooOld() throws Exception {
        ToolVersion toolVersion = adjustToolVersion(TfTool.TF_MIN_VERSION, 0, 0, -1);
        mockTfVersionCommand(toolVersion);
        TfTool.checkVersion();
    }

    @Test
    public void testCheckVersion_exact() throws Exception {
        ToolVersion toolVersion = adjustToolVersion(TfTool.TF_MIN_VERSION, 0, 0, 0);
        mockTfVersionCommand(toolVersion);
        TfTool.checkVersion();
    }

    @Test
    public void testCheckVersion_revGreater() throws Exception {
        ToolVersion toolVersion = adjustToolVersion(TfTool.TF_MIN_VERSION, 0, 0, +1);
        mockTfVersionCommand(toolVersion);
        TfTool.checkVersion();
    }

    @Test
    public void testCheckVersion_minorGreater() throws Exception {
        ToolVersion toolVersion = adjustToolVersion(TfTool.TF_MIN_VERSION, 0, +1, 0);
        mockTfVersionCommand(toolVersion);
        TfTool.checkVersion();
    }

    @Test
    public void testCheckVersion_majorGreater() throws Exception {
        ToolVersion toolVersion = adjustToolVersion(TfTool.TF_MIN_VERSION, +1, 0, 0);
        mockTfVersionCommand(toolVersion);
        TfTool.checkVersion();
    }

    @Test
    public void testThrowBadExitCode_zero() {
        TfTool.throwBadExitCode(0);
    }

    @Test(expected = ToolBadExitCodeException.class)
    public void testThrowBadExitCode_nonZero() {
        TfTool.throwBadExitCode(100);
    }

    @Test
    public void testTryDetectTf_TfHome() {
        setTfHome(exeFile.getParent());
        assertEquals(exeFile.getPath(), TfTool.tryDetectTf());
    }

    @Test
    public void testTryDetectTf_Path() {
        setTfHome(null);
        when(SystemHelper.getEnvironmentVariable("PATH")).thenReturn(exeFile.getParent() + ";C\\path\\to\\bin");
        assertEquals(exeFile.getPath(), TfTool.tryDetectTf());
    }

    @Test
    public void testTryDetectTf_Fail() {
        setTfHome(null);
        when(SystemHelper.getEnvironmentVariable("PATH")).thenReturn(null);
        assertEquals(null, TfTool.tryDetectTf());
    }


    private void setTfHome(final String path) {
        when(SystemHelper.getEnvironmentVariable("TF_HOME")).thenReturn(path);
    }

    private ToolVersion adjustToolVersion(final ToolVersion version, final int major, final int minor, final int revision) {
        return new ToolVersion(version.getMajor() + major, version.getMinor() + minor, version.getRevision() + revision, version.getBuild());
    }

    private void mockTfVersionCommand(final ToolVersion version) throws Exception {
        final TfVersionCommand command = Mockito.mock(TfVersionCommand.class);
        PowerMockito.whenNew(TfVersionCommand.class).withNoArguments().thenReturn(command);
        when(command.runSynchronously()).thenReturn(version);
    }
}
