// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TfsFileUtilTest {
    @Mock
    FileStatusManager fileStatusManager;

    @Mock
    VirtualFile virtualFileAdded;

    @Mock
    VirtualFile virtualFileUnknown;

    @Mock
    VirtualFile virtualDirectory;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(virtualDirectory.isDirectory()).thenReturn(true);
        when(virtualFileAdded.isDirectory()).thenReturn(false);
        when(virtualFileUnknown.isDirectory()).thenReturn(false);

        when(fileStatusManager.getStatus(virtualFileAdded)).thenReturn(FileStatus.ADDED);
        when(fileStatusManager.getStatus(virtualFileUnknown)).thenReturn(FileStatus.UNKNOWN);
    }

    @Test
    public void testFindUnknownFiles_NoChildren() {
        VirtualFile[] files = {virtualDirectory};
        when(virtualDirectory.getChildren()).thenReturn(new VirtualFile[0]);
        assertFalse(TfsFileUtil.findUnknownFiles(files, fileStatusManager));
    }

    @Test
    public void testFindUnknownFiles_KnownFile() {
        VirtualFile[] files = {virtualFileAdded};
        assertFalse(TfsFileUtil.findUnknownFiles(files, fileStatusManager));
    }

    @Test
    public void testFindUnknownFiles_UnknownFile() {
        VirtualFile[] files = {virtualFileUnknown, virtualFileAdded};
        assertTrue(TfsFileUtil.findUnknownFiles(files, fileStatusManager));

        VirtualFile[] files2 = {virtualFileAdded, virtualFileUnknown};
        assertTrue(TfsFileUtil.findUnknownFiles(files2, fileStatusManager));
    }

    @Test
    public void testFindUnknownFiles_NestedKnown() {
        VirtualFile[] files = {virtualDirectory};
        when(virtualDirectory.getChildren()).thenReturn(new VirtualFile[]{virtualFileAdded});
        assertFalse(TfsFileUtil.findUnknownFiles(files, fileStatusManager));
    }

    @Test
    public void testFindUnknownFiles_NestedUnknown() {
        VirtualFile[] files = {virtualDirectory};
        when(virtualDirectory.getChildren()).thenReturn(new VirtualFile[]{virtualFileAdded, virtualFileUnknown});
        assertTrue(TfsFileUtil.findUnknownFiles(files, fileStatusManager));
    }
}
