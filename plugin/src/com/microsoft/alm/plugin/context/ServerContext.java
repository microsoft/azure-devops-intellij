// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.build.webapi.BuildHttpClient;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.rest.GitHttpClientEx;
import com.microsoft.alm.plugin.context.rest.TfvcHttpClientEx;
import com.microsoft.alm.plugin.context.soap.SoapServices;
import com.microsoft.alm.plugin.context.soap.SoapServicesImpl;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.alm.workitemtracking.webapi.WorkItemTrackingHttpClient;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.ws.rs.client.Client;
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
            client = RestClientHelper.getClient(getType(), getAuthenticationInfo());
        }
        return client;
    }

    public synchronized HttpClient getHttpClient() {
        checkDisposed();
        if (httpClient == null && authenticationInfo != null) {
            final Credentials credentials = AuthHelper.getCredentials(type, authenticationInfo);
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);
            final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                    .setSSLContext(PluginServiceProvider.getInstance().getCertificateService().getSSLContext());

            httpClient = httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider).build();
        }
        return httpClient;
    }

    public synchronized GitHttpClientEx getGitHttpClient() {
        final URI collectionUri = getCollectionURI();
        if (collectionUri != null) {
            final GitHttpClientEx gitClient = new GitHttpClientEx(getClient(), collectionUri);
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

    public synchronized TfvcHttpClientEx getTfvcHttpClient() {
        final URI collectionUri = getCollectionURI();
        if (collectionUri != null) {
            final TfvcHttpClientEx tfvcHttpClient = new TfvcHttpClientEx(getClient(), collectionUri);
            return tfvcHttpClient;
        }

        // We don't have enough context to create a TfvcHttpClient
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
