// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.idea.ui.common.LookupHelper;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the ImportPageModelImpl for Tfs
 */
public class TfsImportPageModel extends ImportPageModelImpl {
    private TfsAuthenticationProvider authenticationProvider;
    private final static Logger logger = LoggerFactory.getLogger(TfsImportPageModel.class);


    public TfsImportPageModel(final ImportModel importModel) {
        super(importModel, ServerContextTableModel.TFS_PROJECT_COLUMNS);

        setConnected(false);
        setAuthenticating(false);

        // If we have authenticated before, just use that one
        authenticationProvider = TfsAuthenticationProvider.getInstance();
        if (authenticationProvider.isAuthenticated()) {
            setServerNameInternal(authenticationProvider.getAuthenticationInfo().getServerUri());
            LookupHelper.loadTfsContexts(this, this,
                    authenticationProvider, getTeamProjectProvider(),
                    ServerContextLookupOperation.ContextScope.PROJECT);
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
    public void loadTeamProjects() {
        LookupHelper.authenticateAndLoadTfsContexts(this, this,
                authenticationProvider, getTeamProjectProvider(),
                ServerContextLookupOperation.ContextScope.PROJECT);
    }
}
