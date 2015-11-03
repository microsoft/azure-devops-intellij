// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

public class ServerContextTest extends AbstractTest {
    @Test
    public void constructor() {
        ServerContext context = new ServerContext(ServerContext.Type.TFS, null, null, null, null, null, null);
        Assert.assertEquals(ServerContext.Type.TFS, context.getType());
        Assert.assertNull(context.getAuthenticationInfo());
        Assert.assertNull(context.getUri());
        Assert.assertNull(context.getGitRepository());
        Assert.assertNull(context.getTeamProjectCollectionReference());
        Assert.assertNull(context.getTeamProjectReference());
        Assert.assertFalse(context.hasClient());
        context.dispose();

        URI serverUri = URI.create("http://server");
        AuthenticationInfo info = new AuthenticationInfo("", "", "", "");
        TeamProjectCollectionReference collection = new TeamProjectCollectionReference();
        TeamProjectReference project = new TeamProjectReference();
        GitRepository repo = new GitRepository();
        ServerContext context2 = new ServerContext(ServerContext.Type.TFS, info, serverUri, null, collection, project, repo);
        Assert.assertEquals(ServerContext.Type.TFS, context.getType());
        Assert.assertEquals(info, context2.getAuthenticationInfo());
        Assert.assertEquals(serverUri, context2.getUri());
        Assert.assertEquals(repo, context2.getGitRepository());
        Assert.assertEquals(collection, context2.getTeamProjectCollectionReference());
        Assert.assertEquals(project, context2.getTeamProjectReference());
        Assert.assertFalse(context2.hasClient());
        context2.dispose();
    }

    @Test
    public void isDisposed() {
        ServerContext context = new ServerContext(ServerContext.Type.TFS, null, null, null, null, null, null);
        Assert.assertFalse(context.isDisposed());
        context.dispose();
        Assert.assertTrue(context.isDisposed());

        // make sure that certain methods now throw
        try {
            context.getHttpClient();
            Assert.fail("getHttpClient didn't throw");
        } catch (RuntimeException ex) { /* correct */ }
        try {
            context.getSoapServices();
            Assert.fail("getSoapServices didn't throw");
        } catch (RuntimeException ex) { /* correct */ }
    }
}
