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
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

// REMARK 1:
// 1. rename child folder
// 2. then move parent folder
// 3. try to undo file rename
// -> TF14059: Unable to process the pending changes requested. The set of pending renames would cause a name collision for <original child folder>
// does not repeat if child is file - bug in TFS?

@SuppressWarnings({"HardCodedStringLiteral"})
public class RenamedFolderInMoved extends ChangeTestCase {
  private static final String FOLDERNAME_RENAMED = "RenamedChild";

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
    final String foldernameOriginal = "OriginalChild";
    myChildOriginalInParentOriginal = getChildPath(myParentOriginal, foldernameOriginal);
    myChildOriginalInParentMoved = getChildPath(myParentMoved, foldernameOriginal);
    myChildRenamedInParentOriginal = getChildPath(myParentOriginal, FOLDERNAME_RENAMED);
    myChildRenamedInParentMoved = getChildPath(myParentMoved, FOLDERNAME_RENAMED);
  }

  protected void checkParentChangesPendingChildRolledBack() throws VcsException {
    checkParentChangesPending();
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
    assertFolder(myChildRenamedInParentMoved, 0);
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
    assertFolder(myChildOriginalInParentOriginal, 0);
  }

  protected void checkParentChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentMoved, 1);
    assertFolder(myChildOriginalInParentMoved, 0);
  }

  protected void checkChildChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myParentOriginal, 1);
    assertFolder(myChildRenamedInParentOriginal, 0);
  }

  protected void checkParentAndChildChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentMoved, 1);
    assertFolder(myChildRenamedInParentMoved, 0);
  }

  protected void checkParentChangesCommittedChildPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myChildOriginalInParentMoved, myChildRenamedInParentMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentMoved, 1);
    assertFolder(myChildRenamedInParentMoved, 0);
  }

  protected void checkChildChangeCommittedParentPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myParentOriginal, myParentMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentMoved, 1);
    assertFolder(myChildRenamedInParentMoved, 0);
  }

  protected void checkParentChangesPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myParentOriginal, myParentMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myParentMoved, 1);
    assertFolder(myChildOriginalInParentMoved, 0);
  }

  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myChildOriginalInParentOriginal, myChildRenamedInParentOriginal);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myParentOriginal, 1);
    assertFolder(myChildRenamedInParentOriginal, 0);
  }

  protected void makeOriginalState() throws VcsException {
    createDirInCommand(myParentOriginal);
    createDirInCommand(myChildOriginalInParentOriginal);
    createDirInCommand(mySubfolder1);
    createDirInCommand(mySubfolder2);
  }

  protected void makeParentChanges() throws VcsException {
    moveFileInCommand(myParentOriginal, mySubfolder2);
  }

  protected void makeChildChange(ParentChangesState parentChangesState) throws VcsException, IOException {
    rename(parentChangesState == ParentChangesState.NotDone ? myChildOriginalInParentOriginal : myChildOriginalInParentMoved,
           FOLDERNAME_RENAMED);
  }

  protected Collection<Change> getPendingParentChanges() throws VcsException {
    final Change change = getChanges().getMoveChange(myParentOriginal, myParentMoved);
    return change != null ? Collections.singletonList(change) : Collections.<Change>emptyList();
  }

  @Nullable
  protected Change getPendingChildChange(ParentChangesState parentChangesState) throws VcsException {
    if (parentChangesState == ParentChangesState.NotDone) {
      return getChanges().getMoveChange(myChildOriginalInParentOriginal, myChildRenamedInParentOriginal);
    }
    else if (parentChangesState == ParentChangesState.Pending) {
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

  protected boolean shouldTestRollbackChildPendingParent() {
    return false; // see remark 1
  }
}