// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VcsHelper.class})
public class VcsPullRequestsModelTest extends IdeaAbstractTest {
    VcsPullRequestsModel underTest;

    Project projectMock;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(VcsHelper.class);
        when(VcsHelper.getRepositoryContext(any(Project.class))).thenReturn(RepositoryContext.createGitContext("repo1", "branch1", "repoUrl1"));

        projectMock = Mockito.mock(Project.class);
    }

    @Test
    public void testPullRequestsTreeModel() {
        PullRequestLookupOperation.PullRequestLookupResults results = mock(PullRequestLookupOperation.PullRequestLookupResults.class);

        underTest = new VcsPullRequestsModel(projectMock);
        assertEquals(0, underTest.getModelForView().getRequestedByMeRoot().getChildCount());

        final List<GitPullRequest> myPullRequests = new ArrayList<GitPullRequest>();
        myPullRequests.add(new GitPullRequest());

        when(results.getPullRequests()).thenReturn(myPullRequests);
        when(results.getScope()).thenReturn(PullRequestLookupOperation.PullRequestScope.REQUESTED_BY_ME);
        underTest.appendData(results);
        assertEquals(1, underTest.getModelForView().getRequestedByMeRoot().getChildCount());

        when(results.getPullRequests()).thenReturn(myPullRequests);
        when(results.getScope()).thenReturn(PullRequestLookupOperation.PullRequestScope.ASSIGNED_TO_ME);
        underTest.appendData(results);
        assertEquals(1, underTest.getModelForView().getAssignedToMeRoot().getChildCount());

        underTest.clearData();
        assertEquals(0, underTest.getModelForView().getRequestedByMeRoot().getChildCount());
    }

    @Test
    public void testGetOperationInputs_DefaultValue() {
        underTest = new VcsPullRequestsModel(projectMock);
        assertEquals(true, underTest.getOperationInputs().getPromptForCreds());
    }
}
