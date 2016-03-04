// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication.facades;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.tf.common.authentication.aad.AzureAuthenticator;
import com.microsoft.tf.common.authentication.aad.PersonalAccessTokenFactory;
import com.microsoft.tf.common.authentication.aad.TokenScope;
import com.microsoft.tf.common.authentication.aad.impl.AzureAuthenticatorImpl;
import com.microsoft.tf.common.authentication.aad.impl.PersonalAccessTokenFactoryImpl;
import com.microsoft.visualstudio.services.authentication.DelegatedAuthorization.webapi.model.SessionToken;
import com.microsoftopentechnologies.auth.AuthenticationCallback;
import com.microsoftopentechnologies.auth.AuthenticationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class VsoSwtAuthInfoProvider implements AuthenticationInfoProvider{

    private static final Logger logger = LoggerFactory.getLogger(VsoSwtAuthInfoProvider.class);

    private static final String TOKEN_DESCRIPTION = "VSTS IntelliJ Plugin: %s from: %s on: %s";

    //azure connection strings
    private static final String LOGIN_WINDOWS_NET_AUTHORITY = "login.windows.net";
    private static final String COMMON_TENANT = "common";
    private static final String MANAGEMENT_CORE_RESOURCE = "https://management.core.windows.net/";
    private static final String CLIENT_ID = "502ea21d-e545-4c66-9129-c352ec902969";
    private static final String REDIRECT_URL = "https://xplatalm.com";

    final AzureAuthenticator azureAuthenticator;

    // singleton
    private VsoSwtAuthInfoProvider() {
        azureAuthenticator = new AzureAuthenticatorImpl(LOGIN_WINDOWS_NET_AUTHORITY,
                COMMON_TENANT,
                MANAGEMENT_CORE_RESOURCE,
                CLIENT_ID,
                REDIRECT_URL);
    }

    private static class VsoSwtAuthInfoProviderHolder {
        final static VsoSwtAuthInfoProvider INSTANCE = new VsoSwtAuthInfoProvider();
    }

    public static VsoSwtAuthInfoProvider getProvider() {
        return VsoSwtAuthInfoProviderHolder.INSTANCE;
    }

    @Override
    public void getAuthenticationInfoAsync(final String serverUri, final AuthenticationInfoCallback callback) {
        final SettableFuture<AuthenticationInfo> authenticationInfoFuture = SettableFuture.<AuthenticationInfo>create();
        //invoke AAD authentication library to get an account access token
        try {
            this.azureAuthenticator.getAadAccessTokenAsync(new AuthenticationCallback() {
                @Override
                public void onSuccess(final AuthenticationResult result) {
                    final AuthenticationInfo authenticationInfo;

                    if (result == null) {
                        //User closed the browser window without signing in
                        authenticationInfoFuture.setException(new RuntimeException("User closed the browser and never signed in."));
                    } else {
                        try {
                            final PersonalAccessTokenFactory patFactory = new PersonalAccessTokenFactoryImpl(result);

                            final String emailAddress = AuthHelper.getEmail(result);
                            final String tokenDescription = AuthHelper.getTokenDescription(emailAddress);

                            final SessionToken sessionToken = patFactory.createGlobalSessionToken(tokenDescription,
                                    Arrays.asList(TokenScope.CODE_READ, TokenScope.CODE_WRITE, TokenScope.CODE_MANAGE));

                            authenticationInfo = AuthHelper.createAuthenticationInfo(serverUri, result, sessionToken);

                            authenticationInfoFuture.set(authenticationInfo);

                        } catch (final Throwable t) {
                            logger.warn("getAuthenticationInfoAsync.onSuccess: Failed to setup PAT after authenticating", t);
                            authenticationInfoFuture.setException(t);
                        }
                    }
                }

                @Override
                public void onFailure(final Throwable t) {
                    authenticationInfoFuture.setException(t);
                }
            });
        } catch (IOException e) {
            authenticationInfoFuture.setException(e);
        }

        Futures.addCallback(authenticationInfoFuture, callback);
    }


}
