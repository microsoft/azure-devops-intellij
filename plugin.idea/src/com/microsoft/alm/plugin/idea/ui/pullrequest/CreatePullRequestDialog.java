// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.BaseDialogImpl;
import git4idea.GitBranch;
import git4idea.GitRemoteBranch;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.event.ActionListener;

/**
 * UI class for pull request creation dialog
 */
public class CreatePullRequestDialog extends BaseDialogImpl {

    private CreatePullRequestForm createPullRequestForm;

    public CreatePullRequestDialog(final Project project) {
        super(project,
                TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_DIALOG_CREATE_BUTTON),
                TfPluginBundle.KEY_CREATE_PR_DIALOG_TITLE);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        createPullRequestForm = new CreatePullRequestForm();
        final JPanel createPRForm = createPullRequestForm.getContentPanel();
        createPRForm.setPreferredSize(new Dimension(600, 800));
        return createPRForm;
    }

    public void addActionListener(final ActionListener listener) {
        super.addActionListener(listener);
        this.createPullRequestForm.addActionListener(listener);
    }

    public void setTargetBranchDropdownModel(final ComboBoxModel model) {
        this.createPullRequestForm.setTargetBranchDropdownModel(model);
    }

    public void setSourceBranch(final GitBranch currentBranch) {
        this.createPullRequestForm.setSourceBranch(currentBranch);
    }

    public GitRemoteBranch getSelectedTargetBranch() {
        return this.createPullRequestForm.getSelectedRemoteBranch();
    }

    public void populateDiff(final Project project, final GitChangesContainer changesContainer) {
        if (project != null && changesContainer != null) {
            this.createPullRequestForm.populateDiffPane(project, changesContainer.getGitRepository(),
                    changesContainer.getSourceBranchName(), changesContainer.getTargetBranchName(),
                    changesContainer.getGitCommitCompareInfo());
        }
    }

    public void setTitle(final String title) {
        this.createPullRequestForm.setTitleTextField(title);
    }

    public String getTitle() {
        return this.createPullRequestForm.getTitleText();
    }

    public void setDescription(final String description) {
        this.createPullRequestForm.setDescriptionTextArea(description);
    }

    public String getDescription() {
        return this.createPullRequestForm.getDescriptionText();
    }

    public JComponent getComponent(final String componentPropName) {
        if (StringUtils.isEmpty(componentPropName)) {
            return null;
        }

        return this.createPullRequestForm.getComponent(componentPropName);
    }

    public void setSelectedTargetBranch(final GitRemoteBranch targetBranch) {
        this.createPullRequestForm.setSelectedTargetBranch(targetBranch);
    }

    public void setIsLoading(final boolean loading) {
        this.createPullRequestForm.setLoading(loading);
    }
}
