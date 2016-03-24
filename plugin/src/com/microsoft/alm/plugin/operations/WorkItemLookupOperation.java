// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.teamfoundation.workitemtracking.webapi.WorkItemTrackingHttpClient;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.Wiql;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.WorkItem;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.WorkItemExpand;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.WorkItemQueryResult;
import com.microsoft.teamfoundation.workitemtracking.webapi.models.WorkItemReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

public class WorkItemLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(WorkItemLookupOperation.class);
    private final ServerContext context;

    public static class WitInputs implements Inputs {
        private final String query;
        private final FieldList fields = new FieldList();

        public WitInputs(String query, List<String> fields) {
            assert query != null;
            assert fields != null;
            this.query = query;
            this.fields.addAll(fields);
        }
    }

    public class WitResults extends ResultsImpl {
        private final List<WorkItem> workItems;

        public WitResults(final List<WorkItem> workItems) {
            assert workItems != null;
            this.workItems = workItems;
        }

        public List<WorkItem> getWorkItems() {
            return Collections.unmodifiableList(workItems);
        }
    }

    public WorkItemLookupOperation(final ServerContext context) {
        assert context != null;
        this.context = context;
    }

    public void doWork(final Inputs inputs) {
        onLookupStarted();

        final List<Future> tasks = new ArrayList<Future>();
        final WitInputs witInputs = (WitInputs) inputs;

        try {
            tasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                @Override
                public void run() {
                    doLookup(context, witInputs);
                }
            }));
            OperationExecutor.getInstance().wait(tasks);
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
            WorkItemQueryResult result = witHttpClient.queryByWiql(wiql);

            List<WorkItemReference> itemRefs = result.getWorkItems();
            List<Integer> ids = new IDList(itemRefs.size());
            for (WorkItemReference itemRef : itemRefs) {
                ids.add(itemRef.getId());
            }

            List<WorkItem> items = witHttpClient.getWorkItems(ids, witInputs.fields, result.getAsOf(), WorkItemExpand.NONE);
            logger.debug("doLookup: Found {} work items on repo {}", items.size(), context.getGitRepository().getRemoteUrl());

            super.onLookupResults(new WitResults(items));
        } catch (Throwable t) {
            logger.warn("doLookup: failed with an exception", t);
            terminate(t);
        }
    }

    @Override
    protected void terminate(final Throwable t) {
        super.terminate(t);

        final WitResults results = new WitResults(new ArrayList<WorkItem>());
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
            for(int i = 0; i < size(); i++) {
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
            for(int i = 0; i < size(); i++) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(get(i));
            }
            return sb.toString();
        }
    }
}
