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
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VcsHelper.class, IdeaHelper.class})
public class VcsWorkItemContentProviderTest extends IdeaAbstractTest {
    private VcsWorkItemContentProvider.VcsWorkItemVisibilityPredicate vcsWorkItemVisibilityPredicate;

    @Mock
    private Project mockProject;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(VcsHelper.class, IdeaHelper.class);
        vcsWorkItemVisibilityPredicate = new VcsWorkItemContentProvider.VcsWorkItemVisibilityPredicate();
    }

    @Test
    public void testTabEnabled_NotGitNotVsts() {
        when(IdeaHelper.isRider()).thenReturn(false);
        when(VcsHelper.isVstsRepo(mockProject)).thenReturn(false);
        when(VcsHelper.isTfVcs(mockProject)).thenReturn(false);
        assertFalse(vcsWorkItemVisibilityPredicate.fun(mockProject));
    }

    @Test
    public void testTabEnabled_NotRiderGit() {
        when(VcsHelper.isGitVcs(mockProject)).thenReturn(true);
        when(VcsHelper.isTfVcs(mockProject)).thenReturn(false);
        when(IdeaHelper.isRider()).thenReturn(false);
        assertTrue(vcsWorkItemVisibilityPredicate.fun(mockProject));
    }

    @Test
    public void testTabEnabled_NotRiderTfvc() {
        when(VcsHelper.isGitVcs(mockProject)).thenReturn(false);
        when(VcsHelper.isTfVcs(mockProject)).thenReturn(true);
        when(IdeaHelper.isRider()).thenReturn(false);
        assertTrue(vcsWorkItemVisibilityPredicate.fun(mockProject));
    }

    @Test
    public void testTabEnabled_RiderVsts() {
        when(VcsHelper.isGitVcs(mockProject)).thenReturn(true);
        when(IdeaHelper.isRider()).thenReturn(true);
        when(VcsHelper.isVstsRepo(mockProject)).thenReturn(true);
        assertTrue(vcsWorkItemVisibilityPredicate.fun(mockProject));
    }

    @Test
    public void testTabEnabled_RiderNotVsts() {
        when(VcsHelper.isGitVcs(mockProject)).thenReturn(true);
        when(IdeaHelper.isRider()).thenReturn(true);
        when(VcsHelper.isVstsRepo(mockProject)).thenReturn(false);
        assertFalse(vcsWorkItemVisibilityPredicate.fun(mockProject));
    }
}
