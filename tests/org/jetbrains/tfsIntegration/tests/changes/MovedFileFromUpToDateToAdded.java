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
import org.jetbrains.tfsIntegration.tests.TestChangeListBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

// Remark 1: when checking in move change to 'Added' folder, add change is checked in implicitly

@SuppressWarnings({"HardCodedStringLiteral"})
public class MovedFileFromUpToDateToAdded extends ChangeTestCase {

  private static final String FILE_ORIGINAL = "file_original.txt";

  private FilePath myOriginalFile;
  private FilePath myMovedFile;

  private FilePath mySourceFolder;
  private FilePath myAddedFolder;

  protected void preparePaths() {
    mySourceFolder = getChildPath(mySandboxRoot, "SourceFolder");
    myAddedFolder = getChildPath(mySandboxRoot, "AddedFolder");
    myOriginalFile = getChildPath(mySourceFolder, FILE_ORIGINAL);
    myMovedFile = getChildPath(myAddedFolder, FILE_ORIGINAL);
  }

  protected void checkParentChangesPendingChildRolledBack() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForAddition(myAddedFolder);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySourceFolder, 1);
    assertFolder(myAddedFolder, 0);
    assertFile(myOriginalFile, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangePendingParentRolledBack() throws VcsException {
    // added folder on rollback becomes unversioned
    getChanges().assertTotalItems(2);
    getChanges().assertUnversioned(myAddedFolder);
    getChanges().assertRenamedOrMoved(myOriginalFile, myMovedFile);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySourceFolder, 0);
    assertFolder(myAddedFolder, 1);
    assertFile(myMovedFile, ORIGINAL_CONTENT, false);
  }

  protected void checkParentAndChildChangesPending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertScheduledForAddition(myAddedFolder);
    getChanges().assertRenamedOrMoved(myOriginalFile, myMovedFile);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySourceFolder, 0);
    assertFolder(myAddedFolder, 1);
    assertFile(myMovedFile, ORIGINAL_CONTENT, false);
  }

  protected void checkOriginalStateAfterRollbackParentChild() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertUnversioned(myAddedFolder);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySourceFolder, 1);
    assertFolder(myAddedFolder, 0);
    assertFile(myOriginalFile, ORIGINAL_CONTENT, false);
  }

  protected void checkOriginalStateAfterUpdate() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySourceFolder, 1);
    assertFile(myOriginalFile, ORIGINAL_CONTENT, false);
  }

  protected void checkParentChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySourceFolder, 1);
    assertFolder(myAddedFolder, 0);
    assertFile(myOriginalFile, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangeCommitted() throws VcsException {
    checkParentAndChildChangesCommitted(); // see remark 1
  }

  protected void checkParentAndChildChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySourceFolder, 0);
    assertFolder(myAddedFolder, 1);
    assertFile(myMovedFile, ORIGINAL_CONTENT, false);
  }

  protected void checkParentChangesCommittedChildPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalFile, myMovedFile);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySourceFolder, 0);
    assertFolder(myAddedFolder, 1);
    assertFile(myMovedFile, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangeCommittedParentPending() throws VcsException {
    checkParentAndChildChangesCommitted(); // see remark 1
  }

  protected void checkParentChangesPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForAddition(myAddedFolder);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySourceFolder, 1);
    assertFolder(myAddedFolder, 0);
    assertFile(myOriginalFile, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangePending() throws VcsException {
    final TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(2);
    changes.assertUnversioned(myAddedFolder);
    changes.assertRenamedOrMoved(myOriginalFile, myMovedFile);
    assertFile(myMovedFile, ORIGINAL_CONTENT, false);
  }

  protected void makeOriginalState() throws VcsException {
    createDirInCommand(mySourceFolder);
    createFileInCommand(myOriginalFile, ORIGINAL_CONTENT);
  }

  protected void makeParentChanges() throws VcsException {
    if (myAddedFolder.getIOFile().exists()) {
      if (getChanges().isUnversioned(myAddedFolder)) {
        scheduleForAddition(myAddedFolder);
      }
    }
    else {
      createDirInCommand(myAddedFolder);
    }
  }

  protected void makeChildChange(ParentChangesState parentChangesState) throws VcsException, IOException {
    moveFileInCommand(myOriginalFile, myAddedFolder);
  }

  protected Collection<Change> getPendingParentChanges() throws VcsException {
    final Change change = getChanges().getAddChange(myAddedFolder);
    return change != null ? Collections.singletonList(change) : Collections.<Change>emptyList();
  }

  @Nullable
  protected Change getPendingChildChange(ParentChangesState parentChangesState) throws VcsException {
    return getChanges().getMoveChange(myOriginalFile, myMovedFile);
  }

  @Test
  public void testPendingAndRollback() throws VcsException, IOException {
    super.testPendingAndRollback();
  }

  @Test
  public void testCommitParentThenChildChanges() throws VcsException, IOException {
    super.testCommitParentThenChildChanges();
  }

  // don't test: can't move file to non-existing folder
  //@Test
  //public void testCommitChildThenParentChanges() throws VcsException, IOException {
  //  super.testCommitChildThenParentChanges();
  //}

  @Test
  public void testCommitParentChangesChildPending() throws VcsException, IOException {
    super.testCommitParentChangesChildPending();
  }

  // don't test: can't move file to non-existing folder
  //@Test
  //public void testCommitChildChangesParentPending() throws VcsException, IOException {
  //  super.testCommitChildChangesParentPending();
  //}

}
