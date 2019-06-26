// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.ui.workspace.WorkspaceController;

/**
 * Action to edit the workspace associated with this project
 */
public class EditWorkspaceAction extends DumbAwareAction {

    protected EditWorkspaceAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_EDIT_WORKSPACE_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_EDIT_WORKSPACE_MSG),
                null);
    }

    @Override
    public void actionPerformed(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        final WorkspaceController controller = new WorkspaceController(project, TFSVcs.getInstance(project).getServerContext(false));
        controller.showModalDialog(true);
    }

    @Override
    public void update(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        anActionEvent.getPresentation().setEnabled(project != null);
    }
}
