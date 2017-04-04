// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

/**
 * Immutable
 */
public class AuthenticationInfo {

    public enum CredsType {
        AccessToken,
        PersonalAccessToken,
        NTLM
    }
    // If we all use this constant, it provides future type safely if we ever decided to change NONE to a
    // typed variable instead of 'null'
    public static AuthenticationInfo NONE = null;

    private final String userName;
    private final String password;
    private final String serverUri;
    private final String userNameForDisplay;
    private final String refreshToken;
    private final CredsType type;

    /**
     * Empty constructor for JSON deserialization only.  Do not use this otherwise (which is why it is marked deprecated).
     *
     * @deprecated
     */
    public AuthenticationInfo() {
        this(null, null, null, null, null, null);
    }

    public AuthenticationInfo(final String userName,
                              final String password,
                              final String serverUri,
                              final String userNameForDisplay) {
        this(userName, password, serverUri, userNameForDisplay, null, null);
    }

    public AuthenticationInfo(final String userName,
                              final String password,
                              final String serverUri,
                              final String userNameForDisplay,
                              final CredsType type,
                              final String refreshToken) {
        this.userName = userName;
        this.password = password;
        this.serverUri = serverUri;
        this.userNameForDisplay = userNameForDisplay;
        this.type = type;
        this.refreshToken = refreshToken;
    }

    public String getServerUri() {
        return serverUri;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getUserNameForDisplay() {
        return userNameForDisplay;
    }

    public CredsType getType() {
        return type;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
