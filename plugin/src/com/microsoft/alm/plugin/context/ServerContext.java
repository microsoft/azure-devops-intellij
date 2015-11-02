// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.soap.SoapServices;
import com.microsoft.alm.plugin.context.soap.SoapServicesImpl;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
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
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * This class holds all information needed to contact a TFS/VSO server except for
 * authentication details. Those must be provided to certain methods as needed.
 */
public class ServerContext {
    // constant to indicate there is no context.
    public static final ServerContext NO_CONTEXT = null;

    public enum Type {VSO_DEPLOYMENT, VSO, TFS}

    private final Type type;
    private final AuthenticationInfo authenticationInfo;
    private final URI uri;

    // lazily initialized
    private CloseableHttpClient httpClient;
    private Client client;
    private SoapServices soapServices;

    private final TeamProjectCollectionReference teamProjectCollectionReference;
    private final TeamProjectReference teamProjectReference;
    private final GitRepository gitRepository;

    private boolean disposed = false;

    /**
     * Use ServerContextBuilder to build a context. Only tests should call this constructor.
     */
    protected ServerContext(final Type type, final AuthenticationInfo authenticationInfo, final URI uri,
                            final Client client, final TeamProjectCollectionReference teamProjectCollectionReference,
                            final TeamProjectReference teamProjectReference,
                            final GitRepository gitRepository) {
        this.type = type;
        this.authenticationInfo = authenticationInfo;
        this.uri = uri;
        this.client = client;
        this.teamProjectCollectionReference = teamProjectCollectionReference;
        this.teamProjectReference = teamProjectReference;
        this.gitRepository = gitRepository;
    }

    public URI getUri() {
        return uri;
    }

    public AuthenticationInfo getAuthenticationInfo() {
        return authenticationInfo;
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
        final Client localClient;
        if (type == Type.VSO_DEPLOYMENT) {
            localClient = ClientBuilder.newClient();
            localClient.register(new ClientRequestFilter() {
                @Override
                public void filter(final ClientRequestContext requestContext) throws IOException {
                    requestContext.getHeaders().putSingle("Authorization", "Bearer " + authenticationInfo.getPassword());
                }
            });
        } else {
            final Credentials credentials = AuthHelper.getCredentials(type, authenticationInfo);

            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);

            final ConnectorProvider connectorProvider = new ApacheConnectorProvider();

            final ClientConfig clientConfig = new ClientConfig().connectorProvider(connectorProvider);
            clientConfig.property(ApacheClientProperties.CREDENTIALS_PROVIDER, credentialsProvider);

            clientConfig.property(ApacheClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION, true);
            clientConfig.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);

            //Define fiddler as a local HTTP proxy
            if (System.getProperty("proxySet") != null && System.getProperty("proxySet").equals("true")) {
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

            localClient = ClientBuilder.newClient(clientConfig);
        }

        return localClient;
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
