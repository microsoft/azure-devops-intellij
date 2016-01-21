// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.alm.plugin.TeamServicesException;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.tf.common.authentication.aad.AzureAuthenticator;
import com.microsoft.tf.common.authentication.aad.PersonalAccessTokenFactory;
import com.microsoft.tf.common.authentication.aad.TokenScope;
import com.microsoft.tf.common.authentication.aad.impl.AzureAuthenticatorImpl;
import com.microsoft.tf.common.authentication.aad.impl.PersonalAccessTokenFactoryImpl;
import com.microsoft.visualstudio.services.account.webapi.AccountHttpClient;
import com.microsoft.visualstudio.services.account.webapi.model.Profile;
import com.microsoft.visualstudio.services.authentication.DelegatedAuthorization.webapi.model.SessionToken;
import com.microsoftopentechnologies.auth.AuthenticationCallback;
import com.microsoftopentechnologies.auth.AuthenticationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

/**
 * Use this AuthenticationProvider to authenticate with VSO.
 */
public class VsoAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(VsoAuthenticationProvider.class);

    //azure connection strings
    private static final String LOGIN_WINDOWS_NET_AUTHORITY = "login.windows.net";
    private static final String COMMON_TENANT = "common";
    private static final String MANAGEMENT_CORE_RESOURCE = "https://management.core.windows.net/";
    private static final String CLIENT_ID = "502ea21d-e545-4c66-9129-c352ec902969";
    private static final String REDIRECT_URL = "https://xplatalm.com";
    private static final String TOKEN_DESCRIPTION = "VSTS IntelliJ Plugin: %s from: %s on: %s";

    public static final String VSO_AUTH_URL = "https://app.vssps.visualstudio.com";

    private static class AzureAuthenticatorHolder {
        private static AzureAuthenticator INSTANCE = new AzureAuthenticatorImpl(LOGIN_WINDOWS_NET_AUTHORITY,
                COMMON_TENANT,
                MANAGEMENT_CORE_RESOURCE,
                CLIENT_ID,
                REDIRECT_URL);
    }

    /**
     * @return AzureAuthenticator
     */
    public static AzureAuthenticator getAzureAuthenticator() {
        return AzureAuthenticatorHolder.INSTANCE;
    }

    /**
     * This constructor is protected to allow for testing
     */
    protected VsoAuthenticationProvider() {
    }

    private static class Holder {
        private static VsoAuthenticationProvider INSTANCE = new VsoAuthenticationProvider();
    }

    public static VsoAuthenticationProvider getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo() {
        return ServerContextManager.getInstance().getBestAuthenticationInfo(VSO_AUTH_URL, false);
    }

    @Override
    public boolean isAuthenticated() {
        return getAuthenticationInfo() != null;
    }

    @Override
    public void clearAuthenticationDetails() {
        ServerContextManager.getInstance().remove(VSO_AUTH_URL);
    }

    @Override
    public void authenticateAsync(final String serverUri, final AuthenticationListener listener) {
        AuthenticationListener.Helper.authenticating(listener);

        //invoke AAD authentication library to get an account access token
        try {
            getAzureAuthenticator().getAadAccessTokenAsync(new AuthenticationCallback() {
                @Override
                public void onSuccess(final AuthenticationResult result) {
                    final AuthenticationInfo authenticationInfo;

                    if (result == null) {
                        //User closed the browser window without signing in
                        clearAuthenticationDetails();
                        authenticationInfo = null;
                    } else {
                        final PersonalAccessTokenFactory patFactory = new PersonalAccessTokenFactoryImpl(result);
                        final String tokenDescription = String.format(TOKEN_DESCRIPTION,
                                AuthHelper.getEmail(result), SystemHelper.getComputerName(), SystemHelper.getCurrentDateTimeString());
                        final SessionToken sessionToken = patFactory.createGlobalSessionToken(tokenDescription,
                                Arrays.asList(TokenScope.CODE_READ, TokenScope.CODE_WRITE, TokenScope.CODE_MANAGE));
                        authenticationInfo = AuthHelper.createAuthenticationInfo(serverUri, result, sessionToken);
                        ServerContextManager.getInstance().add(
                                new ServerContextBuilder().type(ServerContext.Type.VSO_DEPLOYMENT)
                                        .uri(VSO_AUTH_URL)
                                        .authentication(authenticationInfo)
                                        .build(),
                                false);
                    }
                    AuthenticationListener.Helper.authenticated(listener, authenticationInfo, null);
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    clearAuthenticationDetails();
                    AuthenticationListener.Helper.authenticated(listener, null, throwable);
                }
            });
        } catch (IOException e) {
            clearAuthenticationDetails();
            AuthenticationListener.Helper.authenticated(listener, null, e);
        }
    }

    /**
     * Returns user profile of user with provided authentication info
     *
     * @param authenticationInfo
     * @return
     */
    public Profile getUserProfile(final AuthenticationInfo authenticationInfo) {
        //TODO: how to do this for TFS? expose this on AuthenticationProvider interface
        final ServerContext context = new ServerContextBuilder()
                .type(ServerContext.Type.VSO_DEPLOYMENT)
                .authentication(authenticationInfo)
                .uri(VSO_AUTH_URL)
                .build();
        final AccountHttpClient accountHttpClient = new AccountHttpClient(context.getClient(), context.getUri());
        try {
            final Profile me = accountHttpClient.getMyProfile();
            // Only update the lastUsedContext if there is no current lastUsedContext
            ServerContextManager.getInstance().add(context,
                    ServerContextManager.getInstance().lastUsedContextIsEmpty());

            return me;
        } catch (Throwable t) {
            //failed to retrieve user profile, auth data is invalid, possible that token was revoked or expired
            logger.warn("getAuthenticatedUserProfile exception", t);
            clearAuthenticationDetails();
            throw new TeamServicesException(TeamServicesException.KEY_VSO_AUTH_SESSION_EXPIRED, t);
        }
    }

    /**
     * Retrieves user profile of signed in user, if successful, saves the current VSO_DEPLOYMENT context
     *
     * @return user profile if successfully authenticated, else throws an exception
     */
    public Profile getAuthenticatedUserProfile() {
        return getUserProfile(getAuthenticationInfo());
    }
}