// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.NullableFunction;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.MockedIdeaApplicationTest;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        CommandUtils.class,
        ProgressManager.class,
        ServiceManager.class,
        TfsFileUtil.class,
        TfvcClient.class,
        VersionControlPath.class})
public class TFSCheckinEnvironmentTest extends MockedIdeaApplicationTest {
    TFSCheckinEnvironment tfsCheckinEnvironment;
    List<String> filePaths = ImmutableList.of("/path/to/file1", "/path/to/file2");
    String comment = "Committing my work";
    List<Change> changes;

    @Mock
    TFSVcs mockTFSVcs;

    @Mock
    Project mockProject;

    @Mock
    ProgressManager mockProgressManager;

    @Mock
    ServerContext mockServerContext;

    @Mock
    NullableFunction mockNullableFunction;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(
                CommandUtils.class,
                ProgressManager.class,
                ServiceManager.class,
                TfsFileUtil.class,
                TfvcClient.class,
                VersionControlPath.class);

        when(mockServerContext.getUri()).thenReturn(URI.create("http://organization.visualstudio.com"));
        when(mockTFSVcs.getServerContext(anyBoolean())).thenReturn(mockServerContext);
        when(ProgressManager.getInstance()).thenReturn(mockProgressManager);
        when(TfvcClient.getInstance()).thenReturn(new ClassicTfvcClient());
        when(mockTFSVcs.getProject()).thenReturn(mockProject);
        tfsCheckinEnvironment = new TFSCheckinEnvironment(mockTFSVcs);
    }

    @Test
    public void testCommit_Happy() {
        setupCommit();
        when(CommandUtils.checkinFiles(mockServerContext, ImmutableList.of("/path/to/file1", "/path/to/file2", "/path/to/file3"),
                comment, null)).thenReturn("12345");

        List<VcsException> exceptions =
                tfsCheckinEnvironment.commit(changes, comment, mockNullableFunction, null);
        assertTrue(exceptions.isEmpty());
    }

    @Test
    public void testCommit_Exception() {
        setupCommit();
        when(CommandUtils.checkinFiles(any(ServerContext.class), any(List.class), any(String.class), any(List.class))).
                thenThrow(new RuntimeException("test exception"));

        List<VcsException> exceptions =
                tfsCheckinEnvironment.commit(changes, comment, mockNullableFunction, null);
        assertEquals(1, exceptions.size());
    }

    private List<String> toCanonicalPaths(List<String> paths) {
        return paths.stream().map(FileUtil::toCanonicalPath).collect(Collectors.toList());
    }

    private void mockAddFiles(List<String> pathsToReturn) {
        when(CommandUtils.addFiles(eq(mockServerContext), anyList())).then(invocation -> {
            List<String> filesToAddPaths = invocation.getArgument(1, List.class);
            if (toCanonicalPaths(filesToAddPaths).equals(toCanonicalPaths(filePaths)))
                return pathsToReturn;
            return Collections.EMPTY_LIST;
        });
    }

    @Test
    public void testScheduleUnversionedFilesForAddition_Happy() {
        List<VirtualFile> mockFiles = setupAdd();
        mockAddFiles(filePaths);

        List<VcsException> exceptions =
                tfsCheckinEnvironment.scheduleUnversionedFilesForAddition(mockFiles);
        verifyStatic(TfsFileUtil.class, times(1));
        TfsFileUtil.markFileDirty(any(Project.class), eq(mockFiles.get(0)));
        verifyStatic(TfsFileUtil.class, times(1));
        TfsFileUtil.markFileDirty(any(Project.class), eq(mockFiles.get(1)));
        assertTrue(exceptions.isEmpty());
    }

    @Test
    public void testScheduleUnversionedFilesForAddition_FailedAdd() {
        List<VirtualFile> mockFiles = setupAdd();
        mockAddFiles(ImmutableList.of(filePaths.get(0)));

        List<VcsException> exceptions =
                tfsCheckinEnvironment.scheduleUnversionedFilesForAddition(mockFiles);
        verifyStatic(TfsFileUtil.class, times(1));
        TfsFileUtil.markFileDirty(any(Project.class), eq(mockFiles.get(0)));
        assertEquals(1, exceptions.size());
        assertEquals(
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_ERROR, filePaths.get(1)).replace('\\', '/'),
                exceptions.get(0).getMessage().replace('\\', '/'));
    }

    private void setupCommit() {
        Change change1 = mock(Change.class);
        Change change2 = mock(Change.class);
        Change change3 = mock(Change.class);
        ContentRevision contentRevision1 = mock(ContentRevision.class);
        ContentRevision contentRevision2 = mock(ContentRevision.class);
        ContentRevision contentRevision3 = mock(ContentRevision.class);
        changes = ImmutableList.of(change1, change2, change3);

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
    }

    private List<VirtualFile> setupAdd() {
        VirtualFile mockVirtualFile1 = mock(VirtualFile.class);
        VirtualFile mockVirtualFile2 = mock(VirtualFile.class);
        when(mockVirtualFile1.getPath()).thenReturn(filePaths.get(0));
        when(mockVirtualFile2.getPath()).thenReturn(filePaths.get(1));
        when(mockVirtualFile1.isValid()).thenReturn(true);
        when(mockVirtualFile2.isValid()).thenReturn(true);

        when(VersionControlPath.getVirtualFile(any())).then(invocation -> {
            String localPath = invocation.getArgument(0, String.class);
            String canonicalPath = FileUtil.toCanonicalPath(localPath);
            if (canonicalPath.equals(FileUtil.toCanonicalPath(filePaths.get(0))))
                return mockVirtualFile1;
            else if (canonicalPath.equals(FileUtil.toCanonicalPath(filePaths.get(1))))
                return mockVirtualFile2;
            else
                return null;
        });

        return ImmutableList.of(mockVirtualFile1, mockVirtualFile2);
    }
}