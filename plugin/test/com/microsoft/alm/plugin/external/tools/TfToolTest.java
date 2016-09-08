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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemHelper.class, TfTool.class})
public class TfToolTest extends AbstractTest {

    @Test(expected = ToolException.class)
    public void testGetValidLocation_noTfHome() {
        PowerMockito.mockStatic(SystemHelper.class);
        when(SystemHelper.getEnvironmentVariable("TF_HOME")).thenReturn(null);
        TfTool.getValidLocation();
    }

    @Test(expected = ToolException.class)
    public void testGetValidLocation_noTfCommandFound() {
        setTfHome("/path/clc");
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

    private void setTfHome(final String path) {
        PowerMockito.mockStatic(SystemHelper.class);
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
