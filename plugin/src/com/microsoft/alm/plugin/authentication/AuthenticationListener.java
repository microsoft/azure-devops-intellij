// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

/**
 * This interface should be implemented by classes that use the AuthenticationProviders.
 */
public interface AuthenticationListener {

    class Helper {
        static void authenticating(final AuthenticationListener listener) {
            if (listener != null) {
                listener.authenticating();
            }
        }

        static void authenticated(final AuthenticationListener listener, final AuthenticationInfo authenticationInfo, final Throwable throwable) {
            if (listener != null) {
                listener.authenticated(authenticationInfo, throwable);
            }
        }
    }

    /**
     * This method is called by the provider when it begins authenticating.
     */
    void authenticating();

    /**
     * This method is called when the provider is done authenticating whether it succeeded or not.
     *
     * @param authenticationInfo the successful authentication result, it will be <code>null</code> if authentication failed for any reason.
     * @param throwable          if an exception caused authentication failure, this is it. Cancelled or successful results do not have an exception.
     */
    void authenticated(final AuthenticationInfo authenticationInfo, final Throwable throwable);

}
