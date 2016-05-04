// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

public class ServerContextManagerTest extends AbstractTest {
    @Test
    public void testEmptyManager() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertEquals(0, manager.getAllServerContexts().size());
        Assert.assertNull(manager.getLastUsedContext());
        manager.clearLastUsedContext();
        ServerContext context = manager.get("foo");
        Assert.assertNull(context);
        manager.remove("foo");
    }

    @Test
    public void testAdd() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertEquals(0, manager.getAllServerContexts().size());
        Assert.assertNull(manager.getLastUsedContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").build();
        manager.add(context);
        Assert.assertEquals(context, manager.getLastUsedContext());
        Assert.assertEquals(1, manager.getAllServerContexts().size());
        ServerContext _context = manager.get(context.getUri().toString());
        Assert.assertEquals(context, _context);

        // add a second context
        ServerContext context2 = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server2/path2").build();
        manager.add(context2);
        Assert.assertEquals(context2, manager.getLastUsedContext());
        Assert.assertEquals(2, manager.getAllServerContexts().size());
        ServerContext _context2 = manager.get(context2.getUri().toString());
        Assert.assertEquals(context2, _context2);

        // add a third context that has a very similar URI
        ServerContext context3 = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server2/path2/3").build();
        manager.add(context3);
        Assert.assertEquals(context3, manager.getLastUsedContext());
        Assert.assertEquals(3, manager.getAllServerContexts().size());
        ServerContext _context3 = manager.get(context3.getUri().toString());
        Assert.assertEquals(context3, _context3);
    }

    @Test
    public void testAddDuplicate() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertEquals(0, manager.getAllServerContexts().size());
        Assert.assertNull(manager.getLastUsedContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").build();
        manager.add(context);
        Assert.assertEquals(context, manager.getLastUsedContext());
        Assert.assertEquals(1, manager.getAllServerContexts().size());
        ServerContext _context = manager.get(context.getUri().toString());
        Assert.assertEquals(context, _context);

        // add a second context that has the SAME URI
        ServerContext context2 = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").build();
        manager.add(context2);
        Assert.assertEquals(context2, manager.getLastUsedContext());
        Assert.assertEquals(1, manager.getAllServerContexts().size());
        ServerContext _context2 = manager.get(context2.getUri().toString());
        Assert.assertEquals(context2, _context2);
        Assert.assertNotEquals(context, _context2);

        // add a third with upper case URI
        ServerContext context3 = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("HTTP://SERVER/PATH").build();
        manager.add(context3);
        Assert.assertEquals(context3, manager.getLastUsedContext());
        Assert.assertEquals(1, manager.getAllServerContexts().size());
        ServerContext _context3 = manager.get(context3.getUri().toString());
        Assert.assertEquals(context3, _context3);
        Assert.assertNotEquals(context, _context3);
    }

    @Test
    public void testRemove() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertEquals(0, manager.getAllServerContexts().size());
        Assert.assertNull(manager.getLastUsedContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").build();
        manager.add(context);
        ServerContext context2 = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server2/path2").build();
        manager.add(context2);
        ServerContext context3 = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server2/path2/3").build();
        manager.add(context3);
        Assert.assertEquals(context3, manager.getLastUsedContext());
        Assert.assertEquals(3, manager.getAllServerContexts().size());

        // Remove context2 and make sure 1 and 3 are left
        manager.remove(context2.getUri().toString());
        Assert.assertEquals(2, manager.getAllServerContexts().size());
        ServerContext _context2 = manager.get(context2.getUri().toString());
        Assert.assertNull(_context2);
        ServerContext _context = manager.get(context.getUri().toString());
        Assert.assertEquals(context, _context);
        ServerContext _context3 = manager.get(context3.getUri().toString());
        Assert.assertEquals(context3, _context3);
        Assert.assertEquals(context3, manager.getLastUsedContext());

        // Remove 3 and assure 1 is left
        manager.remove(context3.getUri().toString());
        Assert.assertEquals(1, manager.getAllServerContexts().size());
        _context3 = manager.get(context3.getUri().toString());
        Assert.assertNull(_context3);
        _context = manager.get(context.getUri().toString());
        Assert.assertEquals(context, _context);
        Assert.assertNull(manager.getLastUsedContext());

        // Remove the last one and make sure they are all gone
        manager.remove(context.getUri().toString());
        Assert.assertEquals(0, manager.getAllServerContexts().size());
        _context = manager.get(context.getUri().toString());
        Assert.assertNull(_context);
        Assert.assertNull(manager.getLastUsedContext());
    }

    @Test
    public void activeTfsContext() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertNull(manager.getLastUsedContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").build();
        manager.add(context);
        Assert.assertEquals(context, manager.getLastUsedContext());

        manager.clearLastUsedContext();
        Assert.assertNull(manager.getLastUsedContext());
    }

    @Test
    public void activeVsoContext() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertNull(manager.getLastUsedContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.VSO).uri("http://server/path").build();
        manager.add(context);
        Assert.assertEquals(context, manager.getLastUsedContext());

        manager.clearLastUsedContext();
        Assert.assertNull(manager.getLastUsedContext());

        ServerContext context2 = new ServerContextBuilder().type(ServerContext.Type.VSO_DEPLOYMENT).build();
        try {
            manager.add(context2);
        } catch (AssertionError ex) { /* correct */ }
    }

    @Test
    public void getServerContext() {
        ServerContextManager manager = new ServerContextManager();
        String uri = "http://server/path";
        Assert.assertNull(manager.getLastUsedContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri(uri).build();
        manager.add(context);

        ServerContext testContext = manager.get(uri);
        Assert.assertNotNull(testContext);
        Assert.assertEquals(uri, testContext.getUri().toString().toLowerCase());

        Collection<ServerContext> contexts = manager.getAllServerContexts();
        Assert.assertEquals(1, contexts.size());
        Assert.assertEquals(uri, contexts.iterator().next().getUri().toString().toLowerCase());
    }

    @Test
    public void clearServerContext() {
        ServerContextManager manager = new ServerContextManager();
        String uri = "http://server/path";
        Assert.assertNull(manager.getLastUsedContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri(uri).build();
        manager.add(context);

        manager.remove(uri);
        Assert.assertNull(manager.getLastUsedContext());
        ServerContext testContext = manager.get(uri);
        Assert.assertNull(testContext);
    }

    /**
     * This test avoids the problems with authenticating by asking for the context
     * that is already the active context.
     */
    @Test
    public void getAuthenticatedContext_simplest() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertNull(manager.getLastUsedContext());

        URI gitUri = URI.create("http://server/_git/repo1");
        AuthenticationInfo info = new AuthenticationInfo("", "", "", "");
        TeamProjectCollectionReference collection = new TeamProjectCollectionReference();
        TeamProjectReference project = new TeamProjectReference();
        GitRepository repo = new GitRepository();
        repo.setRemoteUrl(gitUri.toString());
        ServerContext context = new ServerContext(ServerContext.Type.TFS, info, UUID.randomUUID(), gitUri, gitUri, null, collection, project, repo);
        manager.add(context);

        ServerContext testContext = manager.getAuthenticatedContext(gitUri.toString(), true);
        Assert.assertNotNull(testContext);
        Assert.assertEquals(gitUri, testContext.getUri());
    }
}
