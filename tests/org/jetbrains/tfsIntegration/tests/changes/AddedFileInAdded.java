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
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

// REMARKS:
// 1. add file without parent folder -> only file sheduled for addition, not pending change for parent,
// but TFC shows folder as added too
// 2. when commiting son's addition parent folder is not mentioned, but it appeares as up to date after commit and in commit details
// 3. when parent and child added, and pending change for parent reverted, in TFC after refresh it looks like case 1 

// TODO show parent report unversioned folder as added if some of its children is added (remark 1) ?

@SuppressWarnings({"HardCodedStringLiteral"})
public class AddedFileInAdded extends ChangeTestCase {
  private FilePath myAddedParentFolder;
  private FilePath myAddedChildFile;

  protected void preparePaths() {
    myAddedParentFolder = getChildPath(mySandboxRoot, "AddedFolder");
    myAddedChildFile = getChildPath(myAddedParentFolder, "added_file.txt");
  }

  protected void checkParentChangesPendingChildRolledBack() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertScheduledForAddition(myAddedParentFolder);
    getChanges().assertUnversioned(myAddedChildFile);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFile(myAddedChildFile, FILE_CONTENT, true);
  }

  protected void checkChildChangePendingParentRolledBack() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertUnversioned(myAddedParentFolder); // see remark 1
    getChanges().assertScheduledForAddition(myAddedChildFile);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFile(myAddedChildFile, FILE_CONTENT, true);
  }

  protected void checkParentAndChildChangesPending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertScheduledForAddition(myAddedParentFolder);
    getChanges().assertScheduledForAddition(myAddedChildFile);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFile(myAddedChildFile, FILE_CONTENT, true);
  }

  protected void checkOriginalStateAfterRollbackParentChild() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertUnversioned(myAddedParentFolder);
    getChanges().assertUnversioned(myAddedChildFile);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFile(myAddedChildFile, FILE_CONTENT, true);
  }

  protected void checkOriginalStateAfterUpdate() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 0);
  }

  protected void checkParentChangesCommittedChildPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForAddition(myAddedChildFile);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFile(myAddedChildFile, FILE_CONTENT, true);
  }

  protected void checkChildChangeCommittedParentPending() throws VcsException {
    checkParentAndChildChangesCommitted(); // see remark 2
  }

  protected void checkParentChangesPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForAddition(myAddedParentFolder);
    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 0);
  }

  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertUnversioned(myAddedParentFolder); // see remark 1
    getChanges().assertScheduledForAddition(myAddedChildFile);
    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFile(myAddedChildFile, FILE_CONTENT, true);
  }

  protected void checkParentChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 0);
  }

  protected void checkChildChangeCommitted() throws VcsException {
    checkParentAndChildChangesCommitted(); // see remark 2
  }

  protected void checkParentAndChildChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFile(myAddedChildFile, FILE_CONTENT, false);
  }

  protected void makeOriginalState() throws VcsException {
  }

  protected void makeParentChanges() throws VcsException {
    if (myAddedParentFolder.getIOFile().exists()) {
      // parent folder can be already added, see remark 2
      if (getChanges().isUnversioned(myAddedParentFolder)) {
        scheduleForAddition(myAddedParentFolder);
      }
    }
    else {
      createDirInCommand(myAddedParentFolder);
    }
  }

  protected void makeChildChange(ParentChangesState parentChangesState) throws VcsException {
    if (parentChangesState == ParentChangesState.NotDone) {
      myAddedParentFolder.getIOFile().mkdirs();
      refreshAll();
    }

    if (myAddedChildFile.getIOFile().exists()) {
      scheduleForAddition(myAddedChildFile);
    }
    else {
      createFileInCommand(myAddedChildFile, FILE_CONTENT);
    }
  }

  protected Collection<Change> getPendingParentChanges() throws VcsException {
    final Change change = getChanges().getAddChange(myAddedParentFolder);
    return change != null ? Collections.singletonList(change) : Collections.<Change>emptyList();
  }

  protected Change getPendingChildChange(ParentChangesState parentChangesState) throws VcsException {
    return getChanges().getAddChange(myAddedChildFile);
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
