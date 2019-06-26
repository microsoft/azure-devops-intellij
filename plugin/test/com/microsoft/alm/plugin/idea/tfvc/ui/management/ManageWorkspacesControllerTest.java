// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.management;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.external.models.Server;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.awt.event.ActionEvent;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ManageWorkspacesController.class})
@PowerMockIgnore("javax.swing.*")
public class ManageWorkspacesControllerTest {
    @Mock
    private Project mockProject;

    @Mock
    private ManageWorkspacesModel mockModel;

    @Mock
    private ManageWorkspacesDialog mockDialog;

    @Mock
    private Server mockServer;

    @Mock
    private Workspace mockWorkspace;

    private ManageWorkspacesController controller;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockStatic(ManageWorkspacesController.class);
        whenNew(ManageWorkspacesModel.class).withAnyArguments().thenReturn(mockModel);
        whenNew(ManageWorkspacesDialog.class).withAnyArguments().thenReturn(mockDialog);

        controller = new ManageWorkspacesController(mockProject);
    }

    @Test
    public void testConstructor() {
        verify(mockModel, times(1)).getContextProvider();
        verify(mockDialog, times(1)).addActionListener(controller);
        verify(mockModel, times(1)).addObserver(controller);
        verifyNoMoreInteractions(mockDialog);
        verifyNoMoreInteractions(mockModel);
    }

    @Test
    public void testActionPerformed_Reload() {
        final ActionEvent e = new ActionEvent(this, 1, ManageWorkspacesForm.CMD_RELOAD_WORKSPACES);
        when(mockDialog.getSelectedServer()).thenReturn(mockServer);

        controller.actionPerformed(e);
        verify(mockModel, times(1)).reloadWorkspacesWithProgress(mockServer);
    }

    @Test
    public void testActionPerformed_Proxy() {
        final ActionEvent e = new ActionEvent(this, 1, ManageWorkspacesForm.CMD_EDIT_PROXY);
        when(mockDialog.getSelectedServer()).thenReturn(mockServer);

        controller.actionPerformed(e);
        verify(mockModel, times(1)).editProxy(mockServer);
    }

    @Test
    public void testActionPerformed_Delete() {
        final ActionEvent e = new ActionEvent(this, 1, ManageWorkspacesForm.CMD_DELETE_WORKSPACE);
        when(mockDialog.getSelectedWorkspace()).thenReturn(mockWorkspace);

        controller.actionPerformed(e);
        verify(mockModel, times(1)).deleteWorkspaceWithProgress(mockWorkspace);
    }

    @Test
    public void testActionPerformed_Edit() {
        final ActionEvent e = new ActionEvent(this, 1, ManageWorkspacesForm.CMD_EDIT_WORKSPACE);
        when(mockDialog.getSelectedWorkspace()).thenReturn(mockWorkspace);

        controller.actionPerformed(e);
        verify(mockModel, times(1)).editWorkspaceWithProgress(eq(mockWorkspace), any(Runnable.class));
    }

    @Test
    public void testUpdate_RefreshServer() {
        reset(mockDialog);
        when(mockDialog.getSelectedServer()).thenReturn(mockServer);
        controller.update(null, ManageWorkspacesModel.REFRESH_SERVER);
        verify(mockDialog, times(1)).getSelectedServer();
        verify(mockDialog, times(1)).updateControls(mockServer);
        verifyNoMoreInteractions(mockDialog);
    }

    @Test
    public void testUpdate_RefreshWorkspace() {
        reset(mockDialog);
        when(mockDialog.getSelectedWorkspace()).thenReturn(mockWorkspace);
        controller.update(null, ManageWorkspacesModel.REFRESH_WORKSPACE);
        verify(mockDialog, times(1)).getSelectedWorkspace();
        verify(mockDialog, times(1)).updateControls(mockWorkspace);
        verifyNoMoreInteractions(mockDialog);
    }
}