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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.vcsUtil.VcsUtil;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

@SuppressWarnings({"HardCodedStringLiteral"})
public class UpToDateFileInMoved extends ParentChangeTestCase {
  private static final String FILE_CONTENT = "file content";

  private FilePath myParentFolderOriginal;
  private FilePath myChildFileOriginal;
  private FilePath myParentFolderMoved;
  private FilePath myChildFileInMovedFolder;
  private FilePath mySubfolder;
  private FilePath mySubfolder2;

  protected void preparePaths() {
    final String filename = "up_to_date.txt";
    final String foldernameOriginal = "Folder_Original";

    myParentFolderOriginal = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), foldernameOriginal));
    myChildFileOriginal = VcsUtil.getFilePath(new File(new File(myParentFolderOriginal.getPath()), filename));
    mySubfolder = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), "Subfolder"));
    mySubfolder2 = VcsUtil.getFilePath(new File(new File(mySubfolder.getPath()), "Subfolder2"));
    myParentFolderMoved = VcsUtil.getFilePath(new File(new File(mySubfolder2.getPath()), foldernameOriginal));
    myChildFileInMovedFolder = VcsUtil.getFilePath(new File(new File(myParentFolderMoved.getPath()), filename));
  }

  protected void makeOriginalState() throws VcsException {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    createDirInCommand(myParentFolderOriginal);
    createFileInCommand(myChildFileOriginal, FILE_CONTENT);
    createDirInCommand(mySubfolder);
    createDirInCommand(mySubfolder2);
    commit(getChanges().getChanges(), "original state");
  }

  protected void checkOriginalState() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myParentFolderOriginal, 1);
    assertFile(myChildFileOriginal, FILE_CONTENT, false);
  }

  protected void makeParentChanges() throws VcsException {
    moveFileInCommand(myParentFolderOriginal, VcsUtil.getVirtualFile(mySubfolder2.getIOFile()));
  }

  protected void checkParentChangesPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myParentFolderOriginal, myParentFolderMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentFolderMoved, 1);
    assertFile(myChildFileInMovedFolder, FILE_CONTENT, false);
  }

  protected void checkParentChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentFolderMoved, 1);
    assertFile(myChildFileInMovedFolder, FILE_CONTENT, false);
  }

  protected Collection<Change> getPendingParentChanges() throws VcsException {
    return Collections.singletonList(getChanges().getMoveChange(myParentFolderOriginal, myParentFolderMoved));
  }

  @Test
  public void doTest() throws VcsException {
    super.doTest();
  }

}