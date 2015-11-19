// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import git4idea.GitCommit;
import git4idea.GitRevisionNumber;
import git4idea.repo.GitRepository;
import git4idea.util.GitCommitCompareInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class DiffCompareInfoProviderTest extends IdeaAbstractTest {

    DiffCompareInfoProvider underTest;

    DiffCompareInfoProvider.GitUtilWrapper gitUtilWrapperMock;
    GitRepository gitRepositoryMock;
    Project projectMock;
    VirtualFile fileMock;

    @Before
    public void setUp() throws Exception {
        gitUtilWrapperMock = Mockito.mock(DiffCompareInfoProvider.GitUtilWrapper.class);

        projectMock = Mockito.mock(Project.class);

        gitRepositoryMock = Mockito.mock(GitRepository.class);
        fileMock = Mockito.mock(VirtualFile.class);
        when(gitRepositoryMock.getRoot()).thenReturn(fileMock);

        underTest = new DiffCompareInfoProvider();
        underTest.setUtilWrapper(gitUtilWrapperMock);
    }

    @Test
    public void testGetEmptyDiff() throws Exception {
        final GitCommitCompareInfo empty = underTest.getEmptyDiff(gitRepositoryMock);
        assertCompareInfoEmptiness(empty);
    }

    private void assertCompareInfoEmptiness(final GitCommitCompareInfo empty) {
        assertTrue(empty.getBranchToHeadCommits(gitRepositoryMock).isEmpty());
        assertTrue(empty.getHeadToBranchCommits(gitRepositoryMock).isEmpty());
        assertTrue(empty.getTotalDiff().isEmpty());

        /* this compareInfo consider empty lists also as having some diffs.  So the container is not empty,
        * but each individual list above is empty */
        assertFalse(empty.isEmpty());
    }

    @Test
    public void testBranchesWithoutCommonParentShouldReturnEmptyDiff() throws Exception {
        when(gitUtilWrapperMock.getMergeBase(any(Project.class), any(VirtualFile.class), anyString(), anyString()))
                .thenReturn(null);

        assertCompareInfoEmptiness(underTest.getBranchCompareInfo(projectMock, gitRepositoryMock, "test1", "test2"));
    }

    @Test
    public void testBranchCompare() throws VcsException {
        when(gitUtilWrapperMock.getMergeBase(any(Project.class), any(VirtualFile.class), eq("test2"), eq("test1")))
                .thenReturn(new GitRevisionNumber("myparent"));

        GitCommit commitMock1 = PRGitObjectMockHelper.getCommit(projectMock, fileMock);
        when(gitUtilWrapperMock.history(any(Project.class), any(VirtualFile.class), eq("myparent..")))
                .thenReturn(Collections.singletonList(commitMock1));

        GitCommit commitMock2 = PRGitObjectMockHelper.getCommit(projectMock, fileMock);
        when(gitUtilWrapperMock.history(any(Project.class), any(VirtualFile.class), eq("..myparent")))
                .thenReturn(Collections.singletonList(commitMock2));

        Change diff = Mockito.mock(Change.class);
        when(gitUtilWrapperMock.getDiff(any(Project.class), any(VirtualFile.class), eq("myparent"), eq("test1")))
                .thenReturn(Collections.singletonList(diff));

        final GitCommitCompareInfo compareInfo
                = underTest.getBranchCompareInfo(projectMock, gitRepositoryMock, "test1", "test2");

        List<GitCommit> branchToHeadCommits = compareInfo.getBranchToHeadCommits(gitRepositoryMock);
        assertEquals(1, branchToHeadCommits.size());
        assertEquals(commitMock1, branchToHeadCommits.get(0));

        List<GitCommit> headToBranchCommits = compareInfo.getHeadToBranchCommits(gitRepositoryMock);
        assertEquals(1, headToBranchCommits.size());
        assertEquals(commitMock2, headToBranchCommits.get(0));

        Collection<Change> diffs = compareInfo.getTotalDiff();
        assertEquals(1, diffs.size());
        assertEquals(diff, new LinkedList<Change>(diffs).get(0));
    }


}