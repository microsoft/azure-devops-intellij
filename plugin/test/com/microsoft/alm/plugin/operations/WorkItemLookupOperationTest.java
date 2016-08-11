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
import com.microsoft.alm.workitemtracking.webapi.models.Wiql;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItem;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItemExpand;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItemQueryResult;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItemReference;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServerContextManager.class})
public class WorkItemLookupOperationTest extends AbstractTest {
    private ServerContextManager serverContextManager;

    private void setupLocalTests(List<WorkItem> workItems) {
        MockitoAnnotations.initMocks(this);

        WorkItemReference ref = new WorkItemReference();
        ref.setId(1);
        List<WorkItemReference> workItemRefs = new ArrayList<WorkItemReference>();
        workItemRefs.add(ref);
        WorkItemQueryResult result = new WorkItemQueryResult();
        result.setWorkItems(workItemRefs);

        WorkItemTrackingHttpClient witHttpClient = Mockito.mock(WorkItemTrackingHttpClient.class);
        when(witHttpClient.queryByWiql(any(Wiql.class), any(UUID.class)))
                .thenReturn(result);
        when(witHttpClient.getWorkItems(anyList(), anyList(), any(Date.class), any(WorkItemExpand.class)))
                .thenReturn(workItems);

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

    @Test
    public void testConstructor() {
        try {
            WorkItemLookupOperation operation = new WorkItemLookupOperation(null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            //expected when ServerContext is null
        }

        //construct correctly
        WorkItemLookupOperation operation1 = new WorkItemLookupOperation(RepositoryContext.createGitContext("/root/one", "repo1", "branch1", "gitRemoteUrl"));
    }

    @Test
    public void testDoWork() throws InterruptedException, ExecutionException, TimeoutException {
        List<WorkItem> workItems = new ArrayList<WorkItem>();
        WorkItem item = new WorkItem();
        item.setId(1);
        item.setRev(1);
        workItems.add(item);
        setupLocalTests(workItems);

        WorkItemLookupOperation operation = new WorkItemLookupOperation(RepositoryContext.createGitContext("/root/one", "repo1", "branch1", "gitRemoteUrl"));
        final SettableFuture<Boolean> startedCalled = SettableFuture.create();
        final SettableFuture<Boolean> completedCalled = SettableFuture.create();
        final SettableFuture<WorkItemLookupOperation.WitResults> witResults = SettableFuture.create();
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
                if (!results.hasError() && ((WorkItemLookupOperation.WitResults) results).getWorkItems().size() == 0) {
                    // Skip the initial results call if there aren't items, we don't care about this one
                    return;
                }
                witResults.set((WorkItemLookupOperation.WitResults) results);
            }
        });
        operation.doWork(new WorkItemLookupOperation.WitInputs("query"));
        Assert.assertTrue(startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(completedCalled.get(1, TimeUnit.SECONDS));
        WorkItemLookupOperation.WitResults results = witResults.get(1, TimeUnit.SECONDS);
        Assert.assertNull(results.getError());
        Assert.assertEquals(1, results.getWorkItems().size());
    }

    @Test
    public void testDoWork_failure() throws InterruptedException, ExecutionException, TimeoutException {
        setupLocalTests(null);

        WorkItemLookupOperation operation = new WorkItemLookupOperation(RepositoryContext.createGitContext("/root/one", "repo1", "branch1", "gitRemoteUrl"));
        final SettableFuture<Boolean> startedCalled = SettableFuture.create();
        final SettableFuture<Boolean> completedCalled = SettableFuture.create();
        final SettableFuture<WorkItemLookupOperation.WitResults> witResults = SettableFuture.create();
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
                if (!results.hasError() && ((WorkItemLookupOperation.WitResults) results).getWorkItems().size() == 0) {
                    // Skip the initial results call if there aren't items, we don't care about this one
                    return;
                }
                witResults.set((WorkItemLookupOperation.WitResults) results);
            }
        });
        operation.doWork(new WorkItemLookupOperation.WitInputs("query"));
        Assert.assertTrue(startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(completedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertEquals(NullPointerException.class, witResults.get(1, TimeUnit.SECONDS).getError().getClass());
    }

    @Test
    public void testFieldList() {
        WorkItemLookupOperation.FieldList list = new WorkItemLookupOperation.FieldList();
        Assert.assertEquals("", list.toString());
        list.add("one");
        Assert.assertEquals("one", list.toString());
        list.add("two");
        Assert.assertEquals("one,two", list.toString());
        list.add("three");
        Assert.assertEquals("one,two,three", list.toString());
    }

    @Test
    public void testIDList() {
        WorkItemLookupOperation.IDList list = new WorkItemLookupOperation.IDList(3);
        Assert.assertEquals("", list.toString());
        list.add(1);
        Assert.assertEquals("1", list.toString());
        list.add(2);
        Assert.assertEquals("1,2", list.toString());
        list.add(3);
        Assert.assertEquals("1,2,3", list.toString());
    }

}
