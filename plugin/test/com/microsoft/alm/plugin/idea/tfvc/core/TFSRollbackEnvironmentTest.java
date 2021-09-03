// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.rollback.RollbackProgressListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.tfs.model.connector.TfsPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        CommandUtils.class,
        LocalFileSystem.class,
        ServiceManager.class,
        TfsFileUtil.class,
        TfvcClient.class
})
public class TFSRollbackEnvironmentTest extends IdeaAbstractTest {
    TFSRollbackEnvironment rollbackEnvironment;
    List<String> filePaths = SystemInfo.isWindows
            ? ImmutableList.of("\\path\\to\\file1", "\\path\\to\\file2", "\\path\\to\\file3")
            : ImmutableList.of("/path/to/file1", "/path/to/file2", "/path/to/file3");
    List<VcsException> exceptions;
    List<Change> changes;

    @Mock
    TFSVcs mockTFSVcs;

    @Mock
    Project mockProject;

    @Mock
    RollbackProgressListener mockRollbackProgressListener;

    @Mock
    ServerContext mockServerContext;

    @Mock
    LocalFileSystem mockLocalFileSystem;

    @Mock
    FilePath filePath1, filePath2, filePath3;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(
                CommandUtils.class,
                LocalFileSystem.class,
                ServiceManager.class,
                TfsFileUtil.class,
                TfvcClient.class);

        when(mockTFSVcs.getServerContext(anyBoolean())).thenReturn(mockServerContext);
        when(TfvcClient.getInstance()).thenReturn(new ClassicTfvcClient());
        when(LocalFileSystem.getInstance()).thenReturn(mockLocalFileSystem);
        when(TfsFileUtil.createLocalPath(any(String.class))).thenCallRealMethod();
        when(TfsFileUtil.createLocalPath(any(FilePath.class))).thenCallRealMethod();
        when(TfsFileUtil.getPathItem(any(TfsPath.class))).thenCallRealMethod();
        when(filePath1.getPath()).thenReturn("/path/to/file1");
        when(filePath2.getPath()).thenReturn("/path/to/file2");
        when(filePath3.getPath()).thenReturn("/path/to/file3");
        exceptions = new ArrayList<>();

        rollbackEnvironment = new TFSRollbackEnvironment(mockTFSVcs, mockProject);
    }

    @Test
    public void testRollbackChanges_Happy() {
        setupRollbackChanges();
        VirtualFile mockVirtualFile = mock(VirtualFile.class);
        VirtualFile mockVirtualFileParent = mock(VirtualFile.class);
        when(CommandUtils.undoLocalFiles(mockServerContext, filePaths)).thenReturn(filePaths);
        when(mockVirtualFileParent.exists()).thenReturn(true);
        when(mockVirtualFile.getParent()).thenReturn(mockVirtualFileParent);
        when(mockLocalFileSystem.findFileByIoFile(any(File.class))).thenReturn(mockVirtualFile);

        rollbackEnvironment.rollbackChanges(changes, exceptions, mockRollbackProgressListener);
        ArgumentCaptor<List> arg = ArgumentCaptor.forClass(List.class);
        verifyStatic(times(1));
        TfsFileUtil.refreshAndMarkDirty(eq(mockProject), arg.capture(), eq(true));
        assertEquals(3, arg.getValue().size());
        assertTrue(exceptions.isEmpty());
    }

    @Test
    public void testRollbackChanges_Exception() {
        setupRollbackChanges();
        when(CommandUtils.undoLocalFiles(mockServerContext, filePaths)).thenThrow(new RuntimeException("test error"));

        rollbackEnvironment.rollbackChanges(changes, exceptions, mockRollbackProgressListener);
        verifyStatic(never());
        TfsFileUtil.refreshAndMarkDirty(any(Project.class), any(List.class), anyBoolean());
        assertEquals(1, exceptions.size());
    }

    @Test
    public void testRollbackMissingFileDeletion_Happy() {
        rollbackEnvironment.rollbackMissingFileDeletion(ImmutableList.of(filePath1, filePath2, filePath3),
                exceptions, mockRollbackProgressListener);
        verifyStatic(times(1));
        CommandUtils.forceGetFile(mockServerContext, "/path/to/file1");
        CommandUtils.forceGetFile(mockServerContext, "/path/to/file2");
        CommandUtils.forceGetFile(mockServerContext, "/path/to/file3");
    }

    @Test
    public void testRollbackMissingFileDeletion_Excepion() throws Exception {
        doThrow(new RuntimeException("test error")).when(CommandUtils.class, "forceGetFile", mockServerContext, "/path/to/file1");

        rollbackEnvironment.rollbackMissingFileDeletion(ImmutableList.of(filePath1), exceptions, mockRollbackProgressListener);
        assertEquals(1, exceptions.size());
    }

    private void setupRollbackChanges() {
        Change change1 = mock(Change.class);
        Change change2 = mock(Change.class);
        Change change3 = mock(Change.class);
        changes = ImmutableList.of(change1, change2, change3);
        ContentRevision contentRevision1 = mock(ContentRevision.class);
        ContentRevision contentRevision2 = mock(ContentRevision.class);
        ContentRevision contentRevision3 = mock(ContentRevision.class);

        when(change1.getType()).thenReturn(Change.Type.DELETED);
        when(change2.getType()).thenReturn(Change.Type.MODIFICATION);
        when(change3.getType()).thenReturn(Change.Type.NEW);

        when(contentRevision1.getFile()).thenReturn(filePath1);
        when(contentRevision2.getFile()).thenReturn(filePath2);
        when(contentRevision3.getFile()).thenReturn(filePath3);

        when(change1.getBeforeRevision()).thenReturn(contentRevision1);
        when(change2.getAfterRevision()).thenReturn(contentRevision2);
        when(change3.getAfterRevision()).thenReturn(contentRevision3);
    }
}