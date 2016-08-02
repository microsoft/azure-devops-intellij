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

package org.jetbrains.tfsIntegration.tests.parentchildchange;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.IOException;

@SuppressWarnings({"HardCodedStringLiteral"})
public class ModifiedFileInMoved extends ParentChildChangeTestCase {
  private FilePath myOriginalParentFolder;
  private FilePath myMovedParentFolder;

  private FilePath myFileInOriginalFolder;
  private FilePath myFileInMovedFolder;

  private FilePath mySubfolder1;
  private FilePath mySubfolder2;

  protected void preparePaths() {
    final String folderName = "Folder_Original";
    myOriginalParentFolder = getChildPath(mySandboxRoot, folderName);
    mySubfolder1 = getChildPath(mySandboxRoot, "Subfolder1");
    mySubfolder2 = getChildPath(mySubfolder1, "Subfolder2");
    myMovedParentFolder = getChildPath(mySubfolder2, folderName);
    myFileInOriginalFolder = getChildPath(myOriginalParentFolder, "file.txt");
    myFileInMovedFolder = getChildPath(myMovedParentFolder, "file.txt");
  }

  protected void checkParentChangePendingChildRolledBack() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myMovedParentFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangePendingParentRolledBack() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertModified(myFileInOriginalFolder, ORIGINAL_CONTENT, MODIFIED_CONTENT);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myOriginalParentFolder, 1);
    assertFile(myFileInOriginalFolder, MODIFIED_CONTENT, true);
  }

  protected void checkParentAndChildChangesPending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myMovedParentFolder);
    getChanges().assertModified(myFileInMovedFolder, ORIGINAL_CONTENT, MODIFIED_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, MODIFIED_CONTENT, true);
  }

  protected void checkOriginalStateAfterRollbackParentChild() throws VcsException {
    checkOriginalState();
  }

  protected void checkOriginalStateAfterUpdate() throws VcsException {
    checkOriginalState();
  }

  private void checkOriginalState() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myOriginalParentFolder, 1);
    assertFile(myFileInOriginalFolder, ORIGINAL_CONTENT, false);
  }

  protected void checkParentChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myOriginalParentFolder, 1);
    assertFile(myFileInOriginalFolder, MODIFIED_CONTENT, false);
  }

  protected void checkParentAndChildChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, MODIFIED_CONTENT, false);
  }

  protected void checkParentChangeCommittedChildPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertModified(myFileInMovedFolder, ORIGINAL_CONTENT, MODIFIED_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, MODIFIED_CONTENT, true);
  }

  protected void checkChildChangeCommittedParentPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myMovedParentFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, MODIFIED_CONTENT, false);
  }

  protected void checkParentChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myMovedParentFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertModified(myFileInOriginalFolder);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myOriginalParentFolder, 1);
    assertFile(myFileInOriginalFolder, MODIFIED_CONTENT, true);
  }

  protected void makeOriginalState() throws VcsException {
    createDirInCommand(myOriginalParentFolder);
    createDirInCommand(mySubfolder1);
    createDirInCommand(mySubfolder2);
    createFileInCommand(myFileInOriginalFolder, ORIGINAL_CONTENT);
  }

  protected void makeParentChange() throws VcsException {
    moveFileInCommand(myOriginalParentFolder, mySubfolder2);
  }

  protected void makeChildChange(ParentChangeState parentChangeState) throws VcsException, IOException {
    final FilePath file = parentChangeState == ParentChangeState.NotDone ? myFileInOriginalFolder : myFileInMovedFolder;
    editFiles(file);
    setFileContent(file, MODIFIED_CONTENT);
  }

  @Nullable
  protected Change getPendingParentChange() throws VcsException {
    return getChanges().getMoveChange(myOriginalParentFolder, myMovedParentFolder);
  }

  @Nullable
  protected Change getPendingChildChange(ParentChangeState parentChangeState) throws VcsException {
    return getChanges()
      .getModificationChange(parentChangeState == ParentChangeState.NotDone ? myFileInOriginalFolder : myFileInMovedFolder);
  }

  @Test
  public void testPendingAndRollback() throws VcsException, IOException {
    super.testPendingAndRollback();
  }

  @Test
  public void testCommitParentThenChildChanges() throws VcsException, IOException {
    super.testCommitParentThenChildChanges();
  }

  @Test
  public void testCommitChildThenParentChanges() throws VcsException, IOException {
    super.testCommitChildThenParentChanges();
  }

  @Test
  public void testCommitParentChangesChildPending() throws VcsException, IOException {
    super.testCommitParentChangesChildPending();
  }

  @Test
  public void testCommitChildChangesParentPending() throws VcsException, IOException {
    super.testCommitChildChangesParentPending();
  }
}