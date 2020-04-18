// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import java.util.HashMap;
import java.util.Map;

public class MockAuthenticationProvider extends VsoAuthenticationProvider {
    private final Map<String, AuthenticationInfo> authenticationInfoMap = new HashMap<>();
    public void setAuthenticationInfo(String serverUrl, AuthenticationInfo info) {
        authenticationInfoMap.put(serverUrl, info);
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo(String serverUri) {
        if (!authenticationInfoMap.containsKey(serverUri))
            return super.getAuthenticationInfo(serverUri);

        return authenticationInfoMap.get(serverUri);
    }

    @Override
    public void authenticateAsync(String serverUri, AuthenticationListener listener) {
        // Note this doesn't perfectly emulate the real authentication process, but should work for tests.
        for (Map.Entry<String, AuthenticationInfo> entry : authenticationInfoMap.entrySet()) {
            String authenticatedUrl = entry.getKey();
            if (serverUri.startsWith(authenticatedUrl)) {
                listener.authenticated(entry.getValue(), null);
                return;
            }
        }

        super.authenticateAsync(serverUri, listener);
    }

    @Override
    public boolean isAuthenticated(String serverUri) {
        if (!authenticationInfoMap.containsKey(serverUri))
            return super.isAuthenticated(serverUri);

        return authenticationInfoMap.get(serverUri) != null;
    }
}
