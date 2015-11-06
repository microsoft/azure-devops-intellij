// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport;

import com.microsoft.alm.plugin.idea.ui.common.LoginPageModel;
import com.microsoft.alm.plugin.idea.ui.common.forms.LoginForm;
import com.microsoft.alm.plugin.idea.ui.controls.UserAccountPanel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * This class binds the UI (ImportPage) with the Model (VsoImportPageModel) by attaching listeners to both and keeping them
 * in sync.
 */
public class ImportPageController implements Observer, ActionListener {
    private final ImportPage page;
    private final ImportPageModel pageModel;

    public ImportPageController(final ImportPageModel pageModel, final ImportPage page) {
        this.pageModel = pageModel;
        this.pageModel.addObserver(this);
        this.page = page;
        this.page.addActionListener(this);

        // Initialize the form with the current values from the model
        update(null, null);
    }

    public JPanel getPageAsPanel() {
        if (page instanceof JPanel) {
            return (JPanel) page;
        }

        return null;
    }

    public JComponent getComponent(final String name) {
        return page.getComponent(name);
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (arg == null || arg.equals(LoginPageModel.PROP_CONNECTED)) {
            page.setLoginShowing(!pageModel.isConnected());
        }
        if (arg == null || arg.equals(ImportPageModel.PROP_LOADING)) {
            page.setLoading(pageModel.isLoading());
        }
        if (arg == null || arg.equals(LoginPageModel.PROP_AUTHENTICATING)) {
            page.setAuthenticating(pageModel.isAuthenticating());
        }
        if (arg == null || arg.equals(ImportPageModel.PROP_REPO_NAME)) {
            page.setRepositoryName(pageModel.getRepositoryName());
        }
        if (arg == null || arg.equals(ImportPageModel.PROP_PROJECT_FILTER)) {
            page.setTeamProjectFilter(pageModel.getTeamProjectFilter());
        }
        if (arg == null || arg.equals(LoginPageModel.PROP_USER_NAME)) {
            page.setUserName(pageModel.getUserName());
        }
        if (arg == null || arg.equals(LoginPageModel.PROP_SERVER_NAME)) {
            page.setServerName(pageModel.getServerName());
        }
        if (arg == null) {
            page.setTeamProjectTable(pageModel.getTableModel(), pageModel.getTableSelectionModel());
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        // Update pageModel from page before we initiate any actions
        updateModel();
        pageModel.clearErrors();

        if (LoginForm.CMD_CREATE_ACCOUNT.equals(e.getActionCommand())) {
            // Go to the URL
            pageModel.gotoLink(LoginPageModel.URL_CREATE_ACCOUNT);
        } else if (LoginForm.CMD_LEARN_MORE.equals(e.getActionCommand())) {
            pageModel.gotoLink(LoginPageModel.URL_VSO_JAVA);
        }
        else if (LoginForm.CMD_SIGN_IN.equals(e.getActionCommand()) || LoginForm.CMD_ENTER_KEY.equals(e.getActionCommand())) {
            // User pressed Enter or clicked sign in on the login page
            // Asynchronously query for projects, will prompt for login if needed
            pageModel.loadTeamProjects();
        } else if (ImportForm.CMD_REFRESH.equals(e.getActionCommand())) {
            // Reload the table (the refresh button shouldn't be visible if the query is currently running)
            pageModel.loadTeamProjects();
        } else if (UserAccountPanel.CMD_SIGN_OUT.equals(e.getActionCommand())) {
            // Go back to a disconnected state
            pageModel.signOut();
        } else if (ImportForm.CMD_PROJECT_FILTER_CHANGED.equals(e.getActionCommand())) {
            // No action needed here. We updated the pageModel above which should filter the list automatically.
        }
    }

    public void updateModel() {
        // Update model from page before we initiate any actions
        pageModel.setRepositoryName(page.getRepositoryName());
        pageModel.setTeamProjectFilter(page.getTeamProjectFilter());
        pageModel.setServerName(page.getServerName());
    }
}
