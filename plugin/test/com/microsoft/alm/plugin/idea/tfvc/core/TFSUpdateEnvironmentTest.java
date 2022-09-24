// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.SequentialUpdatesContext;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.exceptions.SyncException;
import com.microsoft.alm.plugin.external.models.SyncResults;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TFVCUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ConflictsEnvironment;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ConflictsHandler;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ResolveConflictHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(MockitoJUnitRunner.class)
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

    @Mock
    private MockedStatic<CommandUtils> commandUtilsStatic;

    @Mock
    private MockedStatic<ConflictsEnvironment> conflictsEnvironmentStatic;

    @Mock
    private MockedStatic<TfsFileUtil> tfsFileUtilStatic;

    @Mock
    private MockedStatic<TFVCUtil> tfvcUtilStatic;

    @Before
    public void setUp() {
        when(mockTFSVcs.getServerContext(anyBoolean())).thenReturn(mockServerContext);
        when(mockTFSVcs.getProject()).thenReturn(mockProject);
        //noinspection ResultOfMethodCallIgnored
        conflictsEnvironmentStatic.when(ConflictsEnvironment::getConflictsHandler).thenReturn(mockConflictsHandler);
        when(mockUpdatedFiles.getGroupById(FileGroup.REMOVED_FROM_REPOSITORY_ID)).thenReturn(mockFileGroupRemove);
        when(mockUpdatedFiles.getGroupById(FileGroup.CREATED_ID)).thenReturn(mockFileGroupCreate);
        when(mockUpdatedFiles.getGroupById(FileGroup.UPDATED_ID)).thenReturn(mockFileGroupUpdate);
        tfvcUtilStatic.when(() -> TFVCUtil.filterValidTFVCPaths(eq(mockProject), anyCollection()))
                .then((Answer<Collection<String>>) invocation -> {
                    @SuppressWarnings("unchecked") Collection<FilePath> argument = (Collection<FilePath>) invocation.getArguments()[1];
                    ArrayList<String> result = new ArrayList<String>();
                    for (FilePath filePath : argument) {
                        result.add(filePath.getPath());
                    }
                    return result;
                });

        updateEnvironment = new TFSUpdateEnvironment(mockProject, mockTFSVcs);
    }

    @Test
    public void testUpdateDirectories_FilesUpToDate() {
        FilePath[] filePaths = setupUpdate(new SyncResults());

        UpdateSession session = updateEnvironment.updateDirectories(filePaths, mockUpdatedFiles, mockProgressIndicator, mockUpdatesContext);
        verifyNoMoreInteractions(mockUpdatedFiles, mockConflictsHandler);
        assertTrue(session.getExceptions().isEmpty());
        verifyStatic(TfsFileUtil.class, times(1));
        TfsFileUtil.refreshAndInvalidate(mockProject, filePaths, false);
    }

    @Test
    public void testUpdateDirectories_FilesStale() {
        SyncResults syncResults = new SyncResults(false, ImmutableList.of("/path/to/file1", "/path/to/directory"), ImmutableList.of("/path/to/newFile"),
                ImmutableList.of("/path/to/file2"), ImmutableList.of(new SyncException("test exception")));
        FilePath[] filePaths = setupUpdate(syncResults);

        UpdateSession session = updateEnvironment.updateDirectories(filePaths, mockUpdatedFiles, mockProgressIndicator, mockUpdatesContext);
        verify(mockFileGroupRemove).add(eq("/path/to/file2"), any(VcsKey.class), isNull());
        verify(mockFileGroupCreate).add(eq("/path/to/newFile"), any(VcsKey.class), isNull());
        verify(mockFileGroupUpdate).add(eq("/path/to/file1"), any(VcsKey.class), isNull());
        verify(mockFileGroupUpdate).add(eq("/path/to/directory"), any(VcsKey.class), isNull());
        verifyNoMoreInteractions(mockFileGroupRemove, mockFileGroupCreate, mockFileGroupUpdate, mockConflictsHandler);
        verifyNoMoreInteractions(mockConflictsHandler);
        assertEquals(1, session.getExceptions().size());
        verifyStatic(TfsFileUtil.class, times(1));
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
        verifyStatic(TfsFileUtil.class, times(1));
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
        lenient().when(filePath3.isDirectory()).thenReturn(false);
        when(filePath3.getPath()).thenReturn("/path/to/file2");
        FilePath[] filePaths = {filePath1, filePath2, filePath3};

        when(CommandUtils.syncWorkspace(mockServerContext, ImmutableList.of("/path/to/file1", "/path/to/directory",
                "/path/to/file2"), true, false)).thenReturn(syncResults);

        return filePaths;
    }
}
