// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.microsoft.alm.plugin.idea.ui.checkout.CheckoutPageModel;
import com.microsoft.alm.plugin.idea.ui.common.forms.BasicForm;
import com.microsoft.alm.plugin.idea.ui.common.forms.LoginForm;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class LoginPageImpl extends JPanel implements LoginPage {
    private final String DATA_FORM = "dataForm";
    private final String LOGIN_FORM = "loginForm";
    private final BasicForm dataForm;
    private final LoginForm loginForm;
    private final CardLayout cardLayout;
    private boolean loginShowing;

    public LoginPageImpl(final LoginForm loginForm, final BasicForm dataForm) {
        cardLayout = new CardLayout();
        this.setLayout(cardLayout);

        this.dataForm = dataForm;
        this.loginForm = loginForm;
        setLoginShowing(true);

        // Make a telemetry entry for this UI dialog
        TfsTelemetryHelper.getInstance().sendDialogOpened(this.getClass().getName(), TfsTelemetryHelper.PropertyMapBuilder.EMPTY);
    }

    @Override
    public void addActionListener(final ActionListener listener) {
        // Hook up all actions
        loginForm.addActionListener(listener);
        dataForm.addActionListener(listener);
    }

    @Override
    public JComponent getComponent(final String name) {
        if (CheckoutPageModel.PROP_SERVER_NAME.equals(name)) {
            return loginForm.getServerNameComponent();
        }

        return null;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        if (loginShowing) {
            return loginForm.getPreferredFocusedComponent();
        } else {
            return dataForm.getPreferredFocusedComponent();
        }
    }

    @Override
    public String getServerName() {
        return loginForm.getServerName();
    }

    @Override
    public void setServerName(final String name) {
        loginForm.setServerName(name);
    }

    @Override
    public void setLoginShowing(final boolean showLogin) {
        if (this.getComponentCount() == 0) {
            this.add(dataForm.getContentPanel(), DATA_FORM);
            addComponentShownListener(dataForm);
            this.add(loginForm.getContentPanel(), LOGIN_FORM);
            addComponentShownListener(loginForm);
        }

        if (loginShowing != showLogin) {
            loginShowing = showLogin;
            if (showLogin) {
                cardLayout.show(this, LOGIN_FORM);
            } else {
                cardLayout.show(this, DATA_FORM);
            }
        }
    }

    @Override
    public void setAuthenticating(final boolean authenticating) {
        loginForm.setAuthenticating(authenticating);
    }

    private void addComponentShownListener(final BasicForm form) {
        form.getContentPanel().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                final JComponent c = form.getPreferredFocusedComponent();
                if (c != null) {
                    c.requestFocusInWindow();
                }
            }
        });
    }
}
