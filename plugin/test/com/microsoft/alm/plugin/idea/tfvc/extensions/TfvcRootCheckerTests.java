// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.extensions;

import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.tfvc.FileSystemTestUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.ClassicTfvcClient;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.TfvcWorkspaceLocator;
import com.microsoft.tfs.model.connector.TfsDetailedWorkspaceInfo;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfsServerPath;
import com.microsoft.tfs.model.connector.TfsWorkspaceMapping;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Path;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class TfvcRootCheckerTests extends IdeaAbstractTest {
    private final TfvcRootChecker checker = new TfvcRootChecker();

    @Mock
    private MockedStatic<ClassicTfvcClient> classicTfvcClientStatic;

    @Before
    public void setUp() {
        classicTfvcClientStatic.when(ClassicTfvcClient::getInstance).thenReturn(new ClassicTfvcClient());
    }

    @Test
    public void isVcsDirTests() {
        Assert.assertTrue(checker.isVcsDir("$TF"));
        Assert.assertTrue(checker.isVcsDir(".tf"));

        Assert.assertFalse(checker.isVcsDir(".tf1"));
        Assert.assertFalse(checker.isVcsDir("anything"));
    }

    @Test
    public void getSupportedVcsKey() {
        Assert.assertEquals(TFSVcs.getKey(), checker.getSupportedVcs());
    }

    private static AutoCloseable mockTfToolPath(String path) {
        var tfToolStatic = Mockito.mockStatic(TfTool.class);
        tfToolStatic.when(TfTool::getLocation).thenReturn(path);
        return tfToolStatic;
    }

    private static AutoCloseable mockPartialWorkspace(Path path, TfsDetailedWorkspaceInfo workspace) {
        var tfvcWorkspaceLocatorStatic = Mockito.mockStatic(TfvcWorkspaceLocator.class);
        tfvcWorkspaceLocatorStatic.when(
                () -> TfvcWorkspaceLocator.getPartialWorkspace(eq(null), eq(path), any(Boolean.class)))
                .thenReturn(workspace);
        return tfvcWorkspaceLocatorStatic;
    }

    private static AutoCloseable mockPartialWorkspaceNotDetermined(Path path) {
        var tfvcWorkspaceLocatorStatic = Mockito.mockStatic(TfvcWorkspaceLocator.class);
        tfvcWorkspaceLocatorStatic.when(
                () -> TfvcWorkspaceLocator.getPartialWorkspace(eq(null), eq(path), any(Boolean.class)))
                .thenReturn(null);
        return tfvcWorkspaceLocatorStatic;
    }

    private static TfsDetailedWorkspaceInfo createWorkspaceWithMapping(String localPath) {
        TfsWorkspaceMapping mapping = new TfsWorkspaceMapping(
                new TfsLocalPath(localPath),
                new TfsServerPath("server", "serverPath"),
                false);
        return new TfsDetailedWorkspaceInfo(Collections.singletonList(mapping), "server", "name");
    }

    @Test
    public void isRootTestNoSettings() throws Exception {
        Path path = FileSystemTestUtil.createTempFileSystem();
        try (var ignored = mockTfToolPath("")) {
            Assert.assertFalse(checker.isRoot(path.toString()));
        }
    }

    @Test
    public void isRootTestNoWorkspace() throws Exception {
        Path path = FileSystemTestUtil.createTempFileSystem("$tf/");
        try (var ignored1 = mockTfToolPath("tf.cmd");
             var ignored2 = mockPartialWorkspace(path, null)) {
            Assert.assertFalse(checker.isRoot(path.toString()));
        }
    }

    @Test
    public void isRootTestWorkspaceNotDetermined() throws Exception {
        Path path = FileSystemTestUtil.createTempFileSystem("$tf/");
        try (var ignored1 = mockTfToolPath("tf.cmd");
             var ignored2 = mockPartialWorkspaceNotDetermined(path)) {
            Assert.assertFalse(checker.isRoot(path.toString()));
        }
    }

    @Test
    public void isRootTestNotMapped() throws Exception {
        Path path = FileSystemTestUtil.createTempFileSystem("$tf/");
        try (var ignored1 = mockTfToolPath("tf.cmd");
             var ignored2 = mockPartialWorkspace(path, createWorkspaceWithMapping("someOtherLocalPath"))) {
            Assert.assertFalse(checker.isRoot(path.toString()));
        }
    }

    @Test
    public void isRootTestMapped() throws Exception {
        Path path = FileSystemTestUtil.createTempFileSystem("$tf/");
        try (var ignored1 = mockTfToolPath("tf.cmd");
             var ignored2 = mockPartialWorkspace(path, createWorkspaceWithMapping(path.toString()))) {
            Assert.assertTrue(checker.isRoot(path.toString()));
        }
    }
}
