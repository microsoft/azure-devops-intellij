// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.mocks;

import com.microsoft.alm.plugin.authentication.TfsAuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.visualstudio.services.account.webapi.model.Account;

import java.net.URI;

public class MockServerContext extends ServerContext {

    public MockServerContext(final TfsAuthenticationInfo authenticationInfo, final URI serverUri) {
        super(Type.TFS, authenticationInfo, serverUri, null);
    }

    public MockServerContext(final VsoAuthenticationInfo authenticationInfo, final URI uri, final Account account) {
        super(Type.VSO, authenticationInfo, uri, account);
    }

}
