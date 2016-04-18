// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.teamfoundation.workitemtracking.webapi.WorkItemTrackingHttpClient;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.Wiql;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.WorkItem;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.WorkItemExpand;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.WorkItemQueryResult;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.WorkItemReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class WorkItemLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(WorkItemLookupOperation.class);

    // The WIT REST API restricts us to getting 200 work items at a time.
    public static final int MAX_WORK_ITEM_COUNT = 200;

    private final String gitRemoteUrl;

    public static class WitInputs implements Inputs {
        private final String query;
        private final FieldList fields;
        private final WorkItemExpand expand;

        /**
         * Constructor for WitInputs with a field's parameter to only return the specified fields of the work item
         *
         * @param query
         * @param fields
         */
        public WitInputs(String query, List<String> fields) {
            assert query != null;
            assert fields != null;
            this.query = query;
            this.fields = new FieldList();
            this.fields.addAll(fields);
            this.expand = WorkItemExpand.NONE;
        }

        /**
         * Constructor for WitInputs that will return all information of the work item
         *
         * @param query
         */
        public WitInputs(String query) {
            assert query != null;
            this.query = query;
            this.fields = null;
            this.expand = WorkItemExpand.ALL;
        }
    }

    public class WitResults extends ResultsImpl {
        private final List<WorkItem> workItems;
        private final ServerContext context;

        public WitResults(final ServerContext context, final List<WorkItem> workItems) {
            assert workItems != null;
            this.workItems = workItems;
            // The context could be null if an error occurred
            this.context = context;
        }

        public boolean maxItemsReached() {
            return workItems.size() >= MAX_WORK_ITEM_COUNT;
        }

        public List<WorkItem> getWorkItems() {
            return Collections.unmodifiableList(workItems);
        }

        public ServerContext getContext() {
            return context;
        }
    }

    public WorkItemLookupOperation(final String gitRemoteUrl) {
        assert gitRemoteUrl != null;
        this.gitRemoteUrl = gitRemoteUrl;
    }

    public void doWork(final Inputs inputs) {
        onLookupStarted();

        final List<ServerContext> authenticatedContexts = new ArrayList<ServerContext>();
        final List<Future> authTasks = new ArrayList<Future>();
        try {
            authTasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                @Override
                public void run() {
                    // Get the authenticated context for the gitRemoteUrl
                    // This should be done on a background thread so as not to block UI or hang the IDE
                    // Get the context before doing the server calls to reduce possibility of using an outdated context with expired credentials
                    final ServerContext context = ServerContextManager.getInstance().getAuthenticatedContext(gitRemoteUrl, false);
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

        final ServerContext latestServerContext = authenticatedContexts.get(0);
        final List<Future> lookupTasks = new ArrayList<Future>();
        final WitInputs witInputs = (WitInputs) inputs;

        try {
            lookupTasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                @Override
                public void run() {
                    // Send results with the new context (no work items)
                    onLookupResults(new WitResults(latestServerContext, new ArrayList<WorkItem>()));

                    // Get the actual work items
                    doLookup(latestServerContext, witInputs);
                }
            }));
            OperationExecutor.getInstance().wait(lookupTasks);
            onLookupCompleted();
        } catch (Throwable t) {
            logger.warn("doWork: failed with an exception", t);
            terminate(t);
        }
    }

    protected void doLookup(final ServerContext context, final WitInputs witInputs) {
        try {
            final WorkItemTrackingHttpClient witHttpClient = context.getWitHttpClient();

            // query server and add results
            Wiql wiql = new Wiql();
            wiql.setQuery(witInputs.query);
            WorkItemQueryResult result = witHttpClient.queryByWiql(wiql, context.getTeamProjectReference().getId());

            int count = 0;
            final List<WorkItemReference> itemRefs = result.getWorkItems();
            final int maxCount = Math.min(itemRefs.size(), MAX_WORK_ITEM_COUNT);
            if (maxCount == 0) {
                return; //no workitem ids matched the wiql
            }

            final List<Integer> ids = new IDList(maxCount);
            final Map<Integer, Integer> workItemOrderMap = new HashMap<Integer, Integer>(maxCount);
            for (WorkItemReference itemRef : itemRefs) {
                ids.add(itemRef.getId());
                workItemOrderMap.put(itemRef.getId(), count);
                count++;
                if (count >= MAX_WORK_ITEM_COUNT) {
                    break;
                }
            }

            final List<WorkItem> items = witHttpClient.getWorkItems(ids, witInputs.fields, result.getAsOf(), witInputs.expand);
            logger.debug("doLookup: Found {} work items on repo {}", items.size(), context.getGitRepository().getRemoteUrl());

            // Correct the order of the work items. The second call here to get the work items,
            // always returns them in id order. We need to use the map we created above to put
            // them back into the correct order based on the query.
            Collections.sort(items, new Comparator<WorkItem>() {
                @Override
                public int compare(final WorkItem wi1, final WorkItem wi2) {
                    Integer index1 = workItemOrderMap.get(wi1.getId());
                    Integer index2 = workItemOrderMap.get(wi2.getId());
                    if (index1 != null && index2 != null) {
                        return index1 - index2;
                    } else if (index1 != null) {
                        return -1;
                    } else if (index2 != null) {
                        return 1;
                    }

                    return 0;
                }
            });

            super.onLookupResults(new WitResults(context, items));
        } catch (Throwable t) {
            logger.warn("doLookup: failed with an exception", t);
            terminate(t);
        }
    }

    @Override
    protected void terminate(final Throwable t) {
        super.terminate(t);

        final WitResults results = new WitResults(null, new ArrayList<WorkItem>());
        results.error = t;
        onLookupResults(results);
        onLookupCompleted();
    }

    private static class IDList extends ArrayList<Integer> {
        public IDList(int initialCapacity) {
            super(initialCapacity);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size(); i++) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(get(i));
            }
            return sb.toString();
        }
    }

    private static class FieldList extends ArrayList<String> {
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size(); i++) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(get(i));
            }
            return sb.toString();
        }
    }
}
