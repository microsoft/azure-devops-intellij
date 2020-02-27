// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.pullrequest;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.ui.common.ValidationListener;
import git4idea.repo.GitRepository;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

/**
 * Controller for CreatePullRequestDialog
 */
public class CreatePullRequestController implements Observer, ActionListener {

    private CreatePullRequestDialog createDialog;
    private CreatePullRequestModel createModel;

    /* default for DI*/
    CreatePullRequestController() {
    }

    public CreatePullRequestController(final Project project, final GitRepository gitRepository) {
        this(new CreatePullRequestDialog(project), new CreatePullRequestModel(project, gitRepository));
    }

    @VisibleForTesting
    public CreatePullRequestController(final CreatePullRequestDialog createDialog,
                                       final CreatePullRequestModel createModel) {
        this.createDialog = createDialog;
        this.createModel = createModel;

        setupDialog();

        // Initialize the form with the current values from the model
        update(null, null);

        // add the observer and action listener after we are fully initialized, otherwise we will just get called
        // in the middle
        this.createModel.addObserver(this);
        this.createDialog.addActionListener(this);

        // load the initial diff
        this.createModel.loadDiff();
    }

    private void setupDialog() {
        this.createDialog.addValidationListener(new ValidationListener() {
            @Override
            public ValidationInfo doValidate() {
                return validate();
            }
        });
    }

    public boolean dispose(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return createModel.dispose(timeout, timeUnit);
    }

    public void showModalDialog() {
        // before we show the dialog, let's do a sanity check.  This is the last chance to
        // exit out the dialog, but the sanity check method should handle displaying all errors to users
        // as the dialog isn't shown yet
        if (this.sanityCheckOnModel()) {
            this.createDialog.showAndGet();
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (CreatePullRequestForm.CMD_TARGET_BRANCH_UPDATED.equals(e.getActionCommand())) {
            this.createModel.setTargetBranch(this.createDialog.getSelectedTargetBranch());
            this.createModel.loadDiff();
        } else if (BaseDialog.CMD_OK.equals(e.getActionCommand())) {
            this.createModel.createPullRequest();
        }
    }

    @Override
    public void update(final Observable observable, final Object arg) {
        if (arg == null || CreatePullRequestModel.PROP_SOURCE_BRANCH.equals(arg)) {
            this.createDialog.setSourceBranch(this.createModel.getSourceBranch());
        }

        if (arg == null || CreatePullRequestModel.PROP_TARGET_BRANCH_COMBO_MODEL.equals(arg)) {
            this.createDialog.setTargetBranchDropdownModel(this.createModel.getRemoteBranchDropdownModel());
        }

        if (arg == null || CreatePullRequestModel.PROP_TARGET_BRANCH.equals(arg)) {
            this.createDialog.setSelectedTargetBranch(this.createModel.getTargetBranch());
        }

        if (arg == null || CreatePullRequestModel.PROP_TITLE.equals(arg)) {
            this.createDialog.setTitle(this.createModel.getTitle());
        }

        if (arg == null || CreatePullRequestModel.PROP_DESCRIPTION.equals(arg)) {
            this.createDialog.setDescription(this.createModel.getDescription());
        }

        if (arg == null || CreatePullRequestModel.PROP_LOADING.equals(arg)) {
            this.createDialog.setIsLoading(this.createModel.isLoading());
        }

        if (arg == null || CreatePullRequestModel.PROP_DIFF_MODEL.equals(arg)) {
            this.createDialog.populateDiff(this.createModel.getProject(), this.createModel.getLocalBranchChanges());
        }

    }

    private boolean sanityCheckOnModel() {
        // if we have no target branches
        if (this.createModel.getRemoteBranchDropdownModel().getSize() == 0) {

            final String warningText
                    = TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_NO_VALID_TARGET_WARNING_MESSAGE);
            final String warningTitle
                    = TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_SANITY_CHECK_FAILED_WARNING_TITLE);

            Messages.showWarningDialog(warningText, warningTitle);

            return false;
        }

        return true;
    }

    private ValidationInfo validate() {
        updateModel();

        ModelValidationInfo validationInfo = this.createModel.validate();
        if (validationInfo != ModelValidationInfo.NO_ERRORS) {
            return new ValidationInfo(validationInfo.getValidationMessage(),
                    this.createDialog.getComponent(validationInfo.getValidationSource()));

        }

        return null;
    }

    private void updateModel() {
        /* there are no action listener on the title and description field, so they must be updated manually */
        this.createModel.setTitle(this.createDialog.getTitle());
        this.createModel.setDescription(this.createDialog.getDescription());
    }

    @VisibleForTesting
    void setCreateDialog(CreatePullRequestDialog createDialog) {
        this.createDialog = createDialog;
    }

    @VisibleForTesting
    void setCreateModel(CreatePullRequestModel createModel) {
        this.createModel = createModel;
    }
}
