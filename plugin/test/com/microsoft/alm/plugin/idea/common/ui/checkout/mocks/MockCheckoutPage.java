// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.checkout.mocks;

import com.microsoft.alm.plugin.idea.common.ui.checkout.CheckoutPage;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import java.awt.event.ActionListener;

public class MockCheckoutPage implements CheckoutPage {
    private String repositoryFilter;

    @Override
    public void addActionListener(ActionListener listener) {
    }

    @Override
    public void setLoginShowing(boolean showLogin) {
    }

    @Override
    public void setLoading(boolean loading) {
    }

    @Override
    public void setAuthenticating(final boolean authenticating) {
    }

    @Override
    public void setAdvanced(boolean advanced) {
    }

    @Override
    public boolean getAdvanced() {
        return false;
    }

    @Override
    public boolean isTfvcServerCheckout() {
        return false;
    }

    @Override
    public void setRepositoryFilter(String filter) {
        repositoryFilter = filter;
    }

    @Override
    public String getRepositoryFilter() {
        return repositoryFilter;
    }

    @Override
    public void setRepositoryTable(ServerContextTableModel tableModel, ListSelectionModel selectionModel) {

    }

    @Override
    public void setParentDirectory(String path) {

    }

    @Override
    public String getParentDirectory() {
        return null;
    }

    @Override
    public JComponent getComponent(String name) {
        return null;
    }

    @Override
    public void setDirectoryName(String name) {

    }

    @Override
    public String getDirectoryName() {
        return null;
    }

    @Override
    public void setUserName(String name) {

    }

    @Override
    public void setServerName(String name) {

    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return null;
    }
}
