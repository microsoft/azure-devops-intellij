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
public class RenamedFileInMoved extends ParentChildChangeTestCase {
  private static final String FILENAME_RENAMED = "renamed.txt";

  private FilePath myParentOriginal;
  private FilePath myParentMoved;
  private FilePath myChildOriginalInParentOriginal;
  private FilePath myChildOriginalInParentMoved;
  private FilePath myChildRenamedInParentOriginal;
  private FilePath myChildRenamedInParentMoved;
  private FilePath mySubfolder1;
  private FilePath mySubfolder2;

  protected void preparePaths() {
    mySubfolder1 = getChildPath(mySandboxRoot, "Subfolder1");
    mySubfolder2 = getChildPath(mySubfolder1, "Subfolder2");

    final String parentFolderName = "ParentFolder";
    myParentOriginal = getChildPath(mySandboxRoot, parentFolderName);
    myParentMoved = getChildPath(mySubfolder2, parentFolderName);
    final String filenameOriginal = "original.txt";
    myChildOriginalInParentOriginal = getChildPath(myParentOriginal, filenameOriginal);
    myChildOriginalInParentMoved = getChildPath(myParentMoved, filenameOriginal);
    myChildRenamedInParentOriginal = getChildPath(myParentOriginal, FILENAME_RENAMED);
    myChildRenamedInParentMoved = getChildPath(myParentMoved, FILENAME_RENAMED);
  }

  protected void checkParentChangePendingChildRolledBack() throws VcsException {
    checkParentChangePending();
  }

  protected void checkChildChangePendingParentRolledBack() throws VcsException {
    checkChildChangePending();
  }

  protected void checkParentAndChildChangesPending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertRenamedOrMoved(myParentOriginal, myParentMoved);
    getChanges().assertRenamedOrMoved(myChildOriginalInParentOriginal, myChildRenamedInParentMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentMoved, 1);
    assertFile(myChildRenamedInParentMoved, FILE_CONTENT, false);
  }

  protected void checkOriginalStateAfterRollbackParentChild() throws VcsException {
    checkOriginalStateAfterUpdate();
  }

  protected void checkOriginalStateAfterUpdate() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myParentOriginal, 1);
    assertFile(myChildOriginalInParentOriginal, FILE_CONTENT, false);
  }

  protected void checkParentChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentMoved, 1);
    assertFile(myChildOriginalInParentMoved, FILE_CONTENT, false);
  }

  protected void checkChildChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myParentOriginal, 1);
    assertFile(myChildRenamedInParentOriginal, FILE_CONTENT, false);
  }

  protected void checkParentAndChildChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentMoved, 1);
    assertFile(myChildRenamedInParentMoved, FILE_CONTENT, false);
  }

  protected void checkParentChangeCommittedChildPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myChildOriginalInParentMoved, myChildRenamedInParentMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentMoved, 1);
    assertFile(myChildRenamedInParentMoved, FILE_CONTENT, false);
  }

  protected void checkChildChangeCommittedParentPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myParentOriginal, myParentMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentMoved, 1);
    assertFile(myChildRenamedInParentMoved, FILE_CONTENT, false);
  }

  protected void checkParentChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myParentOriginal, myParentMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentMoved, 1);
    assertFile(myChildOriginalInParentMoved, FILE_CONTENT, false);
  }

  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myChildOriginalInParentOriginal, myChildRenamedInParentOriginal);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myParentOriginal, 1);
    assertFile(myChildRenamedInParentOriginal, FILE_CONTENT, false);
  }

  protected void makeOriginalState() throws VcsException {
    createDirInCommand(myParentOriginal);
    createFileInCommand(myChildOriginalInParentOriginal, FILE_CONTENT);
    createDirInCommand(mySubfolder1);
    createDirInCommand(mySubfolder2);
  }

  protected void makeParentChange() throws VcsException {
    moveFileInCommand(myParentOriginal, mySubfolder2);
  }

  protected void makeChildChange(ParentChangeState parentChangeState) throws VcsException, IOException {
    rename(parentChangeState == ParentChangeState.NotDone ? myChildOriginalInParentOriginal : myChildOriginalInParentMoved,
           FILENAME_RENAMED);
  }

  @Nullable
  protected Change getPendingParentChange() throws VcsException {
    return getChanges().getMoveChange(myParentOriginal, myParentMoved);
  }

  @Nullable
  protected Change getPendingChildChange(ParentChangeState parentChangeState) throws VcsException {
    if (parentChangeState == ParentChangeState.NotDone) {
      return getChanges().getMoveChange(myChildOriginalInParentOriginal, myChildRenamedInParentOriginal);
    }
    else if (parentChangeState == ParentChangeState.Pending) {
      return getChanges().getMoveChange(myChildOriginalInParentOriginal, myChildRenamedInParentMoved);
    }
    else {
      return getChanges().getMoveChange(myChildOriginalInParentMoved, myChildRenamedInParentMoved);
    }
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