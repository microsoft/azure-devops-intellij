// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.branch;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.ValidationListener;
import git4idea.repo.GitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * Controller for creating a new branch from an existing remote branch
 */
public class CreateBranchController implements Observer, ActionListener {
    private static final Logger logger = LoggerFactory.getLogger(CreateBranchController.class);

    private final CreateBranchDialog dialog;
    private final CreateBranchModel model;

    public CreateBranchController(final Project project, final String defaultBranchName, final GitRepository gitRepository) {
        this(new CreateBranchDialog(
                        project,
                        TfPluginBundle.message(TfPluginBundle.KEY_CREATE_BRANCH_DIALOG_TITLE),
                        TfPluginBundle.message(TfPluginBundle.KEY_CREATE_BRANCH_DIALOG_CREATE_BUTTON),
                        TfPluginBundle.KEY_CREATE_BRANCH_DIALOG_TITLE),
                new CreateBranchModel(project, defaultBranchName, gitRepository));
    }

    public CreateBranchController(final CreateBranchDialog dialog, final CreateBranchModel model) {
        this.dialog = dialog;
        this.dialog.addActionListener(this);

        this.model = model;
        this.model.addObserver(this);

        setupDialog();
        update(null, null);
    }

    private void setupDialog() {
        dialog.addValidationListener(new ValidationListener() {
            @Override
            public ValidationInfo doValidate() {
                return validate();
            }
        });
    }

    public boolean showModalDialog() {
        logger.info("CreateBranchController is showing dialog");
        return dialog.showModalDialog();
    }

    public String getBranchName() {
        return model.getBranchName();
    }

    public boolean createBranch(ServerContext context) {
        model.doBranchCreate(context, null);
        return model.getBranchWasCreated();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        // Update model before action is initiated on it
        updateModel();

        if (BaseDialog.CMD_OK.equals(e.getActionCommand())) {
            logger.info("CreateBranchController create action completed so valid branch name was input");
        }
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (arg == null || CreateBranchModel.PROP_BRANCH_NAME.equals(arg)) {
            dialog.setBranchName(model.getBranchName());
        }

        if (arg == null || CreateBranchModel.PROP_CHECKOUT_BRANCH.equals(arg)) {
            dialog.setCheckoutBranch(model.getCheckoutBranch());
        }

        if (arg == null || CreateBranchModel.PROP_REMOTE_BRANCH_COMBO_MODEL.equals(arg)) {
            dialog.setRemoteBranchDropdownModel(model.getRemoteBranchDropdownModel());
        }

        if (arg == null || CreateBranchModel.PROP_SELECTED_REMOTE_BRANCH.equals(arg)) {
            dialog.setSelectedRemoteBranch(model.getSelectedRemoteBranch());
        }
    }

    protected ValidationInfo validate() {
        updateModel();

        ModelValidationInfo validationInfo = model.validate();
        if (validationInfo != ModelValidationInfo.NO_ERRORS) {
            return new ValidationInfo(validationInfo.getValidationMessage(),
                    dialog.getComponent(validationInfo.getValidationSource()));
        }
        return null;
    }

    protected void updateModel() {
        model.setBranchName(dialog.getBranchName());
        model.setSelectedRemoteBranch(dialog.getSelectedRemoteBranch());
        model.setCheckoutBranch(dialog.getCheckoutBranch());
    }
}
