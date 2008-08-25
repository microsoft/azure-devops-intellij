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

package org.jetbrains.tfsIntegration.tests.changes;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.tests.TFSTestCase;
import org.junit.Test;

import java.io.IOException;

@SuppressWarnings({"HardCodedStringLiteral"})
public class MovedModifiedFile extends TFSTestCase {

  // moved and modified file (up to date sandbox root\file_original.txt -> up to date sandbox root\subfolder\subfolder2\file_moved.txt)

  @Test
  public void testMovedModifiedFileFromUpToDateToUpToDate() throws VcsException, IOException {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);

    final String fileContentOriginal = "content_original";
    final String filenameOriginal = "file_original.txt";
    FilePath fileOriginal = TfsFileUtil.getFilePath(createFileInCommand(mySandboxRoot, filenameOriginal, fileContentOriginal));
    FilePath subfolder = TfsFileUtil.getFilePath(createDirInCommand(mySandboxRoot, "subfolder"));
    FilePath subfolder2 = TfsFileUtil.getFilePath(createDirInCommand(subfolder.getVirtualFile(), "subfolder2"));

    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);
    commit(getChanges().getChanges(), "test");

    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);
    assertFile(fileOriginal, fileContentOriginal, false);

    final String filenameMoved = "file_moved.txt";
    moveFileInCommand(fileOriginal, subfolder2.getVirtualFile());
    rename(getChildPath(subfolder2, filenameOriginal), filenameMoved);
    FilePath fileMoved = getChildPath(subfolder2, filenameMoved);
    editFiles(VcsUtil.getVirtualFile(fileMoved.getIOFile()));
    final String fileContentModified = "content_modified";
    setFileContent(fileMoved, fileContentModified);

    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFile(fileMoved, fileContentModified, true);

    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(fileOriginal, fileMoved, fileContentOriginal, fileContentModified);

    rollback(getChanges().getChanges());
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);
    assertFile(fileOriginal, fileContentOriginal, false);

    moveFileInCommand(fileOriginal, subfolder2.getVirtualFile());
    rename(getChildPath(subfolder2, filenameOriginal), filenameMoved);
    editFiles(VcsUtil.getVirtualFile(fileMoved.getIOFile()));
    setFileContent(fileMoved, fileContentModified);

    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFile(fileMoved, fileContentModified, true);

    commit(getChanges().getChanges(), "test");

    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFile(fileMoved, fileContentModified, false);

    final int latestRevision = getLatestRevisionNumber(mySandboxRoot);

    update(mySandboxRoot, latestRevision - 1);
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);
    assertFile(fileOriginal, fileContentOriginal, false);

    update(mySandboxRoot, latestRevision);
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFile(fileMoved, fileContentModified, false);
  }


}
