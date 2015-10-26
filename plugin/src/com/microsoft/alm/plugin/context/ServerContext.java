// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationInfo;
import com.microsoft.alm.plugin.context.soap.SoapServices;
import com.microsoft.alm.plugin.context.soap.SoapServicesImpl;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.visualstudio.services.account.webapi.model.Account;
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
import java.util.UUID;

/**
 * This class holds all information needed to contact a TFS/VSO server except for
 * authentication details. Those must be provided to certain methods as needed.
 */
public class ServerContext<E extends AuthenticationInfo> {
    // constant to indicate there is no context.
    public static final ServerContext NO_CONTEXT = null;

    public enum Type {VSO_DEPLOYMENT, VSO, TFS}

    private final Type type;
    private final E authenticationInfo;
    private final URI uri;

    // used for VSO
    private final UUID accountId;

    // lazily initialized
    private volatile CloseableHttpClient httpClient;
    private volatile Client client;
    private volatile SoapServices soapServices;

    private TeamProjectCollectionReference teamProjectCollectionReference;
    private TeamProjectReference teamProjectReference;
    private GitRepository gitRepository;

    private boolean disposed = false;

    public static ServerContext<VsoAuthenticationInfo> createVSODeploymentContext(final Account account, final VsoAuthenticationInfo authenticationInfo) {
        assert account != null;
        assert authenticationInfo != null;
        return createVSODeploymentContext(UrlHelper.getVSOAccountURI(account.getAccountName()), account.getAccountId(), authenticationInfo);
    }

    public static ServerContext<VsoAuthenticationInfo> createVSODeploymentContext(final URI accountUri, final UUID accountId, final VsoAuthenticationInfo authenticationInfo) {
        assert accountId != null;
        assert authenticationInfo != null;
        return new ServerContext<VsoAuthenticationInfo>(Type.VSO_DEPLOYMENT, authenticationInfo, accountUri, accountId);
    }

    public static ServerContext<VsoAuthenticationInfo> createVSOContext(final Account account, final VsoAuthenticationInfo authenticationInfo) {
        assert account != null;
        assert authenticationInfo != null;
        return createVSOContext(UrlHelper.getVSOAccountURI(account.getAccountName()), account.getAccountId(), authenticationInfo);
    }

    public static ServerContext<VsoAuthenticationInfo> createVSOContext(final URI accountUri, final UUID accountId, final VsoAuthenticationInfo authenticationInfo) {
        assert accountId != null;
        assert authenticationInfo != null;
        return new ServerContext<VsoAuthenticationInfo>(Type.VSO, authenticationInfo, accountUri, accountId);
    }

    public static ServerContext<VsoAuthenticationInfo> createVSOContext(final ServerContext context, final VsoAuthenticationInfo authenticationInfo) {
        assert context != null;
        assert authenticationInfo != null;
        assert context.getType() != Type.TFS;

        final ServerContext<VsoAuthenticationInfo> vsoContext = createVSOContext(context.getUri(), context.getAccountId(), authenticationInfo);
        vsoContext.setTeamProjectCollectionReference(context.getTeamProjectCollectionReference());
        vsoContext.setTeamProjectReference(context.getTeamProjectReference());
        vsoContext.setGitRepository(context.getGitRepository());

        return vsoContext;
    }

    public static ServerContext<TfsAuthenticationInfo> createTFSContext(final URI uri, final TfsAuthenticationInfo authenticationInfo) {
        assert uri != null;
        assert authenticationInfo != null;
        return new ServerContext<TfsAuthenticationInfo>(Type.TFS, authenticationInfo, uri, null);
    }

    /*
     *   Protected for mocking only.
     */
    protected ServerContext(final Type type, final E authenticationInfo, final URI uri, final UUID accountId) {
        this.type = type;
        this.authenticationInfo = authenticationInfo;
        this.uri = uri;
        this.accountId = accountId;
    }

    public URI getUri() {
        return uri;
    }

    public E getAuthenticationInfo() {
        return authenticationInfo;
    }

    public Type getType() {
        return type;
    }

    /**
     * Always <code>null</code> except for VSO
     *
     * @return
     */
    public UUID getAccountId() {
        return accountId;
    }

    public Client getClient() {
        Client localClient = client;
        if (localClient == null) {
            // double checked locking
            synchronized (this) {
                checkDisposed();
                localClient = client;
                if (localClient == null) {
                    if (getType() == Type.VSO_DEPLOYMENT) {
                        client = localClient = ClientBuilder.newClient();
                        client.register(new ClientRequestFilter() {
                            @Override
                            public void filter(final ClientRequestContext requestContext) throws IOException {
                                requestContext.getHeaders().putSingle("Authorization", "Bearer " + ((VsoAuthenticationInfo) authenticationInfo).getBearerToken());
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

                        client = localClient = ClientBuilder.newClient(clientConfig);
                    }
                }
            }
        }
        return localClient;
    }

    private SslConfigurator getSslConfigurator() {
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

    public HttpClient getHttpClient() {
        HttpClient localHttpClient = httpClient;
        if (localHttpClient == null) {
            // double checked locking
            synchronized (this) {
                checkDisposed();
                localHttpClient = httpClient;
                if (localHttpClient == null && authenticationInfo != null) {
                    final Credentials credentials = AuthHelper.getCredentials(type, authenticationInfo);
                    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY, credentials);
                    localHttpClient = httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
                }
            }
        }
        return localHttpClient;
    }

    public SoapServices getSoapServices() {
        SoapServices localSoapServices = soapServices;
        if (localSoapServices == null) {
            // double checked locking
            synchronized (this) {
                checkDisposed();
                localSoapServices = soapServices;
                if (localSoapServices == null) {
                    soapServices = localSoapServices = new SoapServicesImpl(this);
                }
            }
        }
        return localSoapServices;
    }

    public TeamProjectCollectionReference getTeamProjectCollectionReference() {
        return teamProjectCollectionReference;
    }

    public void setTeamProjectCollectionReference(final TeamProjectCollectionReference teamProjectCollectionReference) {
        this.teamProjectCollectionReference = teamProjectCollectionReference;
    }

    public TeamProjectReference getTeamProjectReference() {
        return teamProjectReference;
    }

    public void setTeamProjectReference(final TeamProjectReference teamProjectReference) {
        this.teamProjectReference = teamProjectReference;
    }

    public GitRepository getGitRepository() {
        return gitRepository;
    }

    public void setGitRepository(final GitRepository gitRepository) {
        this.gitRepository = gitRepository;
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
