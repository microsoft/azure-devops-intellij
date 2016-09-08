// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import org.junit.Assert;
import org.junit.Test;

public class DeleteWorkspaceCommandTest extends AbstractCommandTest {
    @Test
    public void testConstructor_nullContext() {
        final DeleteWorkspaceCommand cmd = new DeleteWorkspaceCommand(null, "ws1");
    }

    @Test
    public void testConstructor_withContext() {
        final DeleteWorkspaceCommand cmd = new DeleteWorkspaceCommand(context, "ws1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final DeleteWorkspaceCommand cmd = new DeleteWorkspaceCommand(null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final DeleteWorkspaceCommand cmd = new DeleteWorkspaceCommand(context, "ws1");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt -collection:http://server:8080/tfs/defaultcollection ******** -delete ws1", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final DeleteWorkspaceCommand cmd = new DeleteWorkspaceCommand(null, "ws1");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspace -noprompt -delete ws1", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final DeleteWorkspaceCommand cmd = new DeleteWorkspaceCommand(null, "ws1");
        final String message = cmd.parseOutput("", "");
        Assert.assertEquals("", message);
    }

    @Test
    public void testParseOutput_noErrors() {
        final DeleteWorkspaceCommand cmd = new DeleteWorkspaceCommand(null, "ws1");
        final String output = "";
        final String message = cmd.parseOutput(output, "");
        Assert.assertEquals("", message);
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final DeleteWorkspaceCommand cmd = new DeleteWorkspaceCommand(null, "ws1");
        final String message = cmd.parseOutput(null, "error");
    }

    @Test
    public void testParseOutput_workspaceNotFoundError() {
        final DeleteWorkspaceCommand cmd = new DeleteWorkspaceCommand(null, "ws1");
        // Make sure the not found error is ignored (i.e. no exception)
        final String message = cmd.parseOutput(null, "The workspace 'ws1' could not be found");
    }

    @Test
    public void testInterpretReturnCode() {
        final DeleteWorkspaceCommand cmd = new DeleteWorkspaceCommand(null, "ws1");
        Assert.assertEquals(0, cmd.interpretReturnCode(0));
        Assert.assertEquals(-1, cmd.interpretReturnCode(-1));
        Assert.assertEquals(0, cmd.interpretReturnCode(100));
        Assert.assertEquals(1, cmd.interpretReturnCode(1));
    }


}
