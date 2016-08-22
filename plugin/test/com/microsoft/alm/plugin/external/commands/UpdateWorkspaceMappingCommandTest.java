// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.junit.Assert;
import org.junit.Test;

public class UpdateWorkspaceMappingCommandTest extends AbstractCommandTest {
    private Workspace.Mapping mapping;

    @Override
    protected void doAdditionalSetup() {
        mapping = new Workspace.Mapping("$/path", "/path", false);
    }

    @Test
    public void testConstructor_nullContext() {
        final UpdateWorkspaceMappingCommand cmd = new UpdateWorkspaceMappingCommand(null, "ws1", mapping, false);
    }

    @Test
    public void testConstructor_withContext() {
        final UpdateWorkspaceMappingCommand cmd = new UpdateWorkspaceMappingCommand(context, "ws1", mapping, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final UpdateWorkspaceMappingCommand cmd = new UpdateWorkspaceMappingCommand(null, null, null, false);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final UpdateWorkspaceMappingCommand cmd = new UpdateWorkspaceMappingCommand(context, "ws1", mapping, false);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workfold -noprompt -collection:http://server:8080/tfs/defaultcollection ******** -map -workspace:ws1 $/path /path", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final UpdateWorkspaceMappingCommand cmd = new UpdateWorkspaceMappingCommand(null, "ws1", mapping, false);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workfold -noprompt -map -workspace:ws1 $/path /path", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_cloak() {
        Workspace.Mapping mapping = new Workspace.Mapping("$/path", "/path", true);
        final UpdateWorkspaceMappingCommand cmd = new UpdateWorkspaceMappingCommand(null, "ws1", mapping, false);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workfold -noprompt -cloak -workspace:ws1 $/path", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_decloak() {
        Workspace.Mapping mapping = new Workspace.Mapping("$/path", "/path", true);
        final UpdateWorkspaceMappingCommand cmd = new UpdateWorkspaceMappingCommand(null, "ws1", mapping, true);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workfold -noprompt -decloak -workspace:ws1 $/path", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_unmap() {
        final UpdateWorkspaceMappingCommand cmd = new UpdateWorkspaceMappingCommand(null, "ws1", mapping, true);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workfold -noprompt -unmap -workspace:ws1 $/path", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final UpdateWorkspaceMappingCommand cmd = new UpdateWorkspaceMappingCommand(null, "ws1", mapping, false);
        final String message = cmd.parseOutput("", "");
        Assert.assertEquals("", message);
    }

    @Test
    public void testParseOutput_noErrors() {
        final UpdateWorkspaceMappingCommand cmd = new UpdateWorkspaceMappingCommand(null, "ws1", mapping, false);
        final String output = "";
        final String message = cmd.parseOutput(output, "");
        Assert.assertEquals("", message);
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final UpdateWorkspaceMappingCommand cmd = new UpdateWorkspaceMappingCommand(null, "ws1", mapping, false);
        final String message = cmd.parseOutput(null, "error");
    }
}
