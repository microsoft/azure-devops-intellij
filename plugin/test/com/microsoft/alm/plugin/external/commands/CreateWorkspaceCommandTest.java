// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.junit.Assert;
import org.junit.Test;

public class CreateWorkspaceCommandTest extends AbstractCommandTest {
    @Test
    public void testConstructor_nullContext() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, "ws1", "comment2", null, null);
    }

    @Test
    public void testConstructor_withContext() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(context, "ws1", "comment2", null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, null, null, null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(context, "ws1", "comment2", null, null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt -collection:http://server:8080/tfs/defaultcollection ******** -new ws1 -comment:comment2", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, "ws1", "comment2", null, null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt -new ws1 -comment:comment2", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_private_checkin() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, "ws1", "comment2", Workspace.FileTime.CHECKIN, Workspace.Permission.PRIVATE);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt -new ws1 -comment:comment2 -filetime:checkin -permission:Private", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_public_checkin() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, "ws1", "comment2", Workspace.FileTime.CHECKIN, Workspace.Permission.PUBLIC);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt -new ws1 -comment:comment2 -filetime:checkin -permission:Public", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_limited_checkin() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, "ws1", "comment2", Workspace.FileTime.CHECKIN, Workspace.Permission.PUBLIC_LIMITED);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt -new ws1 -comment:comment2 -filetime:checkin -permission:PublicLimited", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_private_current() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, "ws1", "comment2", Workspace.FileTime.CURRENT, Workspace.Permission.PRIVATE);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt -new ws1 -comment:comment2 -filetime:current -permission:Private", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_current() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, "ws1", "comment2", Workspace.FileTime.CURRENT, null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt -new ws1 -comment:comment2 -filetime:current", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_private() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, "ws1", "comment2", null, Workspace.Permission.PRIVATE);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt -new ws1 -comment:comment2 -permission:Private", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, "ws1", "comment2", null, null);
        final String message = cmd.parseOutput("", "");
        Assert.assertEquals("", message);
    }

    @Test
    public void testParseOutput_noErrors() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, "ws1", "comment2", null, null);
        final String output = "";
        final String message = cmd.parseOutput(output, "");
        Assert.assertEquals("", message);
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(null, "ws1", "comment2", null, null);
        final String message = cmd.parseOutput(null, "error");
    }
}
