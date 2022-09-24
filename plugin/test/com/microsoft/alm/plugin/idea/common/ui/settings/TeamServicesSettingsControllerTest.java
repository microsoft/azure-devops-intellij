// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.settings;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.authentication.AuthTypes;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import java.awt.event.ActionEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TeamServicesSettingsControllerTest extends IdeaAbstractTest {
    private TeamServicesSettingsController controller;

    @Mock
    private Project mockProject;
    @Mock
    private TeamServicesSettingsForm mockTeamServicesSettingsForm;
    @Mock
    private TeamServicesSettingsModel mockTeamServicesSettingsModel;
    @Mock
    private ServerContextTableModel mockTableModel;
    @Mock
    private ListSelectionModel mockListSelectionModel;
    @Mock
    private JComponent mockComponent;

    @Mock
    private MockedStatic<IdeaHelper> ideaHelper;

    @Before
    public void setUp() {
        ideaHelper.when(IdeaHelper::getCurrentProject).thenReturn(mockProject);
        when(mockTeamServicesSettingsModel.getTableModel()).thenReturn(mockTableModel);
        when(mockTeamServicesSettingsModel.getTableSelectionModel()).thenReturn(mockListSelectionModel);
        when(mockTeamServicesSettingsModel.getOriginalAuthType()).thenReturn(AuthTypes.DEVICE_FLOW);

        controller = new TeamServicesSettingsController(mockTeamServicesSettingsForm, mockTeamServicesSettingsModel);
    }

    @Test
    public void testConstructor() {
        verify(mockTeamServicesSettingsModel, times(1)).addObserver(controller);
        verify(mockTeamServicesSettingsForm, times(1)).addActionListener(controller);
        verify(mockTeamServicesSettingsForm, times(1)).setContextTable(mockTableModel, mockListSelectionModel);
        verify(mockTeamServicesSettingsModel, times(1)).loadSettings();
        verify(mockTeamServicesSettingsForm, times(1)).setAuthType(AuthTypes.DEVICE_FLOW);
    }

    @Test
    public void testIsModified() {
        when(mockTeamServicesSettingsModel.isModified()).thenReturn(true);
        assertTrue(controller.isModified());
    }

    @Test
    public void testGetContentPane() {
        when(mockTeamServicesSettingsForm.getContentPane()).thenReturn(mockComponent);
        assertEquals(mockComponent, controller.getContentPane());
    }

    @Test
    public void testActionPerformed_Reset() {
        reset(mockTeamServicesSettingsModel);
        ActionEvent mockEvent = mock(ActionEvent.class);
        when(mockEvent.getActionCommand()).thenReturn(TeamServicesConfigurable.CMD_RESET_CHANGES);
        controller.actionPerformed(mockEvent);

        verify(mockTeamServicesSettingsModel, times(1)).reset();
        verify(mockTeamServicesSettingsModel, times(1)).getOriginalAuthType();
        verify(mockTeamServicesSettingsForm, times(1)).setAuthType(AuthTypes.DEVICE_FLOW);
        verifyNoMoreInteractions(mockTeamServicesSettingsModel);
    }

    @Test
    public void testActionPerformed_Apply() {
        reset(mockTeamServicesSettingsModel);
        ActionEvent mockEvent = mock(ActionEvent.class);
        when(mockEvent.getActionCommand()).thenReturn(TeamServicesConfigurable.CMD_APPLY_CHANGES);
        controller.actionPerformed(mockEvent);

        verify(mockTeamServicesSettingsModel, times(1)).apply();
        verifyNoMoreInteractions(mockTeamServicesSettingsModel);
    }

    @Test
    public void testActionPerformed_Delete() {
        reset(mockTeamServicesSettingsModel);
        ActionEvent mockEvent = mock(ActionEvent.class);
        when(mockEvent.getActionCommand()).thenReturn(TeamServicesSettingsForm.CMD_DELETE_PASSWORD);
        controller.actionPerformed(mockEvent);

        verify(mockTeamServicesSettingsModel, times(1)).deletePasswords();
        verifyNoMoreInteractions(mockTeamServicesSettingsModel);
    }

    @Test
    public void testActionPerformed_Update() {
        reset(mockTeamServicesSettingsModel);
        ActionEvent mockEvent = mock(ActionEvent.class);
        when(mockEvent.getActionCommand()).thenReturn(TeamServicesSettingsForm.CMD_UPDATE_PASSWORD);
        controller.actionPerformed(mockEvent);

        verify(mockTeamServicesSettingsModel, times(1)).updatePasswords();
        verifyNoMoreInteractions(mockTeamServicesSettingsModel);
    }

    @Test
    public void testActionPerformed_AuthChanged() {
        reset(mockTeamServicesSettingsModel);
        when(mockTeamServicesSettingsForm.getSelectAuthType()).thenReturn(AuthTypes.DEVICE_FLOW);
        ActionEvent mockEvent = mock(ActionEvent.class);
        when(mockEvent.getActionCommand()).thenReturn(TeamServicesSettingsForm.CMD_AUTH_CHANGED);
        controller.actionPerformed(mockEvent);

        verify(mockTeamServicesSettingsModel, times(1)).setUpdatedAuthType(AuthTypes.DEVICE_FLOW);
        verifyNoMoreInteractions(mockTeamServicesSettingsModel);
    }
}
