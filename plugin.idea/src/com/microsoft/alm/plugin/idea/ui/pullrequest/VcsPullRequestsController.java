// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * Controller for the Version Control Pull Requests Tab
 */
public class VcsPullRequestsController implements Observer, ActionListener {

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
    }

    public void actionPerformed(final ActionEvent e) {
        if (VcsPullRequestsForm.CMD_CREATE_NEW_PULL_REQUEST.equals(e.getActionCommand())) {
            model.createNewPullRequest();
        } else if (VcsPullRequestsForm.CMD_REFRESH.equals(e.getActionCommand())) {
            model.loadPullRequests();
        } else if (VcsPullRequestsForm.CMD_STATUS_LINK.equals(e.getActionCommand())) {
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
    }

    public void dispose() {
        model.dispose();
    }

    //setters for unit tests

    void setModel(final VcsPullRequestsModel model) {
        this.model = model;
    }

    void setView(final VcsPullRequestsTab tab) {
        this.tab = tab;
    }
}
