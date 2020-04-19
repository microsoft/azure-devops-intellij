// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.common.ui.settings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.microsoft.alm.L2.L2Test;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.settings.TeamServicesConfigurable;
import com.microsoft.alm.plugin.idea.common.ui.settings.TeamServicesSettingsController;
import com.microsoft.alm.plugin.idea.common.ui.settings.TeamServicesSettingsForm;
import com.microsoft.alm.plugin.idea.common.ui.settings.TeamServicesSettingsModel;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Messages.class})
@PowerMockIgnore({"javax.net.ssl.*", "javax.swing.*", "javax.security.*"})
public class PasswordMgtTests extends L2Test {
    private Project project;
    private TeamServicesSettingsForm form;
    private TeamServicesSettingsModel model;
    private TeamServicesSettingsController controller;

    @Before
    public void testSetup() {
        PowerMockito.mockStatic(Messages.class);
    }

    @Test(timeout = 60000)
    public void testSettingsSetup() throws Exception {
        createSettings();

        verify(form).setContextTable(model.getTableModel(), model.getTableSelectionModel());
        assertEquals(2, model.getTableModel().getRowCount());
        assertFalse(controller.isModified());
    }

    @Test(timeout = 60000)
    public void testDeletePassword() throws Exception {
        createSettings();

        // try to delete with no selection made
        controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_DELETE_PASSWORD));
        assertFalse(controller.isModified());
        assertEquals(2, model.getTableModel().getRowCount());
        verifyStatic(times(1));
        Messages.showWarningDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE));

        // try to delete with selection but then cancel
        when(Messages.showYesNoDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.CANCEL);
        model.getTableSelectionModel().setSelectionInterval(0, 0);
        controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_DELETE_PASSWORD));
        assertFalse(controller.isModified());
        assertEquals(2, model.getTableModel().getRowCount());

        // do delete
        when(Messages.showYesNoDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.OK);
        model.getTableSelectionModel().setSelectionInterval(0, 0);
        controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_DELETE_PASSWORD));
        assertTrue(controller.isModified());
        assertEquals(1, model.getTableModel().getRowCount());
        assertEquals(3, ServerContextManager.getInstance().getAllServerContexts().size());

        // apply delete
        controller.actionPerformed(new ActionEvent(this, 0, TeamServicesConfigurable.CMD_APPLY_CHANGES));
        assertFalse(controller.isModified());
        assertEquals(1, model.getTableModel().getRowCount());
        assertEquals(2, ServerContextManager.getInstance().getAllServerContexts().size());
    }

    @Test
    public void testUpdatePassword() {
        createSettings();
        final List<ServerContext> originalContexts = new ArrayList<ServerContext>(ServerContextManager.getInstance().getAllServerContexts());

        // try to update with no selection made
        controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_UPDATE_PASSWORD));
        assertFalse(controller.isModified());
        assertEquals(2, model.getTableModel().getRowCount());
        verifyStatic(times(1));
        Messages.showWarningDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE));
        List<ServerContext> updatedContexts = new ArrayList<ServerContext>(ServerContextManager.getInstance().getAllServerContexts());
        assertTrue(updatedContexts.size() == originalContexts.size());
        assertTrue(updatedContexts.containsAll(originalContexts));

        // try to delete with selection but then cancel
        when(Messages.showYesNoDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.CANCEL);
        model.getTableSelectionModel().setSelectionInterval(0, 0);
        controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_UPDATE_PASSWORD));
        assertFalse(controller.isModified());
        assertEquals(2, model.getTableModel().getRowCount());
        updatedContexts = new ArrayList<ServerContext>(ServerContextManager.getInstance().getAllServerContexts());
        assertTrue(updatedContexts.size() == originalContexts.size());
        assertTrue(updatedContexts.containsAll(originalContexts));

        // do update
        when(Messages.showYesNoDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.OK);
        model.getTableSelectionModel().setSelectionInterval(0, 0);
        controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_UPDATE_PASSWORD));
        assertFalse(controller.isModified());
        assertEquals(2, model.getTableModel().getRowCount());
        updatedContexts = new ArrayList<ServerContext>(ServerContextManager.getInstance().getAllServerContexts());
        assertTrue(updatedContexts.size() == originalContexts.size());
        assertFalse(updatedContexts.contains(originalContexts.get(0)));
        assertFalse(updatedContexts.contains(originalContexts.get(1)));
        assertEquals(updatedContexts.get(0).getAuthenticationInfo(), updatedContexts.get(1).getAuthenticationInfo());
    }

    @Test
    public void testReset() {
        createSettings();

        // do delete
        when(Messages.showYesNoDialog(project, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.OK);
        model.getTableSelectionModel().setSelectionInterval(0, 1);
        controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_DELETE_PASSWORD));
        assertTrue(controller.isModified());
        assertEquals(0, model.getTableModel().getRowCount());
        assertEquals(3, ServerContextManager.getInstance().getAllServerContexts().size());

        // reset
        controller.actionPerformed(new ActionEvent(this, 0, TeamServicesConfigurable.CMD_RESET_CHANGES));
        assertFalse(controller.isModified());
        assertEquals(2, model.getTableModel().getRowCount());
        assertEquals(3, ServerContextManager.getInstance().getAllServerContexts().size());
    }

    private void createSettings() {
        mockTeamServicesSettingsService();
        project = IdeaHelper.getCurrentProject();
        form = mock(TeamServicesSettingsForm.class);
        model = new TeamServicesSettingsModel(project);
        controller = new TeamServicesSettingsController(form, model);
    }
}
