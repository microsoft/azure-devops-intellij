// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.NullableFunction;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandUtils.class, VersionControlPath.class, TfsFileUtil.class, ProgressManager.class, VcsNotifier.class})
public class TFSUpdateEnvironmentTest extends IdeaAbstractTest {
    TFSCheckinEnvironment tfsCheckinEnvironment;

    @Mock
    TFSVcs mockTFSVcs;

    @Mock
    Project mockProject;

    @Mock
    ProgressManager mockProgressManager;

    @Mock
    ServerContext mockServerContext;

    @Mock
    VcsNotifier mockVcsNotifier;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CommandUtils.class, VersionControlPath.class, TfsFileUtil.class,
                ProgressManager.class, VcsNotifier.class);

        when(mockServerContext.getUri()).thenReturn(URI.create("http://account.visualstudio.com"));
        when(mockTFSVcs.getServerContext(anyBoolean())).thenReturn(mockServerContext);
        when(ProgressManager.getInstance()).thenReturn(mockProgressManager);
        when(mockTFSVcs.getProject()).thenReturn(mockProject);
        when(VcsNotifier.getInstance(mockProject)).thenReturn(mockVcsNotifier);
        tfsCheckinEnvironment = new TFSCheckinEnvironment(mockTFSVcs);
    }

    @Test
    public void testCommit_Happy() {
        Change change1 = mock(Change.class);
        Change change2 = mock(Change.class);
        Change change3 = mock(Change.class);
        String comment = "Committing my work";
        NullableFunction nullableFunction = mock(NullableFunction.class);
        ContentRevision contentRevision1 = mock(ContentRevision.class);
        ContentRevision contentRevision2 = mock(ContentRevision.class);
        ContentRevision contentRevision3 = mock(ContentRevision.class);

        FilePath filePath1 = mock(FilePath.class);
        when(filePath1.getPath()).thenReturn("/path/to/file1");
        when(contentRevision1.getFile()).thenReturn(filePath1);

        FilePath filePath2 = mock(FilePath.class);
        when(filePath2.getPath()).thenReturn("/path/to/file2");
        when(contentRevision2.getFile()).thenReturn(filePath2);

        FilePath filePath3 = mock(FilePath.class);
        when(filePath3.getPath()).thenReturn("/path/to/file3");
        when(contentRevision3.getFile()).thenReturn(filePath3);

        when(change1.getBeforeRevision()).thenReturn(contentRevision1);
        when(change2.getBeforeRevision()).thenReturn(null);
        when(change3.getBeforeRevision()).thenReturn(null);
        when(change1.getAfterRevision()).thenReturn(null);
        when(change2.getAfterRevision()).thenReturn(contentRevision2);
        when(change3.getAfterRevision()).thenReturn(contentRevision3);
        when(CommandUtils.checkinFiles(mockServerContext, ImmutableList.of("/path/to/file1", "/path/to/file2", "/path/to/file3"),
                comment)).thenReturn("12345");

        List<VcsException> exceptions =
                tfsCheckinEnvironment.commit(ImmutableList.of(change1, change2, change3), comment, nullableFunction, null);
        assertTrue(exceptions.isEmpty());
    }

    @Test
    public void testCommit_Exception() {
        Change change1 = mock(Change.class);
        Change change2 = mock(Change.class);
        Change change3 = mock(Change.class);
        String comment = "Committing my work";
        NullableFunction nullableFunction = mock(NullableFunction.class);
        ContentRevision contentRevision1 = mock(ContentRevision.class);
        ContentRevision contentRevision2 = mock(ContentRevision.class);
        ContentRevision contentRevision3 = mock(ContentRevision.class);

        FilePath filePath1 = mock(FilePath.class);
        when(filePath1.getPath()).thenReturn("/path/to/file1");
        when(contentRevision1.getFile()).thenReturn(filePath1);

        FilePath filePath2 = mock(FilePath.class);
        when(filePath2.getPath()).thenReturn("/path/to/file2");
        when(contentRevision2.getFile()).thenReturn(filePath2);

        FilePath filePath3 = mock(FilePath.class);
        when(filePath3.getPath()).thenReturn("/path/to/file3");
        when(contentRevision3.getFile()).thenReturn(filePath3);

        when(change1.getBeforeRevision()).thenReturn(contentRevision1);
        when(change2.getBeforeRevision()).thenReturn(null);
        when(change3.getBeforeRevision()).thenReturn(null);
        when(change1.getAfterRevision()).thenReturn(null);
        when(change2.getAfterRevision()).thenReturn(contentRevision2);
        when(change3.getAfterRevision()).thenReturn(contentRevision3);
        when(CommandUtils.checkinFiles(any(ServerContext.class), any(List.class), any(String.class))).
                thenThrow(new RuntimeException("test exception"));

        List<VcsException> exceptions =
                tfsCheckinEnvironment.commit(ImmutableList.of(change1, change2, change3), comment, nullableFunction, null);
        assertEquals(1, exceptions.size());
    }

    @Test
    public void testScheduleUnversionedFilesForAddition_Happy() {
        List<String> filePaths = ImmutableList.of("/path/to/file1", "/path/to/file2");
        VirtualFile mockVirtualFile1 = mock(VirtualFile.class);
        VirtualFile mockVirtualFile2 = mock(VirtualFile.class);
        when(mockVirtualFile1.getPath()).thenReturn(filePaths.get(0));
        when(mockVirtualFile2.getPath()).thenReturn(filePaths.get(1));
        when(mockVirtualFile1.isValid()).thenReturn(true);
        when(mockVirtualFile2.isValid()).thenReturn(true);
        when(CommandUtils.addFiles(mockServerContext, filePaths)).thenReturn(filePaths);
        when(VersionControlPath.getVirtualFile(filePaths.get(0))).thenReturn(mockVirtualFile1);
        when(VersionControlPath.getVirtualFile(filePaths.get(1))).thenReturn(mockVirtualFile2);

        List<VcsException> exceptions =
                tfsCheckinEnvironment.scheduleUnversionedFilesForAddition(Arrays.asList(mockVirtualFile1, mockVirtualFile2));
        verifyStatic(times(1));
        TfsFileUtil.markFileDirty(any(Project.class), eq(mockVirtualFile1));
        TfsFileUtil.markFileDirty(any(Project.class), eq(mockVirtualFile2));
        assertTrue(exceptions.isEmpty());
    }

    @Test
    public void testScheduleUnversionedFilesForAddition_FailedAdd() {
        List<String> filePaths = ImmutableList.of("/path/to/file1", "/path/to/file2");
        VirtualFile mockVirtualFile1 = mock(VirtualFile.class);
        VirtualFile mockVirtualFile2 = mock(VirtualFile.class);
        when(mockVirtualFile1.getPath()).thenReturn(filePaths.get(0));
        when(mockVirtualFile2.getPath()).thenReturn(filePaths.get(1));
        when(mockVirtualFile1.isValid()).thenReturn(true);
        when(CommandUtils.addFiles(mockServerContext, filePaths)).thenReturn(ImmutableList.of(filePaths.get(0)));
        when(VersionControlPath.getVirtualFile(filePaths.get(0))).thenReturn(mockVirtualFile1);

        List<VcsException> exceptions =
                tfsCheckinEnvironment.scheduleUnversionedFilesForAddition(Arrays.asList(mockVirtualFile1, mockVirtualFile2));
        verifyStatic(times(1));
        TfsFileUtil.markFileDirty(any(Project.class), eq(mockVirtualFile1));
        assertEquals(1, exceptions.size());
        assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_ERROR, filePaths.get(1)), exceptions.get(0).getMessage());
    }
}