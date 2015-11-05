// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.mocks.MockVsoAuthenticationProvider;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.List;

public class ServerContextManagerTest extends AbstractTest {
    // short hand
    private ServerContextManager manager = ServerContextManager.getInstance();

    @Before
    public void init() {
        manager.setActiveContext(ServerContext.NO_CONTEXT);
    }

    @Test
    public void activeTfsContext() {
        Assert.assertNull(manager.getActiveContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).build();
        manager.setActiveContext(context);
        Assert.assertEquals(context, manager.getActiveContext());

        // Check the Tfs method - it should return the same context
        Assert.assertNotNull(manager.getActiveTfsContext());
        Assert.assertEquals(context, manager.getActiveTfsContext());

        manager.setActiveContext(ServerContext.NO_CONTEXT);
        Assert.assertNull(manager.getActiveContext());
    }

    @Test
    public void activeVsoContext() {
        Assert.assertNull(manager.getActiveContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.VSO).build();
        manager.setActiveContext(context);
        Assert.assertEquals(context, manager.getActiveContext());

        // Check the Tfs method - it should return null
        Assert.assertNull(manager.getActiveTfsContext());

        manager.setActiveContext(ServerContext.NO_CONTEXT);
        Assert.assertNull(manager.getActiveContext());

        ServerContext context2 = new ServerContextBuilder().type(ServerContext.Type.VSO_DEPLOYMENT).build();
        try {
            manager.setActiveContext(context2);
        } catch (IllegalArgumentException ex) { /* correct */ }
    }

    @Test
    public void getServerContext() {
        URI uri = URI.create("http://server/path");
        Assert.assertNull(manager.getActiveContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri(uri).build();
        manager.setActiveContext(context);

        ServerContext testContext = manager.getServerContext(uri);
        Assert.assertNotNull(testContext);
        Assert.assertEquals(uri, testContext.getUri());

        List<ServerContext> contexts = manager.getAllServerContexts();
        Assert.assertEquals(1, contexts.size());
        Assert.assertEquals(uri, contexts.get(0).getUri());
    }

    @Test
    public void clearServerContext() {
        URI uri = URI.create("http://server/path");
        Assert.assertNull(manager.getActiveContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri(uri).build();
        manager.setActiveContext(context);

        manager.clearServerContext(uri);
        Assert.assertNull(manager.getActiveContext());
        ServerContext testContext = manager.getServerContext(uri);
        Assert.assertNull(testContext);
    }

    /**
     * This test avoids the problems with authenticating by asking for the context
     * that is already the active context.
     */
    @Test
    public void getAuthenticatedContext_simplest() {
        Assert.assertNull(manager.getActiveContext());

        URI gitUri = URI.create("http://server/_git/repo1");
        AuthenticationInfo info = new AuthenticationInfo("", "", "", "");
        TeamProjectCollectionReference collection = new TeamProjectCollectionReference();
        TeamProjectReference project = new TeamProjectReference();
        GitRepository repo = new GitRepository();
        repo.setRemoteUrl(gitUri.toString());
        ServerContext context = new ServerContext(ServerContext.Type.TFS, info, gitUri, null, collection, project, repo);
        manager.setActiveContext(context);

        ServerContext testContext = manager.getAuthenticatedContext(gitUri.toString(), "new pat", true);
        Assert.assertNotNull(testContext);
        Assert.assertEquals(gitUri, testContext.getUri());
    }


    @Test
    public void createVsoContext_basics() {
        final ServerContext tfsContext = new ServerContextBuilder().type(ServerContext.Type.TFS).build();
        final ServerContext vsoContext = new ServerContextBuilder().type(ServerContext.Type.VSO).build();
        final ServerContext vsoDeploymentContext = new ServerContextBuilder()
                .type(ServerContext.Type.VSO_DEPLOYMENT)
                .uri("http://server.vs.com/path")
                .build();

        try {
            manager.createVsoContext(null, null, null);
            Assert.fail();
        } catch (IllegalArgumentException ex) { /* correct */ }
        try {
            manager.createVsoContext(tfsContext, null, null);
            Assert.fail();
        } catch (IllegalArgumentException ex) { /* correct */ }
        try {
            manager.createVsoContext(vsoContext, null, null);
            Assert.fail();
        } catch (IllegalArgumentException ex) { /* correct */ }
        try {
            manager.createVsoContext(vsoDeploymentContext, null, null);
            Assert.fail();
        } catch (IllegalArgumentException ex) { /* correct */ }

        // Make sure that createVsoContext doesn't throw in the case where we can't get the account
        final AuthenticationInfo info = new AuthenticationInfo("", "", "", "");
        final MockVsoAuthenticationProvider authProvider = new MockVsoAuthenticationProvider(info);
        ServerContext context = manager.createVsoContext(vsoDeploymentContext, authProvider, null);
        Assert.assertNull(context);
    }
}
