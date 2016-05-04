// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.mocks;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;

import java.net.URI;
import java.util.UUID;

public class MockServerContext extends ServerContext {

    public MockServerContext(Type type, final AuthenticationInfo authenticationInfo, final URI serverUri, final TeamProjectCollectionReference collection, final TeamProjectReference project, final GitRepository repo) {
        super(type, authenticationInfo, UUID.randomUUID(), serverUri, serverUri, null, collection, project, repo);
    }
}
