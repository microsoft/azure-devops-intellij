// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import git4idea.GitRemoteBranch;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepoInfo;
import git4idea.repo.GitRepository;
import git4idea.util.GitCommitCompareInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.Observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreatePullRequestModelTest extends IdeaAbstractTest {

    CreatePullRequestModel underTest;

    Project projectMock;

    GitRepository gitRepositoryMock;

    GitRepoInfo gitRepoInfoMock;

    GitRemote tfsRemote;

    DiffCompareInfoProvider diffProviderMock;

    CreatePullRequestModel.ApplicationProvider applicationProviderMock;

    Observer observerMock;

    @Before
    public void setUp() throws Exception {
        projectMock = Mockito.mock(Project.class);
        gitRepoInfoMock = Mockito.mock(GitRepoInfo.class);
        gitRepositoryMock = Mockito.mock(GitRepository.class);
        diffProviderMock = Mockito.mock(DiffCompareInfoProvider.class);
        observerMock = Mockito.mock(Observer.class);
        applicationProviderMock = Mockito.mock(CreatePullRequestModel.ApplicationProvider.class);

        tfsRemote = new GitRemote("origin", Arrays.asList("https://mytest.visualstudio.com/DefaultCollection/_git/testrepo"),
                Arrays.asList("https://pushurl"), Collections.<String>emptyList(), Collections.<String>emptyList());

        when(diffProviderMock.getEmptyDiff(gitRepositoryMock)).thenCallRealMethod();
        when(gitRepositoryMock.getInfo()).thenReturn(gitRepoInfoMock);
        when(gitRepositoryMock.getRemotes()).thenReturn(Collections.singletonList(tfsRemote));
        when(gitRepoInfoMock.getCurrentBranch()).thenReturn(PRGitObjectMockHelper.createLocalBranch("local"));
    }

    /* Testing behavior about setting default target branch */
    @Test
    public void emptyRemoteListsNoTargetBranch() {
        //empty remotes list, branch drop down shouldn't have anything selected
        when(gitRepoInfoMock.getRemoteBranches()).thenReturn(Collections.<GitRemoteBranch>emptyList());
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
        when(gitRepoInfoMock.getRemoteBranches()).thenReturn(Arrays.asList(
                first, second
        ));
        underTest = new CreatePullRequestModel(projectMock, gitRepositoryMock);

        // selected is nul
        assertEquals(first, underTest.getTargetBranch());
    }

    @Test
    public void masterIsAlwaysTheDefaultTargetBranch() throws Exception {
        GitRemoteBranch first = PRGitObjectMockHelper.createRemoteBranch("origin/test1", tfsRemote);
        GitRemoteBranch second = PRGitObjectMockHelper.createRemoteBranch("origin/test2", tfsRemote);
        GitRemoteBranch master = PRGitObjectMockHelper.createRemoteBranch("origin/master", tfsRemote);
        when(gitRepoInfoMock.getRemoteBranches()).thenReturn(Arrays.asList(
                first, second, master
        ));
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
        GitRemote nonTfsRemote = new GitRemote("origin", Arrays.asList("https://mytest.notvso.com/git/testrepo"),
                Arrays.asList("https://pushurl"), Collections.<String>emptyList(), Collections.<String>emptyList());

        when(gitRepositoryMock.getRemotes()).thenReturn(Arrays.asList(tfsRemote, nonTfsRemote));
        GitRemoteBranch nonTfsMaster = PRGitObjectMockHelper.createRemoteBranch("other/master", nonTfsRemote);
        when(gitRepoInfoMock.getRemoteBranches()).thenReturn(Arrays.asList(
                first, second, master, nonTfsMaster
        ));
        underTest = new CreatePullRequestModel(projectMock, gitRepositoryMock);

        // nonTfsMaster should be filtered out
        assertEquals(3, underTest.getRemoteBranchDropdownModel().getSize());
        for(int i = 0; i < underTest.getRemoteBranchDropdownModel().getSize(); ++i) {
            assertNotEquals(nonTfsMaster, underTest.getRemoteBranchDropdownModel().getElementAt(i));
        }
    }

    @Test
    public void noDiffsWhenEitherSourceOrTargetBranchIsNotSet() throws Exception {
        // when we are in detached head state, we can't create pr, so let's not populate dif
        when(gitRepoInfoMock.getCurrentBranch()).thenReturn(null);
        underTest = new CreatePullRequestModel(projectMock, gitRepositoryMock);

        GitChangesContainer changesContainer = underTest.getMyChangesCompareInfo();
        assertEquals(0, changesContainer.getGitCommitCompareInfo().getTotalDiff().size());
        assertEquals(0, changesContainer.getGitCommitCompareInfo().getBranchToHeadCommits(gitRepositoryMock).size());

        // when there is a local branch, but we didn't select any remote branch, also return empty compareInfo
        when(gitRepoInfoMock.getCurrentBranch()).thenReturn(PRGitObjectMockHelper.createLocalBranch("local"));
        when(gitRepoInfoMock.getRemoteBranches()).thenReturn(Collections.<GitRemoteBranch>emptyList());

        changesContainer = underTest.getMyChangesCompareInfo();
        assertEquals(0, changesContainer.getGitCommitCompareInfo().getTotalDiff().size());
        assertEquals(0, changesContainer.getGitCommitCompareInfo().getBranchToHeadCommits(gitRepositoryMock).size());
    }

    @Test
    public void cacheShouldOnlyBeHitOnce() throws Exception {
        GitRemoteBranch master = PRGitObjectMockHelper.createRemoteBranch("origin/master", tfsRemote);
        when(gitRepoInfoMock.getRemoteBranches()).thenReturn(Arrays.asList(master));
        underTest = new CreatePullRequestModel(projectMock, gitRepositoryMock);
        underTest.setDiffCompareInfoProvider(diffProviderMock);
        underTest.setApplicationProvider(applicationProviderMock);

        GitCommitCompareInfo compareInfo = new GitCommitCompareInfo();
        when(diffProviderMock.getBranchCompareInfo(projectMock, gitRepositoryMock,
                "935b168d0601bd05d57489fae04d5c6ec439cfea", "9afa081effdaeafdff089b2aa3543415f6cdb1fb"))
                .thenReturn(compareInfo);

        GitChangesContainer branchChangesContainer = underTest.getMyChangesCompareInfo();
        assertEquals(compareInfo, branchChangesContainer.getGitCommitCompareInfo());

        // verify diff loader is called once
        verify(diffProviderMock, times(1)).getBranchCompareInfo(projectMock, gitRepositoryMock,
                "935b168d0601bd05d57489fae04d5c6ec439cfea", "9afa081effdaeafdff089b2aa3543415f6cdb1fb");

        underTest.getMyChangesCompareInfo();
        underTest.getMyChangesCompareInfo();
        underTest.getMyChangesCompareInfo();

        // diff loader should still only being called once since we hit cache
        verify(diffProviderMock, times(1)).getBranchCompareInfo(projectMock, gitRepositoryMock,
                "935b168d0601bd05d57489fae04d5c6ec439cfea", "9afa081effdaeafdff089b2aa3543415f6cdb1fb");

    }

    @Test
    public void whenWeSetModelWeShouldBeNotified() throws VcsException {
        GitRemoteBranch first = PRGitObjectMockHelper.createRemoteBranch("origin/test1", tfsRemote);
        GitRemoteBranch second = PRGitObjectMockHelper.createRemoteBranch("origin/test2", tfsRemote);
        when(gitRepoInfoMock.getRemoteBranches()).thenReturn(Arrays.asList(
                first, second
        ));
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
        when(gitRepoInfoMock.getCurrentBranch()).thenReturn(null);
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
