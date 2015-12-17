// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.TeamServicesException;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.soap.CatalogService;
import com.microsoft.teamfoundation.core.webapi.CoreHttpClient;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.vss.client.core.model.VssResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public class ServerContextLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(ServerContextLookupOperation.class);

    public enum ContextScope {REPOSITORY, PROJECT}

    private final List<ServerContext> contextList;
    private final ContextScope resultScope;

    public class ServerContextLookupResults extends ResultsImpl {
        private final List<ServerContext> serverContexts = new ArrayList<ServerContext>();

        public List<ServerContext> getServerContexts() {
            return Collections.unmodifiableList(serverContexts);
        }

    }

    public ServerContextLookupOperation(final List<ServerContext> contextList, final ContextScope resultScope) {
        assert contextList != null;
        assert !contextList.isEmpty();
        assert resultScope != null;

        this.contextList = new ArrayList<ServerContext>(contextList.size());
        this.contextList.addAll(contextList);
        this.resultScope = resultScope;
    }

    public void doWork(final Inputs inputs) {
        onLookupStarted();

        try {
            final boolean throwOnError = contextList.size() == 1;

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
                        } catch (Throwable t) {
                            logger.error("doWork: Unable to do lookup on context: " + context.getUri().toString());
                            logger.warn("doWork: Exception", t);

                            // If there's only one context we need to bubble the exception out
                            // But if there's more than one let's just continue
                            if (throwOnError) {
                                terminate(t);
                            }
                        }
                    }
                }));
            }

            // wait for all tasks to complete
            OperationExecutor.getInstance().wait(tasks);

            onLookupCompleted();
        } catch (Throwable ex) {
            logger.error("ServerContextLookupOperation failed with an exception", ex);
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
        doLookup(context, collections);
    }

    protected void doSoapCollectionLookup(final ServerContext context) {
        final CatalogService catalogService = context.getSoapServices().getCatalogService();
        final List<TeamProjectCollectionReference> collections = catalogService.getProjectCollections();
        doLookup(context, collections);
    }

    protected void doLookup(final ServerContext context, final List<TeamProjectCollectionReference> collections) {
        for (final TeamProjectCollectionReference teamProjectCollectionReference : collections) {
            if (isCancelled()) {
                return;
            }

            // --------- resultScope == ContextScope.PROJECT -------
            // Ideally, we would be using the following client to get the list of projects
            // But getProjects doesn't allow us to filter to just Git Team Projects, so we get the list of repos and filter to unique projects
            // -----------------------------------------------------
            //final CoreHttpClient client = new CoreHttpClient(context.getClient(), collectionURI);
            //final List<TeamProjectReference> projects = client.getProjects();
            // -----------------------------------------------------

            try {
                final URI collectionURI = UrlHelper.createUri(context.getUri().toString() + "/" + teamProjectCollectionReference.getName());
                final GitHttpClient gitClient = new GitHttpClient(context.getClient(), collectionURI);
                final List<GitRepository> gitRepositories = gitClient.getRepositories();

                addRepositoryResults(gitRepositories, context, teamProjectCollectionReference);
            } catch (VssResourceNotFoundException e) {
                logger.warn("doLookup: exception querying for Git repos", e);
                if (context.getType() == ServerContext.Type.TFS) {
                    throw new TeamServicesException(TeamServicesException.KEY_TFS_UNSUPPORTED_VERSION, e);
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected void addRepositoryResults(final List<GitRepository> gitRepositories, final ServerContext context, final TeamProjectCollectionReference teamProjectCollectionReference) {
        final List<ServerContext> serverContexts = new ArrayList<ServerContext>(gitRepositories.size());
        final Set<UUID> includedContexts = new HashSet<UUID>(gitRepositories.size());

        for (final GitRepository gitRepository : gitRepositories) {
            // If we are just looking for projects, only get the unique ones
            if (resultScope == ContextScope.PROJECT) {
                final UUID key = gitRepository.getProjectReference().getId();
                if (includedContexts.contains(key)) {
                    continue;
                } else {
                    includedContexts.add(key);
                }
            }

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
        super.onLookupResults(results);
    }
}
