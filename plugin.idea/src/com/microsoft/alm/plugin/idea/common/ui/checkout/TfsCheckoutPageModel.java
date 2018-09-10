// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.checkout;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.idea.common.ui.common.LookupHelper;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the abstract CheckoutPageModel for TFS.
 */
class TfsCheckoutPageModel extends CheckoutPageModelImpl {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutPageModelImpl.class);

    private AuthenticationProvider authenticationProvider;
    private final ServerContextLookupOperation.ContextScope scope;

    public TfsCheckoutPageModel(final CheckoutModel checkoutModel) {
        super(checkoutModel,
                checkoutModel.getRepositoryType() == RepositoryContext.Type.GIT ?
                        ServerContextTableModel.TFS_GIT_REPO_COLUMNS :
                        ServerContextTableModel.TFS_TFVC_REPO_COLUMNS);

        setConnected(false);
        setAuthenticating(false);

        // Check the repository type to get the SCOPE of the query
        // Git => repository scope
        // TFVC => project scope (TFVC repositories are not separate from the team projects)
        scope = (checkoutModel.getRepositoryType() == RepositoryContext.Type.GIT) ?
                ServerContextLookupOperation.ContextScope.REPOSITORY :
                ServerContextLookupOperation.ContextScope.PROJECT;

        final String serverName = this.getServerName();
        //check if the url is a TFS server url or team services account url
        if (!UrlHelper.isTeamServicesUrl(serverName)) {
            authenticationProvider = TfsAuthenticationProvider.getInstance();
            // If we have authenticated before, just use that one
            if (authenticationProvider.isAuthenticated(serverName)) {
                logger.info("TFS auth info already found so reusing that for loading repos");
                final AuthenticationInfo info = authenticationProvider.getAuthenticationInfo(serverName);
                setServerNameInternal(info.getServerUri());
                LookupHelper.loadTfsContexts(this, this,
                        authenticationProvider, getRepositoryProvider(),
                        scope);
            }
        } else {
            authenticationProvider = VsoAuthenticationProvider.getInstance();
            if (authenticationProvider.isAuthenticated(serverName)) {
                logger.info("Azure DevOps Services auth info already found so reusing that for loading repos");
                setServerNameInternal(authenticationProvider.getAuthenticationInfo(serverName).getServerUri());
                LookupHelper.loadVsoContexts(this, this,
                        authenticationProvider, getRepositoryProvider(),
                        scope);
            }
        }

    }

    @Override
    protected AuthenticationInfo getAuthenticationInfo() {
        return authenticationProvider.getAuthenticationInfo(this.getServerName());
    }

    @Override
    public void signOut() {
        super.signOut();
        authenticationProvider.clearAuthenticationDetails(this.getServerName());
    }

    @Override
    public void loadRepositories() {
        //check if the url is a TFS server url or team services account url
        if (UrlHelper.isTeamServicesUrl(getServerName())) {
            authenticationProvider = VsoAuthenticationProvider.getInstance();
            LookupHelper.authenticateAndLoadVsoContexts(this, this,
                    authenticationProvider, getRepositoryProvider(),
                    scope);
        } else {
            authenticationProvider = TfsAuthenticationProvider.getInstance();
            LookupHelper.authenticateAndLoadTfsContexts(this, this,
                    authenticationProvider, getRepositoryProvider(),
                    scope);
        }
    }
}
