// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.checkout;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.LookupHelper;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the abstract CheckoutPageModel for VSO.
 */
public class VsoCheckoutPageModel extends CheckoutPageModelImpl {
    private static final Logger logger = LoggerFactory.getLogger(VsoCheckoutPageModel.class);
    private VsoAuthenticationProvider authenticationProvider = VsoAuthenticationProvider.getInstance();

    public VsoCheckoutPageModel(CheckoutModel checkoutModel) {
        super(checkoutModel, ServerContextTableModel.VSO_REPO_COLUMNS);

        // Set default server name for VSO
        setServerNameInternal(TfPluginBundle.message(TfPluginBundle.KEY_USER_ACCOUNT_PANEL_VSO_SERVER_NAME));

        setConnected(false);
        setAuthenticating(false);

        if (authenticationProvider.isAuthenticated()) {
            LookupHelper.loadVsoContexts(this, this,
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
        LookupHelper.authenticateAndLoadVsoContexts(this, this,
                authenticationProvider, getRepositoryProvider(),
                ServerContextLookupOperation.ContextScope.REPOSITORY);
    }
}
