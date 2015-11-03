// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.mocks;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.services.CredentialsPrompt;

public class MockCredentialsPrompt implements CredentialsPrompt {

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
    public void validateCredentials(String serverUrl, AuthenticationInfo authenticationInfo) {
        // don't do anything here
    }
}
