// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.actions.InstrumentedAction;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.ui.management.ManageWorkspacesController;

public class ManageWorkspacesAction extends InstrumentedAction {

    public ManageWorkspacesAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_MANAGE_WORKSPACES_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_MANAGE_WORKSPACES_MSG),
                null, false);
    }

    @Override
    public void doUpdate(final AnActionEvent e) {
        final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            e.getPresentation().setVisible(project != null);
        } else {
            e.getPresentation().setEnabled(project != null);
        }
    }

    @Override
    public void doActionPerformed(final AnActionEvent e) {
        final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        ManageWorkspacesController controller = new ManageWorkspacesController(project);
        controller.showModalDialog();
    }
}