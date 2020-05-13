// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.checkout;

import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractController;
import com.microsoft.alm.plugin.idea.common.ui.common.LoginPageModel;
import com.microsoft.alm.plugin.idea.common.ui.common.forms.LoginForm;
import com.microsoft.alm.plugin.idea.common.ui.controls.UserAccountPanel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.util.Observable;

/**
 * This class binds the UI with the Model by attaching listeners to both and keeping them
 * in sync.
 */
class CheckoutPageController extends AbstractController {
    private final CheckoutPage page;
    private final CheckoutPageModel model;
    private final CheckoutController parentController;

    public CheckoutPageController(final CheckoutController parentController, final CheckoutPageModel model, final CheckoutPage page) {
        this.parentController = parentController;
        this.model = model;
        this.model.addObserver(this);
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
            page.setLoginShowing(!model.isConnected());
        }
        if (arg == null || arg.equals(CheckoutPageModel.PROP_LOADING)) {
            page.setLoading(model.isLoading());
        }
        if (arg == null || arg.equals(CheckoutPageModel.PROP_ADVANCED)) {
            page.setAdvanced(model.isAdvanced());
        }
        if (arg == null || arg.equals(LoginPageModel.PROP_AUTHENTICATING)) {
            page.setAuthenticating(model.isAuthenticating());
        }
        if (arg == null || arg.equals(CheckoutPageModel.PROP_DIRECTORY_NAME)) {
            page.setDirectoryName(model.getDirectoryName());
        }
        if (arg == null || arg.equals(CheckoutPageModel.PROP_PARENT_DIR)) {
            page.setParentDirectory(model.getParentDirectory());
        }
        if (arg == null || arg.equals(CheckoutPageModel.PROP_REPO_FILTER)) {
            page.setRepositoryFilter(model.getRepositoryFilter());
        }
        if (arg == null || arg.equals(LoginPageModel.PROP_USER_NAME)) {
            page.setUserName(model.getUserName());
        }
        if (arg == null || arg.equals(LoginPageModel.PROP_SERVER_NAME)) {
            page.setServerName(model.getServerName());
        }
        if (arg == null) {
            page.setRepositoryTable(model.getTableModel(), model.getTableSelectionModel());
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        // Update model from page before we initiate any actions
        updateModel();
        model.clearErrors();

        if (LoginForm.CMD_SIGN_IN.equals(e.getActionCommand())) {
            // User pressed Enter or clicked sign in on the login page
            // Asynchronously query for repositories, will prompt for login if needed
            model.loadRepositories();
            super.requestFocus(page);
        } else if (CheckoutForm.CMD_REFRESH.equals(e.getActionCommand())) {
            // Reload the table (the refresh button shouldn't be visible if the query is currently running)
            model.loadRepositories();
        } else if (UserAccountPanel.CMD_SIGN_OUT.equals(e.getActionCommand())) {
            // Go back to a disconnected state
            model.setConnected(false);
            model.signOut();
            super.requestFocus(page);
        } else if (CheckoutForm.CMD_REPO_FILTER_CHANGED.equals(e.getActionCommand())) {
            // No action needed here. We updated the model above which should filter the list automatically.
        } else if (LoginForm.CMD_CREATE_ACCOUNT.equals(e.getActionCommand())) {
            model.gotoLink(CheckoutPageModel.URL_CREATE_ACCOUNT);
        } else if (LoginForm.CMD_LEARN_MORE.equals(e.getActionCommand())) {
            model.gotoLink(CheckoutPageModel.URL_VSO_JAVA);
        } else if (CheckoutForm.CMD_GOTO_TFS.equals(e.getActionCommand())) {
            parentController.gotoEnterVsoURL();
        } else if (CheckoutForm.CMD_GOTO_SPS_PROFILE.equals(e.getActionCommand())) {
            model.gotoLink(VsoAuthenticationProvider.VSO_AUTH_URL);
        }
    }

    @Override
    protected void updateModel() {
        model.setParentDirectory(page.getParentDirectory());
        model.setDirectoryName(page.getDirectoryName());
        model.setRepositoryFilter(page.getRepositoryFilter());
        model.setServerName(page.getServerName());
        model.setAdvanced(page.getAdvanced());
        model.setTfvcServerCheckout(page.isTfvcServerCheckout());
    }
}
