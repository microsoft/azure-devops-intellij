// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

public class UpdateWorkspaceCommandTest extends AbstractCommandTest {
    @Test
    public void testConstructor_nullContext() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, "ws1", "workspace2", "comment2", null, null);
    }

    @Test
    public void testConstructor_withContext() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(context, "ws1", "workspace2", "comment2", null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, null, null, null, null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(context, "ws1", "workspace2", "comment2", null, null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt -collection:http://server:8080/tfs/defaultcollection ******** ws1 -newname:workspace2 -comment:comment2", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, "ws1", "workspace2", "comment2", null, null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt ws1 -newname:workspace2 -comment:comment2", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_private_checkin() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, "ws1", "workspace2", "comment2", UpdateWorkspaceCommand.FileTime.CHECKIN, UpdateWorkspaceCommand.Permission.PRIVATE);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt ws1 -newname:workspace2 -comment:comment2 -filetime:checkin -permission:Private", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_public_checkin() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, "ws1", "workspace2", "comment2", UpdateWorkspaceCommand.FileTime.CHECKIN, UpdateWorkspaceCommand.Permission.PUBLIC);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt ws1 -newname:workspace2 -comment:comment2 -filetime:checkin -permission:Public", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_limited_checkin() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, "ws1", "workspace2", "comment2", UpdateWorkspaceCommand.FileTime.CHECKIN, UpdateWorkspaceCommand.Permission.PUBLIC_LIMITED);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt ws1 -newname:workspace2 -comment:comment2 -filetime:checkin -permission:PublicLimited", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_private_current() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, "ws1", "workspace2", "comment2", UpdateWorkspaceCommand.FileTime.CURRENT, UpdateWorkspaceCommand.Permission.PRIVATE);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt ws1 -newname:workspace2 -comment:comment2 -filetime:current -permission:Private", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_current() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, "ws1", "workspace2", "comment2", UpdateWorkspaceCommand.FileTime.CURRENT, null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt ws1 -newname:workspace2 -comment:comment2 -filetime:current", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_private() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, "ws1", "workspace2", "comment2", null, UpdateWorkspaceCommand.Permission.PRIVATE);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt ws1 -newname:workspace2 -comment:comment2 -permission:Private", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, "ws1", "workspace2", "comment2", null, null);
        final String message = cmd.parseOutput("", "");
        Assert.assertEquals("", message);
    }

    @Test
    public void testParseOutput_noErrors() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, "ws1", "workspace2", "comment2", null, null);
        final String output = "";
        final String message = cmd.parseOutput(output, "");
        Assert.assertEquals("", message);
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final UpdateWorkspaceCommand cmd = new UpdateWorkspaceCommand(null, "ws1", "workspace2", "comment2", null, null);
        final String message = cmd.parseOutput(null, "error");
    }
}
