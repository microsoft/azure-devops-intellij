// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.tf.common.authentication.aad.AzureAuthenticator;
import com.microsoft.tf.common.authentication.aad.impl.AzureAuthenticatorImpl;
import com.microsoftopentechnologies.auth.AuthenticationCallback;
import com.microsoftopentechnologies.auth.AuthenticationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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

    public static final String VSO_ROOT = "http://visualstudio.com";

    private static AuthenticationResult lastDeploymentAuthenticationResult;
    private static AuthenticationInfo lastDeploymentAuthenticationInfo;

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

    private VsoAuthenticationProvider() {
    }

    private static class VsoAuthenticationProviderHolder {
        private static VsoAuthenticationProvider INSTANCE = new VsoAuthenticationProvider();
    }

    public static VsoAuthenticationProvider getInstance() {
        return VsoAuthenticationProviderHolder.INSTANCE;
    }

    public AuthenticationResult getAuthenticationResult() {
        return lastDeploymentAuthenticationResult;
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo() {
        return lastDeploymentAuthenticationInfo;
    }

    @Override
    public boolean isAuthenticated() {
        return lastDeploymentAuthenticationResult != null;
    }

    // TODO this method is not used because it throws an exception
    public void forceRefreshResult() {
        synchronized (this) {
            if (lastDeploymentAuthenticationResult != null) {
                try {
                    // always refresh it -- this is the only way to ensure it is valid
                    lastDeploymentAuthenticationResult = getAzureAuthenticator().refreshAadAccessToken(lastDeploymentAuthenticationResult);
                } catch (IOException e) {
                    lastDeploymentAuthenticationResult = null;
                    // refreshing failed, log exception
                    logger.warn("Refreshing access token failed", e);
                }
            }
        }
    }

    @Override
    public void clearAuthenticationDetails() {
        lastDeploymentAuthenticationResult = null;
        lastDeploymentAuthenticationInfo = null;
    }

    @Override
    public void authenticateAsync(final String serverUri, final AuthenticationListener listener) {
        AuthenticationListener.Helper.authenticating(listener);

        //invoke AAD authentication library to get an account access token
        try {
            getAzureAuthenticator().getAadAccessTokenAsync(new AuthenticationCallback() {
                @Override
                public void onSuccess(final AuthenticationResult result) {
                    if (result == null) {
                        //User closed the browser window without signing in
                        clearAuthenticationDetails();
                    } else {
                        lastDeploymentAuthenticationResult = result;
                        lastDeploymentAuthenticationInfo = AuthHelper.createAuthenticationInfo(serverUri, lastDeploymentAuthenticationResult);
                    }
                    AuthenticationListener.Helper.authenticated(listener, lastDeploymentAuthenticationInfo, null);
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
}