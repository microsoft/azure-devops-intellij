// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.visualstudio.services.authentication.DelegatedAuthorization.webapi.model.SessionToken;
import com.microsoftopentechnologies.auth.AuthenticationResult;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import java.net.UnknownHostException;

/**
 * Static helpers for Authentication
 */
public class AuthHelper {
    private final static String COMPUTER_NAME = "computername";

    public static AuthenticationInfo createAuthenticationInfo(final String serverUri, final Credentials credentials) {
        return new AuthenticationInfo(
                credentials.getUserPrincipal().getName(),
                credentials.getPassword(),
                serverUri,
                credentials.getUserPrincipal().getName()
        );
    }

    public static AuthenticationInfo createAuthenticationInfo(final String serverUri, final AuthenticationResult authenticationResult) {
        return new AuthenticationInfo(
                getUserId(authenticationResult),
                getPassword(authenticationResult),
                serverUri,
                getEmail(authenticationResult)
        );
    }

    public static AuthenticationInfo createAuthenticationInfo(final String serverUri, final AuthenticationResult authenticationResult, final SessionToken sessionToken) {
        return new AuthenticationInfo(
                getUserId(authenticationResult),
                sessionToken.getToken(),
                serverUri,
                getEmail(authenticationResult)
        );
    }

    /**
     * Returns the NTCredentials or UsernamePasswordCredentials object
     *
     * @param type
     * @param authenticationInfo
     * @return
     */
    public static Credentials getCredentials(final ServerContext.Type type, final AuthenticationInfo authenticationInfo) {
        if (type == ServerContext.Type.TFS) {
            return getNTCredentials(authenticationInfo.getUserName(), authenticationInfo.getPassword());
        } else {
            return new UsernamePasswordCredentials(authenticationInfo.getUserName(), authenticationInfo.getPassword());
        }
    }

    /**
     * Returns an NTCredentials object for given username and password
     *
     * @param userName
     * @param password
     * @return
     */
    public static NTCredentials getNTCredentials(final String userName, final String password) {
        assert userName != null;
        assert password != null;

        String user = userName;
        String domain = "";
        final String workstation = getWorkstation();

        // If the username has a backslash, then the domain is the first part and the username is the second part
        if (userName.contains("\\")) {
            String[] parts = userName.split("[\\\\]");
            if (parts.length == 2) {
                domain = parts[0];
                user = parts[1];
            }
        } else if (userName.contains("/")) {
            // If the username has a slash, then the domain is the first part and the username is the second part
            String[] parts = userName.split("[/]");
            if (parts.length == 2) {
                domain = parts[0];
                user = parts[1];
            }
        } else if (userName.contains("@")) {
            // If the username has an asterisk, then the domain is the second part and the username is the first part
            String[] parts = userName.split("[@]");
            if (parts.length == 2) {
                user = parts[0];
                domain = parts[1];
            }
        }

        return new org.apache.http.auth.NTCredentials(user, password, workstation, domain);
    }

    private static String getWorkstation() {
        try {
            final java.net.InetAddress address = java.net.InetAddress.getLocalHost();
            return address.getHostName();
        } catch (UnknownHostException e) {
            return System.getenv(COMPUTER_NAME);
        }
    }

    private static String getUserId(final AuthenticationResult authenticationResult) {
        return authenticationResult.getUserInfo().getUniqueName();
    }

    private static String getPassword(final AuthenticationResult authenticationResult) {
        return authenticationResult.getAccessToken();
    }

    public static String getEmail(final AuthenticationResult authenticationResult) {
        final String email;
        final String identityProvider = authenticationResult.getUserInfo().getIdentityProvider();
        if (identityProvider == null || identityProvider.isEmpty()) {
            email = authenticationResult.getUserInfo().getUniqueName();
        } else {
            email = authenticationResult.getUserInfo().getUniqueName().substring(identityProvider.length() + 1);
        }
        return email;
    }
}
