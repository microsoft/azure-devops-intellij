// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.junit.Assert;
import org.junit.Test;

public class GetWorkspaceCommandTest extends AbstractCommandTest {
    @Test
    public void testConstructor_nullContext() {
        final GetWorkspaceCommand cmd = new GetWorkspaceCommand(null, "workspaceName");
    }

    @Test
    public void testConstructor_withContext() {
        final GetWorkspaceCommand cmd = new GetWorkspaceCommand(context, "workspaceName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final GetWorkspaceCommand cmd = new GetWorkspaceCommand(null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final GetWorkspaceCommand cmd = new GetWorkspaceCommand(context, "workspaceName");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspaces -noprompt -collection:http://server:8080/tfs/defaultcollection ******** workspaceName -format:xml", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final GetWorkspaceCommand cmd = new GetWorkspaceCommand(null, "workspaceName");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspaces -noprompt workspaceName -format:xml", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final GetWorkspaceCommand cmd = new GetWorkspaceCommand(null, "workspaceName");
        final Workspace workspace = cmd.parseOutput("", "");
        Assert.assertEquals(null, workspace);
    }

    @Test
    public void testParseOutput_noErrors() {
        final GetWorkspaceCommand cmd = new GetWorkspaceCommand(null, "workspaceName");
        final String output = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<workspaces>\n" +
                "  <workspace name=\"workspaceName\" owner=\"username\" computer=\"machine\" comment=\"description\" server=\"http://server:8080/tfs/\">\n" +
                "    <working-folder server-item=\"$/TeamProject\" local-item=\"D:\\project1\" type=\"map\" depth=\"full\"/>\n" +
                "  </workspace>\n" +
                "</workspaces>";
        final Workspace workspace = cmd.parseOutput(output, "");
        Assert.assertEquals("workspaceName", workspace.getName());
        Assert.assertEquals("http://server:8080/tfs/", workspace.getServerDisplayName());
        Assert.assertEquals("description", workspace.getComment());
        Assert.assertEquals("machine", workspace.getComputer());
        Assert.assertEquals("username", workspace.getOwner());
        Assert.assertEquals(1, workspace.getMappings().size());
        Assert.assertEquals("D:\\project1", workspace.getMappings().get(0).getLocalPath());
        Assert.assertEquals("$/TeamProject", workspace.getMappings().get(0).getServerPath());
        Assert.assertEquals(false, workspace.getMappings().get(0).isCloaked());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final GetWorkspaceCommand cmd = new GetWorkspaceCommand(null, "workspaceName");
        final Workspace workspace = cmd.parseOutput("/path/path", "error");
    }
}
