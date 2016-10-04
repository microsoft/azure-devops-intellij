// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.workitemtracking.webapi.WorkItemTrackingHttpClient;
import com.microsoft.alm.workitemtracking.webapi.models.Wiql;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItem;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItemExpand;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItemQueryResult;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItemReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkItemLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(WorkItemLookupOperation.class);

    // The WIT REST API restricts us to getting 200 work items at a time.
    public static final int MAX_WORK_ITEM_COUNT = 200;

    private final RepositoryContext repositoryContext;

    public static class WitInputs extends CredInputsImpl {
        private final String query;
        private final FieldList fields;
        private final WorkItemExpand expand;

        /**
         * Constructor for WitInputs with a field's parameter to only return the specified fields of the work item
         *
         * @param query
         * @param fields
         */
        public WitInputs(final String query, final List<String> fields) {
            ArgumentHelper.checkNotNull(query, "query");
            ArgumentHelper.checkNotNull(fields, "fields");
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
        public WitInputs(final String query) {
            ArgumentHelper.checkNotNull(query, "query");
            this.query = query;
            this.fields = null;
            this.expand = WorkItemExpand.ALL;
        }

        public String getQuery() {
            return query;
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

    public WorkItemLookupOperation(final RepositoryContext repositoryContext) {
        logger.info("WorkItemLookupOperation created.");
        ArgumentHelper.checkNotNull(repositoryContext, "repositoryContext");
        this.repositoryContext = repositoryContext;
    }

    public void doWork(final Inputs inputs) {
        try {
            logger.info("WorkItemLookupOperation.doWork()");
            ArgumentHelper.checkNotNull(inputs, "inputs");
            onLookupStarted();
            final boolean allowPrompt = ((CredInputsImpl) inputs).getPromptForCreds();
            final ServerContext latestServerContext = Operation.getServerContext(repositoryContext, false, allowPrompt, logger);

            // Send results with the new context (no work items)
            onLookupResults(new WitResults(latestServerContext, new ArrayList<WorkItem>()));

            // Get the actual work items
            doLookup(latestServerContext, (WitInputs) inputs);

            // let listeners know that we are done
            onLookupCompleted();
        } catch (Throwable t) {
            logger.warn("doWork: failed with an exception", t);
            terminate(t);
        }
    }

    protected void doLookup(final ServerContext context, final WitInputs witInputs) {
        final WorkItemTrackingHttpClient witHttpClient = context.getWitHttpClient();

        // query server and add results
        Wiql wiql = new Wiql();
        wiql.setQuery(witInputs.query);
        logger.debug("WorkItemLookupOperation.doLookup() wiql " + wiql.getQuery());
        WorkItemQueryResult result = witHttpClient.queryByWiql(wiql, context.getTeamProjectReference().getId());

        int count = 0;
        final List<WorkItemReference> itemRefs = result.getWorkItems();
        final int maxCount = Math.min(itemRefs.size(), MAX_WORK_ITEM_COUNT);
        logger.info("WorkItemLookupOperation.doLookup() found " + maxCount);
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

        final List<WorkItem> items = witHttpClient.getWorkItems(ids, witInputs.fields, null, witInputs.expand);
        logger.debug("doLookup: Found {} work items on repo {}", items.size(), repositoryContext.getUrl());

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
    }

    @Override
    protected void terminate(final Throwable t) {
        super.terminate(t);

        final WitResults results = new WitResults(null, new ArrayList<WorkItem>());
        results.error = t;
        onLookupResults(results);
        onLookupCompleted();
    }

    protected static class IDList extends ArrayList<Integer> {
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

    protected static class FieldList extends ArrayList<String> {
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