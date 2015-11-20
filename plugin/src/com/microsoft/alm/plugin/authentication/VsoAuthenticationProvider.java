// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.tf.common.authentication.aad.AzureAuthenticator;
import com.microsoft.tf.common.authentication.aad.impl.AzureAuthenticatorImpl;
import com.microsoft.visualstudio.services.account.webapi.model.Profile;
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

    /**
     * This constructor is protected to allow for testing
     */
    protected VsoAuthenticationProvider() {
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

    /**
     * Gets the user profile, tries to refresh authentication result if first attempt fails.
     * @return Profile userProfile of the authenticated user, null if user isn't authenticated
     */
    public Profile getAuthenticatedUserProfile() {
        if(!isAuthenticated()) {
            return null;
        }

        Profile profile = null;
        try {
            profile = getAzureAuthenticator().getUserProfile(getAuthenticationResult());
        } catch(Throwable t) {
            logger.warn("getAuthenticatedUserProfile", t);
        }

        if(profile == null) {
            //refresh the authentication result and try again
            refreshAuthenticationResult();
            try {
                profile = getAzureAuthenticator().getUserProfile(getAuthenticationResult());
            } catch(Throwable t) {
                logger.warn("getAuthenticatedUserProfile - failed after refreshing authentication result", t);
                throw new RuntimeException("Your previous Team Services session has expired, please 'Sign in...' again."); //TODO: localize
            }
        }

        return profile;
    }

    private void refreshAuthenticationResult() {
        try {
            lastDeploymentAuthenticationResult = getAzureAuthenticator().refreshAadAccessToken(getAuthenticationResult());
        } catch (IOException e) {
            // refreshing failed, log exception
            logger.warn("Refreshing access token failed", e);
            lastDeploymentAuthenticationResult = null;
        }
    }
}