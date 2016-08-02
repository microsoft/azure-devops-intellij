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

package org.jetbrains.tfsIntegration.tests.parentchange;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.vcsUtil.VcsUtil;
import org.junit.Test;
import org.jetbrains.tfsIntegration.tests.parentchange.ParentChangeTestCase;

import java.util.Collection;
import java.util.Collections;

@SuppressWarnings({"HardCodedStringLiteral"})
public class UpToDateFolderInMoved extends ParentChangeTestCase {
  private static final String FILE_CONTENT = "file content";

  private FilePath myParentFolderOriginal;
  private FilePath myChildFolderOriginal;
  private FilePath myParentFolderMoved;
  private FilePath myChildFolderInMovedFolder;
  private FilePath mySubfolder;
  private FilePath mySubfolder2;

  protected void preparePaths() {
    final String filename = "up_to_date.txt";
    final String foldernameOriginal = "Folder_Original";

    myParentFolderOriginal = getChildPath(mySandboxRoot, foldernameOriginal);
    myChildFolderOriginal = getChildPath(myParentFolderOriginal, filename);
    mySubfolder = getChildPath(mySandboxRoot, "Subfolder");
    mySubfolder2 = getChildPath(mySubfolder, "Subfolder2");
    myParentFolderMoved = getChildPath(mySubfolder2, foldernameOriginal);
    myChildFolderInMovedFolder = getChildPath(myParentFolderMoved, filename);
  }

  protected void makeOriginalState() throws VcsException {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    createDirInCommand(myParentFolderOriginal);
    createDirInCommand(myChildFolderOriginal);
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
    assertFolder(myChildFolderOriginal, 0);
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
    assertFolder(myChildFolderInMovedFolder, 0);
  }

  protected void checkParentChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentFolderMoved, 1);
    assertFolder(myChildFolderInMovedFolder, 0);
  }

  protected Collection<Change> getPendingParentChanges() throws VcsException {
    return Collections.singletonList(getChanges().getMoveChange(myParentFolderOriginal, myParentFolderMoved));
  }

  @Test
  public void doTest() throws VcsException {
    super.doTest();
  }

}