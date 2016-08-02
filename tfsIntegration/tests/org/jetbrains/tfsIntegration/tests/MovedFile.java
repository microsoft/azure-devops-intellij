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
import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.tests.TFSTestCase;
import org.junit.Test;

@SuppressWarnings({"HardCodedStringLiteral"})
public class MovedFile extends TFSTestCase {

  // moved file (up to date sandbox root\file_original.txt -> up to date sandbox root\subfolder\subfolder2\file_moved.txt)

  @Test
  public void testMovedFileFromUpToDateToUpToDate() throws VcsException {
    final String fileContent = "content";
    final String filenameOriginal = "file_original.txt";
    FilePath fileOriginal = TfsFileUtil.getFilePath(createFileInCommand(mySandboxRoot, filenameOriginal, fileContent));
    FilePath subfolder = TfsFileUtil.getFilePath(createDirInCommand(mySandboxRoot, "subfolder"));
    FilePath subfolder2 = TfsFileUtil.getFilePath(createDirInCommand(subfolder.getVirtualFile(), "subfolder2"));

    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);

    commit(getChanges().getChanges(), "test");
    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);
    assertFile(fileOriginal, fileContent, false);

    final String filenameMoved = "file_moved.txt";
    moveFileInCommand(fileOriginal, subfolder2.getVirtualFile());
    rename(getChildPath(subfolder2, filenameOriginal), filenameMoved);
    FilePath fileMoved = getChildPath(subfolder2, filenameMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFile(fileMoved, fileContent, false);

    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(fileOriginal, fileMoved);

    rollback(getChanges().getChanges());
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);
    assertFile(fileOriginal, fileContent, false);

    moveFileInCommand(fileOriginal, subfolder2.getVirtualFile());
    rename(getChildPath(subfolder2, filenameOriginal), filenameMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFile(fileMoved, fileContent, false);

    commit(getChanges().getChanges(), "test");

    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFile(fileMoved, fileContent, false);

    final int latestRevision = getLatestRevisionNumber(mySandboxRoot);

    update(mySandboxRoot, latestRevision - 1);
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);
    assertFile(fileOriginal, fileContent, false);

    update(mySandboxRoot, latestRevision);
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFile(fileMoved, fileContent, false);
  }

}
