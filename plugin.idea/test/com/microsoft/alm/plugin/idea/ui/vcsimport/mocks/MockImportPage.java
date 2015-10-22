// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport.mocks;

import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.ui.vcsimport.ImportPage;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of ImportPage
 */
public class MockImportPage implements ImportPage {

    private final List<ActionListener> actionListeners = new ArrayList<ActionListener>();
    private boolean loginShowing = true;
    private String teamProjectFilter;
    private String repositoryName;

    @Override
    public void addActionListener(final ActionListener listener) {
        actionListeners.add(listener);
    }

    @Override
    public void setLoginShowing(final boolean showLogin) {
        loginShowing = showLogin;
    }

    @Override
    public void setLoading(final boolean loading) {

    }

    @Override
    public void setAuthenticating(final boolean authenticating) {

    }

    @Override
    public void setTeamProjectFilter(final String filter) {
        teamProjectFilter = filter;
    }

    @Override
    public String getTeamProjectFilter() {
        return teamProjectFilter;
    }

    @Override
    public void setTeamProjectTable(final ServerContextTableModel tableModel, final ListSelectionModel selectionModel) {

    }

    @Override
    public void setRepositoryName(final String name) {
        repositoryName = name;
    }

    @Override
    public String getRepositoryName() {
        return repositoryName;
    }

    @Override
    public void setUserName(final String name) {

    }

    @Override
    public void setServerName(final String name) {

    }

    @Override
    public String getServerName() {
        return "";
    }

    @Override
    public JComponent getComponent(final String name) {
        return null;
    }
}
