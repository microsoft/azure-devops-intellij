// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

/**
 * This interface should be implemented by classes that use the AuthenticationProviders.
 */
public interface AuthenticationListener {

    class Helper {
        static void onAuthenticating(final AuthenticationListener listener) {
            if (listener != null) {
                listener.onAuthenticating();
            }
        }

        static void onSuccess(final AuthenticationListener listener) {
            if (listener != null) {
                listener.onSuccess();
            }
        }

        static void onFailure(final AuthenticationListener listener, final Throwable throwable) {
            if (listener != null) {
                listener.onFailure(throwable);
            }
        }
    }

    /**
     * This method is called by the provider when it begins authenticating.
     */
    void onAuthenticating();

    /**
     * This method is called when the provider is done successfully authenticating
     */
    void onSuccess();

    /**
     * This method is called if authentication fails.
     *
     * @param throwable
     */
    void onFailure(final Throwable throwable);

}
