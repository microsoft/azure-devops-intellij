// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabFormImpl;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

public class VcsPullRequestsForm extends TabFormImpl {
    private Tree pullRequestsTree;

    //commands
    public static final String CMD_CREATE_NEW_PULL_REQUEST = "createNewPullRequest";
    public static final String CMD_OPEN_SELECTED_PR_IN_BROWSER = "openSelectedPullRequest";
    public static final String CMD_ABANDON_SELECTED_PR = "abandonSelectedPullRequest";
    public static final String TOOLBAR_LOCATION = "Vcs.PullRequests";

    private PullRequestsTreeModel pullRequestsTreeModel;

    public VcsPullRequestsForm() {
        super(TfPluginBundle.KEY_VCS_PR_TITLE,
                TfPluginBundle.KEY_CREATE_PR_DIALOG_TITLE,
                CMD_CREATE_NEW_PULL_REQUEST,
                TfPluginBundle.KEY_VCS_PR_REFRESH_TOOLTIP,
                TOOLBAR_LOCATION);

        ensureInitialized();
    }

    protected void createCustomView() {
        //Tree in a scroll panel
        pullRequestsTree = new Tree();
        pullRequestsTree.setCellRenderer(new PRTreeCellRenderer());
        pullRequestsTree.setShowsRootHandles(true);
        pullRequestsTree.setRootVisible(false);
        pullRequestsTree.setRowHeight(0); //dynamically have row height computed for each row
        scrollPanel = new JBScrollPane(pullRequestsTree);
    }

    protected void updateViewOnLoad() {
        //expand the tree
        if (pullRequestsTreeModel != null) {
            pullRequestsTree.expandRow(0);
            pullRequestsTree.expandRow(pullRequestsTreeModel.getRequestedByMeRoot().getChildCount() + 1);
        }
    }

    public void setPullRequestsTree(final PullRequestsTreeModel treeModel) {
        this.pullRequestsTreeModel = treeModel;
        pullRequestsTree.setModel(treeModel);
        pullRequestsTree.setSelectionModel(treeModel.getSelectionModel());
        searchFilter.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                onFilterChanged();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                onFilterChanged();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                onFilterChanged();
            }

            private void onFilterChanged() {
                if (timer.isRunning()) {
                    timer.restart();
                } else {
                    timer.start();
                }
            }
        });
    }

    public void addActionListener(final ActionListener listener) {
        super.addActionListener(listener);
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

    protected List<JBMenuItem> getMenuItems(final ActionListener listener) {
        return Arrays.asList(
                createMenuItem(TfPluginBundle.KEY_VCS_OPEN_IN_BROWSER, null, VcsPullRequestsForm.CMD_OPEN_SELECTED_PR_IN_BROWSER, listener),
                createMenuItem(TfPluginBundle.KEY_VCS_PR_ABANDON, null, VcsPullRequestsForm.CMD_ABANDON_SELECTED_PR, listener));
    }

    @VisibleForTesting
    Tree getPullRequestTree() {
        return pullRequestsTree;
    }
}
