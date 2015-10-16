// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.checkout.mocks;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.ui.checkout.CheckoutModel;
import com.microsoft.alm.plugin.idea.ui.checkout.CheckoutPageModelImpl;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import org.apache.commons.lang.StringUtils;

public class MockCheckoutPageModel extends CheckoutPageModelImpl {
    private boolean loadRepositoriesCalled = false;
    private boolean cloneSelectedRepoCalled = false;
    private String urlVisited = null;
    private String validationError = null;

    public MockCheckoutPageModel(CheckoutModel model, ServerContextTableModel.Column[] columns) {
        super(model, columns);
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo() {
        return null;
    }

    public void initialize(CheckoutModel checkoutModel) {
        setParentModel(checkoutModel);
    }

    public boolean isLoadRepositoriesCalled() {
        return loadRepositoriesCalled;
    }

    public boolean isCloneSelectedRepoCalled() {
        return cloneSelectedRepoCalled;
    }

    public String getUrlVisited() {
        return urlVisited;
    }

    public void clearInternals() {
        loadRepositoriesCalled = false;
        cloneSelectedRepoCalled = false;
        urlVisited = null;
    }

    public void setValidationError(String error) {
        validationError = error;
    }

    @Override
    public ModelValidationInfo validate() {
        if (validationError != null) {
            // If validationError is set, don't call the default validation
            if (validationError.equals("")) {
                return null;
            }
            return ModelValidationInfo.createWithMessage(validationError);
        }
        return super.validate();
    }

    @Override
    public void gotoLink(String url) {
        urlVisited = url;
        if (StringUtils.isEmpty(url)) {
            // Allow empty strings to go thru to the base class for testing
            super.gotoLink(url);
        }
    }

    @Override
    public void loadRepositories() {
        loadRepositoriesCalled = true;

        // Act like we are connected and loading is done
        setConnected(true);
        setLoading(false);
    }

    @Override
    public void cloneSelectedRepo() {
        cloneSelectedRepoCalled = true;
    }

    // Making this method public so that tests can call it
    @Override
    public void addContext(ServerContext serverContext) {
        super.addContext(serverContext);
    }
}
