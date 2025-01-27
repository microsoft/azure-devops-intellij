// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.external.commands.ResolveConflictsCommand;
import com.microsoft.alm.plugin.external.models.Conflict;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ResolveConflictHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResolveConflictsModelTest extends IdeaAbstractTest {
    public final Conflict CONFLICT2 = new Conflict("/path/to/file2", Conflict.ConflictType.CONTENT);
    public final Conflict CONFLICT3 = new Conflict("/path/to/file3", Conflict.ConflictType.CONTENT);
    public final List<Conflict> CONFLICTS = ImmutableList.of(new Conflict("/path/to/file1", Conflict.ConflictType.CONTENT),
            CONFLICT2, CONFLICT3);

    @Mock
    public Project mockProject;

    @Mock
    public ResolveConflictHelper mockResolveConflictHelper;

    @Mock
    public ProgressManager mockProgressManager;

    @Mock
    public ConflictsTableModel mockConflictsTableModel;

    public ResolveConflictsModel resolveConflictsModel;

    @Mock
    private MockedStatic<ProgressManager> progressManagerStatic;

    @Mock
    private MockedStatic<VcsUtil> vcsUtilStatic;

    @Before
    public void setUp() {
        progressManagerStatic.when(ProgressManager::getInstance).thenReturn(mockProgressManager);

        when(mockConflictsTableModel.getMyConflicts()).thenReturn(CONFLICTS);
        resolveConflictsModel = new ResolveConflictsModel(mockProject, mockResolveConflictHelper, mockConflictsTableModel);
    }

    @Test
    public void testLoadConflicts_Errors() throws Exception {
        when(VcsUtil.runVcsProcessWithProgress(any(VcsRunnable.class), eq(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOADING_PROGRESS_BAR)), eq(false), eq(mockProject))).thenThrow(new VcsException("Test Error"));
        resolveConflictsModel.loadConflicts();

        assertEquals(1, resolveConflictsModel.getErrors().size());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_LOAD_ERROR), resolveConflictsModel.getErrors().get(0).getValidationMessage());
    }

    @Test
    public void testAcceptYours() {
        resolveConflictsModel.acceptYours(new int[0]);
        verify(mockResolveConflictHelper).acceptChangeAsync(Collections.EMPTY_LIST, ResolveConflictsCommand.AutoResolveType.KeepYours, resolveConflictsModel);
    }

    @Test
    public void testAcceptTheirs() {
        resolveConflictsModel.acceptTheirs(new int[0]);
        verify(mockResolveConflictHelper).acceptChangeAsync(Collections.EMPTY_LIST, ResolveConflictsCommand.AutoResolveType.TakeTheirs, resolveConflictsModel);
    }

    @Test
    public void testMerge_Happy() throws Exception {
        resolveConflictsModel.merge(new int[]{1, 2});
        verify(mockResolveConflictHelper).acceptMerge(CONFLICT2, resolveConflictsModel);
        verify(mockResolveConflictHelper).acceptMerge(CONFLICT3, resolveConflictsModel);
        verifyNoMoreInteractions(mockResolveConflictHelper);
    }

    @Test
    public void testMerge_Error() throws Exception {
        doThrow(new VcsException("Test Error")).when(mockResolveConflictHelper).acceptMerge(CONFLICT2, resolveConflictsModel);
        resolveConflictsModel.merge(new int[]{1, 2});

        verify(mockResolveConflictHelper).acceptMerge(CONFLICT2, resolveConflictsModel);
        verify(mockResolveConflictHelper).acceptMerge(CONFLICT3, resolveConflictsModel);
        verifyNoMoreInteractions(mockResolveConflictHelper);
        assertEquals(1, resolveConflictsModel.getErrors().size());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_CONFLICT_MERGE_ERROR, CONFLICT2.getLocalPath(), "Test Error"),
                resolveConflictsModel.getErrors().get(0).getValidationMessage());
    }

    @Test
    public void testProcessSkippedConflicts() {
        resolveConflictsModel.processSkippedConflicts();

        verify(mockResolveConflictHelper).skip(CONFLICTS);
        verifyNoMoreInteractions(mockResolveConflictHelper);
    }
}
