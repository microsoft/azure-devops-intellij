// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.ServerContextStore;
import com.microsoft.teamfoundation.core.webapi.CoreHttpClient;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollection;
import com.microsoft.teamfoundation.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class used to manage ServerContext objects.
 */
public class ServerContextManager {
    private static final Logger logger = LoggerFactory.getLogger(ServerContextManager.class);

    private Map<String, ServerContext> contextMap = new HashMap<String, ServerContext>();
    private String lastUsedContextKey = null;

    private static class Holder {
        private static final ServerContextManager INSTANCE = new ServerContextManager(true);
    }

    /**
     * The constructor is protected for tests.
     */
    protected ServerContextManager() {
        this(false);
    }

    private ServerContextManager(final boolean restore) {
        if (!restore) {
            return;
        }

        try {
            restoreFromSavedState();
        } catch (Throwable t) {
            // being careful here
            logger.error("constructor", t);
        }
    }

    public static ServerContextManager getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized ServerContext getLastUsedContext() {
        final ServerContext context = get(lastUsedContextKey);
        return context;
    }

    public synchronized void clearLastUsedContext() {
        lastUsedContextKey = null;
    }

    public synchronized boolean lastUsedContextIsVSO() {
        final ServerContext lastUsed = getLastUsedContext();
        return lastUsed != null && lastUsed.getType() != ServerContext.Type.TFS;
    }

    public synchronized boolean lastUsedContextIsTFS() {
        final ServerContext lastUsed = getLastUsedContext();
        return lastUsed != null && lastUsed.getType() == ServerContext.Type.TFS;
    }

    public synchronized void add(final ServerContext context) {
        add(context, true);
    }

    public synchronized void add(final ServerContext context, boolean updateLastUsedContext) {
        if (context != null) {
            final String key = context.getKey();
            contextMap.put(key, context);
            getStore().saveServerContext(context);
            if (updateLastUsedContext) {
                lastUsedContextKey = key;
            }
        }
    }

    public synchronized ServerContext get(final String uri) {
        if (!StringUtils.isEmpty(uri)) {
            final ServerContext context = contextMap.get(ServerContext.getKey(uri));
            return context;
        }

        return null;
    }

    public synchronized void remove(final String serverUri) {
        if (StringUtils.isEmpty(serverUri)) {
            return;
        }

        final String key = ServerContext.getKey(serverUri);
        final ServerContext context = get(key);

        if (context != null) {
            getStore().forgetServerContext(key);
            contextMap.remove(key);
            if (StringUtils.equalsIgnoreCase(key, lastUsedContextKey)) {
                clearLastUsedContext();
            }
        }
    }

    public synchronized Collection<ServerContext> getAllServerContexts() {
        return Collections.unmodifiableCollection(contextMap.values());
    }

    private ServerContextStore getStore() {
        return PluginServiceProvider.getInstance().getServerContextStore();
    }

    /**
     * Called once from constructor restore the state from disk between sessions.
     */
    private synchronized void restoreFromSavedState() {
        clearLastUsedContext();
        final List<ServerContext> contexts = getStore().restoreServerContexts();
        for (final ServerContext sc : contexts) {
            add(sc, false);
        }

        //TODO restore lastUsedContextKey

        if (lastUsedContextKey != null) {
            final ServerContext context = get(lastUsedContextKey);
            // restore last used credentials for TFS
            if (context.getType() == ServerContext.Type.TFS) {
                TfsAuthenticationProvider.getInstance().setAuthenticationInfo(context.getAuthenticationInfo());
            } else {
                //VSO or VSO_DEPLOYMENT
                VsoAuthenticationProvider.getInstance().setAuthenticationInfo(context.getAuthenticationInfo());
            }
        }
    }

    /**
     * Get a fully authenticated context from the provided git remote url.
     * Note that if a context does not exist, one will be created and the user will be prompted if necessary.
     * Run this on a background thread.
     */
    public ServerContext getAuthenticatedContext(final String gitRemoteUrl, final boolean setAsActiveContext) {
        try {
            // get context from builder, create PAT if needed, and store in active context
            final ServerContext context = createContextFromRemoteUrl(gitRemoteUrl);
            if (context != null && setAsActiveContext) {
                // Add the context to the manager
                ServerContextManager.getInstance().add(context);
            }
            return context;
        } catch (Throwable t) {
            logger.warn("getAuthenticatedContext unexpected exception", t);
        }
        return null;
    }

    /**
     * Use this method to create a ServerContext from a remote git url.
     * Note that this will require server calls and should be done on a background thread.
     *
     * @param gitRemoteUrl
     * @return
     */
    public ServerContext createContextFromRemoteUrl(final String gitRemoteUrl) {
        assert !StringUtils.isEmpty(gitRemoteUrl);

        // Get matching context from manager
        ServerContext context = get(gitRemoteUrl);
        if (context == null || context.getGitRepository() == null ||
                !StringUtils.equalsIgnoreCase(context.getUsableGitUrl(), gitRemoteUrl)) {
            context = null;
        }

        if (context == null) {
            // Manager didn't have a matching context, so try to look up the auth info
            final AuthenticationInfo authenticationInfo = getAuthenticationInfo(gitRemoteUrl, true);
            if (authenticationInfo != null) {
                // Create a new context object and store it back in the manager
                context = createServerContext(gitRemoteUrl, authenticationInfo);
            }
        }

        return context;
    }

    /**
     * This method tries to find existing authentication info for a given git url.
     * If the auth info cannot be found and the prompt flag is true, the user will be prompted.
     */
    public AuthenticationInfo getAuthenticationInfo(final String gitRemoteUrl, final boolean prompt) {
        AuthenticationInfo authenticationInfo = null;

        // For now I will just do a linear search for an appropriate context info to copy the auth info from
        final URI remoteUri = URI.create(gitRemoteUrl);
        for (final ServerContext context : getAllServerContexts()) {
            if (StringUtils.equalsIgnoreCase(remoteUri.getAuthority(), context.getUri().getAuthority())) {
                authenticationInfo = context.getAuthenticationInfo();
            }
        }

        // If the auth info wasn't found and we are ok to prompt, then prompt
        if (authenticationInfo == null && prompt) {
            final AuthenticationProvider authenticationProvider = getAuthenticationProvider(gitRemoteUrl);
            authenticationInfo = AuthHelper.getAuthenticationInfoSynchronously(authenticationProvider, gitRemoteUrl);
        }

        return authenticationInfo;
    }

    /**
     * Use this method to get the appropriate AuthenticationProvider based on an url.
     *
     * @param url
     * @return
     */
    public AuthenticationProvider getAuthenticationProvider(final String url) {
        if (UrlHelper.isVSO(UrlHelper.getBaseUri(url))) {
            return VsoAuthenticationProvider.getInstance();
        }

        return TfsAuthenticationProvider.getInstance();
    }

    private ServerContext createServerContext(final String gitRemoteUrl, final AuthenticationInfo authenticationInfo) {
        final ServerContext.Type type = UrlHelper.isVSO(UrlHelper.getBaseUri(gitRemoteUrl))
                ? ServerContext.Type.VSO : ServerContext.Type.TFS;
        final Client client = ServerContext.getClient(type, authenticationInfo);
        final Validator validator = new Validator(client);
        final UrlHelper.ParseResult uriParseResult = UrlHelper.tryParse(gitRemoteUrl, validator);
        if (uriParseResult.isSuccess()) {
            final ServerContextBuilder builder = new ServerContextBuilder()
                    .type(type)
                    .uri(gitRemoteUrl)
                    .authentication(authenticationInfo)
                    .teamProject(validator.getRepository().getProjectReference())
                    .repository(validator.getRepository())
                    .collection(validator.getCollection());
            return builder.buildWithClient(client);
        }

        return null;
    }

    private static class Validator implements UrlHelper.ParseResultValidator {
        private final Client client;
        private GitRepository repository;
        private TeamProjectCollection collection;

        public Validator(final Client client) {
            this.client = client;
        }

        public GitRepository getRepository() {
            return repository;
        }

        public TeamProjectCollection getCollection() {
            return collection;
        }

        /**
         * This method gets all the info we need from the server given the parse results.
         * If some call fails we simply return false and ignore the results.
         *
         * @param parseResult
         * @return
         */
        @Override
        public boolean validate(final UrlHelper.ParseResult parseResult) {
            try {
                final URI collectionUri = URI.create(parseResult.getCollectionUrl());
                final GitHttpClient gitClient = new GitHttpClient(client, collectionUri);
                // Get the repository object and team project
                repository = gitClient.getRepository(parseResult.getProjectName(), parseResult.getRepoName());
                // Get the collection object
                final URI serverUri = URI.create(parseResult.getServerUrl());
                final CoreHttpClient coreClient = new CoreHttpClient(client, serverUri);
                collection = coreClient.getProjectCollection(parseResult.getCollectionName());
            } catch (Throwable throwable) {
                logger.error("validate: failed");
                logger.warn("validate", throwable);
                return false;
            }

            return true;
        }
    }
}
