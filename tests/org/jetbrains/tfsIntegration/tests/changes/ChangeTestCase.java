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

import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.tfsIntegration.tests.TFSTestCase;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.io.IOException;

@SuppressWarnings({"HardCodedStringLiteral"})
public abstract class ChangeTestCase extends TFSTestCase {

  protected static final String ORIGINAL_CONTENT = "original content";
  protected static final String MODIFIED_CONTENT = "modified content";
  protected static final String FILE_CONTENT = "file_content";


  protected abstract void preparePaths();

  protected abstract void checkParentChangesPendingChildRolledBack() throws VcsException;

  protected abstract void checkChildChangePendingParentRolledBack() throws VcsException;

  protected abstract void checkParentAndChildChangesPending() throws VcsException;

  protected abstract void checkOriginalStateAfterRollbackParentChild() throws VcsException;

  protected abstract void checkOriginalStateAfterUpdate() throws VcsException;

  protected abstract void checkParentChangesCommitted() throws VcsException;

  protected abstract void checkChildChangeCommitted() throws VcsException;

  protected abstract void checkParentAndChildChangesCommitted() throws VcsException;

  protected abstract void checkParentChangesCommittedChildPending() throws VcsException;

  protected abstract void checkChildChangeCommittedParentPending() throws VcsException;


  protected abstract void makeOriginalState() throws VcsException;

  protected abstract void makeParentChanges() throws VcsException;

  protected abstract void makeChildChange(boolean parentChangesMade) throws VcsException, IOException;

  protected abstract Collection<Change> getPendingParentChanges() throws VcsException;

  @Nullable
  protected abstract Change getPendingChildChange(boolean parentChangesMade) throws VcsException;


  protected void doTestPendingAndRollback() throws VcsException, IOException {
    preparePaths();

    makeOriginalState();
    checkOriginalStateAfterUpdate();

    makeParentChanges();
    makeChildChange(true);
    checkParentAndChildChangesPending();
    updateTo(0);
    checkParentAndChildChangesPending();
    rollback(getPendingParentChanges());
    checkChildChangePendingParentRolledBack();
    rollback(getPendingChildChange(false));
    checkOriginalStateAfterRollbackParentChild();

    makeChildChange(false);
    makeParentChanges();
    checkParentAndChildChangesPending();
    rollback(getPendingChildChange(true));
    checkParentChangesPendingChildRolledBack();
    rollback(getPendingParentChanges());
    checkOriginalStateAfterRollbackParentChild();
  }


  protected void testCommitParentThenChildChanges() throws VcsException, IOException {
    preparePaths();

    makeOriginalState();
    checkOriginalStateAfterUpdate();

    makeParentChanges();
    commit(getPendingParentChanges(), "parent changes");
    checkParentChangesCommitted();
    makeChildChange(true);
    commit(getPendingChildChange(true), "child change");
    checkParentAndChildChangesCommitted();

    updateTo(2);
    checkOriginalStateAfterUpdate();
    updateTo(1);
    checkParentChangesCommitted();
    updateTo(0);
    checkParentAndChildChangesCommitted();
  }

  protected void testCommitChildThenParentChanges() throws VcsException, IOException {
    preparePaths();

    makeOriginalState();
    checkOriginalStateAfterUpdate();
    makeChildChange(false);
    commit(getPendingChildChange(false), "child change");
    checkChildChangeCommitted();
    makeParentChanges();

    final Collection<Change> parentChanges = getPendingParentChanges();
    if (!parentChanges.isEmpty()) {
      commit(parentChanges, "parent changes");
      checkParentAndChildChangesCommitted();
    }

    if (parentChanges.isEmpty()) {
      updateTo(1);
      checkOriginalStateAfterUpdate();
      updateTo(0);
      checkParentAndChildChangesCommitted();
    }
    else {
      updateTo(2);
      checkOriginalStateAfterUpdate();
      updateTo(1);
      checkChildChangeCommitted();
      updateTo(0);
      checkParentAndChildChangesCommitted();
    }
  }

  protected void testCommitParentChangesChildPending() throws VcsException, IOException {
    preparePaths();

    makeOriginalState();
    checkOriginalStateAfterUpdate();
    makeParentChanges();
    makeChildChange(true);
    commit(getPendingParentChanges(), "parent changes");
    checkParentChangesCommittedChildPending();
    commit(getPendingChildChange(true), "child change");
    checkParentAndChildChangesCommitted();

    updateTo(2);
    checkOriginalStateAfterUpdate();
    updateTo(1);
    checkParentChangesCommitted();
    updateTo(0);
    checkParentAndChildChangesCommitted();
  }

  protected void testCommitChildChangesParentPending() throws VcsException, IOException {
    preparePaths();

    makeOriginalState();
    checkOriginalStateAfterUpdate();
    makeParentChanges();
    makeChildChange(true);
    commit(getPendingChildChange(true), "child change");
    checkChildChangeCommittedParentPending();

    final Collection<Change> parentChanges = getPendingParentChanges();
    if (!parentChanges.isEmpty()) {
      commit(parentChanges, "parent changes");
      checkParentAndChildChangesCommitted();
    }

    if (parentChanges.isEmpty()) {
      updateTo(1);
      checkOriginalStateAfterUpdate();
      updateTo(0);
      checkParentAndChildChangesCommitted();
    }
    else {
      updateTo(2);
      checkOriginalStateAfterUpdate();
      updateTo(1);
      checkChildChangeCommitted();
      updateTo(0);
      checkParentAndChildChangesCommitted();
    }
  }


}
