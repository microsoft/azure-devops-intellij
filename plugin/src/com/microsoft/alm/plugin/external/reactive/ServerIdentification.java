// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.reactive;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;

import java.net.URI;

public class ServerIdentification {
    private final URI serverUri;
    private final AuthenticationInfo authenticationInfo;

    public ServerIdentification(URI serverUri, AuthenticationInfo authenticationInfo) {
        this.serverUri = serverUri;
        this.authenticationInfo = authenticationInfo;
    }

    public URI getServerUri() {
        return serverUri;
    }

    public AuthenticationInfo getAuthenticationInfo() {
        return authenticationInfo;
    }
}
