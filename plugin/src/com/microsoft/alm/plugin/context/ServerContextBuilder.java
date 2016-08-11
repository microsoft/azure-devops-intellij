// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.visualstudio.services.account.Account;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.UUID;

public class ServerContextBuilder {
    private final static Logger logger = LoggerFactory.getLogger(ServerContextBuilder.class);

    private ServerContext.Type type;
    private AuthenticationInfo authenticationInfo;
    private UUID userId;
    private URI uri;
    private URI serverUri;
    private TeamProjectCollectionReference teamProjectCollectionReference;
    private TeamProjectReference teamProjectReference;
    private GitRepository gitRepository;
    private Client client;

    public ServerContextBuilder() {
    }

    public ServerContextBuilder(final ServerContext originalContext) {
        ArgumentHelper.checkNotNull(originalContext, "originalContext");

        this.type = originalContext.getType();
        this.uri = originalContext.getUri();
        this.serverUri = originalContext.getServerUri();
        this.teamProjectCollectionReference = originalContext.getTeamProjectCollectionReference();
        this.teamProject(originalContext.getTeamProjectReference());
        this.gitRepository = originalContext.getGitRepository();
        this.authenticationInfo = originalContext.getAuthenticationInfo();
        this.userId = originalContext.getUserId();
        this.client = originalContext.hasClient() ? originalContext.getClient() : null;
    }

    public ServerContextBuilder type(final ServerContext.Type newType) {
        this.type = newType;
        return this;
    }

    public ServerContextBuilder uri(final URI newUri) {
        this.uri = newUri;
        if (!UrlHelper.isGitRemoteUrl(newUri.toString())) {
            this.serverUri = this.uri;
        }
        return this;
    }

    public ServerContextBuilder uri(final String newUri) {
        this.uri = UrlHelper.createUri(newUri);
        if (!UrlHelper.isGitRemoteUrl(newUri)) {
            this.serverUri = this.uri;
        }
        return this;
    }

    public ServerContextBuilder serverUri(final String serverUri) {
        if (!StringUtils.isEmpty(serverUri)) {
            this.serverUri = UrlHelper.createUri(serverUri);
        }
        return this;
    }

    public ServerContextBuilder serverUri(final URI serverUri) {
        this.serverUri = serverUri;
        return this;
    }

    public ServerContextBuilder accountUri(final Account account) {
        this.uri = UrlHelper.getVSOAccountURI(account.getAccountName());
        this.serverUri = this.uri;
        return this;
    }

    public ServerContextBuilder authentication(final AuthenticationInfo authenticationInfo) {
        this.authenticationInfo = authenticationInfo;
        this.client = null; //clear the client when setting new authentication info so cached client with old credentials is not used
        return this;
    }

    public ServerContextBuilder userId(final UUID userId) {
        this.userId = userId;
        return this;
    }

    public ServerContextBuilder userId(final String userId) {
        try {
            this.userId = UUID.fromString(userId);
        } catch (Throwable t) {
            logger.warn("userId: is an invalid UUID", t);
            this.userId = null;
        }
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

    /**
     * This method creates an incomplete TP reference that should be replaced later after getting more information
     * from the server.
     * @param teamProjectName
     * @return
     */
    public ServerContextBuilder teamProject(final String teamProjectName) {
        this.teamProjectReference = new TeamProjectReference();
        this.teamProjectReference.setName(teamProjectName);
        return this;
    }

    public ServerContextBuilder repository(final GitRepository repository) {
        this.gitRepository = repository;
        if (this.gitRepository != null && this.teamProjectReference == null) {
            //set team project reference from the Git repository if it is not set
            this.teamProjectReference = this.gitRepository.getProjectReference();
        }
        return this;
    }

    public ServerContext build() {
        return buildWithClient(client);
    }

    protected ServerContext buildWithClient(Client client) {
        final ServerContext serverContext = new ServerContext(type, authenticationInfo, userId, uri, serverUri, client,
                teamProjectCollectionReference, teamProjectReference, gitRepository);

        return serverContext;
    }
}