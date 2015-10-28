// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.mocks;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;

import java.net.URI;
import java.util.UUID;

public class MockServerContext extends ServerContext {

    public MockServerContext(final AuthenticationInfo authenticationInfo, final URI serverUri) {
        super(Type.TFS, authenticationInfo, serverUri, null);
    }

    public MockServerContext(final AuthenticationInfo authenticationInfo, final URI uri, final UUID accountId) {
        super(Type.VSO, authenticationInfo, uri, accountId);
    }

}
