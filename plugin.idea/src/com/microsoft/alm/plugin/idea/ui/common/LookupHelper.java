// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.TeamServicesException;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationListener;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.operations.AccountLookupOperation;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;

public class LookupHelper {
    private static final Logger logger = LoggerFactory.getLogger(LookupHelper.class);

    public static void authenticateAndLoadTfsContexts(final LoginPageModel loginPageModel,
                                                      final ServerContextLookupPageModel lookupPageModel,
                                                      final AuthenticationProvider authenticationProvider,
                                                      final ServerContextLookupListener lookupListener,
                                                      final ServerContextLookupOperation.ContextScope scope) {
        loginPageModel.clearErrors();

        // Make sure we have a server url
        final String serverName = loginPageModel.getServerName();
        if (StringUtils.isEmpty(serverName)) {
            loginPageModel.addError(ModelValidationInfo.createWithResource(LoginPageModel.PROP_SERVER_NAME,
                    TfPluginBundle.KEY_LOGIN_FORM_TFS_ERRORS_NO_SERVER_NAME));
            loginPageModel.setConnected(false);
            return;
        }

        //verify server url is a valid url
        if (!UrlHelper.isValidUrl(serverName)) {
            loginPageModel.addError(ModelValidationInfo.createWithResource(LoginPageModel.PROP_SERVER_NAME,
                    TfPluginBundle.KEY_LOGIN_FORM_TFS_ERRORS_INVALID_SERVER_URL, serverName));
            loginPageModel.setConnected(false);
            return;
        }

        if (authenticationProvider.isAuthenticated()) {
            loadTfsContexts(loginPageModel, lookupPageModel, authenticationProvider, lookupListener, scope);
        } else {
            authenticationProvider.authenticateAsync(serverName, new AuthenticationListener() {
                @Override
                public void authenticating() {
                    // Push this event back onto the UI thread
                    IdeaHelper.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            // We are starting to authenticate, so set the boolean
                            loginPageModel.setAuthenticating(true);
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
                            loginPageModel.setAuthenticating(false);

                            // Log exception
                            if (throwable != null) {
                                logger.warn("Connecting to TFS server failed", throwable);
                                if (throwable instanceof TeamServicesException) {
                                    loginPageModel.addError(ModelValidationInfo.createWithMessage(LocalizationServiceImpl.getInstance().getExceptionMessage(throwable)));
                                } else {
                                    loginPageModel.addError(ModelValidationInfo.createWithResource(LoginPageModel.PROP_SERVER_NAME,
                                            TfPluginBundle.KEY_LOGIN_PAGE_ERRORS_TFS_CONNECT_FAILED, loginPageModel.getServerName()));
                                }
                                loginPageModel.signOut();
                            } else {
                                // Try to load the contexts
                                loadTfsContexts(loginPageModel, lookupPageModel, authenticationProvider, lookupListener, scope);
                            }
                        }
                    });
                }
            });
        }
    }

    public static void loadTfsContexts(final LoginPageModel loginPageModel,
                                       final ServerContextLookupPageModel lookupPageModel,
                                       final AuthenticationProvider authenticationProvider,
                                       final ServerContextLookupListener lookupListener,
                                       final ServerContextLookupOperation.ContextScope scope) {
        if (!authenticationProvider.isAuthenticated()) {
            loginPageModel.addError(ModelValidationInfo.createWithResource(LoginPageModel.PROP_SERVER_NAME,
                    TfPluginBundle.KEY_LOGIN_PAGE_ERRORS_TFS_CONNECT_FAILED, loginPageModel.getServerName()));
            loginPageModel.signOut();
            return;
        }

        // Update the model properties (and the UI)
        loginPageModel.setConnected(true);
        lookupPageModel.setLoading(true);
        loginPageModel.setUserName(authenticationProvider.getAuthenticationInfo().getUserNameForDisplay());
        lookupPageModel.clearContexts();

        // Create the main tfs context and load other contexts
        final URI serverUrl = UrlHelper.createUri(loginPageModel.getServerName());
        final ServerContext context =
                new ServerContextBuilder().type(ServerContext.Type.TFS)
                        .uri(serverUrl).authentication(authenticationProvider.getAuthenticationInfo()).build();

        lookupListener.loadContexts(Collections.singletonList(context), scope);
    }

    public static void authenticateAndLoadVsoContexts(final LoginPageModel loginPageModel,
                                                      final ServerContextLookupPageModel lookupPageModel,
                                                      final AuthenticationProvider authenticationProvider,
                                                      final ServerContextLookupListener lookupListener,
                                                      final ServerContextLookupOperation.ContextScope scope) {
        loginPageModel.clearErrors();
        if (authenticationProvider.isAuthenticated()) {
            loadVsoContexts(loginPageModel, lookupPageModel, authenticationProvider, lookupListener, scope);
        } else {
            authenticationProvider.authenticateAsync(VsoAuthenticationProvider.VSO_AUTH_URL, new AuthenticationListener() {
                @Override
                public void authenticating() {
                    IdeaHelper.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            // We are starting to authenticate, so set the boolean
                            loginPageModel.setAuthenticating(true);
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
                            loginPageModel.setAuthenticating(false);
                            //Log exception
                            if (throwable != null) {
                                logger.warn("Authenticating with Team Services failed", throwable);
                            }
                            //try to load the contexts from the accounts
                            loadVsoContexts(loginPageModel, lookupPageModel, authenticationProvider, lookupListener, scope);
                        }
                    });
                }
            });
        }
    }

    public static void loadVsoContexts(final LoginPageModel loginPageModel,
                                       final ServerContextLookupPageModel lookupPageModel,
                                       final AuthenticationProvider authenticationProvider,
                                       final ServerContextLookupListener lookupListener,
                                       final ServerContextLookupOperation.ContextScope scope) {
        if (!authenticationProvider.isAuthenticated()) {
            loginPageModel.addError(ModelValidationInfo.createWithResource(TfPluginBundle.KEY_LOGIN_PAGE_ERRORS_VSO_SIGN_IN_FAILED));
            loginPageModel.signOut();
            return;
        }

        loginPageModel.setConnected(true);
        lookupPageModel.setLoading(true);
        loginPageModel.setUserName(authenticationProvider.getAuthenticationInfo().getUserNameForDisplay());
        lookupPageModel.clearContexts();

        final AccountLookupOperation accountLookupOperation = new AccountLookupOperation();
        accountLookupOperation.addListener(new Operation.Listener() {
            @Override
            public void notifyLookupStarted() {
                // nothing to do
            }

            @Override
            public void notifyLookupCompleted() {
                // nothing to do here, we are still loading contexts
            }

            @Override
            public void notifyLookupResults(final Operation.Results results) {
                final ModelValidationInfo validationInfo;
                if (results.hasError()) {
                    validationInfo = ModelValidationInfo.createWithMessage(
                            LocalizationServiceImpl.getInstance().getExceptionMessage(results.getError()));
                } else if (results.isCancelled()) {
                    validationInfo = ModelValidationInfo.createWithResource(TfPluginBundle.KEY_OPERATION_LOOKUP_CANCELED);
                } else {
                    validationInfo = ModelValidationInfo.NO_ERRORS;
                    // Take the list of accounts and use them to query the team projects
                    lookupListener.loadContexts(
                            accountLookupOperation.castResults(results).getServerContexts(),
                            scope);
                }

                // If there was an error or cancellation message, send it back to the user
                if (validationInfo != ModelValidationInfo.NO_ERRORS) {
                    // Push this event back onto the UI thread
                    IdeaHelper.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            loginPageModel.addError(validationInfo);
                            loginPageModel.signOut();
                        }
                    });
                }
            }
        });
        // Start the operation
        accountLookupOperation.doWorkAsync(Operation.EMPTY_INPUTS);
    }

}
