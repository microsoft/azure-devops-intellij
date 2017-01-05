// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.vcsimport;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.idea.common.ui.common.LookupHelper;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the ImportPageModelImpl for Tfs
 */
public class TfsImportPageModel extends ImportPageModelImpl {
    private AuthenticationProvider authenticationProvider;
    private final static Logger logger = LoggerFactory.getLogger(TfsImportPageModel.class);


    public TfsImportPageModel(final ImportModel importModel) {
        super(importModel, ServerContextTableModel.TFS_PROJECT_COLUMNS);

        setConnected(false);
        setAuthenticating(false);

        if (!UrlHelper.isTeamServicesUrl(getServerName())) {
            authenticationProvider = TfsAuthenticationProvider.getInstance();
            if (authenticationProvider.isAuthenticated()) {
                logger.info("TFS auth info already found so reusing that for loading repos");
                final AuthenticationInfo info = authenticationProvider.getAuthenticationInfo();
                setServerNameInternal(info.getServerUri());
                LookupHelper.loadTfsContexts(this, this,
                        authenticationProvider, getTeamProjectProvider(),
                        ServerContextLookupOperation.ContextScope.PROJECT);
            }
        } else {
            authenticationProvider = VsoAuthenticationProvider.getInstance();
            if (authenticationProvider.isAuthenticated()) {
                logger.info("VSTS auth info already found so reusing that for loading repos");
                setServerNameInternal((authenticationProvider.getAuthenticationInfo().getServerUri()));
                LookupHelper.loadVsoContexts(this, this,
                        authenticationProvider, getTeamProjectProvider(),
                        ServerContextLookupOperation.ContextScope.PROJECT);
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
    public void loadTeamProjects() {
        if (!UrlHelper.isTeamServicesUrl(getServerName())) {
            authenticationProvider = TfsAuthenticationProvider.getInstance();
            LookupHelper.authenticateAndLoadTfsContexts(this, this,
                    authenticationProvider, getTeamProjectProvider(),
                    ServerContextLookupOperation.ContextScope.PROJECT);
        } else {
            authenticationProvider = VsoAuthenticationProvider.getInstance();
            LookupHelper.authenticateAndLoadVsoContexts(this, this,
                    authenticationProvider, getTeamProjectProvider(),
                    ServerContextLookupOperation.ContextScope.PROJECT);
        }
    }
}
