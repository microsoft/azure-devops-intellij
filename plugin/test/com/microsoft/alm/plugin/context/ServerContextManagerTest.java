// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.core.webapi.CoreHttpClient;
import com.microsoft.alm.core.webapi.model.TeamProjectCollection;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.context.rest.ConnectionData;
import com.microsoft.alm.plugin.context.rest.LocationServiceData;
import com.microsoft.alm.plugin.context.rest.ServiceDefinition;
import com.microsoft.alm.plugin.context.rest.VstsHttpClient;
import com.microsoft.alm.plugin.context.rest.VstsInfo;
import com.microsoft.alm.plugin.context.rest.VstsUserInfo;
import com.microsoft.alm.plugin.exceptions.TeamServicesException;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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

        // Make sure calling remove with null doesn't throw
        manager.remove(null);
    }

    @Test
    public void testValidateServerConnection_VSTS() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertNull(manager.getLastUsedContext());

        ConnectionData connectionData = new ConnectionData();
        connectionData.setAuthenticatedUser(new VstsUserInfo());
        connectionData.setAuthorizedUser(new VstsUserInfo());
        connectionData.setInstanceId(UUID.randomUUID());
        connectionData.setLocationServiceData(new LocationServiceData());
        ServiceDefinition definition = new ServiceDefinition();
        definition.setServiceType("distributedtask");
        connectionData.getLocationServiceData().setServiceDefinitions(new ServiceDefinition[]{definition});

        try (MockedStatic<VstsHttpClient> vstsHttpClient = Mockito.mockStatic(VstsHttpClient.class)) {
            vstsHttpClient.when(
                            () -> VstsHttpClient.sendRequest(any(Client.class), anyString(), eq(ConnectionData.class)))
                    .thenReturn(connectionData);

            final Client client = Mockito.mock(Client.class);
            ServerContext context = new ServerContextBuilder().type(ServerContext.Type.VSO).uri("https://server.visualstudio.com").buildWithClient(client);

            // Make sure it doesn't throw for a 2015 server
            manager.validateServerConnection(context);
        }
    }

    @Test
    public void testValidateServerConnection_2015Server() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertNull(manager.getLastUsedContext());

        ConnectionData connectionData = new ConnectionData();
        connectionData.setAuthenticatedUser(new VstsUserInfo());
        connectionData.setAuthorizedUser(new VstsUserInfo());
        connectionData.setInstanceId(UUID.randomUUID());
        connectionData.setLocationServiceData(new LocationServiceData());
        ServiceDefinition definition = new ServiceDefinition();
        definition.setServiceType("distributedtask");
        connectionData.getLocationServiceData().setServiceDefinitions(new ServiceDefinition[]{definition});

        try (MockedStatic<VstsHttpClient> vstsHttpClient = Mockito.mockStatic(VstsHttpClient.class)) {
            vstsHttpClient.when(
                            () -> VstsHttpClient.sendRequest(any(Client.class), anyString(), eq(ConnectionData.class)))
                    .thenReturn(connectionData);

            final Client client = Mockito.mock(Client.class);
            ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").buildWithClient(client);

            // Make sure it doesn't throw for a 2015 server
            manager.validateServerConnection(context);
        }
    }

    @Test
    public void testValidateServerConnection_2013Server() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertNull(manager.getLastUsedContext());

        ConnectionData connectionData = new ConnectionData();
        connectionData.setAuthenticatedUser(new VstsUserInfo());
        connectionData.setAuthorizedUser(new VstsUserInfo());
        connectionData.setInstanceId(UUID.randomUUID());
        connectionData.setLocationServiceData(new LocationServiceData());
        ServiceDefinition definition = new ServiceDefinition();
        definition.setServiceType("doesntExist");
        connectionData.getLocationServiceData().setServiceDefinitions(new ServiceDefinition[]{definition});

        try (MockedStatic<VstsHttpClient> vstsHttpClient = Mockito.mockStatic(VstsHttpClient.class)) {
            vstsHttpClient.when(
                            () -> VstsHttpClient.sendRequest(any(Client.class), anyString(), eq(ConnectionData.class)))
                    .thenReturn(connectionData);

            final Client client = Mockito.mock(Client.class);
            ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").buildWithClient(client);

            // Make sure we get unsupported version
            try {
                manager.validateServerConnection(context);
                Assert.fail("should not get here");
            } catch (TeamServicesException ex) {
                Assert.assertEquals(TeamServicesException.KEY_TFS_UNSUPPORTED_VERSION, ex.getMessage());
            }
        }
    }

    @Test
    public void testValidateServerConnection_404() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertNull(manager.getLastUsedContext());

        try (MockedStatic<VstsHttpClient> vstsHttpClient = Mockito.mockStatic(VstsHttpClient.class)) {
            vstsHttpClient.when(
                    () -> VstsHttpClient.sendRequest(any(Client.class), anyString(), eq(ConnectionData.class)))
                    .thenThrow(new VstsHttpClient.VstsHttpClientException(404, "message", null));

            final Client client = Mockito.mock(Client.class);
            ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").buildWithClient(client);

            // Make sure we get unsupported version
            try {
                manager.validateServerConnection(context);
                Assert.fail("should not get here");
            } catch (TeamServicesException ex) {
                Assert.assertEquals(TeamServicesException.KEY_TFS_UNSUPPORTED_VERSION, ex.getMessage());
            }
        }
    }

    @Test
    public void testValidateServerConnection_justVSTSRemoteURL() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertNull(manager.getLastUsedContext());

        ConnectionData connectionData = new ConnectionData();
        connectionData.setAuthenticatedUser(new VstsUserInfo());
        connectionData.setAuthorizedUser(new VstsUserInfo());
        connectionData.setInstanceId(UUID.randomUUID());
        connectionData.setLocationServiceData(new LocationServiceData());
        ServiceDefinition definition = new ServiceDefinition();
        definition.setServiceType("distributedtask");
        connectionData.getLocationServiceData().setServiceDefinitions(new ServiceDefinition[]{definition});

        try (MockedStatic<VstsHttpClient> vstsHttpClient = Mockito.mockStatic(VstsHttpClient.class)) {
            vstsHttpClient.when(
                    () -> VstsHttpClient.sendRequest(any(Client.class), anyString(), eq(ConnectionData.class)))
                    .thenReturn(connectionData);

            final TeamProjectCollection collection = new TeamProjectCollection();
            collection.setName("coll1");
            collection.setId(UUID.randomUUID());
            collection.setUrl("https://server.visualstudio.com/coll1");
            final GitRepository repo = new GitRepository();
            repo.setName("repo1");
            final Client client = Mockito.mock(Client.class);
            ServerContext context = new ServerContextBuilder().type(ServerContext.Type.VSO).uri("https://server.visualstudio.com/project/_git/repo").buildWithClient(client);
            final GitHttpClient gitHttpClient = Mockito.mock(GitHttpClient.class);
            when(gitHttpClient.getRepository(anyString(), anyString())).thenReturn(repo);
            final CoreHttpClient coreHttpClient = Mockito.mock(CoreHttpClient.class);
            when(coreHttpClient.getProjectCollection(anyString())).thenReturn(collection);
            final MyValidator myValidator = new MyValidator(context, gitHttpClient, coreHttpClient, collection);


            // test the code path when we just provide the remote URL
            manager.validateServerConnection(context, myValidator);
        }
    }

    @Test
    public void testValidateServerConnection_justTFSRemoteURL() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertNull(manager.getLastUsedContext());

        ConnectionData connectionData = new ConnectionData();
        connectionData.setAuthenticatedUser(new VstsUserInfo());
        connectionData.setAuthorizedUser(new VstsUserInfo());
        connectionData.setInstanceId(UUID.randomUUID());
        connectionData.setLocationServiceData(new LocationServiceData());
        ServiceDefinition definition = new ServiceDefinition();
        definition.setServiceType("distributedtask");
        connectionData.getLocationServiceData().setServiceDefinitions(new ServiceDefinition[]{definition});

        try (MockedStatic<VstsHttpClient> vstsHttpClient = Mockito.mockStatic(VstsHttpClient.class)) {
            vstsHttpClient.when(
                            () -> VstsHttpClient.sendRequest(any(Client.class), anyString(), eq(ConnectionData.class)))
                    .thenReturn(connectionData);

            final TeamProjectCollection collection = new TeamProjectCollection();
            collection.setName("coll1");
            collection.setId(UUID.randomUUID());
            collection.setUrl("https://server:8080/coll1");
            final GitRepository repo = new GitRepository();
            repo.setName("repo1");
            final Client client = Mockito.mock(Client.class);
            ServerContext context = new ServerContextBuilder().type(ServerContext.Type.VSO).uri("https://server:8080/project/_git/repo").buildWithClient(client);
            final GitHttpClient gitHttpClient = Mockito.mock(GitHttpClient.class);
            when(gitHttpClient.getRepository(anyString(), anyString())).thenReturn(repo);
            final CoreHttpClient coreHttpClient = Mockito.mock(CoreHttpClient.class);
            final MyValidator myValidator = new MyValidator(context, gitHttpClient, coreHttpClient, collection);


            // test the code path when we just provide the remote URL
            manager.validateServerConnection(context, myValidator);
        }
    }

    @Test
    public void testValidateGitUrl() {
        final Client client = Mockito.mock(Client.class);
        final ServerContext context = new ServerContextBuilder().type(ServerContext.Type.VSO).uri("https://dev.azure.com/username").buildWithClient(client);
        final ServerContextManager.Validator validator = new ServerContextManager.Validator(context);

        // We should test that the actual API URL used for VstsInto retrieval doesn't contain a user name; otherwise the
        // HTTP client will use wrong credentials.
        final String serverUrl = "https://dev.azure.com/username";
        final String repositoryUrl = "https://username@dev.azure.com/username/projectname/_git/repositoryname";
        final String repositoryInfoApiUrl = "https://dev.azure.com/username/projectname/_git/repositoryname/vsts/info";

        final GitRepository repository = new GitRepository();
        repository.setRemoteUrl(repositoryUrl);
        repository.setProjectReference(new TeamProjectReference());
        final VstsInfo vstsInfo = new VstsInfo();
        vstsInfo.setServerUrl(serverUrl);
        vstsInfo.setCollectionReference(new TeamProjectCollectionReference());
        vstsInfo.setRepository(repository);

        try (MockedStatic<VstsHttpClient> vstsHttpClient = Mockito.mockStatic(VstsHttpClient.class)) {
            vstsHttpClient.when(() -> VstsHttpClient.sendRequest(client, repositoryInfoApiUrl, VstsInfo.class)).thenReturn(vstsInfo);

            Assert.assertTrue(validator.validateGitUrl(repositoryUrl));
            Assert.assertEquals("https://dev.azure.com/username", validator.getServerUrl());
        }
    }

    @Test
    public void activeTfsContext() {
        ServerContextManager manager = new ServerContextManager();
        Assert.assertNull(manager.getLastUsedContext());
        ServerContext context = new ServerContextBuilder().type(ServerContext.Type.TFS).uri("http://server/path").build();
        manager.add(context);
        Assert.assertEquals(context, manager.getLastUsedContext());
        Assert.assertTrue(manager.lastUsedContextIsTFS());
        Assert.assertFalse(manager.lastUsedContextIsEmpty());

        manager.clearLastUsedContext();
        Assert.assertNull(manager.getLastUsedContext());
        Assert.assertTrue(manager.lastUsedContextIsEmpty());
        Assert.assertFalse(manager.lastUsedContextIsTFS());
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

    @Test
    public void testUpdateAuthenticationInfo() {
        String serverURL1 = "http://server:8080/project";
        String serverURL2 = "http://server2:8080/project";
        String serverURL3 = "http://server:8080/project/_git/repo";

        AuthenticationInfo authInfo = new AuthenticationInfo("user", "pass", serverURL1, "user");
        try (MockedStatic<AuthHelper> authHelper = Mockito.mockStatic(AuthHelper.class)) {
            authHelper.when(
                    () -> AuthHelper.getAuthenticationInfoSynchronously(any(AuthenticationProvider.class), anyString()))
                    .thenReturn(authInfo);

            ServerContextManager manager = new ServerContextManager();
            AuthenticationInfo authInfo1 = new AuthenticationInfo("user1", "pass1", serverURL1, "user1");
            ServerContext context1 = new ServerContext(ServerContext.Type.TFS, authInfo1, UUID.randomUUID(), URI.create(serverURL1), URI.create(serverURL1), null, null, null, null);
            AuthenticationInfo authInfo2 = new AuthenticationInfo("user2", "pass2", serverURL2, "user2");
            ServerContext context2 = new ServerContext(ServerContext.Type.TFS, authInfo2, UUID.randomUUID(), URI.create(serverURL2), URI.create(serverURL2), null, null, null, null);
            ServerContext context3 = new ServerContext(ServerContext.Type.TFS, authInfo1, UUID.randomUUID(), URI.create(serverURL3), URI.create(serverURL1), null, null, null, null);

            manager.add(context1);
            manager.add(context2);
            manager.add(context3);
            Assert.assertEquals(3, manager.getAllServerContexts().size());

            manager.updateAuthenticationInfo(serverURL1);
            Assert.assertEquals(3, manager.getAllServerContexts().size());
            Assert.assertEquals(authInfo, manager.get(serverURL1).getAuthenticationInfo());
            Assert.assertNotEquals(authInfo, manager.get(serverURL2).getAuthenticationInfo());
            Assert.assertEquals(authInfo, manager.get(serverURL3).getAuthenticationInfo());
        }
    }

    private class MyValidator extends ServerContextManager.Validator {

        final GitHttpClient gitHttpClient;
        final CoreHttpClient coreHttpClient;
        final TeamProjectCollection collection;

        public MyValidator(final ServerContext context, final GitHttpClient gitHttpClient, final CoreHttpClient coreHttpClient, final TeamProjectCollection collection) {
            super(context);
            this.gitHttpClient = gitHttpClient;
            this.coreHttpClient = coreHttpClient;
            this.collection = collection;
        }

        @Override
        protected GitHttpClient getGitHttpClient(Client jaxrsClient, URI baseUrl) {
            return gitHttpClient;
        }

        @Override
        protected CoreHttpClient getCoreHttpClient(Client jaxrsClient, URI baseUrl) {
            return coreHttpClient;
        }

        @Override
        protected TeamProjectCollection getCollectionFromServer(ServerContext context, String collectionName) {
            return collection;
        }
    }
}
