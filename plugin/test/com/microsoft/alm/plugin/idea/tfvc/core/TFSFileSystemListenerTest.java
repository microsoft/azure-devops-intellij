// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TFVCUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        CommandUtils.class,
        LocalFileSystem.class,
        ServiceManager.class,
        TFSVcs.class,
        TFVCUtil.class,
        VcsHelper.class,
        VersionControlPath.class,
})
public class TFSFileSystemListenerTest extends IdeaAbstractTest {
    private String CURRENT_FILE_NAME = "file.txt";
    private String NEW_FILE_NAME = "newName.txt";
    private String PARENT_PATH = "/path/to/the";
    private String CURRENT_FILE_PATH = Path.combine(PARENT_PATH, CURRENT_FILE_NAME);
    private String NEW_FILE_PATH = Path.combine(PARENT_PATH, NEW_FILE_NAME);
    private String NEW_DIRECTORY_PATH = "/path/to/new/directory";
    private String MOVED_FILE_PATH = Path.combine(NEW_DIRECTORY_PATH, CURRENT_FILE_NAME);

    private TFSFileSystemListener tfsFileSystemListener;

    @Mock
    private VirtualFile mockVirtualFile;

    @Mock
    private VirtualFile mockVirtualParent;

    @Mock
    private VirtualFile mockNewDirectory;

    @Mock
    private TFSVcs mockTFSVcs;

    @Mock
    private Project mockProject;

    @Mock
    private TfvcClient mockTfvcClient;

    @Mock
    private ServerContext mockServerContext;

    @Mock
    private PendingChange mockPendingChange;

    @Mock
    private LocalFileSystem mockLocalFileSystem;

    @Mock
    private VcsShowConfirmationOption mockVcsShowConfirmationOption;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(
                CommandUtils.class,
                LocalFileSystem.class,
                ServiceManager.class,
                TFSVcs.class,
                TFVCUtil.class,
                VcsHelper.class,
                VersionControlPath.class
        );

        when(ServiceManager.getService(eq(mockProject), any())).thenReturn(new ClassicTfvcClient(mockProject));
        when(mockTFSVcs.getProject()).thenReturn(mockProject);
        when(mockVcsShowConfirmationOption.getValue()).thenReturn(VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);
        when(mockTFSVcs.getDeleteConfirmation()).thenReturn(mockVcsShowConfirmationOption);
        when(VcsHelper.getTFSVcsByPath(mockVirtualFile)).thenReturn(mockTFSVcs);
        when(LocalFileSystem.getInstance()).thenReturn(mockLocalFileSystem);
        when(mockVirtualFile.getPath()).thenReturn(CURRENT_FILE_PATH);
        when(mockVirtualFile.getName()).thenReturn(CURRENT_FILE_NAME);
        when(mockVirtualParent.getPath()).thenReturn(PARENT_PATH);
        when(mockVirtualFile.getParent()).thenReturn(mockVirtualParent);
        when(mockNewDirectory.getPath()).thenReturn(NEW_DIRECTORY_PATH);
        when(mockTFSVcs.getServerContext(anyBoolean())).thenReturn(mockServerContext);
        when(TFSVcs.getInstance(mockProject)).thenReturn(mockTFSVcs);
        when(TFVCUtil.isInvalidTFVCPath(eq(mockTFSVcs), any(FilePath.class))).thenReturn(false);

        FilePath mockFilePath = mock(FilePath.class);
        when(VersionControlPath.getFilePath(CURRENT_FILE_PATH, false)).thenReturn(mockFilePath);
        when(mockPendingChange.getLocalItem()).thenReturn(CURRENT_FILE_PATH);
        when(mockPendingChange.getVersion()).thenReturn("5");

        tfsFileSystemListener = new TFSFileSystemListener(mockProject);
    }

    @Test
    public void testRename_FileNotTfvc() throws Exception {
        when(VcsHelper.getTFSVcsByPath(mockVirtualFile)).thenReturn(null);

        boolean result = tfsFileSystemListener.rename(mockVirtualFile, NEW_FILE_NAME);

        assertFalse(result);
        verifyStatic(never());
        CommandUtils.renameFile(any(ServerContext.class), any(String.class), any(String.class));
    }

    @Test
    public void testRename_FileNoChanges() throws Exception {
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(Collections.EMPTY_LIST);

        boolean result = tfsFileSystemListener.rename(mockVirtualFile, NEW_FILE_NAME);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(NEW_FILE_PATH));
    }

    @Test
    public void testRename_FileEditChanges() throws Exception {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.rename(mockVirtualFile, NEW_FILE_NAME);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(NEW_FILE_PATH));
    }

    @Test
    public void testRename_FileEditRenameChanges() throws Exception {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT, ServerStatusType.RENAME));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.rename(mockVirtualFile, NEW_FILE_NAME);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(NEW_FILE_PATH));
    }

    @Test
    public void testRename_FileUnversionedChange() throws Exception {
        when(mockPendingChange.isCandidate()).thenReturn(true);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.ADD));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.rename(mockVirtualFile, NEW_FILE_NAME);

        assertFalse(result);
        verifyStatic(never());
        CommandUtils.renameFile(any(ServerContext.class), any(String.class), any(String.class));
    }

    @Test
    public void testRename_FileAdd() throws Exception {
        when(mockPendingChange.isCandidate()).thenReturn(false);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.ADD));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.rename(mockVirtualFile, NEW_FILE_NAME);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(NEW_FILE_PATH));
    }

    @Test
    public void testMove_FileNoChanges() throws Exception {
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(Collections.EMPTY_LIST);

        boolean result = tfsFileSystemListener.move(mockVirtualFile, mockNewDirectory);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(MOVED_FILE_PATH));
    }

    @Test
    public void testMove_FileEditChanges() throws Exception {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.move(mockVirtualFile, mockNewDirectory);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(MOVED_FILE_PATH));
    }

    @Test
    public void testMove_FileEditRenameChanges() throws Exception {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT, ServerStatusType.RENAME));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.move(mockVirtualFile, mockNewDirectory);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(MOVED_FILE_PATH));
    }

    @Test
    public void testMove_FileUnversionedChange() throws Exception {
        when(mockPendingChange.isCandidate()).thenReturn(true);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.ADD));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.move(mockVirtualFile, mockNewDirectory);

        assertFalse(result);
        verifyStatic(never());
        CommandUtils.renameFile(any(ServerContext.class), any(String.class), any(String.class));
    }

    @Test
    public void testMove_FileAdd() throws Exception {
        when(mockPendingChange.isCandidate()).thenReturn(false);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.ADD));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.move(mockVirtualFile, mockNewDirectory);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(MOVED_FILE_PATH));
    }

    @Test
    public void testDelete_FileNotTfvc() throws Exception {
        when(VcsHelper.getTFSVcsByPath(mockVirtualFile)).thenReturn(null);

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertFalse(result);
        verifyStatic(never());
        CommandUtils.undoLocalFiles(any(ServerContext.class), any(List.class));
        CommandUtils.deleteFiles(any(ServerContext.class), any(List.class), any(String.class), any(Boolean.class));
    }

    @Test
    public void testDelete_NonVcsDelete() throws Exception {
        when(mockVcsShowConfirmationOption.getValue()).thenReturn(VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY);

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertFalse(result);
        verifyStatic(never());
        CommandUtils.undoLocalFiles(any(ServerContext.class), any(List.class));
        CommandUtils.deleteFiles(any(ServerContext.class), any(List.class), any(String.class), any(Boolean.class));
    }

    @Test
    public void testDelete_NoChanges() throws Exception {
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(Collections.EMPTY_LIST);

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertTrue(result);
        verifyDeleteCmd(CURRENT_FILE_PATH);
    }

    @Test
    public void testDelete_FileUnversionedAdd() throws Exception {
        when(mockPendingChange.isCandidate()).thenReturn(true);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.ADD));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertFalse(result);
        verifyStatic(never());
        CommandUtils.undoLocalFiles(any(ServerContext.class), any(List.class));
        CommandUtils.deleteFiles(any(ServerContext.class), any(List.class), any(String.class), any(Boolean.class));
    }

    @Test
    public void testDelete_FileUnversionedDelete() throws Exception {
        when(mockPendingChange.isCandidate()).thenReturn(true);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.DELETE));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertTrue(result);
        verifyUndoCmd(CURRENT_FILE_PATH);
        verifyDeleteCmd(CURRENT_FILE_PATH);
    }

    @Test
    public void testDelete_FileAdd() throws Exception {
        when(mockPendingChange.isCandidate()).thenReturn(false);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.ADD));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertFalse(result);
        verifyUndoCmd(CURRENT_FILE_PATH);
        verifyStatic(never());
        CommandUtils.deleteFiles(any(ServerContext.class), any(List.class), any(String.class), any(Boolean.class));
    }

    @Test
    public void testDelete_FileDelete() throws Exception {
        when(mockPendingChange.isCandidate()).thenReturn(false);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.DELETE));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertFalse(result);
        verifyStatic(never());
        CommandUtils.undoLocalFiles(eq(mockServerContext), any(List.class));
        CommandUtils.deleteFiles(any(ServerContext.class), any(List.class), any(String.class), any(Boolean.class));
    }

    @Test
    public void testDelete_FileEdit() throws Exception {
        when(mockPendingChange.isCandidate()).thenReturn(false);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertTrue(result);
        verifyUndoCmd(CURRENT_FILE_PATH);
        verifyDeleteCmd(CURRENT_FILE_PATH);
    }

    @Test
    public void testDelete_FileRename() throws Exception {
        when(mockPendingChange.getSourceItem()).thenReturn("$/server/path/to/file.txt");
        when(mockPendingChange.isCandidate()).thenReturn(false);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.RENAME));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertTrue(result);
        verifyDeleteCmd("$/server/path/to/file.txt");
    }

    @Test
    public void testDelete_FileLock() throws Exception {
        when(mockPendingChange.isCandidate()).thenReturn(true);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.LOCK));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertFalse(result);
        verifyStatic(never());
        CommandUtils.undoLocalFiles(any(ServerContext.class), any(List.class));
        CommandUtils.deleteFiles(any(ServerContext.class), any(List.class), any(String.class), any(Boolean.class));
    }

    @Test
    public void testDelete_FileRenameEdit() throws Exception {
        when(mockPendingChange.getSourceItem()).thenReturn("$/server/path/to/file.txt");
        when(mockPendingChange.isCandidate()).thenReturn(false);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT, ServerStatusType.RENAME));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertTrue(result);
        verifyUndoCmd(CURRENT_FILE_PATH);
        verifyDeleteCmd("$/server/path/to/file.txt");
    }

    @Test
    public void testDelete_FileUndeleted() throws Exception {
        when(mockPendingChange.isCandidate()).thenReturn(false);
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.UNDELETE));
        when(CommandUtils.getStatusForFiles(mockProject, mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.delete(mockVirtualFile);

        assertFalse(result);
        verifyUndoCmd(CURRENT_FILE_PATH);
        verifyStatic(never());
        CommandUtils.deleteFiles(any(ServerContext.class), any(List.class), any(String.class), any(Boolean.class));
    }

    private void verifyDeleteCmd(final String path) {
        ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verifyStatic(times(1));
        CommandUtils.deleteFiles(eq(mockServerContext), listArgumentCaptor.capture(), eq((String) null), eq(true));
        assertEquals(1, listArgumentCaptor.getValue().size());
        assertEquals(path, listArgumentCaptor.getValue().get(0));
    }

    private void verifyUndoCmd(final String path) {
        ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verifyStatic(times(1));
        CommandUtils.undoLocalFiles(eq(mockServerContext), listArgumentCaptor.capture());
        assertEquals(1, listArgumentCaptor.getValue().size());
        assertEquals(path, listArgumentCaptor.getValue().get(0));
    }
}