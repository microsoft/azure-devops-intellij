// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowser;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.SwingHelper;
import com.microsoft.alm.plugin.idea.ui.common.forms.BasicForm;
import com.microsoft.alm.plugin.idea.ui.controls.BusySpinnerPanel;
import git4idea.GitBranch;
import git4idea.GitRemoteBranch;
import git4idea.repo.GitRepository;
import git4idea.ui.GitCommitListWithDiffPanel;
import git4idea.util.GitCommitCompareInfo;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ResourceBundle;

public class CreatePullRequestForm implements BasicForm {
    /* commands */
    public static final String CMD_TARGET_BRANCH_UPDATED = "cmdTargetBranchDropDownChanged";

    private JComboBox targetBranchDropdown;
    private JLabel sourceBranch;
    private JLabel sourceBranchLabel;
    private JPanel contentPanel;
    private JTabbedPane quickDiffPane;
    private JLabel titleLabel;
    private JTextField titleTextField;
    private JLabel descriptionLabel;
    private JTextArea descriptionTextArea;
    private JLabel targetBranchLabel;
    private JLabel loadingLabel;
    private BusySpinnerPanel spinner;
    private JPanel spinnerPanel;
    private JScrollPane descriptionScrollPane;
    private JSplitPane splitPane;

    private boolean initialized = false;

    @Override
    public JPanel getContentPanel() {
        ensureInitialized();
        return this.contentPanel;
    }

    private void ensureInitialized() {
        if (!this.initialized) {
            // Make sure the busy spinner size is scaled properly
            Dimension spinnerSize = new Dimension(JBUI.scale(20), JBUI.scale(20));
            spinner.setPreferredSize(spinnerSize);
            spinner.setMinimumSize(spinnerSize);

            // Fix tab keys on text area
            SwingHelper.fixTabKeys(descriptionTextArea);

            // Make sure the comment field has a reasonable height, margins, and font
            SwingHelper.setPreferredHeight(descriptionScrollPane, 80);
            SwingHelper.copyFontAndMargins(descriptionTextArea, titleTextField);

            // Give the description field a min size
            Dimension descriptionScrollPaneSize = new Dimension(JBUI.scale(80), JBUI.scale(80));
            descriptionScrollPane.setPreferredSize(descriptionScrollPaneSize);
            descriptionScrollPane.setMinimumSize(descriptionScrollPaneSize);

            // Make sure splitter is big enough in all DPIs
            splitPane.setDividerSize(JBUI.scale(7));

            this.initialized = true;
        }
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return titleTextField;
    }

    @Override
    public void addActionListener(final ActionListener listener) {
        this.targetBranchDropdown.addActionListener(listener);
    }

    public void setTargetBranchDropdownModel(ComboBoxModel model) {
        if (model != null) {
            this.targetBranchDropdown.setModel(model);
        }
    }

    public void setSourceBranch(GitBranch currentBranch) {
        if (currentBranch != null) {
            this.sourceBranch.setText(currentBranch.getName());
        }
    }

    public void setTitleTextField(final String title) {
        this.titleTextField.setText(title);
    }

    public String getTitleText() {
        return this.titleTextField.getText();
    }

    public void setDescriptionTextArea(final String description) {
        this.descriptionTextArea.setText(description);
    }

    public String getDescriptionText() {
        return this.descriptionTextArea.getText();
    }

    public GitRemoteBranch getSelectedRemoteBranch() {
        Object o = this.targetBranchDropdown.getSelectedItem();
        if (o instanceof GitRemoteBranch) {
            return (GitRemoteBranch) this.targetBranchDropdown.getSelectedItem();
        }

        return null;
    }

    public void setSelectedTargetBranch(final GitRemoteBranch targetBranch) {
        if (targetBranch != null && this.targetBranchDropdown != null) {
            this.targetBranchDropdown.setSelectedItem(targetBranch);
        }
    }

    public JComponent getComponent(@NotNull final String componentPropName) {
        if (componentPropName.equals(CreatePullRequestModel.PROP_TITLE)) {
            return this.titleTextField;
        }

        if (componentPropName.equals(CreatePullRequestModel.PROP_DESCRIPTION)) {
            return this.descriptionTextArea;
        }

        if (componentPropName.equals(CreatePullRequestModel.PROP_SOURCE_BRANCH)) {
            return this.sourceBranch;
        }

        if (componentPropName.equals(CreatePullRequestModel.PROP_TARGET_BRANCH)) {
            return this.targetBranchDropdown;
        }

        return null;
    }

    public void setLoading(boolean loading) {
        if (loading) {
            this.quickDiffPane.removeAll();
            this.spinnerPanel.setVisible(true);
            this.spinner.start(true);
        } else {
            this.spinnerPanel.setVisible(false);
            this.spinner.start(false);
        }
    }

    /**
     * This should only be called from the UI thread anyway, adding synchronized keyword
     * just in case
     */
    public synchronized void populateDiffPane(@NotNull final Project project,
                                              @NotNull final GitRepository gitRepository,
                                              @NotNull final String sourceBranchBeingCompared,
                                              @NotNull final String targetBranchBeingCompared,
                                              @NotNull final GitCommitCompareInfo myCompareInfo) {
        final GitRemoteBranch gitRemoteBranch = this.getSelectedRemoteBranch();
        final String currBranch = this.sourceBranch.getText();

        if (gitRemoteBranch != null && StringUtils.equalsIgnoreCase(gitRemoteBranch.getName(), targetBranchBeingCompared)
                && StringUtils.isNotEmpty(currBranch) && StringUtils.equalsIgnoreCase(currBranch, sourceBranchBeingCompared)) {

            this.quickDiffPane.removeAll();

            JComponent myDiffPanel = createDiffPaneBrowser(project, myCompareInfo);
            this.quickDiffPane.addTab(TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_CHANGES_PANE_TITLE),
                    AllIcons.Actions.Diff, myDiffPanel);

            JComponent myCommitsPanel = createCommitsListPane(project, gitRepository, myCompareInfo);
            this.quickDiffPane.addTab(TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_COMMITS_PANE_TITLE),
                    AllIcons.Actions.Commit, myCommitsPanel);
        }
    }

    private JComponent createCommitsListPane(final Project project, final GitRepository gitRepository,
                                             final GitCommitCompareInfo compareInfo) {
        return new GitCommitListWithDiffPanel(project, compareInfo.getBranchToHeadCommits(gitRepository));
    }

    private JComponent createDiffPaneBrowser(final Project project, final GitCommitCompareInfo compareInfo) {
        List<Change> diff = compareInfo.getTotalDiff();
        final ChangesBrowser changesBrowser = new ChangesBrowser(project, null, diff, null, false, true,
                null, ChangesBrowser.MyUseCase.COMMITTED_CHANGES, null);
        changesBrowser.setChangesToDisplay(diff);
        return changesBrowser;
    }

    private void createUIComponents() {
        this.targetBranchDropdown = new JComboBox();
        this.targetBranchDropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list,
                                                          Object gitRemoteBranch,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                return super.getListCellRendererComponent(list,
                        gitRemoteBranch != null ? ((GitRemoteBranch) gitRemoteBranch).getName() : "",
                        index,
                        isSelected,
                        cellHasFocus);
            }
        });
        this.targetBranchDropdown.setActionCommand(CMD_TARGET_BRANCH_UPDATED);

    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayoutManager(9, 4, new Insets(0, 0, 0, 0), -1, -1));
        sourceBranchLabel = new JLabel();
        this.$$$loadLabelText$$$(sourceBranchLabel, ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("CreatePullRequestDialog.SourceBranchLabel"));
        contentPanel.add(sourceBranchLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleLabel = new JLabel();
        this.$$$loadLabelText$$$(titleLabel, ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("CreatePullRequestDialog.TitleLabel"));
        contentPanel.add(titleLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        descriptionLabel = new JLabel();
        this.$$$loadLabelText$$$(descriptionLabel, ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("CreatePullRequestDialog.DescriptionLabel"));
        contentPanel.add(descriptionLabel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleTextField = new JTextField();
        contentPanel.add(titleTextField, new GridConstraints(5, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        targetBranchLabel = new JLabel();
        this.$$$loadLabelText$$$(targetBranchLabel, ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("CreatePullRequestDialog.TargetBranchLabel"));
        contentPanel.add(targetBranchLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contentPanel.add(targetBranchDropdown, new GridConstraints(2, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sourceBranch = new JLabel();
        sourceBranch.setText("");
        contentPanel.add(sourceBranch, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        splitPane = new JSplitPane();
        splitPane.setOrientation(0);
        contentPanel.add(splitPane, new GridConstraints(7, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        descriptionScrollPane = new JScrollPane();
        splitPane.setLeftComponent(descriptionScrollPane);
        descriptionTextArea = new JTextArea();
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionScrollPane.setViewportView(descriptionTextArea);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(panel1);
        quickDiffPane = new JTabbedPane();
        panel1.add(quickDiffPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        spinnerPanel = new JPanel();
        spinnerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel1.add(spinnerPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        spinner = new BusySpinnerPanel();
        spinnerPanel.add(spinner);
        loadingLabel = new JLabel();
        this.$$$loadLabelText$$$(loadingLabel, ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("CreatePullRequestDialog.LoadingDiffLabel"));
        spinnerPanel.add(loadingLabel);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPanel;
    }
}
