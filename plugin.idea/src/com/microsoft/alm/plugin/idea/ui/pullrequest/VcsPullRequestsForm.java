// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.microsoft.alm.plugin.idea.resources.Icons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.FeedbackAction;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.ui.controls.Hyperlink;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.Observable;

public class VcsPullRequestsForm extends Observable {
    private JPanel tabPanel;
    private JScrollPane scrollPanel;
    private Tree pullRequestsTree;
    private JLabel statusLabel;
    private Hyperlink statusLink;

    //commands
    public static final String CMD_REFRESH = "refresh";
    public static final String CMD_CREATE_NEW_PULL_REQUEST = "createNewPullRequest";
    public static final String CMD_STATUS_LINK = "statusLink";
    public static final String CMD_OPEN_SELECTED_PR_IN_BROWSER = "openSelectedPullRequest";
    public static final String CMD_ABANDON_SELECTED_PR = "abandonSelectedPullRequest";
    public static final String CMD_SEND_FEEDBACK = "sendFeedback";
    public static final String TOOLBAR_LOCATION = "Vcs.PullRequests";

    private boolean initialized = false;
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
            final JPanel toolBarPanel;
            if (ApplicationManager.getApplication() != null) {
                final ActionToolbar prActionsToolbar = createPullRequestActionsToolbar();
                final ActionToolbar feedbackActionsToolbar = createFeedbackActionsToolbar();
                toolBarPanel = new JPanel(new BorderLayout());
                toolBarPanel.add(prActionsToolbar.getComponent(), BorderLayout.LINE_START);
                toolBarPanel.add(feedbackActionsToolbar.getComponent(), BorderLayout.LINE_END);
            } else {
                //skip setup when called from unit tests
                toolBarPanel = new JPanel();
            }

            //status panel with label and link
            final JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
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
        final AnAction createPullRequestAction = new AnAction(TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_CREATE_PR_DIALOG_TITLE),
                AllIcons.ToolbarDecorator.Add) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                setChangedAndNotify(CMD_CREATE_NEW_PULL_REQUEST);
            }
        };
        createPullRequestAction.registerCustomShortcutSet(CommonShortcuts.getNew(), scrollPanel); //Ctrl+N on windows or Cmd+M on Mac

        final AnAction refreshAction = new AnAction(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REFRESH_TOOLTIP),
                TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REFRESH_TOOLTIP), AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                setChangedAndNotify(CMD_REFRESH);
            }
        };
        refreshAction.registerCustomShortcutSet(CommonShortcuts.getRerun(), scrollPanel); //Ctrl+R on windows or Cmd+R on Mac

        final DefaultActionGroup prActions = new DefaultActionGroup(createPullRequestAction, refreshAction);
        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_LOCATION, prActions, false);
        toolbar.setOrientation(SwingConstants.HORIZONTAL);
        toolbar.setTargetComponent(scrollPanel);
        return toolbar;
    }

    private ActionToolbar createFeedbackActionsToolbar() {
        //feedback actions toolbar
        final AnAction sendFeedback = new AnAction(TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE),
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

    public void setStatus(final VcsTabStatus status) {
        switch (status) {
            case NOT_TF_GIT_REPO:
                //Git repository remote is not on TFS or VSTS
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_NOT_CONNECTED));
                statusLabel.setIcon(AllIcons.General.Error);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TITLE));
                statusLink.setVisible(true);
                break;
            case NO_AUTH_INFO:
                //Couldn't find authentication info
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_NOT_AUTHENTICATED));
                statusLabel.setIcon(AllIcons.General.Error);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_SIGN_IN));
                statusLink.setVisible(true);
                break;
            case LOADING_IN_PROGRESS:
                //Loading in progress
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_LOADING));
                statusLabel.setIcon(AllIcons.General.Information);
                statusLink.setText("");
                statusLink.setVisible(false);
                break;
            case LOADING_COMPLETED_ERRORS:
                //unexpected errors during loading
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_LOADING_ERRORS));
                statusLabel.setIcon(AllIcons.General.Warning);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_OPEN_IN_BROWSER));
                statusLink.setVisible(true);
                break;
            case LOADING_COMPLETED:
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_LAST_REFRESHED_AT, new Date().toString()));
                statusLabel.setIcon(AllIcons.General.Information);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_OPEN_IN_BROWSER));
                statusLink.setVisible(true);

                //expand the tree
                if (pullRequestsTreeModel != null) {
                    pullRequestsTree.expandRow(0);
                    pullRequestsTree.expandRow(pullRequestsTreeModel.getRequestedByMeRoot().getChildCount() + 1);
                }
                break;
            default:
                //we should never get here, no action
        }
    }

    public void setPullRequestsTree(final PullRequestsTreeModel treeModel) {
        this.pullRequestsTreeModel = treeModel;
        pullRequestsTree.setModel(treeModel);
        pullRequestsTree.setSelectionModel(treeModel.getSelectionModel());
    }

    public void addActionListener(final ActionListener listener) {
        statusLink.addActionListener(listener);
        addTreeEventListeners(listener);
    }

    private void addTreeEventListeners(final ActionListener listener) {
        //mouse listener
        pullRequestsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                //double click
                if (mouseEvent.getClickCount() == 2) {
                    setChangedAndNotify(CMD_OPEN_SELECTED_PR_IN_BROWSER);
                } else if (mouseEvent.isPopupTrigger() || ((mouseEvent.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK)) {
                    //right click, show pop up
                    showPopupMenu(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY(), listener);
                }
            }
        });

        //keyboard listener
        pullRequestsTree.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    setChangedAndNotify(CMD_OPEN_SELECTED_PR_IN_BROWSER);
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });
    }

    private void showPopupMenu(final Component component, final int x, final int y, final ActionListener listener) {
        final JBPopupMenu menu = new JBPopupMenu();
        final JBMenuItem openMenuItem = createMenuItem(TfPluginBundle.KEY_VCS_PR_OPEN_IN_BROWSER, null, VcsPullRequestsForm.CMD_OPEN_SELECTED_PR_IN_BROWSER, listener);
        menu.add(openMenuItem);
        final JBMenuItem abandonMenuItem = createMenuItem(TfPluginBundle.KEY_VCS_PR_ABANDON, null, VcsPullRequestsForm.CMD_ABANDON_SELECTED_PR, listener);
        menu.add(abandonMenuItem);
        menu.show(component, x, y);
    }

    private JBMenuItem createMenuItem(final String resourceKey, final Icon icon, final String actionCommand, final ActionListener listener) {
        final String text = TfPluginBundle.message(resourceKey);
        final JBMenuItem menuItem = new JBMenuItem(text, icon);
        menuItem.setActionCommand(actionCommand);
        menuItem.addActionListener(listener);
        return menuItem;
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
