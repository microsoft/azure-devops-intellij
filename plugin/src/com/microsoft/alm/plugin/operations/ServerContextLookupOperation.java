// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.client.model.VssResourceNotFoundException;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.core.webapi.CoreHttpClient;
import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.context.soap.CatalogService;
import com.microsoft.alm.plugin.exceptions.TeamServicesException;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class ServerContextLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(ServerContextLookupOperation.class);

    public enum ContextScope {REPOSITORY, PROJECT}

    private static final String HTTP_503_EXCEPTION = "HTTP 503 Service Unavailable";
    private final List<ServerContext> contextList;
    private final ContextScope resultScope;

    public class ServerContextLookupResults extends ResultsImpl {
        private final List<ServerContext> serverContexts = new ArrayList<ServerContext>();

        public List<ServerContext> getServerContexts() {
            return Collections.unmodifiableList(serverContexts);
        }

    }

    public ServerContextLookupOperation(final List<ServerContext> contextList, final ContextScope resultScope) {
        ArgumentHelper.checkNotNullOrEmpty(contextList, "contextList");
        ArgumentHelper.checkNotNull(resultScope, "resultScope");

        this.contextList = new ArrayList<ServerContext>(contextList.size());
        this.contextList.addAll(contextList);
        this.resultScope = resultScope;
    }

    public void doWork(final Inputs inputs) {
        onLookupStarted();

        try {
            final boolean throwOnError = contextList.size() == 1;
            final List<Throwable> operationExceptions = new CopyOnWriteArrayList<Throwable>();

            final List<Future> tasks = new ArrayList<Future>();
            for (final ServerContext context : contextList) {
                // submit each account as a separate piece of work to the executor
                tasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                    @Override
                    public void run() {
                        if (isCancelled()) {
                            return;
                        }

                        try {
                            if (context.getType() == ServerContext.Type.TFS) {
                                doSoapCollectionLookup(context);
                            } else { // VSO_DEPLOYMENT || VSO
                                doRestCollectionLookup(context);
                            }
                        } catch (final Throwable t) {
                            boolean shouldReportError = true;
                            logger.warn("doWork: Unable to do lookup on context: " + context.getUri().toString());
                            logger.warn("doWork: Exception", t);
                            if (AuthHelper.isNotAuthorizedError(t)) {
                                final ServerContext newContext
                                        = ServerContextManager.getInstance().updateAuthenticationInfo(context.getUri().toString());
                                // try again with updated authentication info
                                try {
                                    if (context.getType() == ServerContext.Type.TFS) {
                                        doSoapCollectionLookup(newContext);
                                    } else { // VSO_DEPLOYMENT || VSO
                                        doRestCollectionLookup(newContext);
                                    }
                                    // auth issue has been handled properly, no need to report this error anymore
                                    shouldReportError = false;
                                } catch (final Throwable tAgain) {
                                    logger.warn("Failed to lookup repositories even after re-authentication.", tAgain);
                                }
                            }

                            if (shouldReportError) {
                                operationExceptions.add(t);

                                // If there's only one context we need to bubble the exception out
                                // But if there's more than one let's just continue
                                if (throwOnError) {
                                    // check if error is due to the server URI not being found after a valid authentication
                                    // this is an indication that the server URI contains more than just the base URI
                                    if (t instanceof VssResourceNotFoundException) {
                                        logger.warn(String.format("User authenticated but 404 on server so URI (%s) is malformed", context.getServerUri().toString()));
                                        terminate(new TeamServicesException(TeamServicesException.KEY_TFS_MALFORMED_SERVER_URI, t));
                                    } else {
                                        terminate(t);
                                    }
                                }
                            }
                        }
                    }
                }));
            }

            // wait for all tasks to complete
            OperationExecutor.getInstance().wait(tasks);

            if (operationExceptions.size() > 0) {
                terminate(new TeamServicesException(TeamServicesException.KEY_OPERATION_ERRORS));
            }

            onLookupCompleted();
        } catch (Throwable ex) {
            logger.warn("ServerContextLookupOperation failed with an exception", ex);
            terminate(ex);
        }
    }

    @Override
    public void cancel() {
        super.cancel();

        final ServerContextLookupResults results = new ServerContextLookupResults();
        results.isCancelled = true;
        onLookupResults(results);
        onLookupCompleted();
    }

    @Override
    protected void terminate(final Throwable throwable) {
        super.terminate(throwable);

        final ServerContextLookupResults results = new ServerContextLookupResults();
        results.error = throwable;
        onLookupResults(results);
        onLookupCompleted();
    }


    protected void doRestCollectionLookup(final ServerContext context) {
        final CoreHttpClient rootClient = new CoreHttpClient(context.getClient(), context.getUri());
        final List<TeamProjectCollectionReference> collections = rootClient.getProjectCollections(null, null);
        logger.debug("doRestCollectionLookup: Found {} collections on account: {}.", collections.size(), context.getUri().toString());
        doLookup(context, collections);
    }

    protected void doSoapCollectionLookup(final ServerContext context) {
        final CatalogService catalogService = context.getSoapServices().getCatalogService();
        final List<TeamProjectCollectionReference> collections = catalogService.getProjectCollections();
        logger.debug("doSoapCollectionLookup: Found {} collections on server: {}.", collections.size(), context.getUri().toString());
        doLookup(context, collections);
    }

    protected void doLookup(final ServerContext context, final List<TeamProjectCollectionReference> collections) {
        for (final TeamProjectCollectionReference teamProjectCollectionReference : collections) {
            if (isCancelled()) {
                logger.debug("doLookup: Lookup on collection {} on server {} was cancelled.", teamProjectCollectionReference.getName(), context.getUri().toString());
                return;
            }

            final URI collectionURI = UrlHelper.getCollectionURI(context.getUri(), teamProjectCollectionReference.getName());

            try {
                if (resultScope == ContextScope.PROJECT) {
                    final CoreHttpClient client = new CoreHttpClient(context.getClient(), collectionURI);
                    final List<TeamProjectReference> projects = client.getProjects();
                    logger.debug("doLookup: found {} projects in collection: {} on server: {}.", projects.size(), teamProjectCollectionReference.getName(), context.getUri().toString());
                    addTeamProjectResults(projects, context, teamProjectCollectionReference);
                } else {
                    final GitHttpClient gitClient = new GitHttpClient(context.getClient(), collectionURI);
                    final List<GitRepository> gitRepositories = gitClient.getRepositories();
                    logger.debug("doLookup: found {} Git repositories in collection: {} on server: {}.", gitRepositories.size(), teamProjectCollectionReference.getName(), context.getUri().toString());
                    addRepositoryResults(gitRepositories, context, teamProjectCollectionReference);
                }
            } catch (VssResourceNotFoundException e) {
                if (e.getMessage().contains(HTTP_503_EXCEPTION)) {
                    logger.warn("Collection " + teamProjectCollectionReference.getName() + " is unavailable.", e);
                } else {
                    logger.warn("Failure while trying to find collection repos", e);
                }
            }
        }

    }

    protected void addTeamProjectResults(final List<TeamProjectReference> projects, final ServerContext context, final TeamProjectCollectionReference teamProjectCollectionReference) {
        final List<ServerContext> serverContexts = new ArrayList<ServerContext>(projects.size());

        for (final TeamProjectReference project : projects) {
            final ServerContext projectServerContext = new ServerContextBuilder(context)
                    .teamProject(project).collection(teamProjectCollectionReference).build();
            serverContexts.add(projectServerContext);
        }

        final ServerContextLookupResults results = new ServerContextLookupResults();
        results.serverContexts.addAll(serverContexts);
        super.onLookupResults(results);
    }

    protected void addRepositoryResults(final List<GitRepository> gitRepositories, final ServerContext context, final TeamProjectCollectionReference teamProjectCollectionReference) {
        final List<ServerContext> serverContexts = new ArrayList<ServerContext>(gitRepositories.size());
        for (final GitRepository gitRepository : gitRepositories) {
            final ServerContext gitServerContext = new ServerContextBuilder(context)
                    .uri(gitRepository.getRemoteUrl())
                    .repository(gitRepository)
                    .teamProject(gitRepository.getProjectReference())
                    .collection(teamProjectCollectionReference)
                    .build();
            serverContexts.add(gitServerContext);
        }

        final ServerContextLookupResults results = new ServerContextLookupResults();
        results.serverContexts.addAll(serverContexts);

        logger.debug("addRepositoryResults: {} contexts were added to lookup results for {} Git repositories found on the server: {} with resultScope = {}.",
                serverContexts.size(), gitRepositories.size(), context.getUri().toString(), resultScope.toString());
        super.onLookupResults(results);
    }
}
