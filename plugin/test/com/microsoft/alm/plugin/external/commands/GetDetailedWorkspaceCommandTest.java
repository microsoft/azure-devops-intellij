// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GetDetailedWorkspaceCommandTest extends AbstractCommandTest  {
    private final static String COLLECTION_NAME = "http://server:8080/tfs/defaultcollection";
    private final static String WORKSPACE_NAME = "workspaceName";

    @Mock
    private AuthenticationInfo mockAuthInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConstructor_nullCollection() {
        final GetDetailedWorkspaceCommand cmd = new GetDetailedWorkspaceCommand(null, WORKSPACE_NAME, mockAuthInfo);
    }

    @Test
    public void testConstructor_emptyCollection() {
        final GetDetailedWorkspaceCommand cmd = new GetDetailedWorkspaceCommand(StringUtils.EMPTY, WORKSPACE_NAME, mockAuthInfo);
    }

    @Test
    public void testConstructor_withCollection() {
        final GetDetailedWorkspaceCommand cmd = new GetDetailedWorkspaceCommand(COLLECTION_NAME, WORKSPACE_NAME, mockAuthInfo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullWorkspaceName() {
        final GetDetailedWorkspaceCommand cmd = new GetDetailedWorkspaceCommand(COLLECTION_NAME, null, mockAuthInfo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullAuthInfo() {
        final GetDetailedWorkspaceCommand cmd = new GetDetailedWorkspaceCommand(COLLECTION_NAME, WORKSPACE_NAME, null);
    }

    @Test
    public void testGetArgumentBuilder_withCollection() {
        final GetDetailedWorkspaceCommand cmd = new GetDetailedWorkspaceCommand(COLLECTION_NAME, WORKSPACE_NAME, mockAuthInfo);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspaces -noprompt -format:detailed workspaceName ******** -collection:http://server:8080/tfs/defaultcollection", builder.toString());
    }

    @Test
    public void testGetArgumentBuilder_emptyCollection() {
        final GetDetailedWorkspaceCommand cmd = new GetDetailedWorkspaceCommand(StringUtils.EMPTY, WORKSPACE_NAME, mockAuthInfo);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        Assert.assertEquals("workspaces -noprompt -format:detailed workspaceName ********", builder.toString());
    }

    @Test
    public void testParseOutput_noOutput() {
        final GetDetailedWorkspaceCommand cmd = new GetDetailedWorkspaceCommand(COLLECTION_NAME, WORKSPACE_NAME, mockAuthInfo);
        final Workspace workspace = cmd.parseOutput("", "");
        Assert.assertEquals(null, workspace);
    }

    @Test
    public void testParseOutput_noErrors() {
        final GetDetailedWorkspaceCommand cmd = new GetDetailedWorkspaceCommand(COLLECTION_NAME, WORKSPACE_NAME, mockAuthInfo);
        final String output = "\n===========================================================================================================================================================================================================\n" +
                "Workspace:   WorkspaceName\n" +
                "Owner:       John Smith\n" +
                "Computer:    computerName\n" +
                "Comment:     Workspace created through IntelliJ\n" +
                "Collection:  http://organization.visualstudio.com/\n" +
                "Permissions: Private\n" +
                "File Time:   Current\n" +
                "Location:    Local\n" +
                "File Time:   Current\n" +
                "\n" +
                "Working folders:\n" +
                "\n" +
                "$/WorkspaceName:                                         /Users/JohnSmith/WorkspaceName\n" +
                "(cloaked) $/WorkspaceName/cloaked:\n" +
                "\n";
        final Workspace workspace = cmd.parseOutput(output, "");
        Assert.assertEquals("WorkspaceName", workspace.getName());
        Assert.assertEquals("http://organization.visualstudio.com/", workspace.getServer());
        Assert.assertEquals("Workspace created through IntelliJ", workspace.getComment());
        Assert.assertEquals("computerName", workspace.getComputer());
        Assert.assertEquals("John Smith", workspace.getOwner());
        Assert.assertEquals(2, workspace.getMappings().size());
        Assert.assertEquals("/Users/JohnSmith/WorkspaceName", workspace.getMappings().get(0).getLocalPath());
        Assert.assertEquals("$/WorkspaceName", workspace.getMappings().get(0).getServerPath());
        Assert.assertEquals(false, workspace.getMappings().get(0).isCloaked());
        Assert.assertEquals(StringUtils.EMPTY, workspace.getMappings().get(1).getLocalPath());
        Assert.assertEquals("$/WorkspaceName/cloaked", workspace.getMappings().get(1).getServerPath());
        Assert.assertEquals(true, workspace.getMappings().get(1).isCloaked());
    }

    @Test(expected = RuntimeException.class)
    public void testParseOutput_errors() {
        final GetDetailedWorkspaceCommand cmd = new GetDetailedWorkspaceCommand(COLLECTION_NAME, WORKSPACE_NAME, mockAuthInfo);
        cmd.parseOutput("/path/path", "error");
    }
}
