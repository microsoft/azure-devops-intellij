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
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.awt.event.ActionEvent;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ManageWorkspacesControllerTest {
    @Mock
    private Project mockProject;

    @Mock
    private Server mockServer;

    @Mock
    private Workspace mockWorkspace;

    private ManageWorkspacesController controller;

    @Mock
    private MockedStatic<ManageWorkspacesController> manageWorkspacesControllerStatic;
    
    @Mock
    private MockedConstruction<ManageWorkspacesModel> manageWorkspacesModelConstruction;

    @Mock
    private MockedConstruction<ManageWorkspacesDialog> manageWorkspacesDialogConstruction;
    
    @Before
    public void setUp() throws Exception {
        controller = new ManageWorkspacesController(mockProject);
    }
    
    private ManageWorkspacesModel getMockModel() {
        var models = manageWorkspacesModelConstruction.constructed();
        assertEquals(1, models.size());
        return models.get(0);
    }

    private ManageWorkspacesDialog getMockDialog() {
        var dialogs = manageWorkspacesDialogConstruction.constructed();
        assertEquals(1, dialogs.size());
        return dialogs.get(0);
    }

    @Test
    public void testConstructor() {
        verify(getMockModel(), times(1)).getContextProvider();
        verify(getMockDialog(), times(1)).addActionListener(controller);
        verify(getMockModel(), times(1)).addObserver(controller);
        verifyNoMoreInteractions(getMockDialog());
        verifyNoMoreInteractions(getMockModel());
    }

    @Test
    public void testActionPerformed_Reload() {
        final ActionEvent e = new ActionEvent(this, 1, ManageWorkspacesForm.CMD_RELOAD_WORKSPACES);
        when(getMockDialog().getSelectedServer()).thenReturn(mockServer);

        controller.actionPerformed(e);
        verify(getMockModel(), times(1)).reloadWorkspacesWithProgress(mockServer);
    }

    @Test
    public void testActionPerformed_Proxy() {
        final ActionEvent e = new ActionEvent(this, 1, ManageWorkspacesForm.CMD_EDIT_PROXY);
        when(getMockDialog().getSelectedServer()).thenReturn(mockServer);

        controller.actionPerformed(e);
        verify(getMockModel(), times(1)).editProxy(mockServer);
    }

    @Test
    public void testActionPerformed_Delete() {
        final ActionEvent e = new ActionEvent(this, 1, ManageWorkspacesForm.CMD_DELETE_WORKSPACE);
        when(getMockDialog().getSelectedWorkspace()).thenReturn(mockWorkspace);

        controller.actionPerformed(e);
        verify(getMockModel(), times(1)).deleteWorkspaceWithProgress(mockWorkspace);
    }

    @Test
    public void testActionPerformed_Edit() {
        final ActionEvent e = new ActionEvent(this, 1, ManageWorkspacesForm.CMD_EDIT_WORKSPACE);
        when(getMockDialog().getSelectedWorkspace()).thenReturn(mockWorkspace);

        controller.actionPerformed(e);
        verify(getMockModel(), times(1)).editWorkspaceWithProgress(eq(mockWorkspace), any(Runnable.class));
    }

    @Test
    public void testUpdate_RefreshServer() {
        reset(getMockDialog());
        when(getMockDialog().getSelectedServer()).thenReturn(mockServer);
        controller.update(null, ManageWorkspacesModel.REFRESH_SERVER);
        verify(getMockDialog(), times(1)).getSelectedServer();
        verify(getMockDialog(), times(1)).updateControls(mockServer);
        verifyNoMoreInteractions(getMockDialog());
    }

    @Test
    public void testUpdate_RefreshWorkspace() {
        reset(getMockDialog());
        when(getMockDialog().getSelectedWorkspace()).thenReturn(mockWorkspace);
        controller.update(null, ManageWorkspacesModel.REFRESH_WORKSPACE);
        verify(getMockDialog(), times(1)).getSelectedWorkspace();
        verify(getMockDialog(), times(1)).updateControls(mockWorkspace);
        verifyNoMoreInteractions(getMockDialog());
    }
}