// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.soap.SoapServices;
import com.microsoft.alm.plugin.context.soap.SoapServicesImpl;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Map;

public class ServerContextTest extends AbstractTest {
    @Test
    public void constructor() {
        ServerContext context = new ServerContext(ServerContext.Type.TFS, null, null, null, null, null, null, null, null);
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
        ServerContext context2 = new ServerContext(ServerContext.Type.TFS, info, null, serverUri, serverUri, null, collection, project, repo);
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
        ServerContext context = new ServerContext(ServerContext.Type.TFS, null, null, null, null, null, null, null, null);
        Assert.assertFalse(context.isDisposed());

        // make sure checkDisposed does not throw here
        context.getSoapServices();

        // Now dispose and test when disposed
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

    @Test
    public void clientMethods() {
        ServerContext context = new ServerContext(ServerContext.Type.TFS, null, null, null, null, null, null, null, null);
        // getHttpClient should return null because there is no AuthInfo
        final HttpClient httpClient = context.getHttpClient();
        Assert.assertNull(httpClient);

        // getSoapServices should return a new SoapServicesImpl
        final SoapServices ss = context.getSoapServices();
        Assert.assertNotNull(ss);
        Assert.assertTrue(ss instanceof SoapServicesImpl);
    }

    @Test
    public void getClientConfig() {
        AuthenticationInfo info = new AuthenticationInfo("user1", "pass", "server1", "4display");
        final ClientConfig config = ServerContext.getClientConfig(ServerContext.Type.TFS, info, false);

        final Map<String, Object> properties = config.getProperties();
        Assert.assertEquals(3, properties.size());

        Assert.assertEquals(true, properties.get(ApacheClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION));
        Assert.assertEquals(RequestEntityProcessing.BUFFERED, properties.get(ClientProperties.REQUEST_ENTITY_PROCESSING));

        final CredentialsProvider cp = (CredentialsProvider) properties.get(ApacheClientProperties.CREDENTIALS_PROVIDER);
        final Credentials credentials = cp.getCredentials(AuthScope.ANY);
        Assert.assertEquals(info.getPassword(), credentials.getPassword());
        Assert.assertEquals(info.getUserName(), credentials.getUserPrincipal().getName());

        // Make sure Fiddler properties get set if property is on
        final ClientConfig config2 = ServerContext.getClientConfig(ServerContext.Type.TFS, info, true);
        final Map<String, Object> properties2 = config2.getProperties();
        //proxy setting doesn't automatically mean we need to setup ssl trust store anymore
        Assert.assertEquals(4, properties2.size());
        Assert.assertNotNull(properties2.get(ClientProperties.PROXY_URI));
        Assert.assertNull(properties2.get(ApacheClientProperties.SSL_CONFIG));

        info = new AuthenticationInfo("users1", "pass", "https://tfsonprem.test", "4display");
        final ClientConfig config3 = ServerContext.getClientConfig(ServerContext.Type.TFS, info, false);
        final Map<String, Object> properties3 = config3.getProperties();
        Assert.assertEquals(4, properties3.size());
        Assert.assertNull(properties3.get(ClientProperties.PROXY_URI));
        Assert.assertNotNull(properties3.get(ApacheClientProperties.SSL_CONFIG));
    }

}
