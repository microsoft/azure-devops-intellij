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

package com.microsoft.alm.plugin.idea.tfvc.ui.workspace;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.containers.HashMap;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialogImpl;
import com.microsoft.alm.plugin.idea.common.ui.common.ValidationListener;
import org.apache.commons.lang.StringUtils;

import javax.swing.JComponent;
import java.util.List;
import java.util.Map;

public class WorkspaceDialog extends BaseDialogImpl {
    private static final String PROP_SERVER_CONTEXT = "server_context";

    private WorkspaceForm workspaceForm;

    public WorkspaceDialog(final Project project, final ServerContext serverContext) {
        super(project, TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_WORKSPACE_DIALOG_SAVE_BUTTON),
                TfPluginBundle.KEY_WORKSPACE_DIALOG_TITLE, true, createProperties(serverContext));
    }

    private static Map<String, Object> createProperties(final ServerContext serverContext) {
        final Map<String, Object> properties = new HashMap<String, Object>(4);
        properties.put(PROP_SERVER_CONTEXT, serverContext);
        return properties;
    }

    public JComponent getComponent(final String componentPropName) {
        if (StringUtils.isEmpty(componentPropName) || workspaceForm == null) {
            return null;
        }
        return workspaceForm.getComponent(componentPropName);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        if (workspaceForm != null) {
            return workspaceForm.getPreferredFocusedComponent();
        }
        return super.getPreferredFocusedComponent();
    }

    protected JComponent createCenterPanel() {
        if (workspaceForm == null) {
            // When we create the form we give it a validationListener so that the table can trigger our validate method
            workspaceForm = new WorkspaceForm(getProject(), (ServerContext) getProperty(PROP_SERVER_CONTEXT),
                    new ValidationListener() {
                        @Override
                        public ValidationInfo doValidate() {
                            validate();
                            return null;
                        }
                    });
        }
        return workspaceForm.getContentPane();
    }

    public String getFirstMappingValidationError() {
        return workspaceForm.getFirstMappingValidationError();
    }

    public String getWorkspaceName() {
        return workspaceForm.getWorkspaceName();
    }

    public String getWorkspaceComment() {
        return workspaceForm.getComment();
    }

    public List<Workspace.Mapping> getWorkingFolders() {
        return workspaceForm.getMappings();
    }

    public void setComment(final String comment) {
        workspaceForm.setComment(comment);
    }

    public void setComputer(final String computer) {
        workspaceForm.setComputer(computer);
    }

    public void setLoading(final boolean loading) {
        workspaceForm.setLoading(loading);
    }

    public void setMappings(final List<Workspace.Mapping> mappings) {
        workspaceForm.setMappings(mappings);
    }

    public void setName(final String name) {
        workspaceForm.setWorkspaceName(name);
    }

    public void setOwner(final String owner) {
        workspaceForm.setOwner(owner);
    }

    public void setServer(final String server) {
        workspaceForm.setServer(server);
    }
}
