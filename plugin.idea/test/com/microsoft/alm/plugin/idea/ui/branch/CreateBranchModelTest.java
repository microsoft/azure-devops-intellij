// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.branch;

import com.google.common.collect.ImmutableList;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.utils.GeneralGitHelper;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRefUpdateResult;
import git4idea.GitRemoteBranch;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepoInfo;
import git4idea.repo.GitRepository;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VcsNotifier.class, GeneralGitHelper.class})
@SuppressWarnings("unchecked")
public class CreateBranchModelTest extends IdeaAbstractTest {

    private CreateBranchModel underTest;
    private static final String defaultBranchName = "defaultName";
    private static final GitRemote tfsRemoteMaster = new GitRemote("master", ImmutableList.of("https://mytest.visualstudio.com/DefaultCollection/_git/testrepo"),
            ImmutableList.of("https://pushurl"), Collections.<String>emptyList(), Collections.<String>emptyList());
    private static final GitRemote tfsRemoteBranch1 = new GitRemote("branch1", ImmutableList.of("https://mytest.visualstudio.com/DefaultCollection/_git/testrepo"),
            ImmutableList.of("https://pushurl"), Collections.<String>emptyList(), Collections.<String>emptyList());
    private static final GitRemote tfsRemoteBranch2 = new GitRemote("branch2", ImmutableList.of("https://mytest.visualstudio.com/DefaultCollection/_git/testrepo"),
            ImmutableList.of("https://pushurl"), Collections.<String>emptyList(), Collections.<String>emptyList());
    private static final URI uri = URI.create("https://mytest.visualstudio.com/DefaultCollection/_git/testrepo");

    @Mock
    private Project mockProject;
    @Mock
    private GitRepository mockGitRepository;
    @Mock
    private GitRepoInfo mockGitRepoInfo;
    @Mock
    private GitRemoteBranch mockRemoteMaster;
    @Mock
    private GitRemoteBranch mockRemoteBranch1;
    @Mock
    private GitRemoteBranch mockRemoteBranch2;
    @Mock
    private VcsNotifier mockVcsNotifier;
    @Mock
    private com.microsoft.alm.sourcecontrol.webapi.model.GitRepository mockVstsRepo;
    @Mock
    private TeamProjectReference mockTeamProjectReference;
    @Mock
    private ServerContext mockContext;
    @Mock
    private GitHttpClient mockClient;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(VcsNotifier.class);
        when(VcsNotifier.getInstance(mockProject)).thenReturn(mockVcsNotifier);

        PowerMockito.mockStatic(GeneralGitHelper.class);
        when(GeneralGitHelper.getLastCommitHash(mockProject, mockGitRepository, mockRemoteMaster)).thenReturn("281e2d5f8ba36655570ba808055e81ff64ba14d8");

        when(mockGitRepository.getRemotes()).thenReturn(ImmutableList.of(tfsRemoteBranch1, tfsRemoteBranch2, tfsRemoteMaster));
        when(mockGitRepository.getInfo()).thenReturn(mockGitRepoInfo);

        when(mockRemoteMaster.getRemote()).thenReturn(tfsRemoteMaster);
        when(mockRemoteMaster.getName()).thenReturn("master");

        when(mockRemoteBranch1.getRemote()).thenReturn(tfsRemoteBranch1);
        when(mockRemoteBranch1.getName()).thenReturn("branch1");

        when(mockRemoteBranch2.getRemote()).thenReturn(tfsRemoteBranch2);
        when(mockRemoteBranch2.getName()).thenReturn("branch2");

        when(mockVstsRepo.getId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        when(mockTeamProjectReference.getId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        when(mockGitRepoInfo.getRemoteBranches()).thenReturn(ImmutableList.of(mockRemoteMaster));

        when(mockContext.getUri()).thenReturn(uri);
        when(mockContext.getGitRepository()).thenReturn(mockVstsRepo);
        when(mockContext.getTeamProjectReference()).thenReturn(mockTeamProjectReference);
    }

    @Test
    public void testConstructor_MasterSelected() {
        when(mockGitRepoInfo.getRemoteBranches()).thenReturn(ImmutableList.of(mockRemoteBranch1, mockRemoteMaster));
        underTest = new CreateBranchModel(mockProject, defaultBranchName, mockGitRepository);

        assertNotNull(underTest.getRemoteBranchDropdownModel());
        assertEquals(2, underTest.getRemoteBranchDropdownModel().getSize());
        assertEquals("master", underTest.getSelectedRemoteBranch().getName());
    }

    @Test
    public void testConstructor_NoMasterToSelect() {
        when(mockGitRepoInfo.getRemoteBranches()).thenReturn(ImmutableList.of(mockRemoteBranch1, mockRemoteBranch2));
        underTest = new CreateBranchModel(mockProject, defaultBranchName, mockGitRepository);

        assertNotNull(underTest.getRemoteBranchDropdownModel());
        assertEquals(2, underTest.getRemoteBranchDropdownModel().getSize());
        assertEquals("branch2", underTest.getSelectedRemoteBranch().getName());
    }

    @Test
    public void testConstructor_NoRemotesFound() {
        when(mockGitRepoInfo.getRemoteBranches()).thenReturn(Collections.EMPTY_LIST);
        underTest = new CreateBranchModel(mockProject, defaultBranchName, mockGitRepository);

        assertNotNull(underTest.getRemoteBranchDropdownModel());
        assertEquals(0, underTest.getRemoteBranchDropdownModel().getSize());
        assertNull(underTest.getSelectedRemoteBranch());
    }

    @Test
    public void testValidate_EmptyBranchName() {
        underTest = new CreateBranchModel(mockProject, defaultBranchName, mockGitRepository);
        underTest.setBranchName(StringUtils.EMPTY);

        ModelValidationInfo info = underTest.validate();
        assertNotEquals(ModelValidationInfo.NO_ERRORS, info);
        assertEquals(CreateBranchModel.PROP_BRANCH_NAME, info.getValidationSource());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_CREATE_BRANCH_DIALOG_ERRORS_BRANCH_NAME_EMPTY), info.getValidationMessage());
    }

    @Test
    public void testValidate_DuplicateBranchName() {
        when(mockGitRepoInfo.getRemoteBranches()).thenReturn(ImmutableList.of(mockRemoteMaster));
        underTest = new CreateBranchModel(mockProject, defaultBranchName, mockGitRepository);
        underTest.setBranchName("master");

        ModelValidationInfo info = underTest.validate();
        assertNotEquals(ModelValidationInfo.NO_ERRORS, info);
        assertEquals(CreateBranchModel.PROP_BRANCH_NAME, info.getValidationSource());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_CREATE_BRANCH_DIALOG_ERRORS_BRANCH_NAME_EXISTS, "master"), info.getValidationMessage());
    }

    @Test
    public void testValidate_NoErrors() {
        when(mockGitRepoInfo.getRemoteBranches()).thenReturn(ImmutableList.of(mockRemoteMaster));
        underTest = new CreateBranchModel(mockProject, defaultBranchName, mockGitRepository);
        underTest.setBranchName("newBranch");

        ModelValidationInfo info = underTest.validate();
        assertEquals(ModelValidationInfo.NO_ERRORS, info);
    }

    @Test
    public void testDoBranchCreate_Success() throws Exception {
        GitRefUpdateResult result = new GitRefUpdateResult();
        result.setSuccess(true);
        when(mockClient.updateRefs(any(List.class), any(UUID.class), any(String.class))).thenReturn(ImmutableList.of(result));
        when(mockContext.getGitHttpClient()).thenReturn(mockClient);

        underTest = new CreateBranchModel(mockProject, defaultBranchName, mockGitRepository);
        underTest.setBranchName("testBranch");
        assertTrue(underTest.doBranchCreate(mockContext));
        assertTrue(underTest.getBranchWasCreated());
        verify(mockVcsNotifier).notifyImportantInfo(eq(TfPluginBundle.message(TfPluginBundle.KEY_CREATE_BRANCH_DIALOG_SUCCESSFUL_TITLE)), any(String.class), any(NotificationListener.class));

    }

    @Test
    public void testDoBranchCreate_ResultsReturnedFailed() throws Exception {
        GitRefUpdateResult result = new GitRefUpdateResult();
        result.setSuccess(false);
        result.setCustomMessage("failed");
        when(mockClient.updateRefs(any(List.class), any(UUID.class), any(String.class))).thenReturn(ImmutableList.of(result));
        when(mockContext.getGitHttpClient()).thenReturn(mockClient);

        underTest = new CreateBranchModel(mockProject, defaultBranchName, mockGitRepository);
        underTest.setBranchName("testBranch");
        assertTrue(underTest.doBranchCreate(mockContext));
        assertFalse(underTest.getBranchWasCreated());
        verify(mockVcsNotifier).notifyError(eq(TfPluginBundle.message(TfPluginBundle.KEY_CREATE_BRANCH_DIALOG_FAILED_TITLE)), any(String.class));

    }

    @Test
    public void testDoBranchCreate_NoResultsReturned() throws Exception {
        when(mockClient.updateRefs(any(List.class), any(UUID.class), any(String.class))).thenReturn(Collections.EMPTY_LIST);
        when(mockContext.getGitHttpClient()).thenReturn(mockClient);

        underTest = new CreateBranchModel(mockProject, defaultBranchName, mockGitRepository);
        underTest.setBranchName("testBranch");
        assertTrue(underTest.doBranchCreate(mockContext));
        assertFalse(underTest.getBranchWasCreated());
        verify(mockVcsNotifier).notifyError(eq(TfPluginBundle.message(TfPluginBundle.KEY_CREATE_BRANCH_DIALOG_FAILED_TITLE)), any(String.class));
    }
}
