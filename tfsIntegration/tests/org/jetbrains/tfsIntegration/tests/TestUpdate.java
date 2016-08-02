/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.tests;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.junit.Test;

import java.io.IOException;

@SuppressWarnings({"HardCodedStringLiteral"})
public class TestUpdate extends TFSTestCase {

  @Test
  public void testUpdate() throws VcsException, IOException {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    final VirtualFile root = createDirInCommand(mySandboxRoot, "root");
    commit(getChanges().getChanges(), "first one");

    Runnable testRevMinus2 = new Runnable() {
      public void run() {
        assertFolder(root, 0);
      }
    };

    final VirtualFile folder1 = createDirInCommand(root, "folder1");
    final String fileModifiedContent1 = "fileModifiedRev1";
    final VirtualFile fileModified = createFileInCommand(folder1, "fileModified", fileModifiedContent1);
    final FilePath fileModified_original = TfsFileUtil.getFilePath(fileModified);
    final String fileRenamedContent = "fileRenamedRev1";
    final VirtualFile fileRenamed = createFileInCommand(folder1, "filepathRenamed_original", fileRenamedContent);
    final FilePath filepathRenamed_original = TfsFileUtil.getFilePath(fileRenamed);
    final String fileRenamedModifiedContent1 = "fileRenamedModifiedRev1";
    final VirtualFile fileRenamedModified = createFileInCommand(folder1, "filepathRenamedModified_original", fileRenamedModifiedContent1);
    final FilePath filepathRenamedModified_original = TfsFileUtil.getFilePath(fileRenamedModified);
    final String fileDeletedContent = "fileDeletedRev1";
    final VirtualFile fileDeleted = createFileInCommand(folder1, "fileDeleted", fileDeletedContent);
    final FilePath filepathDeleted = TfsFileUtil.getFilePath(fileDeleted);
    final VirtualFile subfolderRenamed = createDirInCommand(folder1, "subfolderRenamed_original");
    final FilePath filepathSubfolderRenamedOriginal = TfsFileUtil.getFilePath(subfolderRenamed);
    final VirtualFile subfolderDeleted = createDirInCommand(folder1, "subfolderDeleted");
    final FilePath filepathSubfolderDeleted = TfsFileUtil.getFilePath(subfolderDeleted);
    final VirtualFile subfolderMoved = createDirInCommand(folder1, "subfolderMoved");
    final FilePath filepathSubfolderMovedOriginal = TfsFileUtil.getFilePath(subfolderMoved);

    final String comment2 = "test rev2 committed";
    commit(getChanges().getChanges(), comment2);

    Runnable testRevMinus1 = new Runnable() {
      public void run() {
        assertFolder(root, 1);
        assertFolder(folder1, 7);

        assertFolder(filepathSubfolderMovedOriginal, 0);
        assertFolder(filepathSubfolderRenamedOriginal, 0);
        assertFolder(filepathSubfolderDeleted, 0);

        assertFile(filepathDeleted, fileDeletedContent, false);
        assertFile(fileModified_original, fileModifiedContent1, false);
        assertFile(filepathRenamed_original, fileRenamedContent, false);
        assertFile(filepathRenamedModified_original, fileRenamedModifiedContent1, false);
      }
    };

    editFiles(fileModified, fileRenamedModified);
    final String fileModifiedContent2 = "fileModifiedRev2";
    setFileContent(fileModified, fileModifiedContent2);
    rename(fileRenamed, "fileRenamed_renamed");
    rename(fileRenamedModified, "fileRenamedModified_renamed");
    final String fileRenamedModifiedContent2 = "fileRenamedModifiedRev2";
    setFileContent(fileRenamedModified, fileRenamedModifiedContent2);

    final String fileCreatedContent = "fileCreatedContent";
    final VirtualFile fileCreated = createFileInCommand(root, "fileCreated", fileCreatedContent);
    deleteFileInCommand(fileDeleted);

    deleteFileInCommand(subfolderDeleted);
    rename(subfolderRenamed, "subfolderRenamed_newname");

    final VirtualFile subfolderA = createDirInCommand(root, "subfolderA");
    final VirtualFile subfolderB = createDirInCommand(subfolderA, "subfolderB");
    moveFileInCommand(subfolderMoved, subfolderB);

    final String comment3 = "test rev3 committed";
    commit(getChanges().getChanges(), comment3);

    Runnable testLastRev = new Runnable() {
      public void run() {
        assertFolder(root, 3);

        assertFolder(folder1, 4);

        assertFolder(subfolderRenamed, 0);
        assertFile(fileModified, fileModifiedContent2, false);
        assertFile(fileRenamed, fileRenamedContent, false);
        assertFile(fileRenamedModified, fileRenamedModifiedContent2, false);

        assertFolder(subfolderA, 1);
        assertFolder(subfolderB, 1);
        assertFolder(subfolderMoved, 0);

        assertFile(fileCreated, fileCreatedContent, false);
      }
    };

    int latestRevisionNumber = getLatestRevisionNumber(root);

    update(root, latestRevisionNumber - 1);
    testRevMinus1.run();
    update(root, latestRevisionNumber - 2);
    testRevMinus2.run();
    update(root, latestRevisionNumber - 1);
    testRevMinus1.run();
    update(root, latestRevisionNumber);
    testLastRev.run();
    update(root, latestRevisionNumber - 2);
    testRevMinus2.run();
    update(root, latestRevisionNumber);
    testLastRev.run();
  }

  

}