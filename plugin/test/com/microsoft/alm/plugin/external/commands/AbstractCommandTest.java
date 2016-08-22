// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.ProjectState;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import org.junit.Before;

import java.util.UUID;

public class AbstractCommandTest extends AbstractTest {
    protected ServerContext context;

    @Before
    public void setupContext() {
        TeamProjectCollectionReference collection = new TeamProjectCollectionReference();
        collection.setUrl("http://server:8080/tfs/defaultcollection");
        collection.setId(UUID.randomUUID());
        collection.setName("defaultcollection");

        TeamProjectReference project = new TeamProjectReference();
        project.setName("project1");
        project.setId(UUID.randomUUID());
        project.setUrl("http://server:8080/tfs/defaultcollection/project1");
        project.setState(ProjectState.WELL_FORMED);

        context = new ServerContextBuilder()
                .type(ServerContext.Type.TFS)
                .uri("http://server:8080/tfs")
                .serverUri("http://server:8080/tfs")
                .authentication(new AuthenticationInfo("user1", "pass", "http://server:8080/tfs", "user1"))
                .collection(collection)
                .teamProject(project)
                .build();

        doAdditionalSetup();
    }

    protected void doAdditionalSetup() {
    }
}
