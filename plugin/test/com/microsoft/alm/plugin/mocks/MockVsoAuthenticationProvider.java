// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.mocks;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationListener;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;

public class MockVsoAuthenticationProvider extends VsoAuthenticationProvider {
    private AuthenticationInfo authenticationInfo;

    public MockVsoAuthenticationProvider(final AuthenticationInfo authenticationInfo) {
        this.authenticationInfo = authenticationInfo;
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo() {
        return authenticationInfo;
    }

    @Override
    public void authenticateAsync(String serverUri, AuthenticationListener listener) {
        if (listener != null) {
            listener.authenticating();
            listener.authenticated(authenticationInfo, null);
        }
    }

    @Override
    public void clearAuthenticationDetails() {
        authenticationInfo = null;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticationInfo != null;
    }
}
