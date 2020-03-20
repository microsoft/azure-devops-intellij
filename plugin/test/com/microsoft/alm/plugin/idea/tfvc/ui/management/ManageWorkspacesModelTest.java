// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.management;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.exceptions.ToolAuthenticationException;
import com.microsoft.alm.plugin.external.models.Server;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.external.utils.WorkspaceHelper;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.mocks.MockObserver;
import com.microsoft.alm.plugin.idea.common.ui.common.treetable.ContentProvider;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.tfvc.ui.ProxySettingsDialog;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProgressManager.class, VcsUtil.class, Messages.class, ServerContextManager.class, CommandUtils.class,
        IdeaHelper.class, ManageWorkspacesModel.class, WorkspaceHelper.class})
@PowerMockIgnore("javax.swing.*")
public class ManageWorkspacesModelTest extends IdeaAbstractTest {

    @Mock
    private Project mockProject;

    @Mock
    private Runnable mockRunnable;

    @Mock
    private ProgressManager mockProgressManager;

    @Mock
    private ServerContext mockServerContext;

    @Mock
    private ServerContextManager mockServerContextManager;

    @Mock
    private ProxySettingsDialog mockProxySettingsDialog;

    private ManageWorkspacesModel manageWorkspacesModel;
    private Workspace workspace1;
    private List<Workspace> workspaces;
    private Server server;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(ProgressManager.class, VcsUtil.class, Messages.class, ServerContextManager.class,
                CommandUtils.class, IdeaHelper.class, ManageWorkspacesModel.class, WorkspaceHelper.class);

        workspace1 = new Workspace("http://server:8080/tfs/defaultcollection", "workspace1", "computerName",
                "ownerName", "comment for workspace", ImmutableList.of(new Workspace.Mapping("$/root", "/local/path/directory", false)));
        workspaces = ImmutableList.of(workspace1);
        server = new Server("http://server:8080/tfs/defaultcollection", workspaces);

        when(mockServerContextManager.createContextFromTfvcServerUrl(workspace1.getServerDisplayName(), "root", true)).thenReturn(mockServerContext);
        when(ServerContextManager.getInstance()).thenReturn(mockServerContextManager);
        when(ProgressManager.getInstance()).thenReturn(mockProgressManager);

        manageWorkspacesModel = spy(new ManageWorkspacesModel(mockProject));
    }

    @Test
    public void testReloadWorkspacesWithProgress_Happy() throws Exception {
        final MockObserver observer = new MockObserver(manageWorkspacesModel);
        manageWorkspacesModel.reloadWorkspacesWithProgress(server);

        observer.assertAndClearLastUpdate(manageWorkspacesModel, ManageWorkspacesModel.REFRESH_SERVER);
        verifyStatic(never());
        Messages.showErrorDialog(any(Project.class), any(String.class), any(String.class));
    }

    @Test
    public void testReloadWorkspacesWithProgress_Exception() throws Exception {
        final MockObserver observer = new MockObserver(manageWorkspacesModel);
        when(VcsUtil.runVcsProcessWithProgress(any(VcsRunnable.class), eq(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_RELOAD_MSG, server.getName())), eq(true), eq(mockProject)))
                .thenThrow(new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_RELOAD_ERROR_MSG, server.getName())));
        manageWorkspacesModel.reloadWorkspacesWithProgress(server);

        observer.assertAndClearLastUpdate(manageWorkspacesModel, ManageWorkspacesModel.REFRESH_SERVER);
        verifyStatic(times(1));
        Messages.showErrorDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_RELOAD_ERROR_MSG, server.getName())),
                eq(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_RELOAD_ERROR_TITLE)));
    }

    @Test
    public void testReloadWorkspaces_Happy() throws Exception {
        final AuthenticationInfo mockAuthInfo = mock(AuthenticationInfo.class);
        when(mockServerContextManager.getAuthenticationInfo(server.getName(), true)).thenReturn(mockAuthInfo);

        manageWorkspacesModel.reloadWorkspaces(server);
        verifyStatic(times(1));
        CommandUtils.refreshWorkspacesForServer(mockAuthInfo, server.getName());
    }

    @Test(expected = VcsException.class)
    public void testReloadWorkspaces_NullAuth() throws Exception {
        when(mockServerContextManager.getAuthenticationInfo(server.getName(), true)).thenReturn(null);

        manageWorkspacesModel.reloadWorkspaces(server);
    }

    @Test(expected = VcsException.class)
    public void testReloadWorkspaces_Exception() throws Exception {
        doThrow(new RuntimeException()).when(manageWorkspacesModel).getPartialWorkspace(server.getName(), workspace1.getName());

        manageWorkspacesModel.reloadWorkspaces(server);
    }


    @Test
    public void testDeleteWorkspaceWithProgress_UserCancel() throws Exception {
        final MockObserver observer = new MockObserver(manageWorkspacesModel);
        when(Messages.showYesNoDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_MSG, workspace1.getName()),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_TITLE), Messages.getWarningIcon())).thenReturn(Messages.NO);
        manageWorkspacesModel.deleteWorkspaceWithProgress(workspace1);

        observer.assertUpdateNeverOccurred(ManageWorkspacesModel.REFRESH_WORKSPACE);
        verifyStatic(never());
        VcsUtil.runVcsProcessWithProgress(any(VcsRunnable.class), any(String.class), any(Boolean.class), any(Project.class));
        Messages.showErrorDialog(any(Project.class), any(String.class), any(String.class));
    }

    @Test
    public void testDeleteWorkspaceWithProgress_Happy() throws Exception {
        final MockObserver observer = new MockObserver(manageWorkspacesModel);
        when(Messages.showYesNoDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_MSG, workspace1.getName()),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_TITLE), Messages.getWarningIcon())).thenReturn(Messages.YES);
        manageWorkspacesModel.deleteWorkspaceWithProgress(workspace1);

        observer.assertAndClearLastUpdate(manageWorkspacesModel, ManageWorkspacesModel.REFRESH_WORKSPACE);
        verifyStatic(never());
        Messages.showErrorDialog(any(Project.class), any(String.class), any(String.class));
    }

    @Test
    public void testDeleteWorkspaceWithProgress_Exception() throws Exception {
        final MockObserver observer = new MockObserver(manageWorkspacesModel);
        when(Messages.showYesNoDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_MSG, workspace1.getName()),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_TITLE), Messages.getWarningIcon())).thenReturn(Messages.YES);
        when(VcsUtil.runVcsProcessWithProgress(any(VcsRunnable.class), eq(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_MSG, workspace1.getName())), eq(true), eq(mockProject)))
                .thenThrow(new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_ERROR_MSG, workspace1.getName())));
        manageWorkspacesModel.deleteWorkspaceWithProgress(workspace1);

        observer.assertAndClearLastUpdate(manageWorkspacesModel, ManageWorkspacesModel.REFRESH_WORKSPACE);
        verifyStatic(times(1));
        Messages.showErrorDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_ERROR_MSG, workspace1.getName())),
                eq(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DELETE_ERROR_TITLE)));
    }

    @Test
    public void testDeleteWorkspace_Happy() throws Exception {
        doReturn(workspace1).when(manageWorkspacesModel).getPartialWorkspace(server.getName(), workspace1.getName());

        manageWorkspacesModel.deleteWorkspace(workspace1);
        verifyStatic(times(1));
        CommandUtils.deleteWorkspace(mockServerContext, workspace1.getName());
    }

    @Test(expected = VcsException.class)
    public void testDeleteWorkspace_NullWorkspace() throws Exception {
        doReturn(null).when(manageWorkspacesModel).getPartialWorkspace(server.getName(), workspace1.getName());

        manageWorkspacesModel.deleteWorkspace(workspace1);
    }

    @Test(expected = VcsException.class)
    public void testDeleteWorkspace_Exception() throws Exception {
        doThrow(new RuntimeException()).when(manageWorkspacesModel).getPartialWorkspace(server.getName(), workspace1.getName());

        manageWorkspacesModel.deleteWorkspace(workspace1);
    }

    @Test
    public void testEditWorkspaceWithProgress_Happy() throws Exception {
        manageWorkspacesModel.editWorkspaceWithProgress(workspace1, mockRunnable);

        verifyStatic(never());
        Messages.showErrorDialog(any(Project.class), any(String.class), any(String.class));
    }

    @Test
    public void testEditWorkspaceWithProgress_Exception() throws Exception {
        when(VcsUtil.runVcsProcessWithProgress(any(VcsRunnable.class), eq(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_MSG, workspace1.getName())), eq(true), eq(mockProject)))
                .thenThrow(new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_MSG, workspace1.getName())));
        manageWorkspacesModel.editWorkspaceWithProgress(workspace1, mockRunnable);

        verifyStatic(times(1));
        Messages.showErrorDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_MSG, workspace1.getName())),
                eq(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_TITLE)));
    }

    @Test
    public void testEditWorkspace_Happy() throws Exception {
        final AuthenticationInfo mockAuthInfo = mock(AuthenticationInfo.class);
        when(mockServerContextManager.getAuthenticationInfo(server.getName(), true)).thenReturn(mockAuthInfo);
        when(CommandUtils.getDetailedWorkspace(server.getName(), workspace1.getName(), mockAuthInfo)).thenReturn(workspace1);

        manageWorkspacesModel.editWorkspace(workspace1, mockRunnable);
        verifyStatic(times(1));
        IdeaHelper.runOnUIThread(any(Runnable.class), eq(true));
    }

    @Test(expected = VcsException.class)
    public void testEditWorkspace_NullWorkspace() throws Exception {
        when(CommandUtils.getDetailedWorkspace(eq(server.getName()), eq(workspace1.getName()), any(AuthenticationInfo.class))).thenReturn(null);

        manageWorkspacesModel.editWorkspace(workspace1, mockRunnable);
    }

    @Test(expected = VcsException.class)
    public void testEditWorkspace_NullContext() throws Exception {
        doReturn(workspace1).when(manageWorkspacesModel).getPartialWorkspace(server.getName(), workspace1.getName());
        when(mockServerContextManager.createContextFromTfvcServerUrl(workspace1.getServerDisplayName(), "root", true)).thenReturn(null);

        manageWorkspacesModel.editWorkspace(workspace1, mockRunnable);
    }

    @Test(expected = VcsException.class)
    public void testEditWorkspace_Exception() throws Exception {
        doThrow(new RuntimeException()).when(manageWorkspacesModel).getPartialWorkspace(server.getName(), workspace1.getName());

        manageWorkspacesModel.editWorkspace(workspace1, mockRunnable);
    }

    @Test
    public void testEditProxy_Changed() throws Exception {
        final MockObserver observer = new MockObserver(manageWorkspacesModel);
        when(WorkspaceHelper.getProxyServer(server.getName())).thenReturn(StringUtils.EMPTY);
        doNothing().when(WorkspaceHelper.class, "setProxyServer", anyString(), anyString());
        when(mockProxySettingsDialog.showAndGet()).thenReturn(true);
        when(mockProxySettingsDialog.getProxyUri()).thenReturn("http://testUri.com");
        whenNew(ProxySettingsDialog.class).withArguments(mockProject, server.getName(), StringUtils.EMPTY).thenReturn(mockProxySettingsDialog);

        manageWorkspacesModel.editProxy(server);
        observer.assertAndClearLastUpdate(manageWorkspacesModel, ManageWorkspacesModel.REFRESH_SERVER);
        verifyStatic(times(1));
        WorkspaceHelper.setProxyServer(server.getName(), "http://testUri.com");
    }

    @Test
    public void testEditProxy_Canceled() throws Exception {
        final MockObserver observer = new MockObserver(manageWorkspacesModel);
        when(WorkspaceHelper.getProxyServer(server.getName())).thenReturn(StringUtils.EMPTY);
        when(mockProxySettingsDialog.showAndGet()).thenReturn(false);
        whenNew(ProxySettingsDialog.class).withArguments(mockProject, server.getName(), StringUtils.EMPTY).thenReturn(mockProxySettingsDialog);

        manageWorkspacesModel.editProxy(server);
        observer.assertUpdateNeverOccurred(ManageWorkspacesModel.REFRESH_SERVER);
        verify(mockProxySettingsDialog, never()).getProxyUri();
        verifyStatic(never());
        WorkspaceHelper.setProxyServer(server.getName(), "http://testUri.com");
    }

    @Test
    public void testGetPartialWorkspace_Local() {
        when(CommandUtils.getPartialWorkspace(server.getName(), workspace1.getName())).thenReturn(workspace1);
        assertEquals(workspace1, manageWorkspacesModel.getPartialWorkspace(server.getName(), workspace1.getName()));
    }

    @Test
    public void testGetPartialWorkspace_Server() {
        final AuthenticationInfo mockAuthInfo = mock(AuthenticationInfo.class);
        when(CommandUtils.getPartialWorkspace(server.getName(), workspace1.getName())).thenThrow(new ToolAuthenticationException());
        when(CommandUtils.getPartialWorkspace(server.getName(), workspace1.getName(), mockAuthInfo)).thenReturn(workspace1);
        when(mockServerContextManager.getAuthenticationInfo(server.getName(), true)).thenReturn(mockAuthInfo);
        assertEquals(workspace1, manageWorkspacesModel.getPartialWorkspace(server.getName(), workspace1.getName()));
    }

    @Test
    public void testServerWorkspaceContentProvider_getRoots() {
        List<Server> servers = ImmutableList.of(server, new Server("server2", Collections.EMPTY_LIST), new Server("aServer",
                ImmutableList.of(new Workspace("server3", "workspace2", "computerName",
                        "ownerName", "comment for workspace", ImmutableList.of(new Workspace.Mapping("$/root", "/local/path/directory", false))))));
        ContentProvider<Object> provider = manageWorkspacesModel.getContextProvider();
        when(CommandUtils.getAllWorkspaces(null)).thenReturn(servers);

        Object[] returnedList = provider.getRoots().toArray();
        assertEquals(3, returnedList.length);
        assertEquals("aServer", ((Server) returnedList[0]).getName());
        assertEquals(server.getName(), ((Server) returnedList[1]).getName());
        assertEquals("server2", ((Server) returnedList[2]).getName());
    }

    @Test
    public void testServerWorkspaceContentProvider_getChildren() {
        Server server = new Server("server", ImmutableList.of(workspace1, new Workspace("server", "workspace0", "computerName",
                        "ownerName", "comment for workspace", ImmutableList.of(new Workspace.Mapping("$/root", "/local/path/directory", false))),
                new Workspace("server", "aWorkspace", "computerName",
                        "ownerName", "comment for workspace", ImmutableList.of(new Workspace.Mapping("$/root", "/local/path/directory", false)))));
        ContentProvider<Object> provider = manageWorkspacesModel.getContextProvider();

        Object[] returnedList = provider.getChildren(server).toArray();
        assertEquals(3, returnedList.length);
        assertEquals("aWorkspace", ((Workspace) returnedList[0]).getName());
        assertEquals("workspace0", ((Workspace) returnedList[1]).getName());
        assertEquals("workspace1", ((Workspace) returnedList[2]).getName());
    }

    @Test
    public void testServerWorkspaceContentProvider_getChildren_Error() {
        ContentProvider<Object> provider = manageWorkspacesModel.getContextProvider();

        Collection returnedList = provider.getChildren(mockProgressManager);
        assertEquals(0, returnedList.size());
    }
}