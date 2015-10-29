// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

/**
 * This interface is the basis for VSO and TFS authentication.
 */
public interface AuthenticationInfo {
    /**
     * Provides the server uri for which the Authentication is valid.
     * @return
     */
    String getServerUri();

    /**
     * Provides the user name to create a Credentials object to authenticate with the server URI
     * @return
     */
    String getUserName();

    /**
     * Provides a password to create a Credentials object to authenticate with the server URI
     * @return
     */
    String getPassword();

    /**
     * Use this method to get the authenticated user name for display purposes.
     * This returns empty string if isAuthenticated is false
     */
    String getUserNameForDisplay();
}
