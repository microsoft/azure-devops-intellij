// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.icons.AllIcons;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.controls.Hyperlink;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.ResourceBundle;

public class VcsPullRequestsForm {
    //controls
    private JPanel tabPanel;
    private JTree pullRequestsTree;
    private JToolBar actionsToolbar;
    private JLabel statusLabel;
    private JButton addButton;
    private JButton refreshButton;
    private Hyperlink statusLink;

    //commands
    public static final String CMD_REFRESH = "refresh";
    public static final String CMD_CREATE_NEW_PULL_REQUEST = "createNewPullRequest";
    public static final String CMD_STATUS_LINK = "statusLink";

    private boolean initialized = false;
    private Date lastRefreshed;
    private PullRequestsTreeModel pullRequestsTreeModel;

    public JComponent getPanel() {
        ensureInitialized();
        return tabPanel;
    }

    private void ensureInitialized() {
        if (!initialized) {
            pullRequestsTree.setCellRenderer(new PRTreeCellRenderer());
            pullRequestsTree.setRootVisible(false);
            pullRequestsTree.setRowHeight(0); //dynamically have row height computed for each row
            pullRequestsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

            addButton.setActionCommand(CMD_CREATE_NEW_PULL_REQUEST);
            refreshButton.setActionCommand(CMD_REFRESH);
            statusLink.setActionCommand(CMD_STATUS_LINK);

            this.initialized = true;
        }
    }

    public void setConnectionStatus(final boolean connected, final boolean authenticating, final boolean authenticated,
                                    final boolean loading, final boolean loadingErrors) {
        updateStatusText(connected, authenticating, authenticated, loading, loadingErrors);
    }

    private void updateStatusText(final boolean connected, final boolean authenticating, final boolean authenticated,
                                  final boolean loading, final boolean loadingErrors) {
        if (!connected) {
            statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_NOT_CONNECTED));
            statusLabel.setIcon(AllIcons.General.Error);
            statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TITLE));
            statusLink.setVisible(true);
            return;
        }

        if (authenticating) {
            statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_AUTHENTICATING));
            statusLabel.setIcon(AllIcons.General.Information);
            statusLink.setText("");
            statusLink.setVisible(false);
            return;
        }

        if (!authenticated) {
            statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_NOT_AUTHENTICATED));
            statusLabel.setIcon(AllIcons.General.Error);
            statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_SIGN_IN));
            statusLink.setVisible(true);
            return;
        }

        if (loading) {
            //Loading in progress
            statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_LOADING));
            statusLabel.setIcon(AllIcons.General.Information);
            statusLink.setText("");
            statusLink.setVisible(false);
            return;
        }

        if (loadingErrors) {
            statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_LOADING_ERRORS));
            statusLabel.setIcon(AllIcons.General.Warning);
            statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_OPEN_IN_BROWSER));
            statusLink.setVisible(true);
            return;
        }

        //loading complete
        if (lastRefreshed == null) {
            lastRefreshed = new Date();
        }
        statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_LAST_REFRESHED_AT, lastRefreshed.toString()));
        statusLabel.setIcon(AllIcons.General.Information);
        statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_OPEN_IN_BROWSER));
        statusLink.setVisible(true);

        //expand the tree
        if (pullRequestsTreeModel != null) {
            pullRequestsTree.expandRow(0);
            pullRequestsTree.expandRow(pullRequestsTreeModel.getRequestedByMeRoot().getChildCount() + 1);
        }
    }

    public void setLastRefreshed(final Date lastRefreshed) {
        this.lastRefreshed = lastRefreshed;
    }

    public void setPullRequestsTree(final PullRequestsTreeModel treeModel) {
        this.pullRequestsTreeModel = treeModel;
        pullRequestsTree.setModel(treeModel);
    }

    public void addActionListener(final ActionListener listener) {
        addButton.addActionListener(listener);
        refreshButton.addActionListener(listener);
        statusLink.addActionListener(listener);
    }

    //for unit testing
    String getStatusText() {
        return statusLabel.getText();
    }

    String getStatusLinkText() {
        return statusLink.getText();
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
        tabPanel = new JPanel();
        tabPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        actionsToolbar = new JToolBar();
        actionsToolbar.setFloatable(false);
        tabPanel.add(actionsToolbar, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        addButton = new JButton();
        addButton.setBorderPainted(false);
        addButton.setIcon(new ImageIcon(getClass().getResource("/general/add.png")));
        addButton.setText("");
        addButton.setToolTipText(ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("Actions.CreatePullRequest.Message"));
        actionsToolbar.add(addButton);
        refreshButton = new JButton();
        refreshButton.setBorderPainted(false);
        refreshButton.setIcon(new ImageIcon(getClass().getResource("/actions/refresh.png")));
        refreshButton.setOpaque(false);
        refreshButton.setText("");
        refreshButton.setToolTipText(ResourceBundle.getBundle("com/microsoft/alm/plugin/idea/ui/tfplugin").getString("CheckoutDialog.RefreshButton.ToolTip"));
        actionsToolbar.add(refreshButton);
        statusLabel = new JLabel();
        statusLabel.setText("Label");
        tabPanel.add(statusLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        tabPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        pullRequestsTree = new JTree();
        scrollPane1.setViewportView(pullRequestsTree);
        statusLink = new Hyperlink();
        statusLink.setText("");
        tabPanel.add(statusLink, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return tabPanel;
    }
}
