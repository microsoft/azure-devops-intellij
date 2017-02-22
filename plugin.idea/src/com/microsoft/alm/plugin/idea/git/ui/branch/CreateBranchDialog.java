// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.branch;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialogImpl;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryConstants;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import git4idea.GitRemoteBranch;
import org.apache.commons.lang.StringUtils;

import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import java.awt.event.ActionListener;

/**
 * Dialog for creating a new branch from an existing remote branch
 */
public class CreateBranchDialog extends BaseDialogImpl {
    private CreateBranchForm createBranchForm;

    public CreateBranchDialog(final Project project, final String title, final String okButtonText, final String feedbackContext) {
        super(project, title, okButtonText, feedbackContext);
        super.setTitle(title);
        super.setOKButtonText(okButtonText);
        super.init();

        // Make a telemetry entry for this UI dialog
        TfsTelemetryHelper.getInstance().sendDialogOpened(this.getClass().getName(),
                new TfsTelemetryHelper.PropertyMapBuilder()
                        .activeServerContext()
                        .pair(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_DIALOG, title)
                        .build());
    }

    @Override
    protected JComponent createCenterPanel() {
        createBranchForm = new CreateBranchForm();
        return createBranchForm.getContentPanel();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return createBranchForm.getPreferredFocusedComponent();
    }

    @Override
    public void addActionListener(final ActionListener listener) {
        super.addActionListener(listener);
        createBranchForm.addActionListener(listener);
    }

    public void setRemoteBranchDropdownModel(final ComboBoxModel model) {
        createBranchForm.setRemoteBranchDropdownModel(model);
    }

    public void setSelectedRemoteBranch(final GitRemoteBranch targetBranch) {
        createBranchForm.setSelectedRemoteBranch(targetBranch);
    }

    public GitRemoteBranch getSelectedRemoteBranch() {
        return createBranchForm.getSelectedRemoteBranch();
    }

    public void setBranchName(final String path) {
        createBranchForm.setBranchName(path);
    }

    public String getBranchName() {
        return createBranchForm.getBranchName();
    }

    public void setCheckoutBranch(boolean checkoutBranch) {
        createBranchForm.setCheckoutBranch(checkoutBranch);
    }

    public boolean getCheckoutBranch() {
        return createBranchForm.getCheckoutBranch();
    }

    public JComponent getComponent(final String componentPropName) {
        if (StringUtils.isEmpty(componentPropName)) {
            return null;
        }
        return createBranchForm.getComponent(componentPropName);
    }

    @Override
    public void setOkEnabled(final boolean enabled) {
        super.setOkEnabled(enabled);
    }

    @Override
    public void displayError(final String message) {
        setErrorText(message);
    }
}
