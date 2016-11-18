// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.SequentialUpdatesContext;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.SyncResults;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ConflictsEnvironment;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ConflictsHandler;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ResolveConflictHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandUtils.class, ConflictsEnvironment.class, TfsFileUtil.class})
public class TFSUpdateEnvironmentTest extends IdeaAbstractTest {
    TFSUpdateEnvironment updateEnvironment;

    @Mock
    TFSVcs mockTFSVcs;

    @Mock
    UpdatedFiles mockUpdatedFiles;

    @Mock
    ProgressIndicator mockProgressIndicator;

    @Mock
    Ref<SequentialUpdatesContext> mockUpdatesContext;

    @Mock
    ServerContext mockServerContext;

    @Mock
    ConflictsHandler mockConflictsHandler;

    @Mock
    Project mockProject;

    @Mock
    FileGroup mockFileGroupRemove;

    @Mock
    FileGroup mockFileGroupCreate;

    @Mock
    FileGroup mockFileGroupUpdate;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CommandUtils.class, ConflictsEnvironment.class, TfsFileUtil.class);
        when(mockTFSVcs.getServerContext(anyBoolean())).thenReturn(mockServerContext);
        when(mockTFSVcs.getProject()).thenReturn(mockProject);
        when(ConflictsEnvironment.getConflictsHandler()).thenReturn(mockConflictsHandler);
        when(mockUpdatedFiles.getGroupById(FileGroup.REMOVED_FROM_REPOSITORY_ID)).thenReturn(mockFileGroupRemove);
        when(mockUpdatedFiles.getGroupById(FileGroup.CREATED_ID)).thenReturn(mockFileGroupCreate);
        when(mockUpdatedFiles.getGroupById(FileGroup.UPDATED_ID)).thenReturn(mockFileGroupUpdate);
        updateEnvironment = new TFSUpdateEnvironment(mockTFSVcs);
    }

    @Test
    public void testUpdateDirectories_FilesUpToDate() {
        FilePath[] filePaths = setupUpdate(new SyncResults());

        UpdateSession session = updateEnvironment.updateDirectories(filePaths, mockUpdatedFiles, mockProgressIndicator, mockUpdatesContext);
        verifyNoMoreInteractions(mockUpdatedFiles, mockConflictsHandler);
        assertTrue(session.getExceptions().isEmpty());
        verifyStatic(times(1));
        TfsFileUtil.refreshAndInvalidate(mockProject, filePaths, false);
    }

    @Test
    public void testUpdateDirectories_FilesStale() {
        SyncResults syncResults = new SyncResults(false, ImmutableList.of("/path/to/file1", "/path/to/directory"), ImmutableList.of("/path/to/newFile"),
                ImmutableList.of("/path/to/file2"), ImmutableList.of(new VcsException("test exception")));
        FilePath[] filePaths = setupUpdate(syncResults);

        UpdateSession session = updateEnvironment.updateDirectories(filePaths, mockUpdatedFiles, mockProgressIndicator, mockUpdatesContext);
        verify(mockFileGroupRemove).add(eq("/path/to/file2"), any(VcsKey.class), isNull(VcsRevisionNumber.class));
        verify(mockFileGroupCreate).add(eq("/path/to/newFile"), any(VcsKey.class), isNull(VcsRevisionNumber.class));
        verify(mockFileGroupUpdate).add(eq("/path/to/file1"), any(VcsKey.class), isNull(VcsRevisionNumber.class));
        verify(mockFileGroupUpdate).add(eq("/path/to/directory"), any(VcsKey.class), isNull(VcsRevisionNumber.class));
        verifyNoMoreInteractions(mockFileGroupRemove, mockFileGroupCreate, mockFileGroupUpdate, mockConflictsHandler);
        verifyNoMoreInteractions(mockConflictsHandler);
        assertEquals(1, session.getExceptions().size());
        verifyStatic(times(1));
        TfsFileUtil.refreshAndInvalidate(mockProject, filePaths, false);
    }

    @Test
    public void testUpdateDirectories_Conflict() throws Exception {
        SyncResults syncResults = new SyncResults(true, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                Collections.EMPTY_LIST);
        FilePath[] filePaths = setupUpdate(syncResults);

        UpdateSession session = updateEnvironment.updateDirectories(filePaths, mockUpdatedFiles, mockProgressIndicator, mockUpdatesContext);
        verify(mockConflictsHandler).resolveConflicts(eq(mockProject), any(ResolveConflictHelper.class));
        verifyNoMoreInteractions(mockUpdatedFiles);
        assertTrue(session.getExceptions().isEmpty());
        verifyStatic(times(1));
        TfsFileUtil.refreshAndInvalidate(mockProject, filePaths, false);
    }

    private FilePath[] setupUpdate(final SyncResults syncResults) {
        FilePath filePath1 = mock(FilePath.class);
        when(filePath1.isDirectory()).thenReturn(false);
        when(filePath1.getPath()).thenReturn("/path/to/file1");

        FilePath filePath2 = mock(FilePath.class);
        when(filePath2.isDirectory()).thenReturn(true);
        when(filePath2.getPath()).thenReturn("/path/to/directory");

        FilePath filePath3 = mock(FilePath.class);
        when(filePath3.isDirectory()).thenReturn(false);
        when(filePath3.getPath()).thenReturn("/path/to/file2");
        FilePath[] filePaths = {filePath1, filePath2, filePath3};

        when(CommandUtils.syncWorkspace(mockServerContext, ImmutableList.of("/path/to/file1", "/path/to/directory",
                "/path/to/file2"), true)).thenReturn(syncResults);

        return filePaths;
    }
}
