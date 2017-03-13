// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectLevelVcsManager.class, GitUtil.class, TfGitHelper.class})
public class VcsHelperTest extends IdeaAbstractTest {

    @Mock
    private ProjectLevelVcsManager mockProjectLevelVcsManager;
    @Mock
    private Project mockProject;
    @Mock
    private GitRepositoryManager mockGitRepositoryManager;
    @Mock
    private GitRepository mockGitRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(ProjectLevelVcsManager.class, GitUtil.class, TfGitHelper.class);
        when(ProjectLevelVcsManager.getInstance(mockProject)).thenReturn(mockProjectLevelVcsManager);
        when(GitUtil.getRepositoryManager(mockProject)).thenReturn(mockGitRepositoryManager);
        when(mockGitRepositoryManager.getRepositoryForFile(any(VirtualFile.class))).thenReturn(mockGitRepository);
    }

    @Test
    public void testGetTeamProjectFromTfvcServerPath() {
        Assert.assertEquals("", VcsHelper.getTeamProjectFromTfvcServerPath(null));
        Assert.assertEquals("", VcsHelper.getTeamProjectFromTfvcServerPath(""));
        Assert.assertEquals("", VcsHelper.getTeamProjectFromTfvcServerPath("$/"));
        Assert.assertEquals("", VcsHelper.getTeamProjectFromTfvcServerPath("$"));
        Assert.assertEquals("", VcsHelper.getTeamProjectFromTfvcServerPath("/"));
        Assert.assertEquals("proj", VcsHelper.getTeamProjectFromTfvcServerPath("$/proj"));
        Assert.assertEquals("proj", VcsHelper.getTeamProjectFromTfvcServerPath("$/proj/"));
        Assert.assertEquals("proj", VcsHelper.getTeamProjectFromTfvcServerPath("$/proj/one"));
        Assert.assertEquals("proj", VcsHelper.getTeamProjectFromTfvcServerPath("$/proj/one/"));
    }

    @Test
    public void testIsTfVcs_True() {
        setupVcs(false, true);
        assertTrue(VcsHelper.isTfVcs(mockProject));
    }

    @Test
    public void testIsTfVcs_False() {
        setupVcs(false, false);
        assertFalse(VcsHelper.isTfVcs(mockProject));
    }

    @Test
    public void testIsGitVcs_True() {
        setupVcs(true, false);
        assertTrue(VcsHelper.isGitVcs(mockProject));
    }

    @Test
    public void testIsGitVcs_False() {
        setupVcs(false, false);
        assertFalse(VcsHelper.isGitVcs(mockProject));
    }

    @Test
    public void testIsVstsRepo_Tfvc() {
        setupVcs(false, true);
        assertTrue(VcsHelper.isVstsRepo(mockProject));
    }

    @Test
    public void testIsVstsRepo_Neither() {
        setupVcs(false, false);
        assertFalse(VcsHelper.isVstsRepo(mockProject));
    }

    @Test
    public void testIsVstsRepo_GitNoRepoFound() {
        setupVcs(true, false);
        when(mockGitRepositoryManager.getRepositoryForFile(any(VirtualFile.class))).thenReturn(null);
        assertFalse(VcsHelper.isVstsRepo(mockProject));
    }

    @Test
    public void testIsVstsRepo_GitNotVsts() {
        setupVcs(true, false);
        when(TfGitHelper.isTfGitRepository(mockGitRepository)).thenReturn(false);
        assertFalse(VcsHelper.isVstsRepo(mockProject));
    }

    @Test
    public void testIsVstsRepo_GitVsts() {
        setupVcs(true, false);
        when(TfGitHelper.isTfGitRepository(mockGitRepository)).thenReturn(true);
        assertTrue(VcsHelper.isVstsRepo(mockProject));
    }

    private void setupVcs(final boolean isGit, final boolean isTfvc) {
        when(mockProjectLevelVcsManager.checkVcsIsActive(GitVcs.NAME)).thenReturn(isGit);
        when(mockProjectLevelVcsManager.checkVcsIsActive(TFSVcs.TFVC_NAME)).thenReturn(isTfvc);
    }
}
