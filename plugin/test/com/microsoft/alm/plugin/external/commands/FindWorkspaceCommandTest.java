// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.junit.Assert;
import org.junit.Test;

public class FindWorkspaceCommandTest extends AbstractCommandTest {

    @Test
    public void testConstructor_nullContext() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand(null, "/path/localfile.txt");
    }

    @Test
    public void testConstructor_withContext() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand(context, "/path/localfile.txt");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullArgs() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand(null, null);
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand(context, "/path/localfile.txt");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workfold -noprompt -collection:http://server:8080/tfs/defaultcollection ******** /path/localfile.txt", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_nullContext() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand(null, "/path/localfile.txt");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workfold -noprompt /path/localfile.txt", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand(null, "/path/localfile.txt");
        final Workspace workspace = cmd.parseOutput("", "");
        Assert.assertEquals(null, workspace);
    }

    @Test
    public void testParseOutput_noErrors() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand(null, "/path/localfile.txt");
        final String output = "=====================================================================================================================================================\n" +
                "Workspace:  MyWorkspace\n" +
                "Collection: http://server:8080/tfs/\n" +
                "$/project1: /path";
        final Workspace workspace = cmd.parseOutput(output, "");
        Assert.assertEquals("MyWorkspace", workspace.getName());
        Assert.assertEquals("http://server:8080/tfs/", workspace.getServer());
        Assert.assertEquals("", workspace.getComment());
        Assert.assertEquals("", workspace.getComputer());
        Assert.assertEquals("", workspace.getOwner());
        Assert.assertEquals(1, workspace.getMappings().size());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand(null, "/path/localfile.txt");
        final Workspace workspace = cmd.parseOutput("", "error");
    }
}
