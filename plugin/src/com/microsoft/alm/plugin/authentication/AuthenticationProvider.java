// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

/**
 * This interface allows UI components to initiate an async authentication process.
 * Classes using this interface should call setListener to get feedback on events.
 */
public interface AuthenticationProvider {
    /**
     * Use this method to get the current authenticationInfo object.
     * This returns null if isAuthenticated returns false.
     */
    AuthenticationInfo getAuthenticationInfo();

    /**
     * Use this method to initiate the background authentication.
     *
     * @param serverUri the server to authenticate with
     */
    void authenticateAsync(final String serverUri, final AuthenticationListener listener);

    /**
     * Use this method to clear all authentication information and set isAuthenticated to false.
     */
    void clearAuthenticationDetails();

    /**
     * This method returns true if the Authentication has completed and was successful.
     */
    boolean isAuthenticated();

}
