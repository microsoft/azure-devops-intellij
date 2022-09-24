// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusFactory;
import com.intellij.openapi.vcs.FileStatusFactoryImpl;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.idea.MockedIdeaApplicationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TfsFileUtilTest extends MockedIdeaApplicationTest {
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
        when(mockApplication.getService(FileStatusFactory.class)).thenReturn(new FileStatusFactoryImpl());

        when(virtualDirectory.isDirectory()).thenReturn(true);
        when(virtualFileAdded.isDirectory()).thenReturn(false);
        when(virtualFileUnknown.isDirectory()).thenReturn(false);

        var added = FileStatus.ADDED;
        var unknown = FileStatus.UNKNOWN;
        when(fileStatusManager.getStatus(virtualFileAdded)).thenReturn(added);
        when(fileStatusManager.getStatus(virtualFileUnknown)).thenReturn(unknown);
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
