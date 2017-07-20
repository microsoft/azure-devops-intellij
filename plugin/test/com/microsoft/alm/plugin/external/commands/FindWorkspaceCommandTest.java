// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.exceptions.ToolAuthenticationException;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.junit.Assert;
import org.junit.Test;

public class FindWorkspaceCommandTest extends AbstractCommandTest {

    @Test
    public void testConstructor_localPath() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullLocalPath() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand(null);
    }

    @Test
    public void testConstructor_collectionWorkspace() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("collectionName", "workspaceName", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullCollection() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand(null, "workspaceName" , null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullWorkspace() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("collectionName", null, null);
    }

    @Test
    public void testGetArgumentBuilder_localPath() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt");
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workfold -noprompt ********", builder.toString());
        Assert.assertEquals("/path/localfile.txt", builder.getWorkingDirectory());
    }

    @Test
    public void testGetArgumentBuilder_collectionWorkspace() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("collectionName", "workspaceName", null);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workfold -noprompt -collection:collectionName -workspace:workspaceName", builder.toString());
        Assert.assertEquals(null, builder.getWorkingDirectory());
    }

    @Test
    public void testGetArgumentBuilder_authInfo() {
        final AuthenticationInfo authInfo = new AuthenticationInfo("userName", "password", "serverUri", "user");
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("collectionName", "workspaceName", authInfo);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workfold -noprompt -collection:collectionName -workspace:workspaceName ********", builder.toString());
        Assert.assertEquals(null, builder.getWorkingDirectory());
    }

    @Test
    public void testParseOutput_noOutput() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt");
        final Workspace workspace = cmd.parseOutput("", "");
        Assert.assertEquals(null, workspace);
    }

    @Test
    public void testParseOutput_warningOutput() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt");
        final String output = "Warnings\n" +
                "Access denied connecting to TFS server https://laa018-test.visualstudio.com/ (authenticating as Personal Access Token)\n" +
                "=====================================================================================================================================================\n" +
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

    @Test
    public void testParseOutput_noErrors() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt");
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

    @Test
    public void testParseOutput_badOutput() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt");
        final String output = "Workspace:  MyWorkspace\n" +
                "Collection: http://server:8080/tfs/\n" +
                "$/project1: /path";
        final Workspace workspace = cmd.parseOutput(output, "");
        Assert.assertNull(workspace);
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt");
        final Workspace workspace = cmd.parseOutput("", "error");
    }

    @Test(expected = ToolAuthenticationException.class)
    public void testParseOutput_authError() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt");
        final Workspace workspace = cmd.parseOutput("", FindWorkspaceCommand.AUTH_ERROR_PREFIX + " http://serverName:8080/tfs");
    }
}
