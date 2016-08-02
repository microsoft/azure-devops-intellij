// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.vcsimport;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.LookupHelper;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the AbstractImportPageModel for VSO.
 */
public class VsoImportPageModel extends ImportPageModelImpl {
    private VsoAuthenticationProvider authenticationProvider = VsoAuthenticationProvider.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(VsoImportPageModel.class);


    public VsoImportPageModel(final ImportModel importDialogModel) {
        super(importDialogModel, ServerContextTableModel.VSO_PROJECT_COLUMNS);

        // Set default server name for VSO
        setServerNameInternal(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_VSO_SERVER_NAME));

        setConnected(false);
        setAuthenticating(false);

        if (authenticationProvider.isAuthenticated()) {
            // Load the projects from all accounts into the list
            LookupHelper.loadVsoContexts(this, this,
                    authenticationProvider, getTeamProjectProvider(),
                    ServerContextLookupOperation.ContextScope.PROJECT);
        }
    }

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
        LookupHelper.authenticateAndLoadVsoContexts(this, this,
                authenticationProvider, getTeamProjectProvider(),
                ServerContextLookupOperation.ContextScope.PROJECT);
    }
}
