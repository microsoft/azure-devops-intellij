// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.extensions;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class VcsWorkItemContentProviderTest extends IdeaAbstractTest {
    private VcsWorkItemContentProvider.VcsWorkItemVisibilityPredicate vcsWorkItemVisibilityPredicate;

    @Mock
    private Project mockProject;

    @Mock
    private MockedStatic<VcsHelper> vcsHelper;

    @Mock
    private MockedStatic<IdeaHelper> ideaHelper;

    @Before
    public void setUp() {
        vcsWorkItemVisibilityPredicate = new VcsWorkItemContentProvider.VcsWorkItemVisibilityPredicate();
    }

    @Test
    public void testTabEnabled_NotGitNotVsts() {
        ideaHelper.when(IdeaHelper::isRider).thenReturn(false);
        vcsHelper.when(() -> VcsHelper.isVstsRepo(mockProject)).thenReturn(false);
        vcsHelper.when(() -> VcsHelper.isTfVcs(mockProject)).thenReturn(false);
        assertFalse(vcsWorkItemVisibilityPredicate.fun(mockProject));
    }

    @Test
    public void testTabEnabled_NotRiderGit() {
        vcsHelper.when(() -> VcsHelper.isGitVcs(mockProject)).thenReturn(true);
        vcsHelper.when(() -> VcsHelper.isTfVcs(mockProject)).thenReturn(false);
        ideaHelper.when(IdeaHelper::isRider).thenReturn(false);
        assertTrue(vcsWorkItemVisibilityPredicate.fun(mockProject));
    }

    @Test
    public void testTabEnabled_NotRiderTfvc() {
        vcsHelper.when(() -> VcsHelper.isGitVcs(mockProject)).thenReturn(false);
        vcsHelper.when(() -> VcsHelper.isTfVcs(mockProject)).thenReturn(true);
        ideaHelper.when(IdeaHelper::isRider).thenReturn(false);
        assertTrue(vcsWorkItemVisibilityPredicate.fun(mockProject));
    }

    @Test
    public void testTabEnabled_RiderVsts() {
        vcsHelper.when(() -> VcsHelper.isGitVcs(mockProject)).thenReturn(true);
        ideaHelper.when(IdeaHelper::isRider).thenReturn(true);
        vcsHelper.when(() -> VcsHelper.isVstsRepo(mockProject)).thenReturn(true);
        assertTrue(vcsWorkItemVisibilityPredicate.fun(mockProject));
    }

    @Test
    public void testTabEnabled_RiderNotVsts() {
        vcsHelper.when(() -> VcsHelper.isGitVcs(mockProject)).thenReturn(true);
        ideaHelper.when(IdeaHelper::isRider).thenReturn(true);
        vcsHelper.when(() -> VcsHelper.isVstsRepo(mockProject)).thenReturn(false);
        assertFalse(vcsWorkItemVisibilityPredicate.fun(mockProject));
    }
}
