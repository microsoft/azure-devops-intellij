// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.services;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;

/**
 * This simple interface allows the Authentication Providers to prompt for credentials.
 * Currently this only applies to TFS. VSO prompts using it's own out of process mechanism.
 */
public interface CredentialsPrompt {
    boolean prompt(String serverUrl, String defaultUserName);

    String getUserName();

    String getPassword();

    void validateCredentials(String serverUrl, AuthenticationInfo authenticationInfo);
}
