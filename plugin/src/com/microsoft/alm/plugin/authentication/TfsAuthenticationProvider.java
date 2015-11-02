// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.services.CredentialsPrompt;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import org.apache.http.auth.Credentials;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.auth.win.CurrentWindowsCredentials;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Use this AuthenticationProvider to authenticate with a TFS server.
 */
public class TfsAuthenticationProvider implements AuthenticationProvider {
    private final static String USER_NAME = "user.name";

    private AuthenticationInfo lastAuthenticationInfo;

    public TfsAuthenticationProvider() {
    }

    public TfsAuthenticationProvider(final AuthenticationInfo authenticationInfo) {
        assert authenticationInfo != null;
        this.lastAuthenticationInfo = authenticationInfo;
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo() {
        return lastAuthenticationInfo;
    }

    @Override
    public void authenticateAsync(final String serverUrl, final AuthenticationListener listener) {
        // TODO: Create a thread for this work. Push back onto the UI thread to ask for authenticatedContext
        AuthenticationListener.Helper.authenticating(listener);

        final URI serverUri;
        try {
            serverUri = new URI(serverUrl);
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
                // TODO: having this object depend on ServerContext is problematic. Remove the dependency via an interface or callback
                // Test the authenticatedContext against the server
                newAuthenticationInfo = AuthHelper.createAuthenticationInfo(serverUrl, credentials);
                final ServerContext context =
                        new ServerContextBuilder().type(ServerContext.Type.TFS)
                                .uri(serverUri).authentication(newAuthenticationInfo).build();
                context.getSoapServices().getCatalogService().getProjectCollections();
                result = true;
                break;
            } catch (RuntimeException ex) {
                if (ex.getCause() != null && ex.getCause() instanceof HttpResponseException) {
                    HttpResponseException responseException = (HttpResponseException) ex.getCause();
                    if (responseException.getStatusCode() == 401) {
                        continue;
                    }
                }

                error = ex;
                result = false;
                break;
            }
        }

        if (!result) {
            clearAuthenticationDetails();
            AuthenticationListener.Helper.authenticated(listener, null, error);
        } else {
            // We have a valid authenticatedContext, remember it
            lastAuthenticationInfo = newAuthenticationInfo;
            AuthenticationListener.Helper.authenticated(listener, lastAuthenticationInfo, null);
        }
    }

    @Override
    public void clearAuthenticationDetails() {
        lastAuthenticationInfo = null;
    }

    @Override
    public boolean isAuthenticated() {
        return lastAuthenticationInfo != null;
    }

    private Credentials getCredentials(final String serverUrl) {
        final Credentials credentials;
        // Prompt for username AND password
        String password = null;
        String userName = System.getProperty(USER_NAME);
        if (ServerContextManager.isNtlmEnabled()) {
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

}
