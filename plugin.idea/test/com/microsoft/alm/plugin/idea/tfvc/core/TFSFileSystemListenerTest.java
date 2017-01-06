// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandUtils.class, TFSVcs.class, LocalFileSystem.class})
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
    private ServerContext mockServerContext;

    @Mock
    private PendingChange mockPendingChange;

    @Mock
    private LocalFileSystem mockLocalFileSystem;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CommandUtils.class, TFSVcs.class, LocalFileSystem.class);

        when(LocalFileSystem.getInstance()).thenReturn(mockLocalFileSystem);
        when(mockVirtualFile.getPath()).thenReturn(CURRENT_FILE_PATH);
        when(mockVirtualFile.getName()).thenReturn(CURRENT_FILE_NAME);
        when(mockVirtualParent.getPath()).thenReturn(PARENT_PATH);
        when(mockVirtualFile.getParent()).thenReturn(mockVirtualParent);
        when(mockNewDirectory.getPath()).thenReturn(NEW_DIRECTORY_PATH);
        when(mockTFSVcs.getServerContext(anyBoolean())).thenReturn(mockServerContext);
        when(TFSVcs.getInstance(mockProject)).thenReturn(mockTFSVcs);

        tfsFileSystemListener = new TFSFileSystemListener(mockProject);
    }

    @Test
    public void testRename_FileNoChanges() throws Exception {
        when(CommandUtils.getStatusForFiles(mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(Collections.EMPTY_LIST);

        boolean result = tfsFileSystemListener.rename(mockVirtualFile, NEW_FILE_NAME);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(NEW_FILE_PATH));
    }

    @Test
    public void testRename_FileEditChanges() throws Exception {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT));
        when(CommandUtils.getStatusForFiles(mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.rename(mockVirtualFile, NEW_FILE_NAME);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(NEW_FILE_PATH));
    }

    @Test
    public void testRename_FileEditRenameChanges() throws Exception {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT, ServerStatusType.RENAME));
        when(CommandUtils.getStatusForFiles(mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.rename(mockVirtualFile, NEW_FILE_NAME);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(NEW_FILE_PATH));
    }

    @Test
    public void testRename_FileUnversionedChange() throws Exception {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.ADD));
        when(CommandUtils.getStatusForFiles(mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.rename(mockVirtualFile, NEW_FILE_NAME);

        assertFalse(result);
        verifyStatic(never());
        CommandUtils.renameFile(any(ServerContext.class), any(String.class), any(String.class));
    }

    @Test
    public void testMove_FileNoChanges() throws Exception {
        when(CommandUtils.getStatusForFiles(mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(Collections.EMPTY_LIST);

        boolean result = tfsFileSystemListener.move(mockVirtualFile, mockNewDirectory);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(MOVED_FILE_PATH));
    }

    @Test
    public void testMove_FileEditChanges() throws Exception {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT));
        when(CommandUtils.getStatusForFiles(mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.move(mockVirtualFile, mockNewDirectory);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(MOVED_FILE_PATH));
    }

    @Test
    public void testMove_FileEditRenameChanges() throws Exception {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.EDIT, ServerStatusType.RENAME));
        when(CommandUtils.getStatusForFiles(mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.move(mockVirtualFile, mockNewDirectory);

        assertTrue(result);
        verifyStatic(times(1));
        CommandUtils.renameFile(eq(mockServerContext), eq(CURRENT_FILE_PATH), eq(MOVED_FILE_PATH));
    }

    @Test
    public void testMove_FileUnversionedChange() throws Exception {
        when(mockPendingChange.getChangeTypes()).thenReturn(ImmutableList.of(ServerStatusType.ADD));
        when(CommandUtils.getStatusForFiles(mockServerContext, ImmutableList.of(CURRENT_FILE_PATH)))
                .thenReturn(ImmutableList.of(mockPendingChange));

        boolean result = tfsFileSystemListener.move(mockVirtualFile, mockNewDirectory);

        assertFalse(result);
        verifyStatic(never());
        CommandUtils.renameFile(any(ServerContext.class), any(String.class), any(String.class));
    }
}
