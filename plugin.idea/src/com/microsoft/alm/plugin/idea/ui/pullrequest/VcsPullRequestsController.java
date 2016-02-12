// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.ui.common.FeedbackAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

/**
 * Controller for the Version Control Pull Requests Tab
 */
public class VcsPullRequestsController extends MouseAdapter implements Observer, ActionListener {

    private VcsPullRequestsTab tab;
    private VcsPullRequestsModel model;

    // Default constructor for testing
    VcsPullRequestsController() {
    }

    public VcsPullRequestsController(final @NotNull Project project) {
        model = new VcsPullRequestsModel(project);
        tab = new VcsPullRequestsTab(project);

        setupTab();

        // Initialize the form with the current values from the model
        update(null, null);

        // add the observer and action listener after we are fully initialized, otherwise we will just get called
        // in the middle
        model.addObserver(this);

        //load the pull requests
        model.loadPullRequests();
    }

    private void setupTab() {
        tab.addActionListener(this);
        tab.addObserver(this);
        tab.addMouseListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        if (VcsPullRequestsForm.CMD_STATUS_LINK.equals(e.getActionCommand())) {
            if (!model.isConnected()) {
                //import into team services git
                model.importIntoTeamServicesGit();
            } else if (!model.isAuthenticated()) {
                //prompt for credentials and load pull requests
                model.loadPullRequests();
            } else {
                //open current repository in web
                model.openGitRepoLink();
            }
        }
    }

    public JComponent getPanel() {
        return tab.getPanel();
    }

    @Override
    public void update(final Observable observable, final Object arg) {
        if (arg == null
                || VcsPullRequestsModel.PROP_CONNECTED.equals(arg)
                || VcsPullRequestsModel.PROP_AUTHENTICATED.equals(arg)
                || VcsPullRequestsModel.PROP_LOADING.equals(arg)
                || VcsPullRequestsModel.PROP_AUTHENTICATING.equals(arg)
                || VcsPullRequestsModel.PROP_LOADING_ERRORS.equals(arg)) {
            tab.setConnectionStatus(model.isConnected(), model.isAuthenticating(), model.isAuthenticated(),
                    model.isLoading(), model.hasLoadingErrors());
        }
        if (arg == null || VcsPullRequestsModel.PROP_LAST_REFRESHED.equals(arg)) {
            tab.setLastRefreshed(model.getLastRefreshed());
        }
        if (arg == null) {
            tab.setPullRequestsTree(model.getPullRequestsTreeModel());
        }

        //actions from the form
        if (VcsPullRequestsForm.CMD_CREATE_NEW_PULL_REQUEST.equals(arg)) {
            model.createNewPullRequest();
        }
        if (VcsPullRequestsForm.CMD_REFRESH.equals(arg)) {
            model.loadPullRequests();
        }
        if (FeedbackAction.CMD_SEND_SMILE.equals(arg)) {
            model.sendFeedback(true);
        }
        if (FeedbackAction.CMD_SEND_FROWN.equals(arg)) {
            model.sendFeedback(false);
        }
    }

    public void dispose() {
        model.dispose();
    }

    @VisibleForTesting
    void setModel(final VcsPullRequestsModel model) {
        this.model = model;
    }

    @VisibleForTesting
    void setView(final VcsPullRequestsTab tab) {
        this.tab = tab;
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        super.mouseClicked(mouseEvent);
        //double click
        if (mouseEvent.getClickCount() == 2) {
            model.openSelectedPullRequestLink();
        } else if (mouseEvent.isPopupTrigger() || ((mouseEvent.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK)) {
            //right click, show pop up
            tab.showPopupMenu(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY(), this);
        }
    }
}
