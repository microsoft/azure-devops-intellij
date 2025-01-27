// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import git4idea.GitBranch;
import git4idea.GitRevisionNumber;
import git4idea.repo.GitRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GeneralGitHelperTest extends IdeaAbstractTest {
    private static final String mockBranchName = "mockBranchName";

    @Mock
    Project mockProject;

    @Mock
    GitRepository mockGitRepository;

    @Mock
    GitBranch mockGitBranch;

    @Mock
    VirtualFile mockVirtualFile;

    @Before
    public void startUp() {
        Mockito.when(mockGitRepository.getRoot()).thenReturn(mockVirtualFile);
        Mockito.when(mockGitBranch.getName()).thenReturn(mockBranchName);
    }

    @Test
    public void testGetLastCommitHash_Happy() throws VcsException {
        final String lastHash = "935b168d0601bd05d57489fae04d5c6ec439cfea";

        GitRevisionNumber revisionNumber = new GitRevisionNumber(lastHash);

        try (var gitRevisionNumberStatic = Mockito.mockStatic(GitRevisionNumber.class)) {
            gitRevisionNumberStatic.when(
                            () -> GitRevisionNumber.resolve(
                                    Mockito.eq(mockProject),
                                    Mockito.eq(mockVirtualFile),
                                    Mockito.eq(mockBranchName)))
                    .thenReturn(revisionNumber);

            Assert.assertEquals(
                    lastHash,
                    GeneralGitHelper.getLastCommitHash(mockProject, mockGitRepository, mockGitBranch));
        }
    }
}
