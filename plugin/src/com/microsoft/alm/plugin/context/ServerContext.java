// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.soap.SoapServices;
import com.microsoft.alm.plugin.context.soap.SoapServicesImpl;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.teamfoundation.workitemtracking.webapi.WorkItemTrackingHttpClient;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.client.spi.ConnectorProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * This class holds all information needed to contact a TFS/VSO server except for
 * authentication details. Those must be provided to certain methods as needed.
 */
public class ServerContext {
    public enum Type {VSO_DEPLOYMENT, VSO, TFS}

    private final Type type;
    private final AuthenticationInfo authenticationInfo;
    private final UUID userId;
    private final URI uri;
    private final URI serverUri;

    // lazily initialized
    private CloseableHttpClient httpClient;
    private Client client;
    private SoapServices soapServices;

    private final TeamProjectCollectionReference teamProjectCollectionReference;
    private final TeamProjectReference teamProjectReference;
    private final GitRepository gitRepository;

    private boolean disposed = false;

    /**
     * Use this static method to convert a server URI into an appropriate unique key for a serverContext object
     */
    public static String getKey(URI uri) {
        // Ignore case, scheme, and any fragments or queries
        final String key;
        if (uri != null) {
            key = uri.getSchemeSpecificPart().toLowerCase();
        } else {
            key = "";
        }

        return key;
    }

    /**
     * Use this static method to convert a server uri string into an appropriate unique key for a serverContext object
     */
    public static String getKey(String uri) {
        assert uri != null;
        return getKey(UrlHelper.createUri(uri));
    }

    /**
     * Use ServerContextBuilder to build a context. Only tests should call this constructor.
     */
    protected ServerContext(final Type type, final AuthenticationInfo authenticationInfo, final UUID userId, final URI uri,
                            final URI serverUri, final Client client, final TeamProjectCollectionReference teamProjectCollectionReference,
                            final TeamProjectReference teamProjectReference,
                            final GitRepository gitRepository) {
        assert type != null;

        this.type = type;
        this.authenticationInfo = authenticationInfo;
        this.userId = userId;
        this.uri = uri;
        this.serverUri = serverUri;
        this.client = client;
        this.teamProjectCollectionReference = teamProjectCollectionReference;
        this.teamProjectReference = teamProjectReference;
        this.gitRepository = gitRepository;
    }

    public String getKey() {
        return getKey(uri);
    }

    public URI getUri() {
        return uri;
    }

    public URI getServerUri() {
        return serverUri;
    }

    // The url string obtained from the REST SDK is not encoded.
    // Replace space which is the only known valid character in team project and repository name that is not a valid character in URI
    public String getUsableGitUrl() {
        GitRepository repo = getGitRepository();
        if (repo != null && repo.getRemoteUrl() != null) {
            return UrlHelper.getCmdLineFriendlyUrl(this.getGitRepository().getRemoteUrl());
        }

        return null;
    }

    public AuthenticationInfo getAuthenticationInfo() {
        return authenticationInfo;
    }

    public UUID getUserId() {
        return userId;
    }

    public Type getType() {
        return type;
    }

    public synchronized boolean hasClient() {
        return client != null;
    }

    public synchronized Client getClient() {
        if (!hasClient()) {
            client = getClient(getType(), getAuthenticationInfo());
        }
        return client;
    }

    public static Client getClient(final Type type, final AuthenticationInfo authenticationInfo) {
        final ClientConfig clientConfig = getClientConfig(type, authenticationInfo,
                System.getProperty("proxySet") != null && System.getProperty("proxySet").equals("true"));
        final Client localClient = ClientBuilder.newClient(clientConfig);
        return localClient;
    }

    protected static ClientConfig getClientConfig(final Type type, final AuthenticationInfo authenticationInfo, final boolean includeProxySettings) {
        final Credentials credentials = AuthHelper.getCredentials(type, authenticationInfo);

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);

        final ConnectorProvider connectorProvider = new ApacheConnectorProvider();

        final ClientConfig clientConfig = new ClientConfig().connectorProvider(connectorProvider);
        clientConfig.property(ApacheClientProperties.CREDENTIALS_PROVIDER, credentialsProvider);

        clientConfig.property(ApacheClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION, true);
        clientConfig.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);

        //Define fiddler as a local HTTP proxy
        if (includeProxySettings) {
            final String proxyHost;
            if (System.getProperty("proxyHost") != null) {
                proxyHost = System.getProperty("proxyHost");
            } else {
                proxyHost = "127.0.0.1";
            }

            final String proxyPort;
            if (System.getProperty("proxyPort") != null) {
                proxyPort = System.getProperty("proxyPort");
            } else {
                proxyPort = "8888";
            }

            final String proxyUrl = String.format("http://%s:%s", proxyHost, proxyPort);

            clientConfig.property(ClientProperties.PROXY_URI, proxyUrl);
            clientConfig.property(ApacheClientProperties.SSL_CONFIG, getSslConfigurator());
        }
        return clientConfig;
    }

    private static SslConfigurator getSslConfigurator() {
        final String trustStore;
        if (System.getProperty("javax.net.ssl.trustStore") != null) {
            trustStore = System.getProperty("javax.net.ssl.trustStore");
        } else {
            trustStore = "C:" + File.separator + "FiddlerKeystore.jks";
        }

        final String trustStorePassword;
        if (System.getProperty("javax.net.ssl.trustStorePassword") != null) {
            trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        } else {
            trustStorePassword = "TfIntelliJPlugin";
        }

        final SslConfigurator sslConfigurator = SslConfigurator.newInstance()
                .trustStoreFile(trustStore)
                .trustStorePassword(trustStorePassword)
                .trustStoreType("JKS")
                .trustManagerFactoryAlgorithm("PKIX")
                .securityProtocol("SSL");

        return sslConfigurator;
    }

    public synchronized HttpClient getHttpClient() {
        checkDisposed();
        if (httpClient == null && authenticationInfo != null) {
            final Credentials credentials = AuthHelper.getCredentials(type, authenticationInfo);
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);
            httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
        }
        return httpClient;
    }

    public synchronized GitHttpClient getGitHttpClient() {
        final URI collectionUri = getCollectionURI();
        if (collectionUri != null) {
            final GitHttpClient gitClient = new GitHttpClient(getClient(), collectionUri);
            return gitClient;
        }

        // We don't have enough context to create a GitHttpClient
        return null;
    }

    public synchronized WorkItemTrackingHttpClient getWitHttpClient() {
        final URI collectionUri = getCollectionURI();
        if (collectionUri != null) {
            final WorkItemTrackingHttpClient witClient = new WorkItemTrackingHttpClient(getClient(), collectionUri);
            return witClient;
        }

        // We don't have enough context to create a WorkItemTrackingHttpClient
        return null;
    }

    public synchronized SoapServices getSoapServices() {
        checkDisposed();
        if (soapServices == null) {
            soapServices = new SoapServicesImpl(this);
        }
        return soapServices;
    }

    public TeamProjectCollectionReference getTeamProjectCollectionReference() {
        return teamProjectCollectionReference;
    }

    public TeamProjectReference getTeamProjectReference() {
        return teamProjectReference;
    }

    public GitRepository getGitRepository() {
        return gitRepository;
    }

    public URI getCollectionURI() {
        if (teamProjectCollectionReference != null) {
            final URI collectionURI = UrlHelper.getCollectionURI(serverUri, teamProjectCollectionReference.getName());
            return collectionURI;
        }

        //We don't have enough context to create collection URL
        return null;
    }

    public URI getTeamProjectURI() {
        if (teamProjectCollectionReference != null && teamProjectReference != null) {
            final URI teamProjectURI = UrlHelper.getTeamProjectURI(serverUri, teamProjectCollectionReference.getName(), teamProjectReference.getName());
            return teamProjectURI;
        }

        //We don't have enough context to create project URL
        return null;
    }

    private void checkDisposed() {
        if (isDisposed()) {
            throw new RuntimeException(this.getClass().getName() + " disposed.");
        }
    }

    public synchronized boolean isDisposed() {
        return disposed;
    }

    public synchronized void dispose() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                // eat it
            }
            httpClient = null;
        }

        if (client != null) {
            client.close();
            client = null;
        }

        disposed = true;
    }
}
