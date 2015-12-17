// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.visualstudio.services.account.webapi.model.Account;

import javax.ws.rs.client.Client;
import java.net.URI;

public class ServerContextBuilder {
    private ServerContext.Type type;
    private AuthenticationInfo authenticationInfo;
    private URI uri;
    private TeamProjectCollectionReference teamProjectCollectionReference;
    private TeamProjectReference teamProjectReference;
    private GitRepository gitRepository;
    private Client client;

    public ServerContextBuilder() {
    }

    public ServerContextBuilder(final ServerContext originalContext) {
        assert originalContext != null;

        this.type = originalContext.getType();
        this.uri = originalContext.getUri();
        this.teamProjectCollectionReference = originalContext.getTeamProjectCollectionReference();
        this.teamProject(originalContext.getTeamProjectReference());
        this.gitRepository = originalContext.getGitRepository();
        this.authenticationInfo = originalContext.getAuthenticationInfo();
        this.client = originalContext.hasClient() ? originalContext.getClient() : null;
    }

    public ServerContextBuilder type(final ServerContext.Type newType) {
        this.type = newType;
        return this;
    }

    public ServerContextBuilder uri(final URI newUri) {
        this.uri = newUri;
        return this;
    }

    public ServerContextBuilder uri(final String newUri) {
        this.uri = UrlHelper.createUri(newUri);
        return this;
    }

    public ServerContextBuilder accountUri(final Account account) {
        this.uri = UrlHelper.getVSOAccountURI(account.getAccountName());
        return this;
    }

    public ServerContextBuilder authentication(final AuthenticationInfo authenticationInfo) {
        this.authenticationInfo = authenticationInfo;
        return this;
    }

    public ServerContextBuilder collection(final TeamProjectCollectionReference teamProjectCollection) {
        this.teamProjectCollectionReference = teamProjectCollection;
        return this;
    }

    public ServerContextBuilder teamProject(final TeamProjectReference teamProject) {
        this.teamProjectReference = teamProject;
        return this;
    }

    public ServerContextBuilder repository(final GitRepository repository) {
        this.gitRepository = repository;
        return this;
    }

    public ServerContext build() {
        return buildWithClient(client);
    }

    public ServerContext buildWithClient(Client client) {
        final ServerContext serverContext = new ServerContext(type, authenticationInfo, uri, client,
                teamProjectCollectionReference, teamProjectReference, gitRepository);

        return serverContext;
    }
}