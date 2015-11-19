// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.checkout;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationListener;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
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
 * This class implements the abstract CheckoutPageModel for TFS.
 */
class TfsCheckoutPageModel extends CheckoutPageModelImpl {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutPageModelImpl.class);

    private TfsAuthenticationProvider authenticationProvider;

    public TfsCheckoutPageModel(final CheckoutModel checkoutModel) {
        super(checkoutModel, ServerContextTableModel.TFS_REPO_COLUMNS);

        setConnectionStatus(false);
        setAuthenticating(false);

        // check to see if the activeContext is a TFS context, if so, use it
        final ServerContext activeContext = ServerContextManager.getInstance().getActiveTfsContext();
        if (ServerContext.NO_CONTEXT != activeContext) {
            setServerNameInternal(activeContext.getUri().toString());
            setAuthenticationProvider(new TfsAuthenticationProvider(activeContext.getAuthenticationInfo()));
            loadRepositories();
        } else {
            setAuthenticationProvider(new TfsAuthenticationProvider());
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
    protected void setServerNameInternal(String serverName) {
        super.setServerNameInternal(serverName);
    }

    @Override
    public void loadRepositories() {
        clearErrors();

        // Make sure we have a server url
        final String serverName = getServerName();
        if (StringUtils.isEmpty(serverName)) {
            addError(ModelValidationInfo.createWithResource(PROP_SERVER_NAME, TfPluginBundle.KEY_LOGIN_FORM_TFS_ERRORS_NO_SERVER_NAME));
            setConnectionStatus(false);
            return;
        }

        //verify server url is a valid url
        if (!UrlHelper.isValidServerUrl(serverName)) {
            addError(ModelValidationInfo.createWithResource(PROP_SERVER_NAME, TfPluginBundle.KEY_LOGIN_FORM_TFS_ERRORS_INVALID_SERVER_URL, serverName));
            setConnectionStatus(false);
            return;
        }

        if (authenticationProvider.isAuthenticated()) {
            loadRepositoriesInternal();
        } else {
            authenticationProvider.authenticateAsync(getServerName(), new AuthenticationListener() {
                @Override
                public void authenticating() {
                    // We are starting to authenticate, so set the boolean
                    setAuthenticating(true);
                }

                @Override
                public void authenticated(final AuthenticationInfo authenticationInfo, final Throwable throwable) {
                    // Push this event back onto the UI thread
                    IdeaHelper.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            // Authentication is over, so set the boolean
                            setAuthenticating(false);

                            //Log exception if it failed
                            if (throwable != null) {
                                logger.warn("Connecting to TFS server failed", throwable);
                            }
                            //try to load the repos
                            loadRepositoriesInternal();
                        }
                    });
                }
            });
        }
    }

    private void loadRepositoriesInternal() {
        if (!authenticationProvider.isAuthenticated()) {
            addError(ModelValidationInfo.createWithResource(PROP_SERVER_NAME, TfPluginBundle.KEY_LOGIN_PAGE_ERRORS_TFS_CONNECT_FAILED, getServerName()));
            signOut();
            return;
        }

        setConnectionStatus(true);
        setLoading(true);
        setUserName(authenticationProvider.getAuthenticationInfo().getUserNameForDisplay());
        clearContexts();

        final URI serverUrl = UrlHelper.getBaseUri(getServerName());
        final ServerContext context =
                new ServerContextBuilder().type(ServerContext.Type.TFS)
                        .uri(serverUrl).authentication(authenticationProvider.getAuthenticationInfo()).build();

        //successfully logged in and loading repositories, save this context if there is no active context
        if (ServerContextManager.getInstance().getActiveContext() == ServerContext.NO_CONTEXT) {
            ServerContextManager.getInstance().setActiveContext(context);
        }

        getRepositoryProvider().loadContexts(Collections.singletonList(context),
                ServerContextLookupOperation.ContextScope.REPOSITORY);
    }

}
