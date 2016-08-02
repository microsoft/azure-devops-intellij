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
public class ModifiedFileInRenamed extends ParentChildChangeTestCase {
  private FilePath myOriginalParentFolder;
  private FilePath myRenamedParentFolder;

  private FilePath myFileInOriginalFolder;
  private FilePath myFileInRenamedFolder;

  protected void preparePaths() {
    myOriginalParentFolder = getChildPath(mySandboxRoot, "Folder_Original");
    myRenamedParentFolder = getChildPath(mySandboxRoot, "Folder_Renamed");
    myFileInOriginalFolder = getChildPath(myOriginalParentFolder, "file.txt");
    myFileInRenamedFolder = getChildPath(myRenamedParentFolder, "file.txt");
  }

  protected void checkParentChangePendingChildRolledBack() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myRenamedParentFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 1);
    assertFile(myFileInRenamedFolder, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangePendingParentRolledBack() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertModified(myFileInOriginalFolder, ORIGINAL_CONTENT, MODIFIED_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myOriginalParentFolder, 1);
    assertFile(myFileInOriginalFolder, MODIFIED_CONTENT, true);
  }

  protected void checkParentAndChildChangesPending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myRenamedParentFolder);
    getChanges().assertModified(myFileInRenamedFolder, ORIGINAL_CONTENT, MODIFIED_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 1);
    assertFile(myFileInRenamedFolder, MODIFIED_CONTENT, true);
  }

  protected void checkOriginalStateAfterRollbackParentChild() throws VcsException {
    checkOriginalState();
  }

  protected void checkOriginalStateAfterUpdate() throws VcsException {
    checkOriginalState();
  }

  private void checkOriginalState() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myOriginalParentFolder, 1);
    assertFile(myFileInOriginalFolder, ORIGINAL_CONTENT, false);
  }

  protected void checkParentChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 1);
    assertFile(myFileInRenamedFolder, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 1);
    assertFolder(myOriginalParentFolder, 1);
    assertFile(myFileInOriginalFolder, MODIFIED_CONTENT, false);
  }

  protected void checkParentAndChildChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 1);
    assertFile(myFileInRenamedFolder, MODIFIED_CONTENT, false);
  }

  protected void checkParentChangeCommittedChildPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertModified(myFileInRenamedFolder, ORIGINAL_CONTENT, MODIFIED_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 1);
    assertFile(myFileInRenamedFolder, MODIFIED_CONTENT, true);
  }

  protected void checkChildChangeCommittedParentPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myRenamedParentFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 1);
    assertFile(myFileInRenamedFolder, MODIFIED_CONTENT, false);
  }

  protected void checkParentChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myRenamedParentFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myRenamedParentFolder, 1);
    assertFile(myFileInRenamedFolder, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertModified(myFileInOriginalFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myOriginalParentFolder, 1);
    assertFile(myFileInOriginalFolder, MODIFIED_CONTENT, true);
  }

  protected void makeOriginalState() throws VcsException {
    createDirInCommand(myOriginalParentFolder);
    createFileInCommand(myFileInOriginalFolder, ORIGINAL_CONTENT);
  }

  protected void makeParentChange() throws VcsException {
    renameFileInCommand(myOriginalParentFolder, myRenamedParentFolder.getName());
  }

  protected void makeChildChange(ParentChangeState parentChangeState) throws VcsException, IOException {
    final FilePath file = parentChangeState == ParentChangeState.NotDone ? myFileInOriginalFolder : myFileInRenamedFolder;
    editFiles(file);
    setFileContent(file, MODIFIED_CONTENT);
  }

  @Nullable
  protected Change getPendingParentChange() throws VcsException {
    return getChanges().getMoveChange(myOriginalParentFolder, myRenamedParentFolder);
  }

  @Nullable
  protected Change getPendingChildChange(ParentChangeState parentChangeState) throws VcsException {
    return getChanges()
      .getModificationChange(parentChangeState == ParentChangeState.NotDone ? myFileInOriginalFolder : myFileInRenamedFolder);
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
