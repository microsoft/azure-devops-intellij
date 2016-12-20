// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.actions.InstrumentedAction;
import com.microsoft.alm.plugin.idea.tfvc.ui.workspace.WorkspaceController;

/**
 * Action to edit the workspace associated with this project
 */
public class EditWorkspaceAction extends InstrumentedAction {

    protected EditWorkspaceAction() {
        super(false);
    }

    @Override
    public void doActionPerformed(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        final WorkspaceController controller = new WorkspaceController(project);
        controller.showModalDialog(true);
    }

    @Override
    public void doUpdate(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        anActionEvent.getPresentation().setEnabled(project != null);
    }
}
