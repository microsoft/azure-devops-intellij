// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.rest.VstsHttpClient;
import com.microsoft.alm.plugin.context.rest.VstsInfo;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
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
        final ServerContext context = get(getLastUsedContextKey());
        return context;
    }

    private void setLastUsedContextKey(String key) {
        PluginServiceProvider.getInstance().getPropertyService().setProperty(PropertyService.PROP_LAST_CONTEXT_KEY, key);
    }

    private String getLastUsedContextKey() {
        return PluginServiceProvider.getInstance().getPropertyService().getProperty(PropertyService.PROP_LAST_CONTEXT_KEY);
    }

    public synchronized void clearLastUsedContext() {
        setLastUsedContextKey(null);
    }

    public synchronized boolean lastUsedContextIsEmpty() {
        final ServerContext lastUsed = getLastUsedContext();
        return lastUsed == null;
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
                setLastUsedContextKey(key);
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
            if (StringUtils.equalsIgnoreCase(key, getLastUsedContextKey())) {
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
        final List<ServerContext> contexts = getStore().restoreServerContexts();
        for (final ServerContext sc : contexts) {
            add(sc, false);
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
    public AuthenticationInfo getBestAuthenticationInfo(final String url, final boolean prompt) {
        final ServerContext context = get(url);
        final AuthenticationInfo info;
        if (context != null) {
            // return exact match
            info = context.getAuthenticationInfo();
        } else {
            // look for a good enough match
            info = getAuthenticationInfo(url, prompt);
        }
        return info;
    }

    /**
     * This method tries to find existing authentication info for a given git url.
     * If the auth info cannot be found and the prompt flag is true, the user will be prompted.
     */
    public AuthenticationInfo getAuthenticationInfo(final String gitRemoteUrl, final boolean prompt) {
        AuthenticationInfo authenticationInfo = null;

        // For now I will just do a linear search for an appropriate context info to copy the auth info from
        final URI remoteUri = UrlHelper.createUri(gitRemoteUrl);
        for (final ServerContext context : getAllServerContexts()) {
            if (UrlHelper.haveSameAuthority(remoteUri, context.getUri())) {
                authenticationInfo = context.getAuthenticationInfo();
                break;
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
     * Prompts the user for credentials and updates the authenticationInfo for all context's that match the remote URLs authority
     *
     * @param remoteUrl
     */
    public void updateAuthenticationInfo(final String remoteUrl) {
        AuthenticationInfo newAuthenticationInfo = null;
        final URI remoteUri = UrlHelper.createUri(remoteUrl);
        //Linear search through all contexts to find the ones with same authority as remoteUrl
        for (final ServerContext context : getAllServerContexts()) {
            if (UrlHelper.haveSameAuthority(remoteUri, context.getUri())) {
                //remove the context with old credentials
                remove(context.getKey());

                //get new credentials if needed
                if (newAuthenticationInfo == null) {
                    //prompt user
                    final AuthenticationProvider authenticationProvider = getAuthenticationProvider(remoteUrl);
                    newAuthenticationInfo = AuthHelper.getAuthenticationInfoSynchronously(authenticationProvider, remoteUrl);
                }

                if (newAuthenticationInfo != null) {
                    //build a context with new authentication info and add
                    final ServerContextBuilder builder = new ServerContextBuilder(context);
                    builder.authentication(newAuthenticationInfo);
                    add(builder.build(), false);
                }
            }
        }
    }

    /**
     * Use this method to get the appropriate AuthenticationProvider based on an url.
     *
     * @param url
     * @return
     */
    public AuthenticationProvider getAuthenticationProvider(final String url) {
        if (UrlHelper.isVSO(UrlHelper.createUri(url))) {
            return VsoAuthenticationProvider.getInstance();
        }

        return TfsAuthenticationProvider.getInstance();
    }

    private ServerContext createServerContext(final String gitRemoteUrl, final AuthenticationInfo authenticationInfo) {
        final ServerContext.Type type = UrlHelper.isVSO(UrlHelper.createUri(gitRemoteUrl))
                ? ServerContext.Type.VSO : ServerContext.Type.TFS;
        final Client client = ServerContext.getClient(type, authenticationInfo);
        final Validator validator = new Validator(client);
        if (validator.validate(gitRemoteUrl)) {
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
        private static String REPO_INFO_URL_PATH = "/vsts/info";
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

        public boolean validate(final String gitRemoteUrl) {
            //query the server endpoint for VSTS repo, project and collection info
            if (getVstsInfo(gitRemoteUrl)) {
                return true;
            }
            //server endpoint query was not successful, try to parse the url
            final UrlHelper.ParseResult uriParseResult = UrlHelper.tryParse(gitRemoteUrl, this);
            if (uriParseResult.isSuccess()) {
                return true;
            }
            //failed to get VSTS repo, project and collection info
            return false;
        }

        private boolean getVstsInfo(final String gitRemoteUrl) {
            try {
                //Try to query the server endpoint gitRemoteUrl/vsts/info
                final VstsInfo vstsInfo = VstsHttpClient.sendRequest(client, gitRemoteUrl.concat(REPO_INFO_URL_PATH), VstsInfo.class);
                if (vstsInfo == null || vstsInfo.getCollectionReference() == null || vstsInfo.getProjectReference() == null) {
                    //information received from the server is not sufficient
                    return false;
                }

                collection = new TeamProjectCollection();
                collection.setId(vstsInfo.getCollectionReference().getId());
                collection.setName(vstsInfo.getCollectionReference().getName());
                collection.setUrl(vstsInfo.getCollectionReference().getUrl());
                repository = new GitRepository();
                repository.setId(vstsInfo.getRepoId());
                repository.setName(vstsInfo.getRepoName());
                repository.setRemoteUrl(gitRemoteUrl);
                repository.setProjectReference(vstsInfo.getProjectReference());
                return true;

            } catch (Throwable throwable) {
                //failed to get VSTS information, endpoint may not be available on the server
                logger.error("validate: failed for Git remote url: {}", gitRemoteUrl);
                logger.warn("validate", throwable);
                return false;
            }
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
                final URI collectionUri = URI.create(UrlHelper.getCmdLineFriendlyUrl(parseResult.getCollectionUrl()));
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
