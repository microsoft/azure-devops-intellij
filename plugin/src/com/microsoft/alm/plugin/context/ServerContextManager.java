// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.exceptions.TeamServicesException;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.rest.ConnectionData;
import com.microsoft.alm.plugin.context.rest.ServiceDefinition;
import com.microsoft.alm.plugin.context.rest.VstsHttpClient;
import com.microsoft.alm.plugin.context.rest.VstsInfo;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import com.microsoft.alm.plugin.services.ServerContextStore;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import com.microsoft.alm.core.webapi.CoreHttpClient;
import com.microsoft.alm.core.webapi.model.TeamProjectCollection;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        ServerContext contextToValidate = context;

        //If context.uri is remote git repo url, try to parse it if needed
        if (UrlHelper.isGitRemoteUrl(context.getUri().toString()) && (
                context.getServerUri() == null ||
                        context.getTeamProjectCollectionReference() == null ||
                        context.getGitRepository() == null)) {
            //parse url
            Validator validator = new Validator(context);
            if (validator.validate(context.getUri().toString())) {
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
        final String CONNECTION_DATA_REST_API_PATH = "/_apis/connectionData?connectOptions=IncludeServices&lastChangeId=-1&lastChangeId64=-1&api-version=1.0";
        final String TFS2015_NEW_SERVICE = "distributedtask";
        final String TELEMETRY_CONNECTION_EVENT = "TfsConnection";
        final String TELEMETRY_TFS_VERSION = "TFS.Version";
        final String TELEMETRY_TFS2012_OR_OLDER = "TFS2012_or_older";
        final String TELEMETRY_TFS2013 = "TFS2013";
        final String TELEMETRY_TFS2015_OR_LATER = "TFS2015_or_later";

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
                    urlForConnectionData.concat(UrlHelper.URL_SEPARATOR).concat(CONNECTION_DATA_REST_API_PATH),
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

                        TfsTelemetryHelper.getInstance().sendEvent(TELEMETRY_CONNECTION_EVENT,
                                new TfsTelemetryHelper.PropertyMapBuilder().success(true).pair(TELEMETRY_TFS_VERSION, TELEMETRY_TFS2015_OR_LATER).build());

                        return contextWithUserId;
                    }
                }

                //This is TFS 2013
                logger.warn("checkTfsVersionAndConnection: Detected an attempt to connect to a TFS 2013 server");
                TfsTelemetryHelper.getInstance().sendEvent(TELEMETRY_CONNECTION_EVENT,
                        new TfsTelemetryHelper.PropertyMapBuilder().success(false).pair(TELEMETRY_TFS_VERSION, TELEMETRY_TFS2013).build());

                throw new TeamServicesException(TeamServicesException.KEY_TFS_UNSUPPORTED_VERSION);
            }
        } catch (com.microsoft.alm.plugin.context.rest.VstsHttpClient.VstsHttpClientException e) {
            if (e.getStatusCode() == 404) {
                //HTTP not found, so server does not have this endpoint i.e. TFS 2012 or older
                logger.warn("checkTfsVersionAndConnection: Detected an attempt to connect to a TFS 2012 or older version server");
                TfsTelemetryHelper.getInstance().sendEvent(TELEMETRY_CONNECTION_EVENT,
                        new TfsTelemetryHelper.PropertyMapBuilder().success(false).pair(TELEMETRY_TFS_VERSION, TELEMETRY_TFS2012_OR_OLDER).build());
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
            // get context from builder, create PAT if needed, and store in active context
            final ServerContext context = createContextFromRemoteUrl(gitRemoteUrl);
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
    public ServerContext createContextFromRemoteUrl(final String gitRemoteUrl) {
        return createContextFromRemoteUrl(gitRemoteUrl, true);
    }

    public ServerContext createContextFromRemoteUrl(final String gitRemoteUrl, final boolean prompt) {
        ArgumentHelper.checkNotEmptyString(gitRemoteUrl);

        // Get matching context from manager
        ServerContext context = get(gitRemoteUrl);
        if (context == null ||
                context.getGitRepository() == null ||
                context.getServerUri() == null ||
                !StringUtils.equalsIgnoreCase(context.getUsableGitUrl(), gitRemoteUrl)) {
            context = null;
        }

        if (context == null) {
            // Manager didn't have a matching context, so try to look up the auth info
            final AuthenticationInfo authenticationInfo = getAuthenticationInfo(gitRemoteUrl, prompt);
            if (authenticationInfo != null) {
                final ServerContext.Type type = UrlHelper.isTeamServicesUrl(gitRemoteUrl) ? ServerContext.Type.VSO : ServerContext.Type.TFS;
                final ServerContext contextToValidate = new ServerContextBuilder()
                        .type(type).uri(gitRemoteUrl).authentication(authenticationInfo).build();
                context = validateServerConnection(contextToValidate);
            }
        }

        if (context != null && context.getUserId() == null) {
            //validate the context and save it with userId
            context = validateServerConnection(context);
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
     * Updates all contexts with matching authority in URI with new authentication info, will prompt the user
     * Has to be called on a background thread, will hang if called on UI thread
     *
     * @param remoteUrl
     */
    public ServerContext updateAuthenticationInfo(final String remoteUrl) {
        AuthenticationInfo newAuthenticationInfo = null;
        boolean promptUser = true;
        final URI remoteUri = UrlHelper.createUri(remoteUrl);
        ServerContext matchingContext = null;

        //Linear search through all contexts to find the ones with same authority as remoteUrl
        for (final ServerContext context : getAllServerContexts()) {
            if (UrlHelper.haveSameAuthority(remoteUri, context.getUri())) {
                //remove the context with old credentials
                remove(context.getKey());

                //get new credentials by prompting the user one time only
                if (promptUser) {
                    //prompt user
                    final AuthenticationProvider authenticationProvider = getAuthenticationProvider(remoteUrl);
                    newAuthenticationInfo = AuthHelper.getAuthenticationInfoSynchronously(authenticationProvider, remoteUrl);
                    promptUser = false;
                }

                if (newAuthenticationInfo != null) {
                    //build a context with new authentication info and add
                    final ServerContextBuilder builder = new ServerContextBuilder(context);
                    builder.authentication(newAuthenticationInfo);
                    final ServerContext newContext = builder.build();
                    if (StringUtils.equalsIgnoreCase(context.getUri().toString(), remoteUrl)) {
                        add(newContext, true);
                        matchingContext = newContext;
                    } else {
                        add(newContext, false);
                    }
                }
            }
        }
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

    private static class Validator implements UrlHelper.ParseResultValidator {
        private final static String REPO_INFO_URL_PATH = "/vsts/info";
        private String serverUrl;
        private final ServerContext context;
        private GitRepository repository;
        private TeamProjectCollection collection;

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

        /**
         * This method queries the server with the given Git remote URL for repository, project and collection information
         * If unable to get the info, it parses the Git remote url and tries to verify it by querying the server again
         *
         * @param gitRemoteUrl
         * @return true if server information is determined
         */
        public boolean validate(final String gitRemoteUrl) {
            try {
                final String gitUrlToParse;

                //handle SSH Git urls
                if (UrlHelper.isSshGitRemoteUrl(gitRemoteUrl)) {
                    gitUrlToParse = UrlHelper.getHttpsGitUrlFromSshUrl(gitRemoteUrl);
                } else {
                    gitUrlToParse = gitRemoteUrl;
                }

                //query the server endpoint for VSTS repo, project and collection info
                if (getVstsInfo(gitUrlToParse)) {
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

            //failed to get VSTS repo, project and collection info
            return false;
        }

        private boolean getVstsInfo(final String gitRemoteUrl) {
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
                final URI collectionUri = URI.create(UrlHelper.getCmdLineFriendlyUrl(parseResult.getCollectionUrl()));
                final GitHttpClient gitClient = new GitHttpClient(context.getClient(), collectionUri);
                // Get the repository object and team project
                repository = gitClient.getRepository(parseResult.getProjectName(), parseResult.getRepoName());
                // Get the collection object
                final URI serverUri = URI.create(parseResult.getServerUrl());
                if (UrlHelper.isTeamServicesUrl(parseResult.getServerUrl())) {
                    final CoreHttpClient coreClient = new CoreHttpClient(context.getClient(), serverUri);
                    collection = coreClient.getProjectCollection(parseResult.getCollectionName());
                } else {
                    final ServerContext contextToValidate = new ServerContextBuilder(context).serverUri(serverUrl).build();
                    final TeamProjectCollectionReference ref = contextToValidate.getSoapServices().getCatalogService().getProjectCollection(parseResult.getCollectionName());
                    collection = new TeamProjectCollection();
                    collection.setId(ref.getId());
                    collection.setName(ref.getName());
                    collection.setUrl(ref.getUrl());
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
