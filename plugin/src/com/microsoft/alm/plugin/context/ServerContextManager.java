// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.core.webapi.CoreHttpClient;
import com.microsoft.alm.core.webapi.model.TeamProjectCollection;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.rest.ConnectionData;
import com.microsoft.alm.plugin.context.rest.ServiceDefinition;
import com.microsoft.alm.plugin.context.rest.VstsHttpClient;
import com.microsoft.alm.plugin.context.rest.VstsInfo;
import com.microsoft.alm.plugin.exceptions.TeamServicesException;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import com.microsoft.alm.plugin.services.ServerContextStore;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.ArrayList;
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

    private final String CONNECTION_DATA_REST_API_PATH = "/_apis/connectionData?connectOptions=IncludeServices&lastChangeId=-1&lastChangeId64=-1&api-version=1.0";
    private final String TFS2015_NEW_SERVICE = "distributedtask";

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
            // Only persist PATs, not access tokens
            if (shouldBeSaved(context)) {
                getStore().saveServerContext(context);
            }
            if (updateLastUsedContext) {
                setLastUsedContextKey(key);
            }
        }
    }

    private boolean shouldBeSaved(final ServerContext context) {
        boolean shouldBeSaved = (context != null);
        if (shouldBeSaved
                && context.getAuthenticationInfo() != null
                && AuthenticationInfo.CredsType.AccessToken.equals(context.getAuthenticationInfo().getType())) {
            // do not save access tokens
            shouldBeSaved = false;
        }

        return shouldBeSaved;
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
        //copy values from HashMap to a new List make sure the list is immutable
        return Collections.unmodifiableCollection(new ArrayList<ServerContext>(contextMap.values()));
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
     * Validates a provided server context and if validation succeeds saves a server context with the user's team foundation Id
     *
     * @param context
     */
    public ServerContext validateServerConnection(final ServerContext context) {
        Validator validator = new Validator(context);
        return validateServerConnection(context, validator);
    }

    protected ServerContext validateServerConnection(final ServerContext context, Validator validator) {
        ServerContext contextToValidate = context;

        //If context.uri is remote git repo url, try to parse it if needed
        if (UrlHelper.isGitRemoteUrl(context.getUri().toString())) {
            if ((context.getServerUri() == null ||
                    context.getTeamProjectCollectionReference() == null ||
                    context.getGitRepository() == null)) {
                //parse url
                if (validator.validateGitUrl(context.getUri().toString())) {
                    contextToValidate = new ServerContextBuilder(context)
                            .serverUri(validator.getServerUrl())
                            .collection(validator.getCollection())
                            .repository(validator.getRepository())
                            .build();
                } else {
                    //failed to parse
                    contextToValidate = null;
                }
            }
        } else if (context.getTeamProjectReference() != null) {
            final String collectionName = context.getTeamProjectCollectionReference() == null ? StringUtils.EMPTY : context.getTeamProjectCollectionReference().getName();
            // Assume this is a TFVC url and parse the TFVC url to get collection information
            if (validator.validateTfvcUrl(context.getUri().toString(), context.getTeamProjectReference().getName(), collectionName)) {
                contextToValidate = new ServerContextBuilder(context)
                        .serverUri(validator.getServerUrl())
                        .collection(validator.getCollection())
                        .teamProject(validator.getProject())
                        .build();
            }
        }

        logger.info(
                "Validating a {} server context with a server URI {}",
                context.getType(),
                contextToValidate == null ? "" : contextToValidate.getServerUri());

        if (context.getType() == ServerContext.Type.TFS) {
            return checkTfsVersionAndConnection(contextToValidate);
        } else {
            return checkVstsConnection(contextToValidate);
        }
    }


    private ServerContext checkVstsConnection(final ServerContext context) throws TeamServicesException {
        final String CONNECTION_DATA_REST_API_PATH = "/_apis/connectionData?connectOptions=lastChangeId=-1&lastChangeId64=-1&api-version=1.0";

        if (context == null || context.getServerUri() == null) {
            throw new TeamServicesException(TeamServicesException.KEY_VSO_AUTH_FAILED);
        }

        final ConnectionData data = VstsHttpClient.sendRequest(context.getClient(),
                context.getServerUri().toString().concat(CONNECTION_DATA_REST_API_PATH),
                ConnectionData.class);

        if (data == null || data.getAuthenticatedUser() == null) {
            throw new TeamServicesException(TeamServicesException.KEY_VSO_AUTH_FAILED);
        }

        //connection is verified, save the context
        final ServerContext contextWithUserId = new ServerContextBuilder(context)
                .userId(data.getAuthenticatedUser().getId())
                .build();
        add(contextWithUserId);

        return contextWithUserId;
    }

    private ServerContext checkTfsVersionAndConnection(final ServerContext context) throws TeamServicesException {
        if (context == null || context.getServerUri() == null) {
            throw new TeamServicesException(TeamServicesException.KEY_TFS_AUTH_FAILED);
        }

        try {
            final String urlForConnectionData;
            if (context.getCollectionURI() != null) {
                urlForConnectionData = context.getCollectionURI().toString();
            } else {
                urlForConnectionData = context.getServerUri().toString();
            }
            final ConnectionData data = VstsHttpClient.sendRequest(context.getClient(),
                    urlForConnectionData.concat(CONNECTION_DATA_REST_API_PATH),
                    ConnectionData.class);

            if (data == null || data.getAuthenticatedUser() == null) {
                throw new TeamServicesException(TeamServicesException.KEY_TFS_AUTH_FAILED);
            }

            if (data.getLocationServiceData() != null && data.getLocationServiceData().getServiceDefinitions() != null) {
                for (final ServiceDefinition s : data.getLocationServiceData().getServiceDefinitions()) {
                    if (StringUtils.equalsIgnoreCase(s.getServiceType(), TFS2015_NEW_SERVICE)) {
                        //TFS 2015 or higher, save the context with userId
                        final ServerContext contextWithUserId = new ServerContextBuilder(context)
                                .userId(data.getAuthenticatedUser().getId())
                                .build();
                        add(contextWithUserId);
                        final ServerContext lastUsedTfsContext = new ServerContextBuilder(contextWithUserId)
                                .uri(TfsAuthenticationProvider.TFS_LAST_USED_URL).build();
                        add(lastUsedTfsContext);

                        return contextWithUserId;
                    }
                }

                //This is TFS 2013
                logger.warn("checkTfsVersionAndConnection: Detected an attempt to connect to a TFS 2013 server");

                throw new TeamServicesException(TeamServicesException.KEY_TFS_UNSUPPORTED_VERSION);
            }
        } catch (com.microsoft.alm.plugin.context.rest.VstsHttpClient.VstsHttpClientException e) {
            if (e.getStatusCode() == 404) {
                //HTTP not found, so server does not have this endpoint (TFS 2012 or older) or the URL is incorrect
                logger.warn("checkTfsVersionAndConnection: 404 while trying to connect to server so either bad url or unsupported server version");
                throw new TeamServicesException(TeamServicesException.KEY_TFS_UNSUPPORTED_VERSION);
            } else {
                throw new RuntimeException(e);
            }
        }

        //unexpected case
        logger.warn("checkTfsVersionAndConnection: Didn't match TFS 2015 or later, TFS 2013 or TFS 2012 or older server check: {}", context.getUri());
        throw new TeamServicesException(TeamServicesException.KEY_TFS_AUTH_FAILED);
    }

    /**
     * Get a fully authenticated context from the provided git remote url.
     * Note that if a context does not exist, one will be created and the user will be prompted if necessary.
     * Run this on a background thread, will hang if run on the UI thread
     */
    public ServerContext getAuthenticatedContext(final String gitRemoteUrl, final boolean setAsActiveContext) {
        try {
            // get context from builder, create PersonalAccessToken if needed, and store in active context
            final ServerContext context = createContextFromGitRemoteUrl(gitRemoteUrl);
            if (context != null && setAsActiveContext) {
                //nothing to do
                //context is already added to the manager if it is valid
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
    public ServerContext createContextFromGitRemoteUrl(final String gitRemoteUrl) {
        return createContextFromGitRemoteUrl(gitRemoteUrl, true);
    }

    public ServerContext createContextFromGitRemoteUrl(final String gitRemoteUrl, final boolean prompt) {
        ArgumentHelper.checkNotEmptyString(gitRemoteUrl, "gitRemoteUrl");
        // need to make sure not using an SSH url for authentication
        final String httpsGitRemoteUrl = UrlHelper.getHttpsGitUrlFromSshUrl(gitRemoteUrl);

        // Get matching context from manager
        ServerContext context = get(httpsGitRemoteUrl);
        if (context == null ||
                context.getGitRepository() == null ||
                context.getServerUri() == null ||
                !StringUtils.equalsIgnoreCase(context.getUsableGitUrl(), httpsGitRemoteUrl)) {
            context = null;
        }

        if (context == null) {
            // Manager didn't have a matching context, so try to look up the auth info
            final AuthenticationInfo authenticationInfo = getAuthenticationInfo(httpsGitRemoteUrl, prompt);
            if (authenticationInfo != null) {
                final ServerContext.Type type = UrlHelper.isTeamServicesUrl(httpsGitRemoteUrl) ? ServerContext.Type.VSO : ServerContext.Type.TFS;
                final ServerContext contextToValidate = new ServerContextBuilder()
                        .type(type).uri(httpsGitRemoteUrl).authentication(authenticationInfo).build();
                try {
                    context = validateServerConnection(contextToValidate);
                } catch (TeamServicesException e) {
                    logger.warn("Invalid server connection was found:", e);
                }
            }
        }

        if (context != null && context.getUserId() == null) {
            //validate the context and save it with userId
            context = validateServerConnection(context);
        }

        return context;
    }

    public ServerContext createContextFromTfvcServerUrl(URI tfvcServerUrl, String teamProjectName, boolean prompt) {
        return createContextFromTfvcServerUrl(tfvcServerUrl.toString(), teamProjectName, prompt);
    }

    /**
     * @deprecated Use {@link #createContextFromTfvcServerUrl(URI, String, boolean)} instead.
     */
    public ServerContext createContextFromTfvcServerUrl(final String tfvcServerUrl, final String teamProjectName, final boolean prompt) {
        ArgumentHelper.checkNotEmptyString(tfvcServerUrl, "tfvcServerUrl");

        // Get matching context from manager
        ServerContext context = get(tfvcServerUrl);
        logger.info("createContextFromTfvcServerUrl context exists: " + (context != null));
        if (context == null || context.getServerUri() == null ||
                context.getTeamProjectCollectionReference() == null ||
                context.getTeamProjectCollectionReference().getName() == null ||
                context.getTeamProjectReference() == null ||
                context.getTeamProjectReference().getId() == null ||
                !StringUtils.equalsIgnoreCase(context.getTeamProjectReference().getName(), teamProjectName)) {
            context = null;
            logger.info("createContextFromTfvcServerUrl context fully populated: " + (context != null));
        }

        if (context == null) {
            // Manager didn't have a matching context, so try to look up the auth info
            final AuthenticationInfo authenticationInfo = getAuthenticationInfo(tfvcServerUrl, prompt);
            if (authenticationInfo != null) {
                final ServerContext.Type type = UrlHelper.isTeamServicesUrl(tfvcServerUrl) ? ServerContext.Type.VSO : ServerContext.Type.TFS;

                final ServerContext contextToValidate = new ServerContextBuilder()
                        .type(type).uri(tfvcServerUrl).authentication(authenticationInfo)
                        .teamProject(teamProjectName).build();
                logger.info("type = " + type + ", uri = " + tfvcServerUrl + ", projectName = " + teamProjectName);
                try {
                    context = validateServerConnection(contextToValidate);
                } catch (NotAuthorizedException e) {
                    logger.warn("createContextFromTfvcServerUrl: NotAuthorizedException occurred");
                    if (prompt) {
                        // refreshing creds
                        logger.info("createContextFromTfvcServerUrl: refreshing creds");
                        updateAuthenticationInfo(tfvcServerUrl);
                        final AuthenticationInfo authenticationInfoRefreshed = getAuthenticationInfo(tfvcServerUrl, prompt);
                        final ServerContext contextToValidateRefreshed = new ServerContextBuilder()
                                .type(type).uri(tfvcServerUrl).authentication(authenticationInfoRefreshed)
                                .teamProject(teamProjectName).build();
                        context = validateServerConnection(contextToValidateRefreshed);
                    } else {
                        logger.info("createContextFromTfvcServerUrl: Stale creds detected but feature does not allow prompting for refresh");
                    }
                }
            } else {
                logger.warn("createContextFromTfvcServerUrl: authentication info returned null");
            }
        }

        if (context != null && context.getUserId() == null) {
            //validate the context and save it with userId
            context = validateServerConnection(context);
        }

        return context;
    }

    /**
     * This method tries to find existing authentication info for a given server URI.
     * If the auth info cannot be found and the prompt flag is true, the user will be prompted.
     */
    @Nullable
    public AuthenticationInfo getBestAuthenticationInfo(@NotNull URI uri, boolean prompt) {
        final ServerContext context = get(uri.toString());
        final AuthenticationInfo info;
        if (context != null) {
            // return exact match
            info = context.getAuthenticationInfo();
        } else {
            // look for a good enough match
            info = getAuthenticationInfo(uri, prompt);
        }
        return info;
    }

    /**
     * This method tries to find existing authentication info for a given git url.
     * If the auth info cannot be found and the prompt flag is true, the user will be prompted.
     *
     * @deprecated Use {@link #getBestAuthenticationInfo(URI, boolean)} instead.
     */
    public AuthenticationInfo getBestAuthenticationInfo(final String url, final boolean prompt) {
        return getAuthenticationInfo(URI.create(url), prompt);
    }

    /**
     * @deprecated Use {@link #getAuthenticationInfo(URI, boolean)} instead.
     */
    public AuthenticationInfo getAuthenticationInfo(String gitRemoteUrl, boolean prompt) {
        return getAuthenticationInfo(UrlHelper.createUri(gitRemoteUrl), prompt);
    }

    /**
     * This method tries to find existing authentication info for a given server URI.
     * If the auth info cannot be found and the prompt flag is true, the user will be prompted.
     */
    public AuthenticationInfo getAuthenticationInfo(URI serverUri, final boolean prompt) {
        AuthenticationInfo authenticationInfo = null;

        // For now I will just do a linear search for an appropriate context info to copy the auth info from
        for (final ServerContext context : getAllServerContexts()) {
            if (UrlHelper.haveSameAccount(serverUri, context.getUri())) {
                logger.info("AuthenticatedInfo found for url " + serverUri);
                authenticationInfo = context.getAuthenticationInfo();
                break;
            }
        }

        // If the auth info wasn't found and we are ok to prompt, then prompt
        if (authenticationInfo == null && prompt) {
            logger.info("Prompting for credentials");
            String uriString = serverUri.toString();
            final AuthenticationProvider authenticationProvider = getAuthenticationProvider(uriString);
            authenticationInfo = AuthHelper.getAuthenticationInfoSynchronously(authenticationProvider, uriString);
        }

        return authenticationInfo;
    }

    /**
     * Takes existing contexts, creates new auth info for them, and creates new contexts with that info and all
     * other contexts that share that auth info and removes the old contexts from use
     *
     * @param contexts
     * @return
     */
    public void updateServerContextsAuthInfo(final List<ServerContext> contexts) {
        logger.info("updateServerContextsAuthInfo: starting with context count: " + contexts.size() + ", contextMap size: " + contextMap.size());
        final HashMap<AuthenticationInfo, AuthenticationInfo> authInfoMap = new HashMap<AuthenticationInfo, AuthenticationInfo>(contexts.size());
        for (final ServerContext context : contexts) {
            authInfoMap.put(context.getAuthenticationInfo(), getNewAuthInfo(context));
        }
        refreshAuthInfo(authInfoMap);
        logger.info("updateServerContextsAuthInfo: ending with contextMap size: " + contextMap.size());
    }

    /**
     * Checks all contexts for old auth info and creates new contexts with the new auth info
     *
     * @param authInfoMap: keys are old auth infos and the values are the new auth infos
     */
    private void refreshAuthInfo(final HashMap<AuthenticationInfo, AuthenticationInfo> authInfoMap) {
        logger.info("refreshAuthInfo: refreshing auth info");
        for (final ServerContext context : getAllServerContexts()) {
            final AuthenticationInfo contextAuthInfo = context.getAuthenticationInfo();
            if (authInfoMap.containsKey(contextAuthInfo)) {
                logger.info("refreshAuthInfo: Refreshing context auth info for: " + context.getKey());
                remove(context.getKey());
                add(new ServerContextBuilder(context).authentication(authInfoMap.get(contextAuthInfo)).build());
            }
        }
    }

    /**
     * Takes an existing server context and creates a new server context from it with everything the same except
     * it prompts the user for new authentication info.
     *
     * @param context the context to use as a base for a new one
     * @return authentication info for the new context
     */
    private AuthenticationInfo getNewAuthInfo(final ServerContext context) {
        logger.info("Updating context auth info");
        final AuthenticationProvider authenticationProvider = ServerContext.Type.TFS.equals(context.getType()) ?
                TfsAuthenticationProvider.getInstance() : VsoAuthenticationProvider.getInstance();
        // can't determine if TFVC or Git from context so check for a Git URL
        String url = context.getUsableGitUrl() == null ? context.getUri().toString() : context.getUsableGitUrl();
        // if the URL is the default value we give for last used get it from the auth info instead
        if (StringUtils.equals(url, TfsAuthenticationProvider.TFS_LAST_USED_URL) && context.getAuthenticationInfo() != null) {
            url = context.getAuthenticationInfo().getServerUri();
        }

        logger.info("Updating auth info with url: " + url);
        AuthenticationInfo newAuthInfo = AuthHelper.getAuthenticationInfoSynchronously(authenticationProvider, url);
        if (newAuthInfo != null) {
            // Creating a new authentication info object will replace the existing context with a newly constructed one
            // that doesn't preserve the repository information. To fix that, we'll create a new context using the
            // existing context information, and put it into the manager.
            add(new ServerContextBuilder(context).authentication(newAuthInfo).build());
        }

        return newAuthInfo;
    }

    /**
     * Gets back the most updated context with auth info possible. It first checks to see if an existing context exists
     * and if not it tries to create one. If the create fails (possibility auth info used was stale) then the auth info
     * is updated and then we try to create the context again
     * <p/>
     * TODO: Rip out this method and refactor the code to throw up the unauthorized exception instead of swallowing it
     * TODO: so we can specifically retry on that and remove the bad cached creds
     *
     * @param remoteUrl
     * @return context
     */
    public ServerContext getUpdatedContext(final String remoteUrl, final boolean setAsActiveContext) {
        // try to get the context the normal way first
        ServerContext context = getAuthenticatedContext(remoteUrl, setAsActiveContext);

        if (context != null) {
            logger.info("getUpdatedContext found/created context on first attempt");
            return context;
        }

        // if the context was not obtained in the first try, update the auth info and try again if need be
        context = updateAuthenticationInfo(remoteUrl);
        logger.info("getUpdatedContext updated auth info and found a context: " + (context == null ? "false" : "true"));
        return context == null ? getAuthenticatedContext(remoteUrl, setAsActiveContext) : context;
    }

    /**
     * Updates all contexts with matching authority in URI with new authentication info, will prompt the user
     * Has to be called on a background thread, will hang if called on UI thread
     *
     * @param remoteUrl
     */
    public ServerContext updateAuthenticationInfo(final String remoteUrl) {
        logger.info("Updating auth info for url " + remoteUrl);
        AuthenticationInfo newAuthenticationInfo = null;
        boolean promptUser = true;
        final URI remoteUri = UrlHelper.createUri(remoteUrl);
        ServerContext matchingContext = null;

        //Linear search through all contexts to find the ones with same authority as remoteUrl
        for (final ServerContext context : getAllServerContexts()) {
            logger.info("auth info updateAuthenticationInfo compare " + context.getUri().getPath());
            if (UrlHelper.haveSameAccount(remoteUri, context.getUri())) {
                //remove the context with old credentials
                remove(context.getKey());

                logger.info("auth info updateAuthenticationInfo removed");
                //get new credentials by prompting the user one time only
                if (promptUser) {
                    logger.info("auth info updateAuthenticationInfo prompting");
                    //prompt user
                    final AuthenticationProvider authenticationProvider = getAuthenticationProvider(remoteUrl);
                    authenticationProvider.clearAuthenticationDetails(context.getServerUri().toString());
                    newAuthenticationInfo = AuthHelper.getAuthenticationInfoSynchronously(authenticationProvider, remoteUrl);
                    promptUser = false;
                }

                if (newAuthenticationInfo != null) {
                    logger.info("auth info updateAuthenticationInfo not null");
                    //build a context with new authentication info and add
                    final ServerContextBuilder builder = new ServerContextBuilder(context);
                    builder.authentication(newAuthenticationInfo);
                    final ServerContext newContext = builder.build();
                    logger.info(context.getUri().toString() + "       " + remoteUrl);
                    if (StringUtils.equalsIgnoreCase(context.getUri().toString(), remoteUrl)) {
                        logger.info("The updated auth info created a context that matches the remote url");
                        add(newContext, true);
                        matchingContext = newContext;
                    } else {
                        logger.info("The updated auth info created a context that has a different remote url");
                        add(newContext, false);
                    }
                }
            }
        }

        logger.info("auth info updateAuthenticationInfo returning an updated context: "
                + (matchingContext == null ? "false" : "true"));
        return matchingContext;
    }

    /**
     * Use this method to get the appropriate AuthenticationProvider based on an url.
     *
     * @param url
     * @return
     */
    public AuthenticationProvider getAuthenticationProvider(final String url) {
        if (UrlHelper.isTeamServicesUrl(url)) {
            return VsoAuthenticationProvider.getInstance();
        }

        return TfsAuthenticationProvider.getInstance();
    }

    protected static class Validator implements UrlHelper.ParseResultValidator {
        private final static String TFVC_BRANCHES_URL_PATH = "/_apis/tfvc/branches";
        private final static String REPO_INFO_URL_PATH = "/vsts/info";
        private String serverUrl;
        private final ServerContext context;
        private GitRepository repository;
        private TeamProjectCollection collection;
        private TeamProjectReference project;

        public Validator(final ServerContext context) {
            this.context = context;
        }

        public String getServerUrl() {
            return serverUrl;
        }

        public GitRepository getRepository() {
            return repository;
        }

        public TeamProjectCollection getCollection() {
            return collection;
        }

        public TeamProjectReference getProject() {
            return project;
        }

        protected GitHttpClient getGitHttpClient(final Client jaxrsClient, final URI baseUrl) {
            return new GitHttpClient(jaxrsClient, baseUrl);
        }

        protected CoreHttpClient getCoreHttpClient(final Client jaxrsClient, final URI baseUrl) {
            return new CoreHttpClient(jaxrsClient, baseUrl);
        }

        protected TeamProjectCollection getCollectionFromServer(final ServerContext context, String collectionName) {
            final TeamProjectCollectionReference ref =
                    context.getSoapServices().getCatalogService().getProjectCollection(collectionName);

            TeamProjectCollection collection = new TeamProjectCollection();
            collection.setId(ref.getId());
            collection.setName(ref.getName());
            collection.setUrl(ref.getUrl());
            return collection;
        }

        protected TeamProjectReference getProjectFromServer(final ServerContext context, URI collectionURI, String teamProjectName) {
            final CoreHttpClient client = new CoreHttpClient(context.getClient(), collectionURI);
            for (TeamProjectReference ref : client.getProjects()) {
                if (StringUtils.equalsIgnoreCase(ref.getName(), teamProjectName)) {
                    return ref;
                }
            }
            return null;
        }

        /**
         * This method queries the server with the given Git remote URL for repository, project and collection information
         * If unable to get the info, it parses the Git remote url and tries to verify it by querying the server again
         *
         * @param gitRemoteUrl
         * @return true if server information is determined
         */
        public boolean validateGitUrl(final String gitRemoteUrl) {
            try {
                final String gitUrlToParse;

                //handle SSH Git urls
                if (UrlHelper.isSshGitRemoteUrl(gitRemoteUrl)) {
                    gitUrlToParse = UrlHelper.getHttpsGitUrlFromSshUrl(gitRemoteUrl);
                } else {
                    // Trim user info from the URI (e.g. https://username@dev.azure.com → https://dev.azure.com, because
                    // otherwise the HTTP client will try to use authentication data from the URL instead of the data
                    // baked into the client itself).
                    gitUrlToParse = UrlHelper.removeUserInfo(gitRemoteUrl);
                }

                //query the server endpoint for VSTS repo, project and collection info
                if (getVstsInfoForGit(gitUrlToParse)) {
                    return true;
                }
                //server endpoint query was not successful, try to parse the url
                final UrlHelper.ParseResult uriParseResult = UrlHelper.tryParse(gitUrlToParse, this);
                if (uriParseResult.isSuccess()) {
                    return true;
                }
            } catch (Throwable t) {
                logger.warn("validate: {} of git remote url failed", gitRemoteUrl);
                logger.warn("validate: unexpected exception ", t);
            }

            logger.info("validateGitUrl: failed to get Azure DevOps repo, project and collection info");
            return false;
        }

        private boolean getVstsInfoForGit(final String gitRemoteUrl) {
            try {
                //Try to query the server endpoint gitRemoteUrl/vsts/info
                final VstsInfo vstsInfo = VstsHttpClient.sendRequest(context.getClient(), gitRemoteUrl.concat(REPO_INFO_URL_PATH), VstsInfo.class);
                if (vstsInfo == null || vstsInfo.getCollectionReference() == null ||
                        vstsInfo.getRepository() == null || vstsInfo.getRepository().getProjectReference() == null) {
                    //information received from the server is not sufficient
                    return false;
                }

                serverUrl = vstsInfo.getServerUrl();

                collection = new TeamProjectCollection();
                collection.setId(vstsInfo.getCollectionReference().getId());
                collection.setName(vstsInfo.getCollectionReference().getName());
                collection.setUrl(vstsInfo.getCollectionReference().getUrl());
                repository = vstsInfo.getRepository();
                return true;

            } catch (Throwable throwable) {
                //failed to get VSTS information, endpoint may not be available on the server
                logger.warn("validate: failed for Git remote url: {}", gitRemoteUrl);
                logger.warn("validate", throwable);
                if (AuthHelper.isNotAuthorizedError(throwable)) {
                    throw new TeamServicesException(TeamServicesException.KEY_VSO_AUTH_FAILED, throwable);
                }
                return false;
            }
        }

        /**
         * This method queries the server with the given TFVC URL for collection information
         *
         * @param collectionUrl
         * @param teamProjectName
         * @param possibleCollectionName
         * @return true if server information is determined
         */
        public boolean validateTfvcUrl(final String collectionUrl, final String teamProjectName, final String possibleCollectionName) {
            try {
                final String collectionName;
                final String serverUrl;
                if (UrlHelper.isTeamServicesUrl(collectionUrl)) {
                    if (UrlHelper.isOrganizationUrl(collectionUrl)) {
                        // For organizations the serverUrl == the collectionUrl
                        serverUrl = collectionUrl;
                        // Collection name is the account, i.e. azure.com/account
                        collectionName = UrlHelper.getAccountFromOrganization(serverUrl);
                    } else {
                        // The Team Services collection is ALWAYS defaultCollection, and both the url with defaultcollection
                        // and the url without defaultCollection will validate just fine. However, it expects you to refer to
                        // the collection by the account name. So, we just need to grab the account name and use that to
                        // recreate the url.
                        // If validation fails, we return false.
                        final String accountName = UrlHelper.getVSOAccountName(UrlHelper.createUri(collectionUrl));
                        serverUrl = UrlHelper.getVSOAccountURI(accountName).toString();
                        collectionName = accountName;
                    }
                    if (!validateTfvcCollectionUrl(serverUrl)) {
                        return false;
                    }
                } else {
                    // A full Team Foundation Server collection url is required for the validate call to succeed. So,
                    // we try the url given. If that fails, we assume it is a server Url and the collection is the
                    // defaultCollection. If that assumption fails we return false.
                    if (validateTfvcCollectionUrl(collectionUrl)) {
                        final String[] parts = splitTfvcCollectionUrl(collectionUrl);
                        serverUrl = parts[0];
                        collectionName = parts[1];
                    } else if (StringUtils.isNotEmpty(possibleCollectionName)
                            && validateTfvcCollectionUrl(UrlHelper.getCollectionURI(UrlHelper.createUri(collectionUrl),
                            possibleCollectionName).toString())) {
                        serverUrl = collectionUrl;
                        collectionName = possibleCollectionName;
                    } else {
                        serverUrl = collectionUrl;
                        collectionName = UrlHelper.DEFAULT_COLLECTION;
                        if (!validateTfvcCollectionUrl(UrlHelper.getCollectionURI(UrlHelper.createUri(serverUrl), collectionName).toString())) {
                            return false;
                        }
                    }
                }

                this.serverUrl = serverUrl;
                // Get the collection object from the server (different based on VSO vs OnPrem)
                if (UrlHelper.isTeamServicesUrl(serverUrl)) {
                    final CoreHttpClient coreClient = getCoreHttpClient(context.getClient(), UrlHelper.createUri(serverUrl));
                    collection = coreClient.getProjectCollection(collectionName);
                } else {
                    final ServerContext contextToValidate = new ServerContextBuilder(context).serverUri(serverUrl).build();
                    collection = getCollectionFromServer(contextToValidate, collectionName);
                }
                // Get the Team Project object from the server
                this.project = getProjectFromServer(context, UrlHelper.getCollectionURI(UrlHelper.createUri(serverUrl), collectionName), teamProjectName);
                return true;
            } catch (VstsHttpClient.VstsHttpClientException e) {
                logger.warn("validate: unexpected VstsHttpClientException ", e);
                if (e.getStatusCode() == 401) {
                    throw new NotAuthorizedException("Unauthorized user", e);
                }
            } catch (Throwable t) {
                logger.warn("validate: {} of server url failed", collectionUrl);
                logger.warn("validate: unexpected exception ", t);
            }

            logger.info("validateTfvcUrl: failed to get collection info");
            return false;
        }

        /**
         * This method parses the collection url into 2 parts:
         * 0: the server url (ending in a slash)
         * 1: the collection name (no slashes)
         *
         * @param collectionUrl
         * @return
         */
        private String[] splitTfvcCollectionUrl(final String collectionUrl) {
            final String[] result = new String[2];
            if (StringUtils.isEmpty(collectionUrl)) {
                return result;
            }

            // Now find the TRUE last separator (before the collection name)
            final String trimmedUrl = UrlHelper.trimTrailingSeparators(collectionUrl);
            final int index = trimmedUrl.lastIndexOf(UrlHelper.URL_SEPARATOR);
            if (index >= 0) {
                // result0 is the server url without the collection name
                result[0] = trimmedUrl.substring(0, index + 1);
                // result1 is just the collection name (no separators)
                result[1] = trimmedUrl.substring(index + 1);
            } else {
                // We can't determine the collection name so leave it empty
                result[0] = collectionUrl;
                result[1] = StringUtils.EMPTY;
            }

            return result;
        }

        private boolean validateTfvcCollectionUrl(final String collectionUrl) {
            //Try to query the server endpoint for branches to see if the collection url is correct
            try {
                VstsHttpClient.sendRequest(context.getClient(), UrlHelper.combine(collectionUrl, TFVC_BRANCHES_URL_PATH), String.class);
                logger.info("validateTfvcCollectionUrl: validated url: " + collectionUrl);
                return true;
            } catch (VstsHttpClient.VstsHttpClientException e) {
                if (e.getStatusCode() == 404) {
                    // Add the DefaultCollection to the url and try again
                    logger.info("validateTfvcCollectionUrl: found 404 for url: " + collectionUrl);
                    return false;
                } else {
                    logger.warn("validateTfvcCollectionUrl failed", e);
                    throw e;
                }
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
                serverUrl = parseResult.getServerUrl();
                final URI collectionUri = UrlHelper.createUri(parseResult.getCollectionUrl());
                final GitHttpClient gitClient = getGitHttpClient(context.getClient(), collectionUri);
                // Get the repository object and team project
                repository = gitClient.getRepository(parseResult.getProjectName(), parseResult.getRepoName());
                // Get the collection object
                final URI serverUri = UrlHelper.createUri(parseResult.getServerUrl());
                if (UrlHelper.isTeamServicesUrl(parseResult.getServerUrl())) {
                    final CoreHttpClient coreClient = getCoreHttpClient(context.getClient(), serverUri);
                    collection = coreClient.getProjectCollection(parseResult.getCollectionName());
                } else {
                    final ServerContext contextToValidate = new ServerContextBuilder(context).serverUri(serverUrl).build();
                    collection = getCollectionFromServer(contextToValidate, parseResult.getCollectionName());
                }
            } catch (Throwable throwable) {
                logger.error("validate: failed for parseResult " + parseResult.toString());
                logger.warn("validate", throwable);
                return false;
            }

            return true;
        }
    }
}
