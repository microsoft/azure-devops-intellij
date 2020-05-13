// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.checkout;

import com.microsoft.alm.plugin.idea.common.ui.common.LoginPageImpl;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.common.ui.common.forms.LoginForm;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;

/**
 * This class is a panel that switches between showing the VsoLoginForm and the CheckoutForm.
 * The loginShowing property controls which form is shown.
 */
class CheckoutPageImpl extends LoginPageImpl implements CheckoutPage {
    private final CheckoutForm checkoutForm;

    public CheckoutPageImpl(LoginForm loginForm, CheckoutForm checkoutForm) {
        super(loginForm, checkoutForm);
        // Keeping our own pointer to the checkoutForm
        this.checkoutForm = checkoutForm;
    }

    // Checkout form accessors //

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
    public void setLoading(final boolean loading) {
        checkoutForm.setLoading(loading);
    }

    @Override
    public void setAdvanced(boolean advanced) {
        checkoutForm.setAdvanced(advanced);
    }

    @Override
    public boolean getAdvanced() {
        return checkoutForm.isAdvanced();
    }

    @Override
    public boolean isTfvcServerCheckout() {
        return checkoutForm.isTfvcServerCheckout();
    }

    // Overrides of LoginPage //

    @Override
    public void setServerName(final String name) {
        super.setServerName(name);
        checkoutForm.setServerName(name);
    }

    @Override
    public JComponent getComponent(final String name) {
        if (CheckoutPageModel.PROP_PARENT_DIR.equals(name)) {
            return checkoutForm.getParentDirectoryComponent();
        }
        if (CheckoutPageModel.PROP_DIRECTORY_NAME.equals(name)) {
            return checkoutForm.getDirectoryNameComponent();
        }

        return super.getComponent(name);
    }
}
