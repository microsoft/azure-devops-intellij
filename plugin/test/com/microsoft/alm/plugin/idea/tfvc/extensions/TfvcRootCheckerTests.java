// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.extensions;

import com.microsoft.alm.plugin.external.exceptions.WorkspaceCouldNotBeDeterminedException;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.tfvc.FileSystemTestUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.ClassicTfvcClient;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.TfvcWorkspaceLocator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ClassicTfvcClient.class, CommandUtils.class, TfTool.class, TfvcWorkspaceLocator.class})
public class TfvcRootCheckerTests extends IdeaAbstractTest {
    private final TfvcRootChecker checker = new TfvcRootChecker();

    @Before
    public void setUp() {
        PowerMockito.mockStatic(
                ClassicTfvcClient.class,
                TfvcWorkspaceLocator.class);
        when(ClassicTfvcClient.getInstance()).thenReturn(new ClassicTfvcClient());
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

    private static void mockTfToolPath(String path) {
        PowerMockito.mockStatic(TfTool.class);
        when(TfTool.getLocation()).thenReturn(path);
    }

    private static void mockPartialWorkspace(Path path, Workspace workspace) {
        PowerMockito.mockStatic(CommandUtils.class);
        when(TfvcWorkspaceLocator.getPartialWorkspace(eq(path), any(Boolean.class))).thenReturn(workspace);
    }

    private static void mockPartialWorkspaceNotDetermined(Path path) {
        PowerMockito.mockStatic(CommandUtils.class);
        when(TfvcWorkspaceLocator.getPartialWorkspace(eq(path), any(Boolean.class)))
                .thenThrow(new WorkspaceCouldNotBeDeterminedException());
    }

    private static Workspace createWorkspaceWithMapping(String localPath) {
        Workspace.Mapping mapping = new Workspace.Mapping("serverPath", localPath, false);
        return new Workspace("server", "name", "computer", "owner", "comment", Collections.singletonList(mapping));
    }

    @Test
    public void isRootTestNoSettings() throws IOException {
        Path path = FileSystemTestUtil.createTempFileSystem();
        mockTfToolPath("");

        Assert.assertFalse(checker.isRoot(path.toString()));
    }

    @Test
    public void isRootTestNoWorkspace() throws IOException {
        Path path = FileSystemTestUtil.createTempFileSystem("$tf/");
        mockTfToolPath("tf.cmd");
        mockPartialWorkspace(path, null);

        Assert.assertFalse(checker.isRoot(path.toString()));
    }

    @Test
    public void isRootTestWorkspaceNotDetermined() throws IOException {
        Path path = FileSystemTestUtil.createTempFileSystem("$tf/");
        mockTfToolPath("tf.cmd");
        mockPartialWorkspaceNotDetermined(path);

        Assert.assertFalse(checker.isRoot(path.toString()));
    }

    @Test
    public void isRootTestNotMapped() throws IOException {
        Path path = FileSystemTestUtil.createTempFileSystem("$tf/");
        mockTfToolPath("tf.cmd");
        mockPartialWorkspace(path, createWorkspaceWithMapping("someOtherLocalPath"));

        Assert.assertFalse(checker.isRoot(path.toString()));
    }

    @Test
    public void isRootTestMapped() throws IOException {
        Path path = FileSystemTestUtil.createTempFileSystem("$tf/");
        mockTfToolPath("tf.cmd");
        mockPartialWorkspace(path, createWorkspaceWithMapping(path.toString()));

        Assert.assertTrue(checker.isRoot(path.toString()));
    }
}
