// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport;

import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.ui.common.forms.LoginForm;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * This class is a panel that switches between showing the TfsLoginForm and the ImportForm.
 * The loginShowing property controls which form is shown.
 */
public class ImportPageImpl extends JPanel implements ImportPage {

    private final ImportForm importForm;
    private final LoginForm loginForm;
    private boolean loginShowing;

    public ImportPageImpl(LoginForm loginForm, ImportForm importForm) {
        this.setLayout(new BorderLayout());

        this.loginForm = loginForm;
        this.importForm = importForm;
        setLoginShowing(true);

        // Make a telemetry entry for this UI dialog
        TfsTelemetryHelper.getInstance().sendDialogOpened(this.getClass().getName(), TfsTelemetryHelper.PropertyMapBuilder.EMPTY);
    }

    @Override
    public void addActionListener(final ActionListener listener) {
        //hook up all action listeners
        loginForm.addActionListener(listener);
        loginForm.getContentPanel().registerKeyboardAction(listener, LoginForm.CMD_ENTER_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        importForm.addActionListener(listener);
    }

    @Override
    public void setLoginShowing(final boolean showLogin) {
        if (loginShowing != showLogin) {
            loginShowing = showLogin;
            this.removeAll();
            if (showLogin) {
                this.add(loginForm.getContentPanel(), BorderLayout.CENTER);
            }
            else {
                this.add(importForm.getContentPanel(), BorderLayout.CENTER);
            }
            this.revalidate();
            this.repaint();
            initFocus();
        }
    }

    public void initFocus(){
        if (loginShowing) {
            loginForm.initFocus();
        } else {
            importForm.initFocus();
        }
    }

    @Override
    public void setLoading(final boolean loading) {
        importForm.setLoading(loading);
    }

    @Override
    public void setAuthenticating(final boolean authenticating) {
        loginForm.setAuthenticating(authenticating);
    }

    @Override
    public void setTeamProjectFilter(final String filter) {
        importForm.setTeamProjectFilter(filter);
    }

    @Override
    public String getTeamProjectFilter() {
        return importForm.getTeamProjectFilter();
    }

    @Override
    public void setTeamProjectTable(final ServerContextTableModel tableModel, final ListSelectionModel selectionModel) {
        importForm.setTeamProjectTable(tableModel, selectionModel);
    }

    @Override
    public void setRepositoryName(final String name) {
        importForm.setRepositoryName(name);
    }

    @Override
    public String getRepositoryName() {
        return importForm.getRepositoryName();
    }

    @Override
    public void setUserName(final String name) {
        importForm.setUserName(name);
    }

    @Override
    public void setServerName(final String name) {
        loginForm.setServerName(name);
        importForm.setServerName(name);
    }

    @Override
    public String getServerName() {
        return loginForm.getServerName();
    }

    @Override
    public JComponent getComponent(final String name) {
        if(VsoImportPageModel.PROP_REPO_NAME.equals(name)) {
            return importForm.getRepositoryNameComponent();
        }
        if(VsoImportPageModel.PROP_SERVER_NAME.equals(name)) {
            return loginForm.getServerNameComponent();
        }
        return null;
    }
}
