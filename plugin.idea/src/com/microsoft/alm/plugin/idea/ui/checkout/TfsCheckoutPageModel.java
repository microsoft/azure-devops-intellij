// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.checkout;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
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

    private AuthenticationProvider authenticationProvider;

    public TfsCheckoutPageModel(final CheckoutModel checkoutModel) {
        super(checkoutModel, ServerContextTableModel.TFS_REPO_COLUMNS);

        setConnected(false);
        setAuthenticating(false);

        //check if the url is a TFS server url or team services account url
        if (!UrlHelper.isTeamServicesUrl(getServerName())) {
            authenticationProvider = TfsAuthenticationProvider.getInstance();
            // If we have authenticated before, just use that one
            if (authenticationProvider.isAuthenticated()) {
                setServerNameInternal(authenticationProvider.getAuthenticationInfo().getServerUri());
                LookupHelper.loadTfsContexts(this, this,
                        authenticationProvider, getRepositoryProvider(),
                        ServerContextLookupOperation.ContextScope.REPOSITORY);
            }
        } else {
            authenticationProvider = VsoAuthenticationProvider.getInstance();
            if (authenticationProvider.isAuthenticated()) {
                setServerNameInternal(authenticationProvider.getAuthenticationInfo().getServerUri());
                LookupHelper.loadVsoContexts(this, this,
                        authenticationProvider, getRepositoryProvider(),
                        ServerContextLookupOperation.ContextScope.REPOSITORY);
            }
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
        //check if the url is a TFS server url or team services account url
        if (!UrlHelper.isTeamServicesUrl(getServerName())) {
            authenticationProvider = TfsAuthenticationProvider.getInstance();
            LookupHelper.authenticateAndLoadTfsContexts(this, this,
                    authenticationProvider, getRepositoryProvider(),
                    ServerContextLookupOperation.ContextScope.REPOSITORY);
        } else {
            authenticationProvider = VsoAuthenticationProvider.getInstance();
            LookupHelper.authenticateAndLoadVsoContexts(this, this,
                    authenticationProvider, getRepositoryProvider(),
                    ServerContextLookupOperation.ContextScope.REPOSITORY);
        }
    }
}
