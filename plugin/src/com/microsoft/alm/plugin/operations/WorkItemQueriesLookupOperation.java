// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.workitemtracking.webapi.WorkItemTrackingHttpClient;
import com.microsoft.alm.workitemtracking.webapi.models.QueryExpand;
import com.microsoft.alm.workitemtracking.webapi.models.QueryHierarchyItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WorkItemQueriesLookupOperation makes a call to the server to find the queries under a specific directory
 */
public class WorkItemQueriesLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(WorkItemQueriesLookupOperation.class);

    private final RepositoryContext repositoryContext;

    // the different root query directories that are standard for users
    public enum QueryRootDirectories {
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

    public WorkItemQueriesLookupOperation(final RepositoryContext repositoryContext) {
        logger.info("WorkItemQueriesLookupOperation created");
        ArgumentHelper.checkNotNull(repositoryContext, "repositoryContext");
        this.repositoryContext = repositoryContext;
    }

    public void doWork(final Inputs inputs) {
        try {
            logger.info("WorkItemQueriesLookupOperation.doWork()");
            ArgumentHelper.checkNotNull(inputs, "inputs");

            // Trigger the started event
            onLookupStarted();

            // Get the server context object
            final ServerContext context = Operation.getServerContext(repositoryContext, false, false, logger);

            // Do the lookup
            doLookup(context, (QueryInputs) inputs);

            // Trigger the completed event
            onLookupCompleted();
        } catch (Throwable t) {
            // If any errors happen we need to terminate the operation
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