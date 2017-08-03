// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Server;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class GetAllWorkspacesCommandTest extends AbstractCommandTest {

    @Test
    public void testConstructor_noContext() {
        final GetAllWorkspacesCommand cmd = new GetAllWorkspacesCommand();
    }

    @Test
    public void testConstructor_withContext() {
        final GetAllWorkspacesCommand cmd = new GetAllWorkspacesCommand(context);
    }

    @Test
    public void testConstructor_withAuthInfo() {
        final AuthenticationInfo authInfo = mock(AuthenticationInfo.class);
        final String serverUrl = "http://account.visualstudio.com/";
        final GetAllWorkspacesCommand cmd = new GetAllWorkspacesCommand(authInfo, serverUrl);
    }

    @Test
    public void testGetArgumentBuilder_noContext() {
        final GetAllWorkspacesCommand cmd = new GetAllWorkspacesCommand();
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        assertEquals("workspaces -noprompt", builder.toString());
        assertEquals(null, builder.getWorkingDirectory());
    }

    @Test
    public void testGetArgumentBuilder_withContext() {
        final GetAllWorkspacesCommand cmd = new GetAllWorkspacesCommand(context);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        assertEquals("workspaces -noprompt -collection:http://server:8080/tfs/defaultcollection ********", builder.toString());
        assertEquals(null, builder.getWorkingDirectory());
    }

    @Test
    public void testGetArgumentBuilder_withAuthInfo() {
        final AuthenticationInfo authInfo = mock(AuthenticationInfo.class);
        final String serverUrl = "http://account.visualstudio.com/";
        final GetAllWorkspacesCommand cmd = new GetAllWorkspacesCommand(authInfo, serverUrl);
        final ToolRunner.ArgumentBuilder builder = cmd.getArgumentBuilder();
        assertEquals("workspaces -noprompt ******** -collection:http://account.visualstudio.com/", builder.toString());
        assertEquals(null, builder.getWorkingDirectory());
    }

    @Test
    public void testParseOutput_noOutput() {
        final GetAllWorkspacesCommand cmd = new GetAllWorkspacesCommand();
        final List<Server> servers = cmd.parseOutput("", "");
        assertEquals(0, servers.size());
    }

    @Test
    public void testParseOutput_noErrors() {
        final GetAllWorkspacesCommand cmd = new GetAllWorkspacesCommand();
        final String output = "Collection: http://acount1:8080/tfs/DefaultCollection/\n" +
                "Workspace           Owner      Computer            Comment\n" +
                "------------------- ---------- ------------------- --------------------------------------------------------------------------------------------------------------------------------\n" +
                "account1_workspace1 John Smith computerName\n" +
                "\n" +
                "Collection: http://account2:8081/tfs/defaultcollection/\n" +
                "Workspace                       Owner      Computer     Comment\n" +
                "------------------------------- ---------- ------------ -------------------------------------------------------------------------------------------------------------------------------\n" +
                "account2_workspace1             John Smith computerName Automatically created by IntelliJ IDEA for '$/tfsTest'\n" +
                "account2_workspace2             John Smith computerName\n" +
                "account2_workspace3             John Smith computerName Automatically created by IntelliJ IDEA for '$/tfsTest'\n" +
                "account2_workspace4             John Smith computerName Automatically created by IntelliJ IDEA for '$/tfsTest'\n" +
                "account2_workspace5             John Smith computerName\n" +
                "account2_workspace6             John Smith computerName Automatically created by IntelliJ IDEA for '$/tfsTest'\n" +
                "account2_workspace7             John Smith computerName Workspace created through IntelliJ\n" +
                "account2_workspace8             John Smith computerName Workspace created through IntelliJ but changed by TFS\n" +
                "account2_workspace9999999999999 John Smith computerName Workspace created through IntelliJ\n" +
                "\n" +
                "Collection: https://account3.visualstudio.com/\n" +
                "Workspace      Owner      Computer     Comment\n" +
                "-------------- ---------- ------------ ------------------------------------------------------------------------------------------------------------------------------------------------\n" +
                "TFVC_11        John Smith computerName Workspace created through IntelliJ\n" +
                "TFVC_11111     John Smith computerName Workspace created through IntelliJ\n" +
                "TFVC_11dfdfdf  John Smith computerName Workspace created through IntelliJ\n" +
                "\n" +
                "Collection: https://demo.visualstudio.com/\n" +
                "Workspace Owner      Computer     Comment\n" +
                "--------- ---------- ------------ ---------------------------------------------------------------------------------------------------------------------------------------------------\n" +
                "newWorksp John Smith computerName";

        final List<Server> servers = cmd.parseOutput(output, "");
        assertEquals(4, servers.size());

        // first server
        assertEquals("http://acount1:8080/tfs/DefaultCollection/", servers.get(0).getName());
        assertEquals(1, servers.get(0).getWorkspaces().size());
        checkWorkspace("account1_workspace1", "John Smith", "computerName", StringUtils.EMPTY, servers.get(0).getWorkspaces().get(0));
        // second server
        assertEquals("http://account2:8081/tfs/defaultcollection/", servers.get(1).getName());
        assertEquals(9, servers.get(1).getWorkspaces().size());
        checkWorkspace("account2_workspace1", "John Smith", "computerName", "Automatically created by IntelliJ IDEA for '$/tfsTest'", servers.get(1).getWorkspaces().get(0));
        checkWorkspace("account2_workspace2", "John Smith", "computerName", StringUtils.EMPTY, servers.get(1).getWorkspaces().get(1));
        checkWorkspace("account2_workspace3", "John Smith", "computerName", "Automatically created by IntelliJ IDEA for '$/tfsTest'", servers.get(1).getWorkspaces().get(2));
        checkWorkspace("account2_workspace4", "John Smith", "computerName", "Automatically created by IntelliJ IDEA for '$/tfsTest'", servers.get(1).getWorkspaces().get(3));
        checkWorkspace("account2_workspace5", "John Smith", "computerName", StringUtils.EMPTY, servers.get(1).getWorkspaces().get(4));
        checkWorkspace("account2_workspace6", "John Smith", "computerName", "Automatically created by IntelliJ IDEA for '$/tfsTest'", servers.get(1).getWorkspaces().get(5));
        checkWorkspace("account2_workspace7", "John Smith", "computerName", "Workspace created through IntelliJ", servers.get(1).getWorkspaces().get(6));
        checkWorkspace("account2_workspace8", "John Smith", "computerName", "Workspace created through IntelliJ but changed by TFS", servers.get(1).getWorkspaces().get(7));
        checkWorkspace("account2_workspace9999999999999", "John Smith", "computerName", "Workspace created through IntelliJ", servers.get(1).getWorkspaces().get(8));
        // third server
        assertEquals("https://account3.visualstudio.com/", servers.get(2).getName());
        assertEquals(3, servers.get(2).getWorkspaces().size());
        checkWorkspace("TFVC_11", "John Smith", "computerName", "Workspace created through IntelliJ", servers.get(2).getWorkspaces().get(0));
        checkWorkspace("TFVC_11111", "John Smith", "computerName", "Workspace created through IntelliJ", servers.get(2).getWorkspaces().get(1));
        checkWorkspace("TFVC_11dfdfdf", "John Smith", "computerName", "Workspace created through IntelliJ", servers.get(2).getWorkspaces().get(2));
        // fourth server
        assertEquals("https://demo.visualstudio.com/", servers.get(3).getName());
        assertEquals(1, servers.get(3).getWorkspaces().size());
        checkWorkspace("newWorksp", "John Smith", "computerName", StringUtils.EMPTY, servers.get(3).getWorkspaces().get(0));
    }

    private void checkWorkspace(final String workspaceName, final String owner, final String computer, final String comment, final Workspace workspace) {
        assertEquals(workspaceName, workspace.getName());
        assertEquals(owner, workspace.getOwner());
        assertEquals(computer, workspace.getComputer());
        assertEquals(comment, workspace.getComment());
    }
}
