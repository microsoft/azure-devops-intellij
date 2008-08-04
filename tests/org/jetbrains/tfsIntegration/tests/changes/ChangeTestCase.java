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

import java.util.Collection;

@SuppressWarnings({"HardCodedStringLiteral"})
public abstract class ChangeTestCase extends TFSTestCase {

  protected abstract void preparePaths();

  protected abstract void checkParentChangesPending() throws VcsException;

  protected abstract void checkParentChangesPendingChildRolledBack() throws VcsException;

  protected abstract void checkChildChangePendingParentRolledBack() throws VcsException;

  protected abstract void checkChildChangePending() throws VcsException;

  protected abstract void checkParentAndChildChangesPending() throws VcsException;

  protected abstract void checkOriginalStateAfterRollbackParent() throws VcsException;

  protected abstract void checkOriginalStateAfterRollbackChild() throws VcsException;

  protected abstract void checkOriginalStateAfterRollbackParentChild() throws VcsException;

  protected abstract void checkOriginalStateAfterUpdate() throws VcsException;

  protected abstract void checkParentChangesCommitted() throws VcsException;

  protected abstract void checkChildChangeCommitted() throws VcsException;

  protected abstract void checkParentAndChildChangesCommitted() throws VcsException;

  protected abstract void checkParentChangesCommittedChildPending() throws VcsException;

  protected abstract void checkChildChangeCommittedParentPending() throws VcsException;


  protected abstract void makeOriginalState() throws VcsException;

  protected abstract void makeParentChanges() throws VcsException;

  protected abstract void makeChildChange() throws VcsException;

  protected abstract Collection<Change> getPendingParentChanges() throws VcsException;

  protected abstract Change getPendingChildChange() throws VcsException;


  protected void doTestPendingAndRollback() throws VcsException {
    preparePaths();

    makeOriginalState();
    checkOriginalStateAfterUpdate();

    // check parent changes
    makeParentChanges();
    checkParentChangesPending();
    updateTo(0);
    checkParentChangesPending();
    rollback(getPendingParentChanges());
    checkOriginalStateAfterRollbackParent();

    // check child change
    makeChildChange();
    checkChildChangePending();
    updateTo(0);
    checkChildChangePending();
    rollback(getPendingChildChange());
    checkOriginalStateAfterRollbackChild();

    // check both
    makeParentChanges();
    makeChildChange();
    checkParentAndChildChangesPending();
    updateTo(0);
    checkParentAndChildChangesPending();
    rollback(getPendingParentChanges());
    checkChildChangePendingParentRolledBack();
    rollback(getPendingChildChange());
    checkOriginalStateAfterRollbackParentChild();

    makeChildChange();
    makeParentChanges();
    checkParentAndChildChangesPending();
    updateTo(0);
    checkParentAndChildChangesPending();
    rollback(getPendingChildChange());
    checkParentChangesPendingChildRolledBack();
    rollback(getPendingParentChanges());
    checkOriginalStateAfterRollbackParentChild();
  }


  protected void testCommitParentThenChildChanges() throws VcsException {
    preparePaths();

    makeOriginalState();
    checkOriginalStateAfterUpdate();
    makeParentChanges();
    commit(getPendingParentChanges(), "parent changes");
    checkParentChangesCommitted();
    makeChildChange();
    commit(getPendingChildChange(), "child change");
    checkParentAndChildChangesCommitted();

    updateTo(2);
    checkOriginalStateAfterUpdate();
    updateTo(1);
    checkParentChangesCommitted();
    updateTo(0);
    checkParentAndChildChangesCommitted();
  }

  protected void testCommitChildThenParentChanges() throws VcsException {
    preparePaths();

    makeOriginalState();
    checkOriginalStateAfterUpdate();
    makeChildChange();
    commit(getPendingParentChanges(), "child change");
    checkChildChangeCommitted();
    makeParentChanges();
    commit(getPendingParentChanges(), "parent changes");
    checkParentAndChildChangesCommitted();

    updateTo(2);
    checkOriginalStateAfterUpdate();
    updateTo(1);
    checkChildChangeCommitted();
    updateTo(0);
    checkParentAndChildChangesCommitted();
  }

  protected void testCommitParentChangesChildPending() throws VcsException {
    preparePaths();

    makeOriginalState();
    checkOriginalStateAfterUpdate();
    makeParentChanges();
    makeChildChange();
    commit(getPendingParentChanges(), "parent changes");
    checkParentChangesCommittedChildPending();
    makeChildChange();
    commit(getPendingChildChange(), "child change");
    checkParentAndChildChangesCommitted();

    updateTo(2);
    checkOriginalStateAfterUpdate();
    updateTo(1);
    checkParentChangesCommitted();
    updateTo(0);
    checkParentAndChildChangesCommitted();
  }

  protected void testCommitChildChangesParentPending() throws VcsException {
    preparePaths();

    makeOriginalState();
    checkOriginalStateAfterUpdate();
    makeParentChanges();
    makeChildChange();
    commit(getPendingChildChange(), "child change");
    checkChildChangeCommittedParentPending();
    commit(getPendingParentChanges(), "parent changes");
    checkParentAndChildChangesCommitted();

    updateTo(2);
    checkOriginalStateAfterUpdate();
    updateTo(1);
    checkChildChangeCommitted();
    updateTo(0);
    checkParentAndChildChangesCommitted();
  }



}
