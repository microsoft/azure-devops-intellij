// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.context.rest.VstsHttpClient;
import com.microsoft.alm.plugin.services.CredentialsPrompt;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.auth.win.CurrentWindowsCredentials;
import org.apache.http.impl.client.WinHttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Use this AuthenticationProvider to authenticate with a TFS server.
 */
public class TfsAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(TfsAuthenticationProvider.class);

    private final static String USER_NAME = "user.name";
    public final static String TFS_LAST_USED_URL = "http://_TFS_LAST_USED_URL_";

    private static class Holder {
        private final static TfsAuthenticationProvider INSTANCE = new TfsAuthenticationProvider();
    }

    private TfsAuthenticationProvider() {
    }

    public static TfsAuthenticationProvider getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo() {
        return ServerContextManager.getInstance().getBestAuthenticationInfo(TFS_LAST_USED_URL, false);
    }

    @Override
    public void authenticateAsync(final String serverUrl, final AuthenticationListener listener) {
        logger.info("starting TfsAuthenticator");
        final TfsAuthenticator authenticator = new TfsAuthenticator(serverUrl, listener);
        authenticator.start();
    }

    @Override
    public void clearAuthenticationDetails() {
        ServerContextManager.getInstance().remove(TFS_LAST_USED_URL);
    }

    @Override
    public boolean isAuthenticated() {
        return getAuthenticationInfo() != null;
    }

    private static class TfsAuthenticator extends Thread {
        private final String serverUrl;
        private final AuthenticationListener listener;

        public TfsAuthenticator(final String serverUrl, final AuthenticationListener listener) {
            this.serverUrl = serverUrl;
            this.listener = listener;
        }

        @Override
        public void run() {
            logger.info("Async authentication starting");
            AuthenticationListener.Helper.authenticating(listener);

            try {
                final URI serverUri = new URI(serverUrl);
            } catch (URISyntaxException e) {
                AuthenticationListener.Helper.authenticated(listener, null, e);
                return;
            }

            Credentials credentials;
            AuthenticationInfo newAuthenticationInfo = null;
            boolean result = false;
            Exception error = null;

            for (int retry = 0; retry < 4; retry++) {
                // After the first try, we force the user to enter credentials
                credentials = getCredentials(serverUrl);
                if (credentials == null) {
                    // The user canceled the login prompt, so break out of the loop
                    result = false;
                    break;
                }

                try {
                    // Test the authenticatedContext against the server
                    newAuthenticationInfo = AuthHelper.createAuthenticationInfo(serverUrl, credentials);
                    final CredentialsPrompt prompt = PluginServiceProvider.getInstance().getCredentialsPrompt();
                    final String authenticatedUrl = prompt.validateCredentials(serverUrl, newAuthenticationInfo);
                    // recreate the auth info with the url that we validated against
                    newAuthenticationInfo = AuthHelper.createAuthenticationInfo(authenticatedUrl, credentials);
                    result = true;
                    break;
                } catch (RuntimeException ex) {
                    if (ex.getCause() != null && ex.getCause() instanceof VstsHttpClient.VstsHttpClientException) {
                        VstsHttpClient.VstsHttpClientException responseException = (VstsHttpClient.VstsHttpClientException) ex.getCause();
                        if (responseException.getStatusCode() == 401) {
                            continue;
                        }
                    }

                    error = ex;
                    result = false;
                    break;
                }
            }

            logger.info("Async authentication done - result: " + result);
            if (!result) {
                TfsAuthenticationProvider.getInstance().clearAuthenticationDetails();
                AuthenticationListener.Helper.authenticated(listener, null, error);
            } else {
                AuthenticationListener.Helper.authenticated(listener, newAuthenticationInfo, null);
            }
        }


        private Credentials getCredentials(final String serverUrl) {
            final Credentials credentials;
            // Prompt for username AND password
            String password;
            String userName = System.getProperty(USER_NAME);
            if (isNtlmEnabled()) {
                //user current logged in user name if on windows
                userName = CurrentWindowsCredentials.getCurrentUsername();
            }
            CredentialsPrompt prompt = PluginServiceProvider.getInstance().getCredentialsPrompt();

            if (prompt.prompt(serverUrl, userName)) {
                userName = prompt.getUserName();
                password = prompt.getPassword();
                credentials = AuthHelper.getNTCredentials(userName, password);
            } else {
                credentials = null;
            }

            return credentials;
        }

        private boolean isNtlmEnabled() {
            final String propertyNtlmEnabled = System.getProperty("ntlmEnabled");
            final boolean isNtlmEnabled = propertyNtlmEnabled == null || Boolean.parseBoolean(propertyNtlmEnabled);
            return isNtlmEnabled && WinHttpClients.isWinAuthAvailable();
        }
    }
}
