// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;


import org.apache.http.auth.Credentials;

/**
 * This implementation of the AuthenticationInfo interface represents the TFS authentication information.
 */
public class TfsAuthenticationInfo implements AuthenticationInfo {
    private final String userName;
    private final String password;
    private final String serverUri;
    private final String userNameForDisplay;

    /**
     * Default constructor for serialization/deserialization
     */
    public TfsAuthenticationInfo() {
        serverUri = null;
        userName = null;
        password = null;
        userNameForDisplay = null;
    }

    public TfsAuthenticationInfo(final String serverUri, final Credentials credentials) {
        this.serverUri = serverUri;
        this.userName = credentials.getUserPrincipal().getName();
        this.password = credentials.getPassword();
        this.userNameForDisplay = this.userName;
    }

    @Override
    public String getServerUri() {
        return serverUri;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUserNameForDisplay() {
        return userNameForDisplay;
    }
}
