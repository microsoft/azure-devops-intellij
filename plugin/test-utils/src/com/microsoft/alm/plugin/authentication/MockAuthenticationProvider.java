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

    private final Map<String, Boolean> authenticatedMap = new HashMap<>();
    public void setAuthenticated(String serverUrl, boolean authenticated) {
        authenticatedMap.put(serverUrl, authenticated);
    }

    @Override
    public boolean isAuthenticated(String serverUri) {
        if (!authenticatedMap.containsKey(serverUri))
            return super.isAuthenticated(serverUri);

        return authenticatedMap.get(serverUri);
    }
}
