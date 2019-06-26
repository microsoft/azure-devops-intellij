// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.management;

import com.intellij.openapi.project.Project;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * Controller for the ManageWorkspaces dialog
 */
public class ManageWorkspacesController implements Observer, ActionListener {
    private final ManageWorkspacesDialog dialog;
    private final ManageWorkspacesModel model;

    public ManageWorkspacesController(final Project project) {
        this.model = new ManageWorkspacesModel(project);
        this.dialog = new ManageWorkspacesDialog(project, model.getContextProvider());

        this.dialog.addActionListener(this);
        this.model.addObserver(this);

        update(null, null);
    }

    public void showModalDialog() {
        dialog.showModalDialog();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (ManageWorkspacesForm.CMD_RELOAD_WORKSPACES.equals(e.getActionCommand())) {
            model.reloadWorkspacesWithProgress(dialog.getSelectedServer());
        } else if (ManageWorkspacesForm.CMD_DELETE_WORKSPACE.equals(e.getActionCommand())) {
            model.deleteWorkspaceWithProgress(dialog.getSelectedWorkspace());
        } else if (ManageWorkspacesForm.CMD_EDIT_PROXY.equals(e.getActionCommand())) {
            model.editProxy(dialog.getSelectedServer());
        } else if (ManageWorkspacesForm.CMD_EDIT_WORKSPACE.equals(e.getActionCommand())) {
            // need to pass update so it can run once update is complete in the background or else we won't pick up changes
            model.editWorkspaceWithProgress(dialog.getSelectedWorkspace(), new Runnable() {
                @Override
                public void run() {
                    dialog.updateControls(dialog.getSelectedWorkspace());
                }
            });
        }
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (ManageWorkspacesModel.REFRESH_SERVER.equals(arg)) {
            dialog.updateControls(dialog.getSelectedServer());
        } else if (ManageWorkspacesModel.REFRESH_WORKSPACE.equals(arg)) {
            dialog.updateControls(dialog.getSelectedWorkspace());
        }
    }
}