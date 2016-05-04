// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

public class ServerContextBuilderTest {
    @Test
    public void testEmpty() {
        ServerContextBuilder builder = new ServerContextBuilder();
        ServerContext context = builder.type(ServerContext.Type.TFS).build();
        Assert.assertEquals(ServerContext.Type.TFS, context.getType());
        Assert.assertNull(context.getAuthenticationInfo());
        Assert.assertNull(context.getUri());
        Assert.assertNull(context.getGitRepository());
        Assert.assertNull(context.getTeamProjectCollectionReference());
        Assert.assertNull(context.getTeamProjectReference());
        Assert.assertFalse(context.hasClient());
        context.dispose();
    }

    @Test
    public void testFull() {
        URI serverUri = URI.create("http://server");
        AuthenticationInfo info = new AuthenticationInfo("", "", "", "");
        TeamProjectCollectionReference collection = new TeamProjectCollectionReference();
        TeamProjectReference project = new TeamProjectReference();
        GitRepository repo = new GitRepository();

        ServerContextBuilder builder = new ServerContextBuilder();
        builder.type(ServerContext.Type.TFS)
                .authentication(info)
                .collection(collection)
                .repository(repo)
                .teamProject(project)
                .uri(serverUri);
        ServerContext context = builder.build();
        Assert.assertEquals(ServerContext.Type.TFS, context.getType());
        Assert.assertEquals(info, context.getAuthenticationInfo());
        Assert.assertEquals(serverUri, context.getUri());
        Assert.assertEquals(repo, context.getGitRepository());
        Assert.assertEquals(collection, context.getTeamProjectCollectionReference());
        Assert.assertEquals(project, context.getTeamProjectReference());
        Assert.assertFalse(context.hasClient());
        context.dispose();
    }

    @Test
    public void testCopy() {
        URI serverUri = URI.create("http://server");
        AuthenticationInfo info = new AuthenticationInfo("", "", "", "");
        TeamProjectCollectionReference collection = new TeamProjectCollectionReference();
        TeamProjectReference project = new TeamProjectReference();
        GitRepository repo = new GitRepository();
        ServerContext originalContext = new ServerContext(ServerContext.Type.TFS, info, null, serverUri, serverUri, null, collection, project, repo);

        ServerContextBuilder builder = new ServerContextBuilder(originalContext);
        ServerContext context = builder.build();
        Assert.assertEquals(ServerContext.Type.TFS, context.getType());
        Assert.assertEquals(info, context.getAuthenticationInfo());
        Assert.assertEquals(serverUri, context.getUri());
        Assert.assertEquals(repo, context.getGitRepository());
        Assert.assertEquals(collection, context.getTeamProjectCollectionReference());
        Assert.assertEquals(project, context.getTeamProjectReference());
        Assert.assertFalse(context.hasClient());
        context.dispose();
    }

}
