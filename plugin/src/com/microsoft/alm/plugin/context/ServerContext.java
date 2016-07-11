// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.build.webapi.BuildHttpClient;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.soap.SoapServices;
import com.microsoft.alm.plugin.context.soap.SoapServicesImpl;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.alm.workitemtracking.webapi.WorkItemTrackingHttpClient;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.BasicScheme;
import org.apache.commons.httpclient.auth.NTLMScheme;
import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.commons.lang.StringUtils;
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

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
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
        ArgumentHelper.checkNotNull(uri, "uri");

        return getKey(UrlHelper.createUri(uri));
    }

    /**
     * Use ServerContextBuilder to build a context. Only tests should call this constructor.
     */
    protected ServerContext(final Type type, final AuthenticationInfo authenticationInfo, final UUID userId, final URI uri,
                            final URI serverUri, final Client client, final TeamProjectCollectionReference teamProjectCollectionReference,
                            final TeamProjectReference teamProjectReference,
                            final GitRepository gitRepository) {

        ArgumentHelper.checkNotNull(type, "type");

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
        clientConfig.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);

        // For TFS OnPrem we only support NTLM authentication right now. Since 2016 servers support Basic as well,
        // we need to let the server and client negotiate the protocol instead of preemptively assuming Basic.
        // TODO: This prevents PATs from being used OnPrem. We need to fix this soon to support PATs onPrem.
        clientConfig.property(ApacheClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION, type != Type.TFS);

        //Define a local HTTP proxy
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
        }

        // if this is a onPrem server and the uri starts with https, we need to setup ssl
        if (isSSLEnabledOnPrem(type, authenticationInfo.getServerUri())) {
            clientConfig.property(ApacheClientProperties.SSL_CONFIG, getSslConfigurator());
        }

        return clientConfig;
    }

    private static boolean isSSLEnabledOnPrem(final Type type, final String serverUri) {
        return type == Type.TFS && serverUri.toLowerCase().startsWith("https://");
    }

    private static SslConfigurator getSslConfigurator() {
        /**
         * Set up trust store and key store for the https connection.
         *
         * http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html
         * See table 6.
         */
        final SslConfigurator sslConfigurator = SslConfigurator.newInstance();

        // Trust stores are used to store CA certificates. It is used to trust the server connection.
        setupTrustStore(sslConfigurator);

        // Key stores are used to store client certificates.  It is used to authenticate the client.
        setupKeyStore(sslConfigurator);

        return sslConfigurator;
    }

    private static SslConfigurator setupTrustStore(final SslConfigurator sslConfigurator) {
        // Create trust store from .cer
        // keytool.exe  -import -trustcacerts -alias root -file cacert.cer -keystore truststore.jks
        final String trustStore = System.getProperty("javax.net.ssl.trustStore");
        final String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword", StringUtils.EMPTY);

        final String trustStoreType = System.getProperty("javax.net.ssl.trustStoreType", "JKS");
        final String trustManagerFactoryAlgorithm = System.getProperty("ssl.TrustManagerFactory.algorithm", "PKIX");

        if (trustStore != null) {
            sslConfigurator
                    .trustStoreFile(trustStore)
                    .trustStorePassword(trustStorePassword)
                    .trustStoreType(trustStoreType)
                    .trustManagerFactoryAlgorithm(trustManagerFactoryAlgorithm)
                    .securityProtocol("SSL");
        }

        return sslConfigurator;
    }

    private static SslConfigurator setupKeyStore(final SslConfigurator sslConfigurator) {
        // Create keystore from pkx:
        // keytool -importkeystore -srckeystore mycert.pfx -srcstoretype pkcs12 -destkeystore keystore.jks -deststoretype JKS
        final String keyStore = System.getProperty("javax.net.ssl.keyStore");
        final String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword", StringUtils.EMPTY);

        if (keyStore != null) {
            sslConfigurator
                    .keyStoreFile(keyStore)
                    .keyStorePassword(keyStorePassword);
        }

        return sslConfigurator;
    }

    public synchronized HttpClient getHttpClient() {
        checkDisposed();
        if (httpClient == null && authenticationInfo != null) {
            final Credentials credentials = AuthHelper.getCredentials(type, authenticationInfo);
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);
            final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

            if (isSSLEnabledOnPrem(Type.TFS, authenticationInfo.getServerUri())) {
                final SslConfigurator sslConfigurator = getSslConfigurator();
                final SSLContext sslContext = sslConfigurator.createSSLContext();

                httpClientBuilder.setSslcontext(sslContext);
            }

            httpClient = httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider).build();
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

    public synchronized BuildHttpClient getBuildHttpClient() {
        final URI collectionUri = getCollectionURI();
        if (collectionUri != null) {
            final BuildHttpClient buildClient = new BuildHttpClient(getClient(), collectionUri);
            return buildClient;
        }

        // We don't have enough context to create a BuildHttpClient
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
