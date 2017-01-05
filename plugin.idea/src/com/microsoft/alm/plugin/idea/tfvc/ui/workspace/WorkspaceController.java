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
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.ui.common.ValidationListener;
import org.apache.commons.lang.StringUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * The controller for the WorkspaceDialog
 */
public class WorkspaceController implements Observer, ActionListener {
    private final WorkspaceDialog dialog;
    private final WorkspaceModel model;
    private boolean suspendEvents = false;
    private final Project project;

    /**
     * Use this constructor to edit the workspace for the current project.
     *
     * @param project
     */
    public WorkspaceController(final Project project) {
        this(project, new WorkspaceDialog(project), new WorkspaceModel());
        this.model.loadWorkspace(project);
    }

    /**
     * This constructor is used by the "checkout from version control" flow where the project path is not the same as
     * the workspace path.
     *
     * @param project
     * @param repositoryContext
     * @param workspaceName
     */
    public WorkspaceController(final Project project, final RepositoryContext repositoryContext, final String workspaceName) {
        this(project, new WorkspaceDialog(project), new WorkspaceModel());
        this.model.loadWorkspace(repositoryContext, workspaceName);
    }

    protected WorkspaceController(final Project project, final WorkspaceDialog dialog, final WorkspaceModel model) {
        this.project = project;
        this.dialog = dialog;
        this.dialog.addActionListener(this);

        this.model = model;
        this.model.addObserver(this);

        setupDialog();
    }

    /**
     * Shows the dialog modally and optionally saves the workspace when the user hits OK.
     * If you choose not to auto save the workspace, you should call saveWorkspace if this method returns true.
     *
     * @param autoSaveWorkspace
     * @return
     */
    public boolean showModalDialog(final boolean autoSaveWorkspace) {
        final boolean result = dialog.showModalDialog();
        if (result && autoSaveWorkspace) {
            saveWorkspace(project.getBasePath(), false, null);
        }

        return result;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        // The OK action is already handled
    }

    public void saveWorkspace(final String workspaceRootPath, final boolean syncFiles, final Runnable onSuccess) {
        // Update model before action is initiated on it
        updateModel();
        // Trigger the save workspace method
        // (this method will do all the background work and notify the user of the result)
        model.saveWorkspace(project, workspaceRootPath, syncFiles, onSuccess);
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (suspendEvents) {
            return;
        }

        if (arg == null || arg.equals(WorkspaceModel.PROP_COMMENT)) {
            dialog.setComment(model.getComment());
        }
        if (arg == null || arg.equals(WorkspaceModel.PROP_COMPUTER)) {
            dialog.setComputer(model.getComputer());
        }
        if (arg == null || arg.equals(WorkspaceModel.PROP_MAPPINGS)) {
            dialog.setMappings(model.getMappings());
        }
        if (arg == null || arg.equals(WorkspaceModel.PROP_NAME)) {
            dialog.setName(model.getName());
        }
        if (arg == null || arg.equals(WorkspaceModel.PROP_OWNER)) {
            dialog.setOwner(model.getOwner());
        }
        if (arg == null || arg.equals(WorkspaceModel.PROP_SERVER)) {
            dialog.setServer(model.getServer());
        }

        // Loading is special we only want to update it when it is the only thing changing
        if (arg != null && arg.equals(WorkspaceModel.PROP_LOADING)) {
            dialog.setLoading(model.isLoading());
        }

    }

    private void setupDialog() {
        dialog.addValidationListener(new ValidationListener() {
            @Override
            public ValidationInfo doValidate() {
                return validate();
            }
        });
    }

    protected ValidationInfo validate() {
        updateModel();

        // Check the model first
        final ModelValidationInfo validationInfo = model.validate();
        if (validationInfo != ModelValidationInfo.NO_ERRORS) {
            return new ValidationInfo(validationInfo.getValidationMessage(),
                    dialog.getComponent(validationInfo.getValidationSource()));
        }

        // The check the dialog
        final String error = dialog.getFirstMappingValidationError();
        if (StringUtils.isNotEmpty(error)) {
            return new ValidationInfo(error);
        }

        return null;
    }

    protected void updateModel() {
        // Don't trigger any UI events while we are updating the model from the UI controls
        suspendEvents = true;
        try {
            model.setComment(dialog.getWorkspaceComment());
            model.setMappings(dialog.getWorkingFolders());
            model.setName(dialog.getWorkspaceName());
        } finally {
            suspendEvents = false;
        }
    }

    protected WorkspaceDialog getDialog() {
        return dialog;
    }

    protected WorkspaceModel getModel() {
        return model;
    }
}
