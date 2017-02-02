// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.settings;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServerContextManager.class, Messages.class, ProgressManager.class})
public class TeamServicesSettingsModelTest extends IdeaAbstractTest {
    private TeamServicesSettingsModel teamServicesSettingsModel;

    @Mock
    private Project mockProject;
    @Mock
    private ServerContextManager mockServerContextManager;
    @Mock
    private ServerContext mockServerContext1;
    @Mock
    private ServerContext mockServerContext2;
    @Mock
    private ProgressManager mockProgressManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(ServerContextManager.class, Messages.class, ProgressManager.class);

        when(mockServerContext1.getKey()).thenReturn("mockServerContext1");
        when(mockServerContext2.getKey()).thenReturn("mockServerContext2");
        when(mockServerContextManager.getAllServerContexts()).thenReturn(ImmutableList.of(mockServerContext1, mockServerContext2));
        when(ServerContextManager.getInstance()).thenReturn(mockServerContextManager);
        when(ProgressManager.getInstance()).thenReturn(mockProgressManager);

        teamServicesSettingsModel = new TeamServicesSettingsModel(mockProject);
    }

    @Test
    public void testIsModified_True() {
        teamServicesSettingsModel.setDeleteContexts(ImmutableList.of(mockServerContext1));

        assertTrue(teamServicesSettingsModel.isModified());
        assertEquals(1, teamServicesSettingsModel.getDeleteContexts().size());
    }

    @Test
    public void testIsModified_False() {
        teamServicesSettingsModel.setDeleteContexts(Collections.EMPTY_LIST);

        assertFalse(teamServicesSettingsModel.isModified());
        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
    }

    @Test
    public void testLoadSettings() {
        // running it twice to test we clear contexts
        teamServicesSettingsModel.loadSettings();
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());

        teamServicesSettingsModel.loadSettings();
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
    }

    @Test
    public void testApply() {
        teamServicesSettingsModel.setDeleteContexts(ImmutableList.of(mockServerContext1, mockServerContext2));
        teamServicesSettingsModel.apply();

        assertEquals(0, teamServicesSettingsModel.getTableModel().getRowCount());
        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        verifyStatic(times(1));
        mockServerContextManager.remove("mockServerContext1");
        mockServerContextManager.remove("mockServerContext2");
    }

    @Test
    public void testReset() {
        teamServicesSettingsModel.setDeleteContexts(ImmutableList.of(mockServerContext1, mockServerContext2));
        teamServicesSettingsModel.reset();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
    }

    @Test
    public void testDeletePasswords_OK() {
        teamServicesSettingsModel.getTableSelectionModel().setSelectionInterval(0, 0);
        when(Messages.showYesNoDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.YES);
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext1, mockServerContext2)));
        teamServicesSettingsModel.deletePasswords();

        assertEquals(1, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(1, teamServicesSettingsModel.getTableModel().getRowCount());
        assertEquals(mockServerContext2, teamServicesSettingsModel.getTableModel().getServerContext(0));
        verifyStatic(times(1));
        Messages.showYesNoDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG)), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE)), any(Icon.class));
    }

    @Test
    public void testDeletePasswords_Cancel() {
        teamServicesSettingsModel.getTableSelectionModel().setSelectionInterval(0, 0);
        when(Messages.showYesNoDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.CANCEL);
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext1, mockServerContext2)));
        teamServicesSettingsModel.deletePasswords();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        assertEquals(mockServerContext1, teamServicesSettingsModel.getTableModel().getServerContext(0));
        assertEquals(mockServerContext2, teamServicesSettingsModel.getTableModel().getServerContext(1));
        verifyStatic(times(1));
        Messages.showYesNoDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG)), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE)), any(Icon.class));
    }

    @Test
    public void testDeletePasswords_NoRowSelected() {
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext1, mockServerContext2)));
        teamServicesSettingsModel.deletePasswords();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        assertEquals(mockServerContext1, teamServicesSettingsModel.getTableModel().getServerContext(0));
        assertEquals(mockServerContext2, teamServicesSettingsModel.getTableModel().getServerContext(1));
        verifyStatic(times(1));
        Messages.showWarningDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE));
    }

    @Test
    public void testUpdatePasswords_OK() {
        teamServicesSettingsModel.getTableSelectionModel().setSelectionInterval(0, 0);
        when(Messages.showYesNoDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.YES);
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext1, mockServerContext2)));
        teamServicesSettingsModel.updatePasswords();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        verifyStatic(times(1));
        Messages.showYesNoDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG)), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE)), any(Icon.class));
    }

    @Test
    public void testUpdatePasswords_Cancel() {
        teamServicesSettingsModel.getTableSelectionModel().setSelectionInterval(0, 0);
        when(Messages.showYesNoDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.CANCEL);
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext1, mockServerContext2)));
        teamServicesSettingsModel.updatePasswords();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        assertEquals(mockServerContext1, teamServicesSettingsModel.getTableModel().getServerContext(0));
        assertEquals(mockServerContext2, teamServicesSettingsModel.getTableModel().getServerContext(1));
        verifyStatic(times(1));
        Messages.showYesNoDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG)), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE)), any(Icon.class));
    }

    @Test
    public void testUpdatePasswords_NoRowSelected() {
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext1, mockServerContext2)));
        teamServicesSettingsModel.updatePasswords();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        verifyStatic(times(1));
        Messages.showWarningDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE));
    }
}
