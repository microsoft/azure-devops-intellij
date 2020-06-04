// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.exceptions.ToolAuthenticationException;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.models.WorkspaceInformation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;

public class FindWorkspaceCommandTest extends AbstractCommandTest {

    @Test
    public void testConstructor_localPath() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt", null, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullLocalPath() {
        @SuppressWarnings("ConstantConditions") FindWorkspaceCommand cmd = new FindWorkspaceCommand(null, null, true);
    }

    @Test
    public void testConstructor_collectionWorkspace() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("collectionName", "workspaceName", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullCollection() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand(null, "workspaceName", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullWorkspace() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("collectionName", null, null);
    }

    @Test
    public void testGetArgumentBuilder_localPath() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt", null, true);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workfold -noprompt -login:username,pw", builder.toString());
        Assert.assertEquals("/path/localfile.txt", builder.getWorkingDirectory());
    }

    @Test
    public void findWorkspaceCommandShouldOnlyBeCachedWithoutLocalPathDefined() {
        Assert.assertFalse(new FindWorkspaceCommand("/tmp", null, true).shouldPrepareCachedRunner());
        Assert.assertTrue(new FindWorkspaceCommand("collection", "workspace", null).shouldPrepareCachedRunner());
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
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt", null, true);
        WorkspaceInformation workspace = cmd.parseOutput("", "");
        Assert.assertEquals(null, workspace);
    }

    @Test
    public void testParseOutput_warningOutput() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt", null, true);
        final String output = "Warnings\n" +
                "Access denied connecting to TFS server https://laa018-test.visualstudio.com/ (authenticating as Personal Access Token)\n" +
                "=====================================================================================================================================================\n" +
                "Workspace:  MyWorkspace\n" +
                "Collection: http://server:8080/tfs/\n" +
                "$/project1: /path";
        Workspace workspace = Objects.requireNonNull(cmd.parseOutput(output, "").getDetailed());
        Assert.assertEquals("MyWorkspace", workspace.getName());
        Assert.assertEquals("http://server:8080/tfs/", workspace.getServerDisplayName());
        Assert.assertEquals("", workspace.getComment());
        Assert.assertEquals("", workspace.getComputer());
        Assert.assertEquals("", workspace.getOwner());
        Assert.assertEquals(1, workspace.getMappings().size());
    }

    @Test
    public void testParseOutput_noErrors() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt", null, true);
        final String output = "=====================================================================================================================================================\n" +
                "Workspace:  MyWorkspace\n" +
                "Collection: http://server:8080/tfs/\n" +
                "$/project1: /path";
        Workspace workspace = Objects.requireNonNull(cmd.parseOutput(output, "").getDetailed());
        Assert.assertEquals("MyWorkspace", workspace.getName());
        Assert.assertEquals("http://server:8080/tfs/", workspace.getServerDisplayName());
        Assert.assertEquals("", workspace.getComment());
        Assert.assertEquals("", workspace.getComputer());
        Assert.assertEquals("", workspace.getOwner());
        Assert.assertEquals(1, workspace.getMappings().size());
    }

    @Test
    public void testParseOutput_badOutput() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt", null, true);
        final String output = "Workspace:  MyWorkspace\n" +
                "Collection: http://server:8080/tfs/\n" +
                "$/project1: /path";
        WorkspaceInformation workspace = cmd.parseOutput(output, "");
        Assert.assertNull(workspace);
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt", null, true);
        cmd.parseOutput("", "error");
    }

    @Test(expected = ToolAuthenticationException.class)
    public void testParseOutput_authServerError() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt", null, false);
        cmd.parseOutput("", FindWorkspaceCommand.AUTH_ERROR_SERVER + " http://serverName:8080/tfs");
    }

    @Test(expected = ToolAuthenticationException.class)
    public void testParseOutput_authFederatedError() {
        final FindWorkspaceCommand cmd = new FindWorkspaceCommand("/path/localfile.txt", null, false);
        cmd.parseOutput("", "An error occurred: " + FindWorkspaceCommand.AUTH_ERROR_FEDERATED + ": http://serverName:8080/tfs");
    }
}
