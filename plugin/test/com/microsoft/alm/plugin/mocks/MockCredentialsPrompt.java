// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.mocks;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.services.CredentialsPrompt;

import java.util.HashMap;
import java.util.Map;

public class MockCredentialsPrompt implements CredentialsPrompt {

    private Map<String,Boolean> serverSuccessMap = new HashMap<String, Boolean>();

    public void registerServer(String serverUrl, Boolean success) {
        serverSuccessMap.put(serverUrl, success);
    }

    public void unregisterServer(String serverUrl) {
        serverSuccessMap.remove(serverUrl);
    }

    @Override
    public boolean prompt(String serverUrl, String defaultUserName) {
        return true;
    }

    @Override
    public String getUserName() {
        return "userName1";
    }

    @Override
    public String getPassword() {
        return "password1";
    }

    @Override
    public String validateCredentials(String serverUrl, AuthenticationInfo authenticationInfo) {
        if (serverSuccessMap.containsKey(serverUrl) && !serverSuccessMap.get(serverUrl)) {
            throw new RuntimeException("Purposefully throwing a failure for testing.");
        }

        return serverUrl;
    }
}
