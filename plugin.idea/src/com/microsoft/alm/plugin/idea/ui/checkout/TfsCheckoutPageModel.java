// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.checkout;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.idea.ui.common.LookupHelper;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the abstract CheckoutPageModel for TFS.
 */
class TfsCheckoutPageModel extends CheckoutPageModelImpl {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutPageModelImpl.class);

    private TfsAuthenticationProvider authenticationProvider;

    public TfsCheckoutPageModel(final CheckoutModel checkoutModel) {
        super(checkoutModel, ServerContextTableModel.TFS_REPO_COLUMNS);

        setConnected(false);
        setAuthenticating(false);

        // If we have authenticated before, just use that one
        authenticationProvider = TfsAuthenticationProvider.getInstance();
        if (authenticationProvider.isAuthenticated()) {
            setServerNameInternal(authenticationProvider.getAuthenticationInfo().getServerUri());
            LookupHelper.loadTfsContexts(this, this,
                    authenticationProvider, getRepositoryProvider(),
                    ServerContextLookupOperation.ContextScope.REPOSITORY);
        }
    }

    @Override
    protected AuthenticationInfo getAuthenticationInfo() {
        return authenticationProvider.getAuthenticationInfo();
    }

    @Override
    public void signOut() {
        super.signOut();
        authenticationProvider.clearAuthenticationDetails();
    }

    @Override
    public void loadRepositories() {
        LookupHelper.authenticateAndLoadTfsContexts(this, this,
                authenticationProvider, getRepositoryProvider(),
                ServerContextLookupOperation.ContextScope.REPOSITORY);
    }
}
