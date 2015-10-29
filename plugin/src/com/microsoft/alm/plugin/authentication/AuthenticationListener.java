// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

/**
 * This interface should be implemented by classes that use the AuthenticationProviders.
 */
public interface AuthenticationListener <E extends AuthenticationInfo> {
    /**
     * This method is called by the provider when it begins authenticating.
     */
    void authenticating();

    /**
     * This method is called when the provider is done authenticating whether it succeeded or not.
     * @param authenticationInfo the successful authentication result, it will be <code>null</code> if authentication failed for any reason.
     * @param throwable if authentication failed, this is the exception. Cancelled or successful results do not have an exception.
     */
    void authenticated(final E authenticationInfo, final Throwable throwable);
}
