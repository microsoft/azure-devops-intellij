// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationListener;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;

/**
 * This class implements the ImportPageModelImpl for Tfs
 */
public class TfsImportPageModel extends ImportPageModelImpl {
    private TfsAuthenticationProvider authenticationProvider;
    private final static Logger logger = LoggerFactory.getLogger(TfsImportPageModel.class);


    public TfsImportPageModel(final ImportModel importModel) {
        super(importModel, ServerContextTableModel.TFS_PROJECT_COLUMNS);

        setConnectionStatus(false);
        setAuthenticating(false);

        // If we have authenticated before, just use that one
        authenticationProvider = TfsAuthenticationProvider.getInstance();
        if (authenticationProvider.isAuthenticated()) {
            setServerNameInternal(authenticationProvider.getAuthenticationInfo().getServerUri());
            loadTeamProjects();
        }
    }

    @Override
    protected AuthenticationInfo getAuthenticationInfo() {
        return authenticationProvider.getAuthenticationInfo();
    }

    private void setAuthenticationProvider(TfsAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    public void signOut() {
        super.signOut();
        authenticationProvider.clearAuthenticationDetails();
    }

    @Override
    public void loadTeamProjects() {
        clearErrors();

        // Make sure we have a server url
        final String serverName = getServerName();
        if (StringUtils.isEmpty(serverName)) {
            addError(ModelValidationInfo.createWithResource(PROP_SERVER_NAME, TfPluginBundle.KEY_LOGIN_FORM_TFS_ERRORS_NO_SERVER_NAME));
            setConnectionStatus(false);
            return;
        }

        //verify server url is a valid url
        if (!UrlHelper.isValidUrl(serverName)) {
            addError(ModelValidationInfo.createWithResource(PROP_SERVER_NAME, TfPluginBundle.KEY_LOGIN_FORM_TFS_ERRORS_INVALID_SERVER_URL, serverName));
            setConnectionStatus(false);
            return;
        }

        if (authenticationProvider.isAuthenticated()) {
            loadTeamProjectsInternal();
        } else {
            authenticationProvider.authenticateAsync(getServerName(), new AuthenticationListener() {
                @Override
                public void authenticating() {
                    // Push this event back onto the UI thread
                    IdeaHelper.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            // We are starting to authenticate, so set the boolean
                            setAuthenticating(true);
                        }
                    });
                }

                @Override
                public void authenticated(final AuthenticationInfo authenticationInfo, final Throwable throwable) {
                    // Push this event back onto the UI thread
                    IdeaHelper.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            // Authentication is over, so set the boolean
                            setAuthenticating(false);
                            //Log exception
                            if (throwable != null) {
                                logger.warn("Connecting to TFS server failed", throwable);
                            }
                            //try to load the team projects
                            loadTeamProjectsInternal();
                        }
                    });
                }
            });
        }
    }

    private void loadTeamProjectsInternal() {
        if (!authenticationProvider.isAuthenticated()) {
            addError(ModelValidationInfo.createWithResource(PROP_SERVER_NAME, TfPluginBundle.KEY_LOGIN_PAGE_ERRORS_TFS_CONNECT_FAILED, getServerName()));
            signOut();
            return;
        }

        // Update the model properties (and the UI)
        setConnectionStatus(true);
        setLoading(true);
        setUserName(authenticationProvider.getAuthenticationInfo().getUserNameForDisplay());
        clearContexts();

        // Create the tfs context and load repositories
        final URI serverUrl = UrlHelper.createUri(getServerName());
        final ServerContext context =
                new ServerContextBuilder().type(ServerContext.Type.TFS)
                        .uri(serverUrl).authentication(authenticationProvider.getAuthenticationInfo()).build();

        getTeamProjectProvider().loadContexts(Collections.singletonList(context), ServerContextLookupOperation.ContextScope.PROJECT);
    }

}
