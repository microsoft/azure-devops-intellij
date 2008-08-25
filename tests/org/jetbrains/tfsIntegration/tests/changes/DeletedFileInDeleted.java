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
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

// REMARKS:
// 1. when parent item is scheduled for deletion after its child was, child is reported as not downloaded and having no pending changes
// 

@SuppressWarnings({"HardCodedStringLiteral"})
public class DeletedFileInDeleted extends ChangeTestCase {
  private FilePath myParentFolder;
  private FilePath myChildFile;

  protected void preparePaths() {
    myParentFolder = getChildPath(mySandboxRoot, "Folder");
    myChildFile = getChildPath(myParentFolder, "file.txt");
  }

  protected void checkParentChangesPendingChildRolledBack() throws VcsException {
    Assert.fail("Can't happen"); // see remark 1
  }

  protected void checkChildChangePendingParentRolledBack() throws VcsException {
    checkOriginalStateAfterUpdate();
  }

  protected void checkParentAndChildChangesPending() throws VcsException {
    checkParentChangesPending(); // see remark 1
  }

  protected void checkOriginalStateAfterRollbackParentChild() throws VcsException {
    checkOriginalStateAfterUpdate();
  }

  protected void checkOriginalStateAfterUpdate() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentFolder, 1);
    assertFile(myChildFile, FILE_CONTENT, false);
  }

  protected void checkParentChangesCommittedChildPending() throws VcsException {
    Assert.fail("Can't happen"); // see remark 1
  }

  protected void checkChildChangeCommittedParentPending() throws VcsException {
    checkParentAndChildChangesPending();
  }

  protected void checkParentChangesPending() throws VcsException {
    // see remark 1
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForDeletion(myParentFolder);

    assertFolder(mySandboxRoot, 0);
  }

  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForDeletion(myChildFile);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentFolder, 0);
  }

  protected void checkParentChangesCommitted() throws VcsException {
    checkParentAndChildChangesCommitted();
  }

  protected void checkChildChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentFolder, 0);
  }

  protected void checkParentAndChildChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 0);
  }

  protected void makeOriginalState() throws VcsException {
    createDirInCommand(myParentFolder);
    createFileInCommand(myChildFile, FILE_CONTENT);
  }

  protected void makeParentChanges() throws VcsException {
    deleteFileInCommand(myParentFolder);
  }

  protected void makeChildChange(ParentChangesState parentChangesState) throws VcsException {
    if (parentChangesState == ParentChangesState.NotDone) {
      deleteFileInCommand(myChildFile);
    }
  }

  protected Collection<Change> getPendingParentChanges() throws VcsException {
    final Change change = getChanges().getDeleteChange(myParentFolder);
    return change != null ? Collections.singletonList(change) : Collections.<Change>emptyList();
  }

  protected Change getPendingChildChange(ParentChangesState parentChangesState) throws VcsException {
    return parentChangesState == ParentChangesState.NotDone ? getChanges().getDeleteChange(myChildFile) : null;
  }

  @Test
  public void testPendingAndRollback() throws VcsException, IOException {
    super.testPendingAndRollback();
  }

  // don't test it, see remark 1
  //@Test
  //public void testCommitParentThenChildChanges() throws VcsException, IOException {
  //  super.testCommitParentThenChildChanges();
  //}

  @Test
  public void testCommitChildThenParentChanges() throws VcsException, IOException {
    super.testCommitChildThenParentChanges();
  }

  // don't test it, see remark 1
  //@Test
  //public void testCommitParentChangesChildPending() throws VcsException, IOException {
  //  super.testCommitParentChangesChildPending();
  //}

  @Test
  public void testCommitChildChangesParentPending() throws VcsException, IOException {
    super.testCommitChildChangesParentPending();
  }

}