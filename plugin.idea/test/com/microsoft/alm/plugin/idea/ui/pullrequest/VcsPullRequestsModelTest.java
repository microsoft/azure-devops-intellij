// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VcsPullRequestsModelTest extends IdeaAbstractTest {
    VcsPullRequestsModel underTest;

    Project projectMock;

    @Before
    public void setUp() {
        projectMock = Mockito.mock(Project.class);
    }

    @Test
    public void testObservable() {
        underTest = new VcsPullRequestsModel(projectMock);

        final Observer observerMock = Mockito.mock(Observer.class);
        underTest.addObserver(observerMock);

        underTest.setConnected(underTest.isConnected());
        verify(observerMock, never()).update(underTest, VcsPullRequestsModel.PROP_CONNECTED);
        underTest.setConnected(!underTest.isConnected());
        verify(observerMock, times(1)).update(underTest, VcsPullRequestsModel.PROP_CONNECTED);

        underTest.setLoading(underTest.isLoading());
        verify(observerMock, never()).update(underTest, VcsPullRequestsModel.PROP_LOADING);
        underTest.setLoading(!underTest.isLoading());
        verify(observerMock, times(1)).update(underTest, VcsPullRequestsModel.PROP_LOADING);

        underTest.setLastRefreshed(underTest.getLastRefreshed());
        verify(observerMock, never()).update(underTest, VcsPullRequestsModel.PROP_LAST_REFRESHED);
        underTest.setLastRefreshed(new Date());
        verify(observerMock, times(1)).update(underTest, VcsPullRequestsModel.PROP_LAST_REFRESHED);

    }

    @Test
    public void testPullRequestsTreeModel() {
        underTest = new VcsPullRequestsModel(projectMock);
        assertEquals(0, underTest.getPullRequestsTreeModel().getRequestedByMeRoot().getChildCount());

        final List<GitPullRequest> myPullRequests = new ArrayList<GitPullRequest>();
        myPullRequests.add(new GitPullRequest());
        underTest.appendPullRequests(myPullRequests, PullRequestLookupOperation.PullRequestScope.REQUESTED_BY_ME);
        assertEquals(1, underTest.getPullRequestsTreeModel().getRequestedByMeRoot().getChildCount());

        underTest.appendPullRequests(myPullRequests, PullRequestLookupOperation.PullRequestScope.ASSIGNED_TO_ME);
        assertEquals(1, underTest.getPullRequestsTreeModel().getAssignedToMeRoot().getChildCount());

        underTest.clearPullRequests();
        assertEquals(0, underTest.getPullRequestsTreeModel().getRequestedByMeRoot().getChildCount());
    }

    @Test
    public void testLoadPullRequests() {
        underTest = new VcsPullRequestsModel(projectMock);

        //setup Git repository mock
        final GitRepository gitRepositoryMock = Mockito.mock(GitRepository.class);
        final GitRemote tfsRemote = new GitRemote("origin", Arrays.asList("https://mytest.visualstudio.com/DefaultCollection/_git/testrepo"),
                Arrays.asList("https://pushurl"), Collections.<String>emptyList(), Collections.<String>emptyList());
        when(gitRepositoryMock.getRemotes()).thenReturn(Collections.singletonList(tfsRemote));

        //setup Git repository provider mock
        final VcsPullRequestsModel.GitRepositoryProvider gitRepositoryProviderMock = Mockito.mock(VcsPullRequestsModel.GitRepositoryProvider.class);
        underTest.setGitRepositoryProvider(gitRepositoryProviderMock);
        when(gitRepositoryProviderMock.getGitRepository(projectMock)).thenReturn(gitRepositoryMock);

        //Setup server context mock
        final ServerContext serverContextMock = Mockito.mock(ServerContext.class);

        //Setup server context provider mock
        final VcsPullRequestsModel.ServerContextProvider contextProviderMock = Mockito.mock(VcsPullRequestsModel.ServerContextProvider.class);
        underTest.setServerContextProvider(contextProviderMock);
        when(contextProviderMock.getAuthenticatedServerContext(gitRepositoryMock)).thenReturn(serverContextMock);

        //call loadPullRequests() and verify state
        underTest.loadPullRequests();
        assertTrue(underTest.isConnected());
        assertTrue(underTest.isLoading());
    }
}
