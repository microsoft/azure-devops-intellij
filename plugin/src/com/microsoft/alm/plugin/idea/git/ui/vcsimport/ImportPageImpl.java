// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.vcsimport;

import com.microsoft.alm.plugin.idea.common.ui.common.LoginPageImpl;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.common.ui.common.forms.LoginForm;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;

/**
 * This class is a panel that switches between showing the TfsLoginForm and the ImportForm.
 */
public class ImportPageImpl extends LoginPageImpl implements ImportPage {
    private final ImportForm importForm;

    public ImportPageImpl(LoginForm loginForm, ImportForm importForm) {
        super(loginForm, importForm);
        this.importForm = importForm;
    }

    // Import form accessors //

    @Override
    public void setLoading(final boolean loading) {
        importForm.setLoading(loading);
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

    // Overrides of LoginPage //

    @Override
    public void setServerName(final String name) {
        super.setServerName(name);
        importForm.setServerName(name);
    }

    @Override
    public JComponent getComponent(final String name) {
        if (ImportPageModel.PROP_REPO_NAME.equals(name)) {
            return importForm.getRepositoryNameComponent();
        }

        return super.getComponent(name);
    }
}
