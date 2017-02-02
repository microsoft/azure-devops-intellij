// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.settings;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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

@RunWith(PowerMockRunner.class)
@PrepareForTest({IdeaHelper.class})
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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(IdeaHelper.class);

        when(IdeaHelper.getCurrentProject()).thenReturn(mockProject);
        when(mockTeamServicesSettingsModel.getTableModel()).thenReturn(mockTableModel);
        when(mockTeamServicesSettingsModel.getTableSelectionModel()).thenReturn(mockListSelectionModel);

        controller = new TeamServicesSettingsController(mockTeamServicesSettingsForm, mockTeamServicesSettingsModel);
    }

    @Test
    public void testConstructor() {
        verify(mockTeamServicesSettingsModel, times(1)).addObserver(controller);
        verify(mockTeamServicesSettingsForm, times(1)).addActionListener(controller);
        verify(mockTeamServicesSettingsForm, times(1)).setContextTable(mockTableModel, mockListSelectionModel);
        verify(mockTeamServicesSettingsModel, times(1)).loadSettings();
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
}
