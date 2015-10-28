// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

/**
 * Immutable
 */
public class AuthenticationInfo {
    private final String userName;
    private final String password;
    private final String serverUri;
    private final String userNameForDisplay;

    public AuthenticationInfo(final String userName, final String password, final String serverUri, final String userNameForDisplay) {
        this.userName = userName;
        this.password = password;
        this.serverUri = serverUri;
        this.userNameForDisplay = userNameForDisplay;
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

}
