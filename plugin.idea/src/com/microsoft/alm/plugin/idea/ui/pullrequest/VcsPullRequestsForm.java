// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.microsoft.alm.plugin.idea.resources.Icons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.FeedbackAction;
import com.microsoft.alm.plugin.idea.ui.controls.Hyperlink;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.Date;
import java.util.Observable;

public class VcsPullRequestsForm extends Observable {
    private JPanel tabPanel;
    private JPanel toolBarPanel;
    private ActionToolbar prActionsToolbar;
    private ActionToolbar feedbackActionsToolbar;
    private JScrollPane scrollPanel;
    private Tree pullRequestsTree;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private Hyperlink statusLink;

    //commands
    public static final String CMD_REFRESH = "refresh";
    public static final String CMD_CREATE_NEW_PULL_REQUEST = "createNewPullRequest";
    public static final String CMD_STATUS_LINK = "statusLink";
    public static final String CMD_OPEN_SELECTED_PR_IN_BROWSER = "openSelectedPullRequest";
    public static final String CMD_ABANDON_SELECTED_PR = "abandonSelectedPullRequest";
    public static final String CMD_COMPLETE_SELECTED_PR = "completeSelectedPullRequest";
    public static final String CMD_SEND_FEEDBACK = "sendFeedback";
    public static final String TOOLBAR_LOCATION = "Vcs.PullRequests";

    private boolean initialized = false;
    private Date lastRefreshed;
    private PullRequestsTreeModel pullRequestsTreeModel;

    public VcsPullRequestsForm() {
        ensureInitialized();
    }

    public JComponent getPanel() {
        ensureInitialized();
        return tabPanel;
    }

    private void ensureInitialized() {
        if (!initialized) {
            //Tree in a scroll panel
            pullRequestsTree = new Tree();
            pullRequestsTree.setCellRenderer(new PRTreeCellRenderer());
            pullRequestsTree.setShowsRootHandles(true);
            pullRequestsTree.setRootVisible(false);
            pullRequestsTree.setRowHeight(0); //dynamically have row height computed for each row
            scrollPanel = new JBScrollPane(pullRequestsTree);

            //toolbars
            if (ApplicationManager.getApplication() != null) {
                prActionsToolbar = createPullRequestActionsToolbar();
                feedbackActionsToolbar = createFeedbackActionsToolbar();
                toolBarPanel = new JPanel(new BorderLayout());
                toolBarPanel.add(prActionsToolbar.getComponent(), BorderLayout.LINE_START);
                toolBarPanel.add(feedbackActionsToolbar.getComponent(), BorderLayout.LINE_END);
            } else {
                //skip setup when called from unit tests
                toolBarPanel = new JPanel();
            }

            //status panel with label and link
            statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            statusLabel = new JLabel();
            statusLink = new Hyperlink();
            statusLink.setActionCommand(CMD_STATUS_LINK);
            statusPanel.add(statusLabel);
            statusPanel.add(statusLink);

            //tabPanel
            tabPanel = new JPanel(new BorderLayout());
            tabPanel.add(toolBarPanel, BorderLayout.PAGE_START);
            tabPanel.add(scrollPanel, BorderLayout.CENTER);
            tabPanel.add(statusPanel, BorderLayout.PAGE_END);

            this.initialized = true;
        }
    }

    private ActionToolbar createPullRequestActionsToolbar() {
        final AnAction createPullRequestAction = new AnAction("",
                TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_DIALOG_TITLE),
                AllIcons.ToolbarDecorator.Add) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                setChangedAndNotify(CMD_CREATE_NEW_PULL_REQUEST);
            }
        };
        final AnAction refreshAction = new AnAction("",
                TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REFRESH_TOOLTIP), AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                setChangedAndNotify(CMD_REFRESH);
            }
        };
        final DefaultActionGroup prActions = new DefaultActionGroup(createPullRequestAction, refreshAction);
        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_LOCATION, prActions, false);
        toolbar.setOrientation(SwingConstants.HORIZONTAL);
        toolbar.setTargetComponent(scrollPanel);
        return toolbar;
    }

    private ActionToolbar createFeedbackActionsToolbar() {
        //feedback actions toolbar
        final AnAction sendFeedback = new AnAction("",
                TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE), Icons.Smile) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                final FeedbackAction action = new FeedbackAction(anActionEvent.getProject(),
                        TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_TITLE));
                action.actionPerformed(new ActionEvent(anActionEvent.getInputEvent().getSource(),
                        anActionEvent.getInputEvent().getID(), CMD_SEND_FEEDBACK));
            }
        };

        final DefaultActionGroup feedbackActions = new DefaultActionGroup(sendFeedback);
        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_LOCATION, feedbackActions, false);
        toolbar.setOrientation(SwingConstants.HORIZONTAL);
        toolbar.setTargetComponent(scrollPanel);
        return toolbar;
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
        pullRequestsTree.setSelectionModel(treeModel.getSelectionModel());
    }

    public void addActionListener(final ActionListener listener) {
        statusLink.addActionListener(listener);
    }

    public void addMouseListener(final MouseListener listener) {
        pullRequestsTree.addMouseListener(listener);
    }

    protected void setChangedAndNotify(final String propertyName) {
        super.setChanged();
        super.notifyObservers(propertyName);
    }

    @VisibleForTesting
    String getStatusText() {
        return statusLabel.getText();
    }

    @VisibleForTesting
    String getStatusLinkText() {
        return statusLink.getText();
    }

}
