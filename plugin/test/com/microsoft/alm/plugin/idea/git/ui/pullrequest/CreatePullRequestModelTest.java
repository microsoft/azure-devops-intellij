// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.pullrequest;

import com.google.common.collect.Maps;
import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.project.Project;
import com.intellij.vcs.log.Hash;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.git.utils.GeneralGitHelper;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.repo.GitHooksInfo;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepoInfo;
import git4idea.repo.GitRepository;
import git4idea.util.GitCommitCompareInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GeneralGitHelper.class)
public class CreatePullRequestModelTest extends IdeaAbstractTest {

    CreatePullRequestModel underTest;

    Project projectMock;
    GitRepository gitRepositoryMock;
    GitRemote tfsRemote;
    DiffCompareInfoProvider diffProviderMock;
    CreatePullRequestModel.ApplicationProvider applicationProviderMock;
    Observer observerMock;
    GitLocalBranch currentBranch;

    private void mockGitRepoBranches(GitLocalBranch currentBranch, GitRemoteBranch... remoteBranches) {
        HashMap<GitRemoteBranch, Hash> remoteBranchMap = Maps.newHashMap();
        for (GitRemoteBranch remoteBranch : remoteBranches) {
            remoteBranchMap.put(remoteBranch, null);
        }

        GitRepoInfo gitRepoInfo = new GitRepoInfo(
                currentBranch,
                null,
                Repository.State.NORMAL,
                Collections.emptyList(),
                Collections.emptyMap(),
                remoteBranchMap,
                Collections.emptyList(),
                Collections.emptyList(),
                new GitHooksInfo(false, false),
                false
        );
        when(gitRepositoryMock.getInfo()).thenReturn(gitRepoInfo);
    }

    @Before
    public void setUp() {
        projectMock = Mockito.mock(Project.class);
        gitRepositoryMock = Mockito.mock(GitRepository.class);
        diffProviderMock = Mockito.mock(DiffCompareInfoProvider.class);
        observerMock = Mockito.mock(Observer.class);
        applicationProviderMock = Mockito.mock(CreatePullRequestModel.ApplicationProvider.class);
        currentBranch = PRGitObjectMockHelper.createLocalBranch("local");

        tfsRemote = new GitRemote("origin", Collections.singletonList("https://mytest.visualstudio.com/DefaultCollection/_git/testrepo"),
                Collections.singletonList("https://pushurl"), Collections.emptyList(), Collections.emptyList());

        when(diffProviderMock.getEmptyDiff(gitRepositoryMock)).thenCallRealMethod();
        when(gitRepositoryMock.getRemotes()).thenReturn(Collections.singletonList(tfsRemote));

        mockGitRepoBranches(currentBranch);
    }

    /* Testing behavior about setting default target branch */
    @Test
    public void emptyRemoteListsNoTargetBranch() {
        //empty remotes list, branch drop down shouldn't have anything selected
        mockGitRepoBranches(currentBranch);
        underTest = new CreatePullRequestModel(projectMock, gitRepositoryMock);

        // never null
        assertNotNull(underTest.getRemoteBranchDropdownModel());

        // selected is nul
        assertNull(underTest.getTargetBranch());
    }

    @Test
    public void firstBranchBecomeTargetBranchWhenNoMaster() {
        //when there are more than one remotes but non is named master, return the first
        GitRemoteBranch first = PRGitObjectMockHelper.createRemoteBranch("origin/test1", tfsRemote);
        GitRemoteBranch second = PRGitObjectMockHelper.createRemoteBranch("origin/test2", tfsRemote);
        mockGitRepoBranches(currentBranch, first, second);
        underTest = new CreatePullRequestModel(projectMock, gitRepositoryMock);

        // selected is nul
        assertEquals(first, underTest.getTargetBranch());
    }

    @Test
    public void masterIsAlwaysTheDefaultTargetBranch() {
        GitRemoteBranch first = PRGitObjectMockHelper.createRemoteBranch("origin/test1", tfsRemote);
        GitRemoteBranch second = PRGitObjectMockHelper.createRemoteBranch("origin/test2", tfsRemote);
        GitRemoteBranch master = PRGitObjectMockHelper.createRemoteBranch("origin/master", tfsRemote);
        mockGitRepoBranches(currentBranch, first, second, master);
        underTest = new CreatePullRequestModel(projectMock, gitRepositoryMock);

        // always return master
        assertEquals(master, underTest.getTargetBranch());

    }

    @Test
    public void targetDropDownOnlyShowsTfRemoteBranches() {
        GitRemoteBranch first = PRGitObjectMockHelper.createRemoteBranch("origin/test1", tfsRemote);
        GitRemoteBranch second = PRGitObjectMockHelper.createRemoteBranch("origin/test2", tfsRemote);
        GitRemoteBranch master = PRGitObjectMockHelper.createRemoteBranch("origin/master", tfsRemote);

        // two remotes, non tfs repo should be filtered out
        GitRemote nonTfsRemote = new GitRemote("origin", Collections.singletonList("https://mytest.notvso.com/git/testrepo"),
                Collections.singletonList("https://pushurl"), Collections.emptyList(), Collections.emptyList());

        when(gitRepositoryMock.getRemotes()).thenReturn(Arrays.asList(tfsRemote, nonTfsRemote));
        GitRemoteBranch nonTfsMaster = PRGitObjectMockHelper.createRemoteBranch("other/master", nonTfsRemote);
        mockGitRepoBranches(currentBranch, first, second, master, nonTfsMaster);
        underTest = new CreatePullRequestModel(projectMock, gitRepositoryMock);

        // nonTfsMaster should be filtered out
        assertEquals(3, underTest.getRemoteBranchDropdownModel().getSize());
        for (int i = 0; i < underTest.getRemoteBranchDropdownModel().getSize(); ++i) {
            assertNotEquals(nonTfsMaster, underTest.getRemoteBranchDropdownModel().getElementAt(i));
        }
    }

    @Test
    public void noDiffsWhenEitherSourceOrTargetBranchIsNotSet() throws Exception {
        // when we are in detached head state, we can't create pr, so let's not populate dif
        mockGitRepoBranches(null);
        underTest = new CreatePullRequestModel(projectMock, gitRepositoryMock);

        GitChangesContainer changesContainer = underTest.getMyChangesCompareInfo();
        assertEquals(0, changesContainer.getGitCommitCompareInfo().getTotalDiff().size());
        assertEquals(0, changesContainer.getGitCommitCompareInfo().getBranchToHeadCommits(gitRepositoryMock).size());

        // when there is a local branch, but we didn't select any remote branch, also return empty compareInfo
        GitLocalBranch mockLocalBranch = PRGitObjectMockHelper.createLocalBranch("local");
        mockGitRepoBranches(mockLocalBranch);

        changesContainer = underTest.getMyChangesCompareInfo();
        assertEquals(0, changesContainer.getGitCommitCompareInfo().getTotalDiff().size());
        assertEquals(0, changesContainer.getGitCommitCompareInfo().getBranchToHeadCommits(gitRepositoryMock).size());
    }

    @Test
    public void cacheShouldOnlyBeHitOnce() throws Exception {
        final String currentBranchCommitHash = "935b168d0601bd05d57489fae04d5c6ec439cfea";
        final String remoteBranchCommitHash = "9afa081effdaeafdff089b2aa3543415f6cdb1fb";
        GitRemoteBranch master = PRGitObjectMockHelper.createRemoteBranch("origin/master", tfsRemote);
        mockGitRepoBranches(currentBranch, master);

        PowerMockito.mockStatic(GeneralGitHelper.class);
        when(GeneralGitHelper.getLastCommitHash(projectMock, gitRepositoryMock, currentBranch)).thenReturn(currentBranchCommitHash);
        when(GeneralGitHelper.getLastCommitHash(projectMock, gitRepositoryMock, master)).thenReturn(remoteBranchCommitHash);

        underTest = new CreatePullRequestModel(projectMock, gitRepositoryMock);
        underTest.setDiffCompareInfoProvider(diffProviderMock);
        underTest.setApplicationProvider(applicationProviderMock);

        GitCommitCompareInfo compareInfo = new GitCommitCompareInfo();
        when(diffProviderMock.getBranchCompareInfo(projectMock, gitRepositoryMock,
                currentBranchCommitHash, remoteBranchCommitHash))
                .thenReturn(compareInfo);

        GitChangesContainer branchChangesContainer = underTest.getMyChangesCompareInfo();
        assertEquals(compareInfo, branchChangesContainer.getGitCommitCompareInfo());

        // verify diff loader is called once
        verify(diffProviderMock, times(1)).getBranchCompareInfo(projectMock, gitRepositoryMock,
                currentBranchCommitHash, remoteBranchCommitHash);

        underTest.getMyChangesCompareInfo();
        underTest.getMyChangesCompareInfo();
        underTest.getMyChangesCompareInfo();

        // diff loader should still only being called once since we hit cache
        verify(diffProviderMock, times(1)).getBranchCompareInfo(projectMock, gitRepositoryMock,
                currentBranchCommitHash, remoteBranchCommitHash);

    }

    @Test
    public void whenWeSetModelWeShouldBeNotified() {
        GitRemoteBranch first = PRGitObjectMockHelper.createRemoteBranch("origin/test1", tfsRemote);
        GitRemoteBranch second = PRGitObjectMockHelper.createRemoteBranch("origin/test2", tfsRemote);
        mockGitRepoBranches(currentBranch, first, second);
        underTest = new CreatePullRequestModel(projectMock, gitRepositoryMock);
        underTest.addObserver(observerMock);

        // first is already the default branch, should not trigger observer
        underTest.setTargetBranch(first);
        verify(observerMock, never()).update(underTest, CreatePullRequestModel.PROP_TARGET_BRANCH);

        // switch to second should trigger update
        underTest.setTargetBranch(second);
        verify(observerMock/* default times(1)*/).update(underTest, CreatePullRequestModel.PROP_TARGET_BRANCH);

        underTest.setLoading(!underTest.isLoading());
        verify(observerMock).update(underTest, CreatePullRequestModel.PROP_LOADING);

        underTest.setLocalBranchChanges(GitChangesContainer.createChangesContainer(null, null, null, null, null, null));
        verify(observerMock).update(underTest, CreatePullRequestModel.PROP_DIFF_MODEL);
    }

    @Test
    public void emptyTitleShouldNotPassValidation() {
        underTest = getValidModel();

        underTest.setTitle(null);
        ModelValidationInfo info = underTest.validate();

        assertNotEquals(ModelValidationInfo.NO_ERRORS, info);

        assertEquals(CreatePullRequestModel.PROP_TITLE, info.getValidationSource());
    }

    @Test
    public void emptyDescriptionShouldNotPassValidation() {
        underTest = getValidModel();

        underTest.setDescription(null);
        ModelValidationInfo info = underTest.validate();

        assertNotEquals(ModelValidationInfo.NO_ERRORS, info);

        assertEquals(CreatePullRequestModel.PROP_DESCRIPTION, info.getValidationSource());
    }

    @Test
    public void emptySourceBranchShouldNotPassValidation() {
        mockGitRepoBranches(null);
        underTest = getValidModel();

        ModelValidationInfo info = underTest.validate();

        assertNotEquals(ModelValidationInfo.NO_ERRORS, info);

        assertEquals(CreatePullRequestModel.PROP_SOURCE_BRANCH, info.getValidationSource());
    }

    @Test
    public void emptyTargetBranchShouldNotPassValidation() {
        underTest = getValidModel();
        underTest.setTargetBranch(null);

        ModelValidationInfo info = underTest.validate();

        assertNotEquals(ModelValidationInfo.NO_ERRORS, info);

        assertEquals(CreatePullRequestModel.PROP_TARGET_BRANCH, info.getValidationSource());
    }

    private CreatePullRequestModel getValidModel() {
        final CreatePullRequestModel model = new CreatePullRequestModel(projectMock, gitRepositoryMock);
        model.setTitle("a title");
        model.setDescription("has description");
        model.setTargetBranch(PRGitObjectMockHelper.createRemoteBranch("origin/test2", tfsRemote));

        return model;
    }
}
