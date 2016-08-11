// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.alm.workitemtracking.webapi.WorkItemTrackingHttpClient;
import com.microsoft.alm.workitemtracking.webapi.models.QueryExpand;
import com.microsoft.alm.workitemtracking.webapi.models.QueryHierarchyItem;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItemQueryResult;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItemReference;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServerContextManager.class})
public class WorkItemQueriesLookupOperationTest extends AbstractTest {
    private ServerContextManager serverContextManager;
    private RepositoryContext defaultRepositoryContext =
            RepositoryContext.createGitContext("/root/one", "repo1", "branch1", "http://one.vs.com/_git/repo1");

    private void setupLocalTests(List<QueryHierarchyItem> queries) {
        MockitoAnnotations.initMocks(this);

        WorkItemReference ref = new WorkItemReference();
        ref.setId(1);
        List<WorkItemReference> workItemRefs = new ArrayList<WorkItemReference>();
        workItemRefs.add(ref);
        WorkItemQueryResult result = new WorkItemQueryResult();
        result.setWorkItems(workItemRefs);

        WorkItemTrackingHttpClient witHttpClient = Mockito.mock(WorkItemTrackingHttpClient.class);
        when(witHttpClient.getQueries(any(UUID.class), any(QueryExpand.class), Matchers.eq(1), Matchers.eq(false)))
                .thenReturn(queries);

        AuthenticationInfo authInfo = new AuthenticationInfo("user", "pass", "serverURI", "user");
        ServerContext authenticatedContext = Mockito.mock(ServerContext.class);
        when(authenticatedContext.getWitHttpClient()).thenReturn(witHttpClient);
        when(authenticatedContext.getTeamProjectReference()).thenReturn(new TeamProjectReference());
        when(authenticatedContext.getGitRepository()).thenReturn(new GitRepository());

        serverContextManager = Mockito.mock(ServerContextManager.class);
        when(serverContextManager.getAuthenticatedContext(anyString(), anyBoolean())).thenReturn(authenticatedContext);
        when(serverContextManager.getUpdatedContext(anyString(), anyBoolean())).thenReturn(authenticatedContext);
        when(serverContextManager.createContextFromGitRemoteUrl(anyString(), anyBoolean())).thenReturn(authenticatedContext);

        PowerMockito.mockStatic(ServerContextManager.class);
        when(ServerContextManager.getInstance()).thenReturn(serverContextManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullUrl() {
        WorkItemQueriesLookupOperation operation = new WorkItemQueriesLookupOperation(null);
    }

    @Test
    public void testConstructor_goodInputs() {
        WorkItemQueriesLookupOperation operation = new WorkItemQueriesLookupOperation(defaultRepositoryContext);
    }

    @Test
    public void testDoWork_myQueries() throws InterruptedException, ExecutionException, TimeoutException {
        TestData data = new TestData();
        setupLocalTests(getRootDirectories(3, 3));

        WorkItemQueriesLookupOperation operation = new WorkItemQueriesLookupOperation(defaultRepositoryContext);
        data.setupListener(operation);
        operation.doWork(new WorkItemQueriesLookupOperation.QueryInputs(WorkItemQueriesLookupOperation.QueryRootDirectories.MY_QUERIES));

        Assert.assertTrue(data.startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(data.completedCalled.get(1, TimeUnit.SECONDS));
        WorkItemQueriesLookupOperation.QueryResults results = data.witResults.get(1, TimeUnit.SECONDS);
        Assert.assertNull(results.getError());
        Assert.assertEquals(3, results.getQueries().size());
        Assert.assertEquals("my-0", results.getQueries().get(0).getName());
        Assert.assertEquals("my-1", results.getQueries().get(1).getName());
        Assert.assertEquals("my-2", results.getQueries().get(2).getName());
    }

    @Test
    public void testDoWork_sharedQueries() throws InterruptedException, ExecutionException, TimeoutException {
        TestData data = new TestData();
        setupLocalTests(getRootDirectories(3, 3));

        WorkItemQueriesLookupOperation operation = new WorkItemQueriesLookupOperation(defaultRepositoryContext);
        data.setupListener(operation);
        operation.doWork(new WorkItemQueriesLookupOperation.QueryInputs(WorkItemQueriesLookupOperation.QueryRootDirectories.SHARED_QUERIES));

        Assert.assertTrue(data.startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(data.completedCalled.get(1, TimeUnit.SECONDS));
        WorkItemQueriesLookupOperation.QueryResults results = data.witResults.get(1, TimeUnit.SECONDS);
        Assert.assertNull(results.getError());
        Assert.assertEquals(3, results.getQueries().size());
        Assert.assertEquals("shared-0", results.getQueries().get(0).getName());
        Assert.assertEquals("shared-1", results.getQueries().get(1).getName());
        Assert.assertEquals("shared-2", results.getQueries().get(2).getName());
    }

    @Test
    public void testDoWork_noQueries() throws InterruptedException, ExecutionException, TimeoutException {
        TestData data = new TestData();
        setupLocalTests(getRootDirectories(0, 0));

        WorkItemQueriesLookupOperation operation = new WorkItemQueriesLookupOperation(defaultRepositoryContext);
        data.setupListener(operation);
        operation.doWork(new WorkItemQueriesLookupOperation.QueryInputs(WorkItemQueriesLookupOperation.QueryRootDirectories.MY_QUERIES));

        Assert.assertTrue(data.startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(data.completedCalled.get(1, TimeUnit.SECONDS));
        WorkItemQueriesLookupOperation.QueryResults results = data.witResults.get(1, TimeUnit.SECONDS);
        Assert.assertNull(results.getError());
        Assert.assertEquals(0, results.getQueries().size());
    }

    @Test
    public void testDoWork_aTreeOfQueries() throws InterruptedException, ExecutionException, TimeoutException {
        TestData data = new TestData();
        List<QueryHierarchyItem> rootDirectories = getRootDirectories(3, 3);
        rootDirectories.get(0).getChildren().add(getQueryFolder("folder1", true, 3, "folder1-"));
        rootDirectories.get(1).getChildren().add(getQueryFolder("folder2", true, 3, "folder2-"));
        setupLocalTests(rootDirectories);

        WorkItemQueriesLookupOperation operation = new WorkItemQueriesLookupOperation(defaultRepositoryContext);
        data.setupListener(operation);
        operation.doWork(new WorkItemQueriesLookupOperation.QueryInputs(WorkItemQueriesLookupOperation.QueryRootDirectories.SHARED_QUERIES));

        Assert.assertTrue(data.startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(data.completedCalled.get(1, TimeUnit.SECONDS));
        WorkItemQueriesLookupOperation.QueryResults results = data.witResults.get(1, TimeUnit.SECONDS);
        Assert.assertNull(results.getError());
        Assert.assertEquals(4, results.getQueries().size());
        Assert.assertEquals("shared-0", results.getQueries().get(0).getName());
        Assert.assertEquals("shared-1", results.getQueries().get(1).getName());
        Assert.assertEquals("shared-2", results.getQueries().get(2).getName());
        Assert.assertEquals("folder2", results.getQueries().get(3).getName());
        Assert.assertEquals(3, results.getQueries().get(3).getChildren().size());
        Assert.assertEquals("folder2-0", results.getQueries().get(3).getChildren().get(0).getName());
        Assert.assertEquals("folder2-1", results.getQueries().get(3).getChildren().get(1).getName());
        Assert.assertEquals("folder2-2", results.getQueries().get(3).getChildren().get(2).getName());
    }

    @Test
    public void testDoWork_failure() throws InterruptedException, ExecutionException, TimeoutException {
        TestData data = new TestData();
        setupLocalTests(null);

        WorkItemQueriesLookupOperation operation = new WorkItemQueriesLookupOperation(defaultRepositoryContext);
        data.setupListener(operation);
        operation.doWork(new WorkItemQueriesLookupOperation.QueryInputs(WorkItemQueriesLookupOperation.QueryRootDirectories.MY_QUERIES));

        Assert.assertTrue(data.startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(data.completedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertEquals(NullPointerException.class, data.witResults.get(1, TimeUnit.SECONDS).getError().getClass());
    }

    private class TestData {
        public final SettableFuture<Boolean> startedCalled;
        public final SettableFuture<Boolean> completedCalled;
        public final SettableFuture<WorkItemQueriesLookupOperation.QueryResults> witResults;

        public TestData() {
            this.startedCalled = SettableFuture.create();
            this.completedCalled = SettableFuture.create();
            this.witResults = SettableFuture.create();
        }

        public void setupListener(WorkItemQueriesLookupOperation operation) {
            operation.addListener(new Operation.Listener() {
                @Override
                public void notifyLookupStarted() {
                    startedCalled.set(true);
                }

                @Override
                public void notifyLookupCompleted() {
                    completedCalled.set(true);
                }

                @Override
                public void notifyLookupResults(Operation.Results results) {
                    witResults.set((WorkItemQueriesLookupOperation.QueryResults) results);
                }
            });
        }
    }


    private List<QueryHierarchyItem> getRootDirectories(int numMyQueries, int numSharedQueries) {
        final List<QueryHierarchyItem> directories = new ArrayList<QueryHierarchyItem>();
        directories.add(getQueryFolder("My Queries", false, numMyQueries, "my-"));
        directories.add(getQueryFolder("Shared Queries", true, numMyQueries, "shared-"));
        return directories;
    }

    private QueryHierarchyItem getQueryFolder(String name, boolean isPublic, int numChildren, String queryPrefix) {
        final List<QueryHierarchyItem> queries = new ArrayList<QueryHierarchyItem>();
        for (int i = 0; i < numChildren; i++) {
            queries.add(getQueryHierarchyItem(queryPrefix + i, false, true));
        }
        final QueryHierarchyItem queryFolder = getQueryHierarchyItem(name, true, isPublic);
        queryFolder.setChildren(queries);
        queryFolder.setHasChildren(queries.size() > 0);
        return queryFolder;
    }

    private QueryHierarchyItem getQueryHierarchyItem(String name, boolean isFolder, boolean isPublic) {
        final QueryHierarchyItem query = new QueryHierarchyItem();
        query.setName(name);
        query.setId(UUID.randomUUID());
        query.setFolder(isFolder);
        query.setPublic(isPublic);
        return query;
    }
}
