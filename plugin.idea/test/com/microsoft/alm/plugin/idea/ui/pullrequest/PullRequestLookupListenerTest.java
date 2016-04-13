// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PullRequestLookupListenerTest extends IdeaAbstractTest {
    PullRequestsLookupListener underTest;
    VcsPullRequestsModel modelMock;
    ServerContext contextMock;

    @Before
    public void setUp() {
        modelMock = Mockito.mock(VcsPullRequestsModel.class);
        contextMock = Mockito.mock(ServerContext.class);
        underTest = new PullRequestsLookupListener(modelMock);
    }

    @Test
    public void testNotifyLookupStarted() {
        underTest.notifyLookupStarted();
        verify(modelMock).setTabStatus(VcsTabStatus.LOADING_IN_PROGRESS);
    }

    @Test
    public void testNotifyLookupCompleted() {
        when(modelMock.getTabStatus()).thenReturn(VcsTabStatus.LOADING_IN_PROGRESS);
        underTest.notifyLookupCompleted();
        verify(modelMock).setTabStatus(VcsTabStatus.LOADING_COMPLETED);
    }

    @Test
    public void testNotifyLookupResults() {
        PullRequestLookupOperation.PullRequestLookupResults resultsMock = Mockito.mock(PullRequestLookupOperation.PullRequestLookupResults.class);
        underTest.notifyLookupResults(resultsMock);
        verify(resultsMock).isCancelled();
        verify(resultsMock).getPullRequests();
        verify(resultsMock).getScope();
        verify(modelMock).appendPullRequests(anyList(), any(PullRequestLookupOperation.PullRequestScope.class));
    }

    @Test
    public void testLookupCancelled() {
        PullRequestLookupOperation.PullRequestLookupResults resultsMock = Mockito.mock(PullRequestLookupOperation.PullRequestLookupResults.class);
        when(resultsMock.isCancelled()).thenReturn(true);
        underTest.notifyLookupResults(resultsMock);
        verify(resultsMock).isCancelled();
        verify(modelMock).setTabStatus(VcsTabStatus.LOADING_COMPLETED);
    }
}
