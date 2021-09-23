// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import git4idea.GitVcs;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.Nullable;
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
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectLevelVcsManager.class, TfGitHelper.class})
public class VcsHelperTest extends IdeaAbstractTest {

    @Mock
    private ProjectLevelVcsManager mockProjectLevelVcsManager;
    @Mock
    private Project mockProject;
    @Mock
    private GitRepository mockGitRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(ProjectLevelVcsManager.class, TfGitHelper.class);
        when(ProjectLevelVcsManager.getInstance(mockProject)).thenReturn(mockProjectLevelVcsManager);
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
        assertFalse(VcsHelper.isVstsRepo(mockProject));
    }

    @Test
    public void testIsVstsRepo_GitVsts() {
        setupVcs(true, false, mockGitRepository);
        assertTrue(VcsHelper.isVstsRepo(mockProject));
    }

    @Test
    public void testIsVstsRepo_NullProject() {
        assertFalse(VcsHelper.isVstsRepo(null));
    }

    private void setupVcs(final boolean isGit, final boolean isTfvc) {
        setupVcs(isGit, isTfvc, null);
    }

    private void setupVcs(final boolean isGit, final boolean isTfvc, @Nullable GitRepository tfGitRepository) {
        when(TfGitHelper.getTfGitRepository(mockProject)).thenReturn(tfGitRepository);
        when(mockProjectLevelVcsManager.checkVcsIsActive(GitVcs.NAME)).thenReturn(isGit);
        when(mockProjectLevelVcsManager.checkVcsIsActive(TFSVcs.TFVC_NAME)).thenReturn(isTfvc);
    }
}
