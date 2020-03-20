// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.workitem;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.git.ui.branch.CreateBranchController;
import com.microsoft.alm.plugin.operations.WorkItemLookupOperation;
import com.microsoft.alm.workitemtracking.webapi.WorkItemTrackingHttpClient;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItem;
import git4idea.repo.GitRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CreateBranchController.class, VcsWorkItemsModel.class, VcsHelper.class})
public class VcsWorkItemsModelTest extends IdeaAbstractTest {
    private VcsWorkItemsModel model;
    private WorkItemLookupOperation operation;

    @Mock
    private Project mockProject;
    @Mock
    private GitRepository mockGitRepository;
    @Mock
    private WorkItemsTableModel mockTableModel;
    @Mock
    private CreateBranchController mockCreateBranchController;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(VcsHelper.class);
        when(VcsHelper.getRepositoryContext(any(Project.class))).thenReturn(
                RepositoryContext.createGitContext("/root/one", "repo1", "branch1", URI.create("http://repoUrl1")));

        model = new VcsWorkItemsModel(mockProject);
        operation = new WorkItemLookupOperation(
                RepositoryContext.createGitContext("/root/one", "repo1", "branch1", URI.create("http://remoteURL")));
    }

    @Test
    public void testAppendData() {
        model.appendData(createResults(5, 0));
        Assert.assertEquals(5, model.getModelForView().getRowCount());
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(i, model.getModelForView().getWorkItem(i).getId());
        }

        model.appendData(createResults(5, 5));
        Assert.assertEquals(10, model.getModelForView().getRowCount());
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(i, model.getModelForView().getWorkItem(i).getId());
        }
    }

    @Test
    public void testClearData() {
        model.appendData(createResults(5, 0));
        Assert.assertEquals(5, model.getModelForView().getRowCount());
        model.clearData();
        Assert.assertEquals(0, model.getModelForView().getRowCount());
    }

    //TODO: FLAKY TEST
    //TODO: This test calls createBranch which does work on another thread. We should be waiting for the other thread to
    //TODO: finish or forcing this to be synchronous while testing.
//    @Test
//    public void testCreateBranch_Success() throws Exception {
//        setupBranchCreate(true, "branchName", true);
//        VcsWorkItemsModel spyModel = Mockito.spy(new VcsWorkItemsModel(mockProject, mockTableModel));
//        doReturn(true).when(spyModel).createWorkItemBranchAssociation(any(ServerContext.class), any(String.class), any(Integer.class));
//        spyModel.createBranch();
//
//        verify(mockCreateBranchController, times(1)).showModalDialog();
//        verify(mockCreateBranchController, times(1)).getBranchName();
//        verify(mockCreateBranchController, times(1)).createBranch(any(ServerContext.class));
//        verify(spyModel, times(1)).createWorkItemBranchAssociation(any(ServerContext.class), any(String.class), any(Integer.class));
//    }

    @Test
    public void testCreateBranch_NotTfRepo() {
        when(VcsHelper.getRepositoryContext(any(Project.class))).thenReturn(null);
        VcsWorkItemsModel spyModel = Mockito.spy(new VcsWorkItemsModel(mockProject, mockTableModel));
        spyModel.createBranch();

        verify(mockTableModel, never()).getSelectedWorkItems();
        verify(spyModel, never()).createWorkItemBranchAssociation(any(ServerContext.class), any(String.class), any(Integer.class));
    }

    @Test
    public void testCreateBranch_CreateBranchFailed() throws Exception {
        setupBranchCreate(true, "branchName", false);
        VcsWorkItemsModel spyModel = Mockito.spy(new VcsWorkItemsModel(mockProject, mockTableModel));
        spyModel.createBranch();

        verify(mockCreateBranchController, times(1)).showModalDialog();
        verify(mockCreateBranchController, times(1)).getBranchName();
        verify(spyModel, never()).createWorkItemBranchAssociation(any(ServerContext.class), any(String.class), any(Integer.class));
    }

    @Test
    public void testCreateBranch_Canceled() throws Exception {
        setupBranchCreate(false, null, false);
        VcsWorkItemsModel spyModel = Mockito.spy(new VcsWorkItemsModel(mockProject, mockTableModel));
        spyModel.createBranch();

        verify(mockCreateBranchController, times(1)).showModalDialog();
        verify(mockCreateBranchController, never()).getBranchName();
        verify(mockCreateBranchController, never()).createBranch(any(ServerContext.class));
        verify(spyModel, never()).createWorkItemBranchAssociation(any(ServerContext.class), any(String.class), any(Integer.class));
    }

    @Test
    public void testCreateWorkItemBranchAssociation() {
        ServerContext mockContext = mock(ServerContext.class);
        com.microsoft.alm.sourcecontrol.webapi.model.GitRepository mockVstsRepo = mock(com.microsoft.alm.sourcecontrol.webapi.model.GitRepository.class);
        TeamProjectReference mockTeamProjectReference = mock(TeamProjectReference.class);
        WorkItemTrackingHttpClient mockClient = mock(WorkItemTrackingHttpClient.class);

        when(mockVstsRepo.getId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        when(mockTeamProjectReference.getId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        when(mockContext.getGitRepository()).thenReturn(mockVstsRepo);
        when(mockContext.getTeamProjectReference()).thenReturn(mockTeamProjectReference);
        when(mockContext.getWitHttpClient()).thenReturn(mockClient);

        assertTrue(model.createWorkItemBranchAssociation(mockContext, "branchName", 10));
    }

    @Test
    public void testGetOperationInputs_DefaultValue() {
        WorkItemLookupOperation.WitInputs inputs = (WorkItemLookupOperation.WitInputs) model.getOperationInputs();
        assertEquals(WorkItemHelper.getAssignedToMeQuery(), inputs.getQuery());
    }

    private void setupBranchCreate(boolean showDialog, String branchName, boolean createBranch) throws Exception {
        // mock branch controller for when its created
        when(mockCreateBranchController.showModalDialog()).thenReturn(showDialog);
        when(mockCreateBranchController.getBranchName()).thenReturn(branchName);
        when(mockCreateBranchController.createBranch(any(ServerContext.class))).thenReturn(createBranch);
        whenNew(CreateBranchController.class).withAnyArguments().thenReturn(mockCreateBranchController);

        // mock work item
        final WorkItem item = new WorkItem();
        item.setId(10);
        when(mockTableModel.getSelectedWorkItems()).thenReturn(ImmutableList.of(item));
    }

    private WorkItemLookupOperation.WitResults createResults(final int numberOfItems, final int startingIndex) {
        final List<WorkItem> list = new ArrayList<WorkItem>();
        for (int i = startingIndex; i < numberOfItems + startingIndex; i++) {
            final WorkItem item = new WorkItem();
            item.setId(i);
            list.add(item);
        }
        return operation.new WitResults(null, list);
    }
}
