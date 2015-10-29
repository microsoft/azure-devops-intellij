// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.checkout;

import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.ui.common.forms.LoginForm;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * This class is a panel that switches between showing the VsoLoginForm and the CheckoutForm.
 * The loginShowing property controls which form is shown.
 */
class CheckoutPageImpl extends JPanel implements CheckoutPage {

    private final CheckoutForm checkoutForm;
    private final LoginForm loginForm;
    private boolean loginShowing;

    public CheckoutPageImpl(LoginForm loginForm, CheckoutForm checkoutForm) {
        this.setLayout(new BorderLayout());

        this.checkoutForm = checkoutForm;
        this.loginForm = loginForm;
        setLoginShowing(true);

        // Make a telemetry entry for this UI dialog
        TfsTelemetryHelper.getInstance().sendDialogOpened(this.getClass().getName(), TfsTelemetryHelper.PropertyMapBuilder.EMPTY);
    }

    @Override
    public void setLoginShowing(final boolean showLogin) {
        if (loginShowing != showLogin) {
            loginShowing = showLogin;
            this.removeAll();
            if (showLogin) {
                this.add(loginForm.getContentPanel(), BorderLayout.CENTER);
            } else {
                this.add(checkoutForm.getContentPanel(), BorderLayout.CENTER);
            }
            this.revalidate();
            this.repaint();
            initFocus();
        }
    }

    public void initFocus() {
        if (loginShowing) {
            loginForm.initFocus();
        } else {
            checkoutForm.initFocus();
        }
    }


    // Checkout form accessors
    @Override
    public void setRepositoryFilter(final String filter) {
        checkoutForm.setRepositoryFilter(filter);
    }

    @Override
    public String getRepositoryFilter() {
        return checkoutForm.getRepositoryFilter();
    }

    @Override
    public void setRepositoryTable(final ServerContextTableModel tableModel, final ListSelectionModel selectionModel) {
        checkoutForm.setRepositoryTable(tableModel, selectionModel);
    }

    @Override
    public void setParentDirectory(final String path) {
        checkoutForm.setParentDirectory(path);
    }

    @Override
    public String getParentDirectory() {
        return checkoutForm.getParentDirectory();
    }

    @Override
    public JComponent getComponent(final String name) {
        if (CheckoutPageModel.PROP_PARENT_DIR.equals(name)) {
            return checkoutForm.getParentDirectoryComponent();
        }
        if (CheckoutPageModel.PROP_DIRECTORY_NAME.equals(name)) {
            return checkoutForm.getDirectoryNameComponent();
        }
        if (CheckoutPageModel.PROP_SERVER_NAME.equals(name)) {
            return loginForm.getServerNameComponent();
        }
        return null;
    }

    @Override
    public void setDirectoryName(final String name) {
        checkoutForm.setDirectoryName(name);
    }

    @Override
    public String getDirectoryName() {
        return checkoutForm.getDirectoryName();
    }

    @Override
    public void setUserName(final String name) {
        checkoutForm.setUserName(name);
    }

    @Override
    public String getServerName() {
        return loginForm.getServerName();
    }

    @Override
    public void setServerName(final String name) {
        loginForm.setServerName(name);
        checkoutForm.setServerName(name);
    }

    @Override
    public void setLoading(final boolean loading) {
        checkoutForm.setLoading(loading);
    }

    @Override
    public void setAuthenticating(final boolean authenticating) {
        loginForm.setAuthenticating(authenticating);
    }

    @Override
    public void addActionListener(final ActionListener listener) {
        // Hook up all actions
        loginForm.addActionListener(listener);
        loginForm.getContentPanel().registerKeyboardAction(listener, LoginForm.CMD_ENTER_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        checkoutForm.addActionListener(listener);
    }
}
