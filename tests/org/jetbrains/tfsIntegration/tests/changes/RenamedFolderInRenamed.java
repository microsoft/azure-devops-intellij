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
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

@SuppressWarnings({"HardCodedStringLiteral"})
public class RenamedFolderInRenamed extends ChangeTestCase {
  private static final String CHILDFOLDERNAME_RENAMED = "ChildRenamed";

  private FilePath myParentOriginal;
  private FilePath myParentRenamed;
  private FilePath myChildOriginalInParentOriginal;
  private FilePath myChildOriginalInParentRenamed;
  private FilePath myChildRenamedInParentOriginal;
  private FilePath myChildRenamedInParentRenamed;

  protected void preparePaths() {
    myParentOriginal = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), "Original"));
    myParentRenamed = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), "Renamed"));
    final String filenameOriginal = "ChildOriginal";
    myChildOriginalInParentOriginal = VcsUtil.getFilePath(new File(myParentOriginal.getIOFile(), filenameOriginal));
    myChildOriginalInParentRenamed = VcsUtil.getFilePath(new File(myParentRenamed.getIOFile(), filenameOriginal));
    myChildRenamedInParentOriginal = VcsUtil.getFilePath(new File(myParentOriginal.getIOFile(), CHILDFOLDERNAME_RENAMED));
    myChildRenamedInParentRenamed = VcsUtil.getFilePath(new File(myParentRenamed.getIOFile(), CHILDFOLDERNAME_RENAMED));
  }

  protected void checkParentChangesPendingChildRolledBack() throws VcsException {
    checkParentChangesPending();
  }

  protected void checkChildChangePendingParentRolledBack() throws VcsException {
    checkChildChangePending();
  }

  protected void checkParentAndChildChangesPending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertRenamedOrMoved(myParentOriginal, myParentRenamed);
    getChanges().assertRenamedOrMoved(myChildOriginalInParentOriginal, myChildRenamedInParentRenamed);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentRenamed, 1);
    assertFolder(myChildRenamedInParentRenamed, 0);
  }

  protected void checkOriginalStateAfterRollbackParentChild() throws VcsException {
    checkOriginalStateAfterUpdate();
  }

  protected void checkOriginalStateAfterUpdate() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentOriginal, 1);
    assertFolder(myChildOriginalInParentOriginal, 0);
  }

  protected void checkParentChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentRenamed, 1);
    assertFolder(myChildOriginalInParentRenamed, 0);
  }

  protected void checkChildChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentOriginal, 1);
    assertFolder(myChildRenamedInParentOriginal, 0);
  }

  protected void checkParentAndChildChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentRenamed, 1);
    assertFolder(myChildRenamedInParentRenamed, 0);
  }

  protected void checkParentChangesCommittedChildPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myChildOriginalInParentRenamed, myChildRenamedInParentRenamed);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentRenamed, 1);
    assertFolder(myChildRenamedInParentRenamed, 0);
  }

  protected void checkChildChangeCommittedParentPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myParentOriginal, myParentRenamed);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentRenamed, 1);
    assertFolder(myChildRenamedInParentRenamed, 0);
  }

  protected void checkParentChangesPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myParentOriginal, myParentRenamed);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentRenamed, 1);
    assertFolder(myChildOriginalInParentRenamed, 0);
  }

  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myChildOriginalInParentOriginal, myChildRenamedInParentOriginal);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentOriginal, 1);
    assertFolder(myChildRenamedInParentOriginal, 0);
  }

  protected void makeOriginalState() throws VcsException {
    createDirInCommand(myParentOriginal);
    createDirInCommand(myChildOriginalInParentOriginal);
  }

  protected void makeParentChanges() throws VcsException {
    rename(myParentOriginal, myParentRenamed.getName());
  }

  protected void makeChildChange(ParentChangesState parentChangesState) throws VcsException, IOException {
    rename(parentChangesState == ParentChangesState.NotDone ? myChildOriginalInParentOriginal : myChildOriginalInParentRenamed,
           CHILDFOLDERNAME_RENAMED);
  }

  protected Collection<Change> getPendingParentChanges() throws VcsException {
    final Change change = getChanges().getMoveChange(myParentOriginal, myParentRenamed);
    return change != null ? Collections.singletonList(change) : Collections.<Change>emptyList();
  }

  @Nullable
  protected Change getPendingChildChange(ParentChangesState parentChangesState) throws VcsException {
    if (parentChangesState == ParentChangesState.NotDone) {
      return getChanges().getMoveChange(myChildOriginalInParentOriginal, myChildRenamedInParentOriginal);
    }
    else if (parentChangesState == ParentChangesState.Pending) {
      return getChanges().getMoveChange(myChildOriginalInParentOriginal, myChildRenamedInParentRenamed);
    }
    else {
      return getChanges().getMoveChange(myChildOriginalInParentRenamed, myChildRenamedInParentRenamed);
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