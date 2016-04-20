// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.ui.common.tabs.Tab;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabImpl;
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
    private static final String EVENT_NAME = "Pull Requests";

    private Tab tab;
    private VcsPullRequestsModel model;

    // Default constructor for testing
    VcsPullRequestsController() {
    }

    public VcsPullRequestsController(final @NotNull Project project) {
        model = new VcsPullRequestsModel(project);
        tab = new TabImpl(new VcsPullRequestsForm(), EVENT_NAME);

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
    }

    public void actionPerformed(final ActionEvent e) {
        if (VcsPullRequestsForm.CMD_STATUS_LINK.equals(e.getActionCommand())) {
            if (model.getTabStatus() == VcsTabStatus.NOT_TF_GIT_REPO) {
                //import into team services git
                model.importIntoTeamServicesGit();
            } else if (model.getTabStatus() == VcsTabStatus.NO_AUTH_INFO) {
                //prompt for credentials and load pull requests
                model.loadPullRequests();
            } else {
                //open current repository in web
                model.openGitRepoLink();
            }
        } else if (VcsPullRequestsForm.CMD_OPEN_SELECTED_PR_IN_BROWSER.equals(e.getActionCommand())) {
            //pop up menu - open PR link in web
            model.openSelectedPullRequestLink();
        } else if (VcsPullRequestsForm.CMD_ABANDON_SELECTED_PR.equals(e.getActionCommand())) {
            //pop up menu - abandon PR
            model.abandonSelectedPullRequest();
        }
    }

    public JComponent getPanel() {
        return tab.getPanel();
    }

    @Override
    public void update(final Observable observable, final Object arg) {
        if (arg == null
                || VcsPullRequestsModel.PROP_PR_TAB_STATUS.equals(arg)) {
            tab.setStatus(model.getTabStatus());
        }
        if (arg == null) {
            tab.setViewModel(model.getPullRequestsTreeModel());
        }

        //actions from the form
        if (VcsPullRequestsForm.CMD_CREATE_NEW_PULL_REQUEST.equals(arg)) {
            model.createNewPullRequest();
        }
        if (VcsPullRequestsForm.CMD_REFRESH.equals(arg)) {
            model.loadPullRequests();
        }
        if (VcsPullRequestsForm.CMD_OPEN_SELECTED_PR_IN_BROWSER.equals(arg)) {
            model.openSelectedPullRequestLink();
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
    void setView(final TabImpl tab) {
        this.tab = tab;
    }
}
