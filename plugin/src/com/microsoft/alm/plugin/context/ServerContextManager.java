// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.operations.AccountLookupOperation;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.ServerContextStore;
import com.microsoft.teamfoundation.core.webapi.CoreHttpClient;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollection;
import com.microsoft.teamfoundation.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.tf.common.authentication.aad.PersonalAccessTokenFactory;
import com.microsoft.tf.common.authentication.aad.TokenScope;
import com.microsoft.tf.common.authentication.aad.impl.PersonalAccessTokenFactoryImpl;
import com.microsoft.visualstudio.services.account.webapi.model.Account;
import com.microsoft.visualstudio.services.authentication.DelegatedAuthorization.webapi.model.SessionToken;
import com.microsoftopentechnologies.auth.AuthenticationResult;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.WinHttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Singleton class used to manage ServerContext objects.
 */
public class ServerContextManager {

    private static final Logger logger = LoggerFactory.getLogger(ServerContextManager.class);

    private static final boolean NTLM_ENABLED;
    //TODO remove the concept of 1 active context and replace with a list of contexts that you retrieve by URI
    private ServerContext activeContext = null;

    static {
        final String propertyNtlmEnabled = System.getProperty("ntlmEnabled");
        NTLM_ENABLED = propertyNtlmEnabled == null || Boolean.parseBoolean(propertyNtlmEnabled);
    }

    public static boolean isNtlmEnabled() {
        return NTLM_ENABLED && WinHttpClients.isWinAuthAvailable();
    }

    private static class ServerContextManagerHolder {
        private static final ServerContextManager INSTANCE = new ServerContextManager();
    }

    private ServerContextManager() {
        try {
            restoreFromSavedState();
        } catch (Throwable t) {
            // being careful here
            logger.error("constructor", t);
        }
    }

    public static ServerContextManager getInstance() {
        return ServerContextManagerHolder.INSTANCE;
    }

    public synchronized ServerContext getActiveContext() {
        return activeContext;
    }

    //TODO this should go away when TFS Auth Provider works the same as VSO auth provider
    public synchronized ServerContext getActiveTfsContext() {
        final ServerContext currentContext = getActiveContext();
        if (currentContext != ServerContext.NO_CONTEXT &&
                currentContext.getType() == ServerContext.Type.TFS) {
            return currentContext;
        }
        return null;
    }

    public synchronized void setActiveContext(final ServerContext context) {
        activeContext = context;
        if (context != ServerContext.NO_CONTEXT) {
            switch (context.getType()) {
                case TFS:
                case VSO:
                    getStore().saveServerContext(context);
                    break;
                case VSO_DEPLOYMENT:
                    throw new IllegalArgumentException("ServerContext type not allowed: " + ServerContext.Type.VSO_DEPLOYMENT);
            }
        }
    }

    public synchronized void clearServerContext(final URI serverUri) {
        final ServerContextStore.Key key = ServerContextStore.Key.create(serverUri);
        getStore().forgetServerContext(key);
        //TODO -- only one for now
        if (activeContext != ServerContext.NO_CONTEXT) {
            final ServerContextStore.Key activeKey = ServerContextStore.Key.create(activeContext);
            if (activeKey.stringValue().equals(key.stringValue())) {
                activeContext = null;
            }
        }
    }

    public synchronized ServerContext getServerContext(final URI uri) {
        //TODO -- only one for now
        if (activeContext != ServerContext.NO_CONTEXT &&
                StringUtils.equalsIgnoreCase(activeContext.getUri().getHost(), uri.getHost())) {
            return activeContext;
        }
        return ServerContext.NO_CONTEXT;
    }

    public synchronized List<ServerContext> getAllServerContexts() {
        //TODO -- only one for now
        return activeContext == ServerContext.NO_CONTEXT ? Collections.<ServerContext>emptyList() : Collections.singletonList(activeContext);
    }

    private ServerContextStore getStore() {
        return PluginServiceProvider.getInstance().getServerContextStore();
    }

    /**
     * Called once from constructor restore the state from disk between sessions.
     */
    private synchronized void restoreFromSavedState() {
        List<ServerContext> loaded = getStore().restoreServerContexts();
        activeContext = loaded.size() > 0 ? loaded.get(0) : ServerContext.NO_CONTEXT;
    }

    /**
     * Get a fully authenticated context from the provided git remote url.
     * Note that if a context does not exist, one will be created and the user will be prompted if necessary.
     * Run this on a background thread.
     */
    public ServerContext getAuthenticatedContext(String gitRemoteUrl, String patDescription, boolean setAsActiveContext) {
        // get context from builder, create PAT if needed, and store in active context
        ServerContext context = createContextFromRemoteUrl(gitRemoteUrl);
        if (context != null) {
            if (context.getType() == ServerContext.Type.VSO_DEPLOYMENT) {
                // Generate a PAT and get the new context
                final ServerContext newContext = createVsoContext(context,
                        VsoAuthenticationProvider.getInstance(),
                        patDescription);
                if (newContext != null) {
                    context = newContext;
                } else {
                    logger.error("Unable to create PAT token");
                }
            }

            if (setAsActiveContext) {
                // Set the active context for later use
                ServerContextManager.getInstance().setActiveContext(context);
            }
        }

        return context;
    }

    /**
     * This method takes a VSO_DEPLOYMENT context and returns the VSO context that has the correct PAT
     * @param originalContext the VSO_DEPLOYMENT context to use to generate the PAT
     * @param authenticationProvider the provider used to create the original context
     * @param tokenDescription the description to use for the generated PAT
     * @return a new VSO context object
     */
    public ServerContext createVsoContext(ServerContext originalContext, VsoAuthenticationProvider authenticationProvider, String tokenDescription) {
        // If the context is a VSO_DEPLOYMENT, then we can generate the PAT and create a new context
        // Otherwise, throw an exception
        if (originalContext == null || originalContext.getType() != ServerContext.Type.VSO_DEPLOYMENT) {
            throw new IllegalArgumentException("originalContext must be a VSO_DEPLOYMENT context");
        }
        if (authenticationProvider == null) {
            throw new IllegalArgumentException("authenticationProvider must be set");
        }

        //generate PAT
        final AuthenticationResult result = authenticationProvider.getAuthenticationResult();
        final PersonalAccessTokenFactory patFactory = new PersonalAccessTokenFactoryImpl(result);

        final String accountName = UrlHelper.getVSOAccountName(originalContext.getUri());
        final Account account = AccountLookupOperation.getAccount(result, accountName);

        if (account != null) {
            //TODO: handle case where session token cannot be created
            SessionToken sessionToken = patFactory.createSessionToken(tokenDescription,
                    Arrays.asList(TokenScope.CODE_READ, TokenScope.CODE_WRITE, TokenScope.CODE_MANAGE), account.getAccountId());
            //create a VSO context with session token (remove the original client and allow that to be recreated)
            final AuthenticationInfo finalAuthenticationInfo = AuthHelper.createAuthenticationInfo(originalContext.getUri().toString(), result, sessionToken);
            final ServerContext newContext =
                    new ServerContextBuilder(originalContext).type(ServerContext.Type.VSO)
                            .authentication(finalAuthenticationInfo).buildWithClient(null);
            return newContext;
        }

        logger.debug("Account not found: " + accountName);
        return null;
    }

    /**
     * Use this method to create a ServerContext from a remote git url.
     * Note that this will require server calls and should be done on a background thread.
     * @param gitRemoteUrl
     * @return
     */
    public ServerContext createContextFromRemoteUrl(final String gitRemoteUrl) {
        assert !StringUtils.isEmpty(gitRemoteUrl);

        try {
            // Get matching context from manager
            ServerContext context = getActiveContext();
            if (context == ServerContext.NO_CONTEXT || context.getGitRepository() == null ||
                    !StringUtils.equalsIgnoreCase(context.getGitRepository().getRemoteUrl().replace(" ", "%20"), gitRemoteUrl)) {
                context = null;
            }

            if (context == null) {
                // Manager didn't have a matching context, so create one
                final AuthenticationProvider authenticationProvider = getAuthenticationProvider(gitRemoteUrl);
                final AuthenticationInfo authenticationInfo = AuthHelper.getAuthenticationInfoSynchronously(authenticationProvider, gitRemoteUrl);
                if (authenticationInfo != null) {
                    // Create a new context object and store it back in the manager
                    context = createServerContext(gitRemoteUrl, authenticationInfo);
                }
            }

            return context;
        } catch (Throwable ex) {
            throw new RuntimeException(ex.getLocalizedMessage(), ex);
        }
    }

    /**
     * Use this method to get the appropriate AuthenticationProvider based on an url.
     * @param url
     * @return
     */
    public AuthenticationProvider getAuthenticationProvider(final String url) {
        if (UrlHelper.isVSO(UrlHelper.getBaseUri(url))) {
            return VsoAuthenticationProvider.getInstance();
        }

        return new TfsAuthenticationProvider();
    }

    private ServerContext createServerContext(String gitRemoteUrl, AuthenticationInfo authenticationInfo) {
        ServerContext.Type type = UrlHelper.isVSO(UrlHelper.getBaseUri(gitRemoteUrl))
                ? ServerContext.Type.VSO_DEPLOYMENT : ServerContext.Type.TFS;
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

            // Set the uri of the context to the server uri (TODO change context so that it can be any URI in the hierarchy)
            final URI serverUri = URI.create(uriParseResult.getServerUrl());
            builder.uri(serverUri);

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
         * @param parseResult
         * @return
         */
        @Override
        public boolean validate(UrlHelper.ParseResult parseResult) {
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
                //TODO Log the failure
                return false;
            }

            return true;
        }
    }

}
