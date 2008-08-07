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
import com.intellij.vcsUtil.VcsUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

// see remarks for AddedFileInAdded test case

@SuppressWarnings({"HardCodedStringLiteral"})
public class AddedFolderInAdded extends ChangeTestCase {
  private FilePath myAddedParentFolder;
  private FilePath myAddedChildFolder;

  protected void preparePaths() {
    myAddedParentFolder = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), "AddedFolder"));
    myAddedChildFolder = VcsUtil.getFilePath(new File(new File(myAddedParentFolder.getPath()), "AddedSubfolder"));
  }

  protected void checkParentChangesPendingChildRolledBack() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertScheduledForAddition(myAddedParentFolder);
    getChanges().assertUnversioned(myAddedChildFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  protected void checkChildChangePendingParentRolledBack() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertUnversioned(myAddedParentFolder);
    getChanges().assertScheduledForAddition(myAddedChildFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  protected void checkParentAndChildChangesPending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertScheduledForAddition(myAddedParentFolder);
    getChanges().assertScheduledForAddition(myAddedChildFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  protected void checkOriginalStateAfterRollbackParentChild() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertUnversioned(myAddedParentFolder);
    getChanges().assertUnversioned(myAddedChildFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  protected void checkOriginalStateAfterUpdate() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 0);
  }

  protected void checkParentChangesCommittedChildPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForAddition(myAddedChildFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myAddedParentFolder, 1);
    assertFolder(myAddedChildFolder, 0);
  }

  protected void checkChildChangeCommittedParentPending() throws VcsException {
    checkParentAndChildChangesCommitted(); // see remark 2
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
    assertFolder(myAddedChildFolder, 0);
  }

  protected void makeOriginalState() throws VcsException {
  }

  protected void makeParentChanges() throws VcsException {
    if (myAddedParentFolder.getIOFile().exists()) {
      if (getChanges().isUnversioned(myAddedParentFolder)) {
        scheduleForAddition(myAddedParentFolder);
      }
    }
    else {
      createDirInCommand(myAddedParentFolder);
    }
  }

  protected void makeChildChange(boolean parentChangeMade) throws VcsException {
    if (!parentChangeMade) {
      myAddedParentFolder.getIOFile().mkdirs();
      refreshAll();
    }

    if (myAddedChildFolder.getIOFile().exists()) {
      scheduleForAddition(myAddedChildFolder);
    }
    else {
      createDirInCommand(myAddedChildFolder);
    }
  }

  protected Collection<Change> getPendingParentChanges() throws VcsException {
    final Change change = getChanges().getAddChange(myAddedParentFolder);
    return change != null ? Collections.singletonList(change) : Collections.<Change>emptyList();
  }

  protected Change getPendingChildChange(boolean parentChangesMade) throws VcsException {
    return getChanges().getAddChange(myAddedChildFolder);
  }

  @Test
  public void doTestPendingAndRollback() throws VcsException, IOException {
    super.doTestPendingAndRollback();
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