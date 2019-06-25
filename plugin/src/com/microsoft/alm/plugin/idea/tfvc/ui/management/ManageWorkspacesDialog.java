// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.ui.management;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.util.containers.HashMap;
import com.microsoft.alm.plugin.external.models.Server;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialogImpl;
import com.microsoft.alm.plugin.idea.common.ui.common.treetable.ContentProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import java.awt.event.ActionListener;
import java.util.Map;

public class ManageWorkspacesDialog extends BaseDialogImpl {
    private static final String PROP_CONTEXT_PROVIDER = "context_provider";

    private ManageWorkspacesForm manageWorkspacesForm;

    public ManageWorkspacesDialog(final Project project, final ContentProvider<Object> contentProvider) {
        super(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_CLOSE_BUTTON),
                TfPluginBundle.KEY_TFVC_MANAGE_WORKSPACES_DIALOG_TITLE, true, createProperties(contentProvider));
    }

    private static Map<String, Object> createProperties(final ContentProvider<Object> contentProvider) {
        final Map<String, Object> properties = new HashMap<String, Object>(1);
        properties.put(PROP_CONTEXT_PROVIDER, contentProvider);
        return properties;
    }

    @Override
    public void addActionListener(final ActionListener listener) {
        super.addActionListener(listener);
        manageWorkspacesForm.addActionListener(listener);
    }

    @NotNull
    protected Action[] createActions() {
        return new Action[]{getOKAction(), getHelpAction()};
    }


    @Nullable
    protected JComponent createCenterPanel() {
        if (manageWorkspacesForm == null) {
            manageWorkspacesForm = new ManageWorkspacesForm((ContentProvider<Object>) getProperty(PROP_CONTEXT_PROVIDER));
            manageWorkspacesForm.setShowWorkspaces(true);
        }
        return manageWorkspacesForm.getContentPane();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        VcsDirtyScopeManager.getInstance(getProject()).markEverythingDirty();
    }

    public Workspace getSelectedWorkspace() {
        return manageWorkspacesForm.getSelectedWorkspace();
    }

    public Server getSelectedServer() {
        return manageWorkspacesForm.getSelectedServer();
    }

    public void updateControls(final Object selectedServerOrWorkspace) {
        manageWorkspacesForm.updateControls(selectedServerOrWorkspace);
    }
}