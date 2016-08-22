// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.workspace;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialog;
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

    public WorkspaceController(final Project project) {
        this(project, new WorkspaceDialog(project), new WorkspaceModel());
    }

    public WorkspaceController(final Project project, final WorkspaceDialog dialog, final WorkspaceModel model) {
        this.project = project;
        this.dialog = dialog;
        this.dialog.addActionListener(this);

        this.model = model;
        this.model.addObserver(this);
        this.model.loadWorkspace(project);

        setupDialog();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        // Update model before action is initiated on it
        updateModel();

        if (BaseDialog.CMD_OK.equals(e.getActionCommand())) {
            // Trigger the save workspace method
            // (this method will do all the background work and notify the user of the result)
            model.saveWorkspace(project);
        }
    }

    public boolean showModalDialog() {
        return dialog.showModalDialog();
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
}
