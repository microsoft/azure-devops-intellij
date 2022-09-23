// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.common.ui.settings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.ui.TestDialogManager;
import com.microsoft.alm.L2.L2Test;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.settings.TeamServicesConfigurable;
import com.microsoft.alm.plugin.idea.common.ui.settings.TeamServicesSettingsController;
import com.microsoft.alm.plugin.idea.common.ui.settings.TeamServicesSettingsForm;
import com.microsoft.alm.plugin.idea.common.ui.settings.TeamServicesSettingsModel;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;

public class PasswordMgtTests extends L2Test {
    private TeamServicesSettingsModel model;
    private TeamServicesSettingsController controller;

    private static void verifyShowsMessage(String expectedMessage, int messageResult, Runnable action) {
        AtomicReference<String> messageShown = new AtomicReference<>();
        TestDialog oldTestDialog = TestDialogManager.setTestDialog(message -> {
            messageShown.set(message);
            return messageResult;
        });

        try {
            action.run();
            assertEquals("Message shown in dialog", expectedMessage, messageShown.get());
        } finally {
            TestDialogManager.setTestDialog(oldTestDialog);
        }
    }

    public void testSettingsSetup() {
        createSettings();
        assertEquals(2, model.getTableModel().getRowCount());
        assertFalse(controller.isModified());
    }

    public void testDeletePassword() throws Exception {
        createSettings();

        // try to delete with no selection made
        verifyShowsMessage(
                TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED),
                Messages.OK,
                () -> controller.actionPerformed(
                        new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_DELETE_PASSWORD)));
        assertFalse(controller.isModified());
        assertEquals(2, model.getTableModel().getRowCount());

        // try to delete with selection but then cancel
        verifyShowsMessage(
                TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG),
                Messages.CANCEL,
                () -> {
                    model.getTableSelectionModel().setSelectionInterval(0, 0);
                    controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_DELETE_PASSWORD));
                }
        );
        assertFalse(controller.isModified());
        assertEquals(2, model.getTableModel().getRowCount());

        // do delete
        verifyShowsMessage(
                TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG),
                Messages.OK,
                () -> {
                    model.getTableSelectionModel().setSelectionInterval(0, 0);
                    controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_DELETE_PASSWORD));
                }
        );
        assertTrue(controller.isModified());
        assertEquals(1, model.getTableModel().getRowCount());
        assertEquals(3, ServerContextManager.getInstance().getAllServerContexts().size());

        // apply delete
        controller.actionPerformed(new ActionEvent(this, 0, TeamServicesConfigurable.CMD_APPLY_CHANGES));
        assertFalse(controller.isModified());
        assertEquals(1, model.getTableModel().getRowCount());
        assertEquals(2, ServerContextManager.getInstance().getAllServerContexts().size());
    }

    public void testUpdatePassword() {
        createSettings();
        final List<ServerContext> originalContexts = new ArrayList<ServerContext>(ServerContextManager.getInstance().getAllServerContexts());

        // try to update with no selection made
        verifyShowsMessage(
                TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED),
                Messages.OK,
                () -> controller.actionPerformed(
                        new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_UPDATE_PASSWORD)));
        assertFalse(controller.isModified());
        assertEquals(2, model.getTableModel().getRowCount());
        List<ServerContext> updatedContexts = new ArrayList<ServerContext>(ServerContextManager.getInstance().getAllServerContexts());
        assertTrue(updatedContexts.size() == originalContexts.size());
        assertTrue(updatedContexts.containsAll(originalContexts));

        // try to delete with selection but then cancel
        verifyShowsMessage(
                TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG),
                Messages.CANCEL,
                () -> {
                    model.getTableSelectionModel().setSelectionInterval(0, 0);
                    controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_UPDATE_PASSWORD));
                }
        );
        assertFalse(controller.isModified());
        assertEquals(2, model.getTableModel().getRowCount());
        updatedContexts = new ArrayList<ServerContext>(ServerContextManager.getInstance().getAllServerContexts());
        assertTrue(updatedContexts.size() == originalContexts.size());
        assertTrue(updatedContexts.containsAll(originalContexts));

        // do update
        verifyShowsMessage(
                TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG),
                Messages.OK,
                () -> {
                    model.getTableSelectionModel().setSelectionInterval(0, 0);
                    controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_UPDATE_PASSWORD));
                }
        );
        assertFalse(controller.isModified());
        assertEquals(2, model.getTableModel().getRowCount());
        updatedContexts = new ArrayList<>(ServerContextManager.getInstance().getAllServerContexts());
        assertEquals(updatedContexts.size(), originalContexts.size());
        assertFalse(updatedContexts.contains(originalContexts.get(0)));
        assertFalse(updatedContexts.contains(originalContexts.get(1)));
        assertEquals(updatedContexts.get(0).getAuthenticationInfo(), updatedContexts.get(1).getAuthenticationInfo());
    }

    public void testReset() {
        createSettings();

        // do delete
        verifyShowsMessage(
                TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG),
                Messages.OK,
                () -> {
                    model.getTableSelectionModel().setSelectionInterval(0, 1);
                    controller.actionPerformed(new ActionEvent(this, 0, TeamServicesSettingsForm.CMD_DELETE_PASSWORD));
                }
        );
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
        Project project = IdeaHelper.getCurrentProject();
        TeamServicesSettingsForm form = mock(TeamServicesSettingsForm.class);
        model = new TeamServicesSettingsModel(project);
        controller = new TeamServicesSettingsController(form, model);
    }
}
