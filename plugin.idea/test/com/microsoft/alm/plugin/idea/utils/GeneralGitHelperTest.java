// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.ui.pullrequest.PRGitObjectMockHelper;
import git4idea.GitBranch;
import git4idea.GitCommit;
import git4idea.history.GitHistoryUtils;
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

import java.util.Arrays;
import java.util.Collections;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GitHistoryUtils.class)
public class GeneralGitHelperTest extends IdeaAbstractTest {
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
    }

    @Test
    public void testGetLastCommitHash_Happy() throws VcsException {
        final String lastHash = "935b168d0601bd05d57489fae04d5c6ec439cfea";
        final String secondToLastHash = "9afa081effdaeafdff089b2aa3543415f6cdb1fb";

        GitCommit lastCommit = PRGitObjectMockHelper.getCommit(mockProject, mockVirtualFile, "subject", "message", lastHash);
        GitCommit secondToLastCommit = PRGitObjectMockHelper.getCommit(mockProject, mockVirtualFile, "subject", "message", secondToLastHash);

        PowerMockito.mockStatic(GitHistoryUtils.class);
        Mockito.when(GitHistoryUtils.history(Mockito.eq(mockProject), Mockito.eq(mockVirtualFile),
                Mockito.any(String.class))).thenReturn(Arrays.asList(lastCommit, secondToLastCommit));

        Assert.assertEquals(lastHash, GeneralGitHelper.getLastCommitHash(mockProject, mockGitRepository, mockGitBranch));
    }

    @Test(expected = VcsException.class)
    public void testGetLastCommitHash_NoHistory() throws VcsException {
        PowerMockito.mockStatic(GitHistoryUtils.class);
        Mockito.when(GitHistoryUtils.history(Mockito.eq(mockProject), Mockito.eq(mockVirtualFile),
                Mockito.any(String.class))).thenReturn(Collections.EMPTY_LIST);

        GeneralGitHelper.getLastCommitHash(mockProject, mockGitRepository, mockGitBranch);
    }
}
