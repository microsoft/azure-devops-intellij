// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.workitemtracking.webapi.WorkItemTrackingHttpClient;
import com.microsoft.alm.workitemtracking.webapi.models.QueryExpand;
import com.microsoft.alm.workitemtracking.webapi.models.QueryHierarchyItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

/**
 * WorkItemQueriesLookupOperation makes a call to the server to find the queries under a specific directory
 */
public class WorkItemQueriesLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(WorkItemQueriesLookupOperation.class);

    private final String gitRemoteUrl;

    // the different root query directories that are standard for users
    public enum QueryRootDirectories{
        MY_QUERIES,
        SHARED_QUERIES
    }

    public static class QueryInputs implements Inputs {
        private final QueryRootDirectories directory;

        /**
         * Constructor for QueryInputs that takes a specific root directory to search under
         *
         * @param directory
         */
        public QueryInputs(final QueryRootDirectories directory) {
            this.directory = directory;
        }
    }

    /**
     * Constructor for QueryResults that shows the output of the server call along with what inputs were used to obtain the results
     */
    public class QueryResults extends ResultsImpl {
        private final List<QueryHierarchyItem> queries;
        private final QueryInputs inputs;

        public QueryResults(final List<QueryHierarchyItem> queries, final QueryInputs inputs) {
            this.queries = queries;
            this.inputs = inputs;
        }

        public List<QueryHierarchyItem> getQueries() {
            return Collections.unmodifiableList(queries);
        }

        public QueryInputs getInputs() {
            return inputs;
        }
    }

    public WorkItemQueriesLookupOperation(final String gitRemoteUrl) {
        logger.info("WorkItemQueriesLookupOperation created");
        ArgumentHelper.checkNotEmptyString(gitRemoteUrl);
        this.gitRemoteUrl = gitRemoteUrl;
    }

    public void doWork(final Inputs inputs) {
        logger.info("WorkItemQueriesLookupOperation.doWork()");
        ArgumentHelper.checkNotNull(inputs, "inputs");
        onLookupStarted();

        final List<ServerContext> authenticatedContexts = new ArrayList<ServerContext>();
        //TODO: get rid of the calls that create more background tasks unless they run in parallel
        final List<Future> authTasks = new ArrayList<Future>();
        try {
            // TODO: refactor this into a common class
            authTasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                @Override
                public void run() {
                    // Get the authenticated context for the gitRemoteUrl
                    // This should be done on a background thread so as not to block UI or hang the IDE
                    // Get the context before doing the server calls to reduce possibility of using an outdated context with expired credentials
                    final ServerContext context = ServerContextManager.getInstance().getUpdatedContext(gitRemoteUrl, false);
                    if (context != null) {
                        authenticatedContexts.add(context);
                    }
                }
            }));
            OperationExecutor.getInstance().wait(authTasks);
        } catch (Throwable t) {
            logger.warn("doWork: failed to get authenticated server context", t);
            terminate(new NotAuthorizedException(gitRemoteUrl));
        }

        if (authenticatedContexts == null || authenticatedContexts.size() != 1) {
            //no context was found, user might have cancelled
            terminate(new NotAuthorizedException(gitRemoteUrl));
        }

        final ServerContext context = authenticatedContexts.get(0);
        final List<Future> lookupTasks = new ArrayList<Future>();
        try {
            lookupTasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                @Override
                public void run() {
                    doLookup(context, (QueryInputs) inputs);
                }
            }));
            OperationExecutor.getInstance().wait(lookupTasks);
            onLookupCompleted();
        } catch (Throwable t) {
            logger.warn("doWork: failed with an exception", t);
            terminate(t);
        }

    }

    protected void doLookup(final ServerContext context, final QueryInputs inputs) {
        logger.info("WorkItemQueriesLookupOperation.doLookup()");
        try {
            final WorkItemTrackingHttpClient witHttpClient = context.getWitHttpClient();
            final List<QueryHierarchyItem> rootDirectories = witHttpClient.getQueries(context.getTeamProjectReference().getId(), QueryExpand.WIQL, 1, false);

            final List<QueryHierarchyItem> queries = new ArrayList<QueryHierarchyItem>();
            for (QueryHierarchyItem directory : rootDirectories) {
                // If My Queries is the input then according to the WIT team check for if it's a directory
                // that is not public and has children (do not check name due to localization).
                // The other option is Shared Queries which would be public with children.
                // TODO Consider making the enum a flags enum and allowing the user to get both root directories at the same time (right now you have to specify one or the other)
                if (inputs.directory == QueryRootDirectories.MY_QUERIES
                        && directory.isFolder() && !directory.isPublic() && directory.getHasChildren()) {
                    queries.addAll(directory.getChildren());
                } else if (inputs.directory == QueryRootDirectories.SHARED_QUERIES
                        && directory.isFolder() && directory.isPublic() && directory.getHasChildren()) {
                    queries.addAll(directory.getChildren());
                }
            }

            super.onLookupResults(new QueryResults(queries, inputs));
        } catch (Throwable t) {
            logger.warn("doLookup: failed with an exception", t);
            terminate(t);
        }
    }

    @Override
    protected void terminate(final Throwable t) {
        super.terminate(t);

        final QueryResults results = new QueryResults(new ArrayList<QueryHierarchyItem>(), null);
        results.error = t;
        onLookupResults(results);
        onLookupCompleted();
    }
}