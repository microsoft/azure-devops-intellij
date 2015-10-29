// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.tf.common.authentication.aad.AzureAuthenticator;
import com.microsoft.tf.common.authentication.aad.impl.AzureAuthenticatorImpl;
import com.microsoft.visualstudio.services.authentication.DelegatedAuthorization.webapi.model.SessionToken;
import com.microsoftopentechnologies.auth.AuthenticationCallback;
import com.microsoftopentechnologies.auth.AuthenticationResult;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Use this AuthenticationProvider to authenticate with VSO.
 */
public class VsoAuthenticationProvider implements AuthenticationProvider<VsoAuthenticationInfo> {
    //azure connection strings
    private static final String LOGIN_WINDOWS_NET_AUTHORITY = "login.windows.net";
    private static final String COMMON_TENANT = "common";
    private static final String MANAGEMENT_CORE_RESOURCE = "https://management.core.windows.net/";
    private static final String CLIENT_ID = "502ea21d-e545-4c66-9129-c352ec902969";
    private static final String REDIRECT_URL = "https://xplatalm.com";

    public static final String VSO_ROOT = "http://visualstudio.com";

    private VsoAuthenticationInfo authenticationInfo;

    private static class AzureAuthenticatorHolder {
        private static AzureAuthenticator INSTANCE = new AzureAuthenticatorImpl(LOGIN_WINDOWS_NET_AUTHORITY,
                COMMON_TENANT,
                MANAGEMENT_CORE_RESOURCE,
                CLIENT_ID,
                REDIRECT_URL);
    }

    /**
     * @return
     */
    public static AzureAuthenticator getAzureAuthenticator() {
        return AzureAuthenticatorHolder.INSTANCE;
    }

    public VsoAuthenticationProvider() {
    }

    public VsoAuthenticationProvider(VsoAuthenticationInfo authenticationInfo) {
        assert authenticationInfo != null;
        this.authenticationInfo = authenticationInfo;
    }

    @Override
    public VsoAuthenticationInfo getAuthenticationInfo() {
        this.authenticationInfo = getValidAuthenticationInfo(authenticationInfo, true);
        return this.authenticationInfo;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticationInfo != null;
    }

    @Override
    public void clearAuthenticationDetails() {
        authenticationInfo = null;
    }

    @Override
    public void authenticateAsync(final String serverUri, final AuthenticationListener<VsoAuthenticationInfo> listener) {
        onAuthenticating(listener);

        //invoke AAD authentication library to get an account access token
        try {
            getAzureAuthenticator().getAadAccessTokenAsync(new AuthenticationCallback() {
                @Override
                public void onSuccess(final AuthenticationResult result) {
                    if (result == null) {
                        //User closed the browser window without signing in
                        clearAuthenticationDetails();
                        onAuthenticated(listener, null, null);
                    } else {
                        authenticationInfo = new VsoAuthenticationInfo(serverUri, result, null);
                        onAuthenticated(listener, authenticationInfo, null);
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    clearAuthenticationDetails();
                    onAuthenticated(listener, null, throwable);
                }
            });
        } catch (IOException e) {
            onAuthenticated(listener, null, e);
        }
    }

    private VsoAuthenticationInfo getValidAuthenticationInfo(final VsoAuthenticationInfo vsoAuthenticationInfo, final boolean autoRefresh) {
        AuthenticationResult authenticationResult = AuthHelper.getAuthenticationResult(vsoAuthenticationInfo);
        if (authenticationResult != null) {
            if (!isExpired(authenticationResult)) {
                // found an unexpired one
                return vsoAuthenticationInfo;
            } else if (autoRefresh) {
                try {
                    // refresh it
                    final AuthenticationResult newResult = getAzureAuthenticator().refreshAadAccessToken(authenticationResult);
                    if (newResult != null) {
                        return new VsoAuthenticationInfo(vsoAuthenticationInfo.getServerUri(), newResult, vsoAuthenticationInfo.getSessionToken());
                    }
                } catch (IOException e) {
                    // refreshing failed, get a new one
                    return getNewAccessToken(vsoAuthenticationInfo);
                }
            }
        } else if (autoRefresh) {
            // get a new one
            return getNewAccessToken(null);
        }
        return null;
    }

    private VsoAuthenticationInfo getNewAccessToken(final VsoAuthenticationInfo vsoAuthenticationInfo) {
        try {
            final AuthenticationResult authenticationResult = getAzureAuthenticator().getAadAccessToken();
            if (authenticationResult != null) { // will return null if user cancels login sequence
                return new VsoAuthenticationInfo(vsoAuthenticationInfo.getServerUri(), authenticationResult, vsoAuthenticationInfo.getSessionToken());
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isExpired(final AuthenticationResult authenticationResult) {
        final long secondsToExpiry = authenticationResult.getExpiresOn();
        Date dateOfExpiry = new Date(secondsToExpiry * 1000);
        Date dateNow = new Date();
        return dateNow.after(dateOfExpiry);
    }

    private static boolean isExpired(final SessionToken sessionToken) {
        Date dateOfExpiry = sessionToken.getValidTo();
        Date dateNow = new Date();
        return dateNow.after(dateOfExpiry);
    }

    //TODO this should be turned into a utility method and combined with the TFS version
    private static void onAuthenticating(final AuthenticationListener<VsoAuthenticationInfo> listener) {
        if (listener != null) {
            listener.authenticating();
        }
    }

    //TODO this should be turned into a utility method and combined with the TFS version
    private static void onAuthenticated(final AuthenticationListener<VsoAuthenticationInfo> listener, final VsoAuthenticationInfo vsoAuthenticationInfo, Throwable throwable) {
        if (listener != null) {
            listener.authenticated(vsoAuthenticationInfo, throwable);
        }
    }
}
