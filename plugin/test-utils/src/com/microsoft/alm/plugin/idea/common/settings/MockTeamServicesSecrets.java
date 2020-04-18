// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.settings;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;

import java.io.IOException;

public class MockTeamServicesSecrets extends TeamServicesSecrets {
    private AuthenticationInfo forcedAuthenticationInfo;
    private boolean ignoreWrites;

    @Override
    public AuthenticationInfo load(String key) throws IOException {
        return forcedAuthenticationInfo == null ? super.load(key) : forcedAuthenticationInfo;
    }

    @Override
    public void save(ServerContext context) {
        if (!ignoreWrites)
            super.save(context);
    }

    public void forceUseAuthenticationInfo(AuthenticationInfo authenticationInfo) {
        forcedAuthenticationInfo = authenticationInfo;
    }

    public void setIgnoreWrites(boolean ignoreWrites) {
        this.ignoreWrites = ignoreWrites;
    }
}
