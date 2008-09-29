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

import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSChangeList;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.tests.ChangeHelper;
import org.jetbrains.tfsIntegration.tests.TFSTestCase;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"HardCodedStringLiteral"})
public abstract class ParentChildChangeTestCase extends TFSTestCase {

  protected enum ParentChangeState {
    NotDone,
    Pending,
    Committed
  }

  protected static final String ORIGINAL_CONTENT = "original content";

  protected static final String MODIFIED_CONTENT = "modified content";

  protected static final String FILE_CONTENT = "file_content";

  private static final String PARENT_CHANGES_COMMIT_COMMENT = "parent changes";

  private static final String CHILD_CHANGES_COMMIT_COMMENT = "child change";

  private static final String INITIAL_STATE_COMMIT_COMMENT = "initial state";

  protected abstract void preparePaths();

  protected abstract void checkParentChangePendingChildRolledBack() throws VcsException;

  protected abstract void checkChildChangePendingParentRolledBack() throws VcsException;

  protected abstract void checkParentAndChildChangesPending() throws VcsException;

  protected abstract void checkOriginalStateAfterRollbackParentChild() throws VcsException;

  protected abstract void checkOriginalStateAfterUpdate() throws VcsException;

  protected abstract void checkParentChangeCommitted() throws VcsException;

  protected abstract void checkChildChangeCommitted() throws VcsException;

  protected abstract void checkParentAndChildChangesCommitted() throws VcsException;

  protected abstract void checkParentChangeCommittedChildPending() throws VcsException;

  protected abstract void checkChildChangeCommittedParentPending() throws VcsException;

  protected abstract void checkParentChangePending() throws VcsException;

  protected abstract void checkChildChangePending() throws VcsException;


  protected abstract void makeOriginalState() throws VcsException;

  protected abstract void makeParentChange() throws VcsException;

  protected abstract void makeChildChange(ParentChangeState parentChangeState) throws VcsException, IOException;

  protected abstract
  @Nullable
  Change getPendingParentChange() throws VcsException;

  @Nullable
  protected abstract Change getPendingChildChange(ParentChangeState parentChangeState) throws VcsException;

  protected boolean shouldTestRollbackChildPendingParent() {
    return true;
  }


  protected void testPendingAndRollback() throws VcsException, IOException {
    boolean originalStateChangesCommitted = initialize();

    makeParentChange();
    checkParentChangePending();
    makeChildChange(ParentChangeState.Pending);
    checkParentAndChildChangesPending();
    updateTo(0);
    checkParentAndChildChangesPending();
    rollback(getPendingParentChange());
    checkChildChangePendingParentRolledBack();
    final Change childChange = getPendingChildChange(ParentChangeState.NotDone);
    if (childChange != null) {
      rollback(childChange);
    }
    checkOriginalStateAfterRollbackParentChild();

    if (shouldTestRollbackChildPendingParent()) {
      makeChildChange(ParentChangeState.NotDone);
      checkChildChangePending();
      makeParentChange();
      checkParentAndChildChangesPending();
      final Change childChange2 = getPendingChildChange(ParentChangeState.Pending);
      if (childChange2 != null) {
        rollback(childChange2);
        checkParentChangePendingChildRolledBack();
      }
      rollback(getPendingParentChange());
      checkOriginalStateAfterRollbackParentChild();
    }

    makeParentChange();
    makeChildChange(ParentChangeState.Pending);
    Collection<Change> parentAndChildChanges = new ArrayList<Change>();
    final Change pendingParentChange = getPendingParentChange();
    if (pendingParentChange != null) {
      parentAndChildChanges.add(pendingParentChange);
    }
    final Change pendingChildChange = getPendingChildChange(ParentChangeState.Pending);
    if (pendingChildChange != null) {
      parentAndChildChanges.add(pendingChildChange);
    }
    rollback(parentAndChildChanges);
    checkOriginalStateAfterRollbackParentChild();
  }


  protected void testCommitParentThenChildChanges() throws VcsException, IOException {
    boolean originalStateChangesCommitted = initialize();

    makeParentChange();
    Change parentChange = getPendingParentChange();
    if (parentChange != null) {
      commit(parentChange, PARENT_CHANGES_COMMIT_COMMENT);
      checkParentChangeCommitted();
    }
    makeChildChange(ParentChangeState.Committed);
    final Change childChange = getPendingChildChange(ParentChangeState.Committed);
    final Change childChangeToCheck = ChangeHelper.getChangeWithCachedContent(childChange);
    commit(childChange, CHILD_CHANGES_COMMIT_COMMENT);
    checkParentAndChildChangesCommitted();

    updateTo(2);
    checkOriginalStateAfterUpdate();
    updateTo(1);
    checkParentChangeCommitted();
    updateTo(0);
    checkParentAndChildChangesCommitted();

    assertHistory(parentChange, childChangeToCheck, true, originalStateChangesCommitted);
  }

  protected void testCommitChildThenParentChanges() throws VcsException, IOException {
    boolean originalStateChangesCommitted = initialize();

    makeChildChange(ParentChangeState.NotDone);
    final Change childChange = getPendingChildChange(ParentChangeState.NotDone);
    final Change childChangeToCheck = ChangeHelper.getChangeWithCachedContent(childChange);
    commit(childChange, CHILD_CHANGES_COMMIT_COMMENT);
    checkChildChangeCommitted();
    makeParentChange();

    Change parentChange = getPendingParentChange();
    if (parentChange != null) {
      commit(parentChange, PARENT_CHANGES_COMMIT_COMMENT);
      checkParentAndChildChangesCommitted();
    }

    if (parentChange == null) {
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

    assertHistory(parentChange, childChangeToCheck, false, originalStateChangesCommitted);
  }

  protected void testCommitParentChangesChildPending() throws VcsException, IOException {
    boolean originalStateChangesCommitted = initialize();

    makeParentChange();
    makeChildChange(ParentChangeState.Pending);
    Change parentChange = getPendingParentChange();
    if (parentChange != null) {
      commit(parentChange, PARENT_CHANGES_COMMIT_COMMENT);
    }
    checkParentChangeCommittedChildPending();
    final Change childChange = getPendingChildChange(ParentChangeState.Committed);
    final Change childChangeToCheck = ChangeHelper.getChangeWithCachedContent(childChange);
    commit(childChange, CHILD_CHANGES_COMMIT_COMMENT);
    checkParentAndChildChangesCommitted();

    updateTo(2);
    checkOriginalStateAfterUpdate();
    updateTo(1);
    checkParentChangeCommitted();
    updateTo(0);
    checkParentAndChildChangesCommitted();

    assertHistory(parentChange, childChangeToCheck, true, originalStateChangesCommitted);
  }

  protected void testCommitChildChangesParentPending() throws VcsException, IOException {
    boolean originalStateChangesCommitted = initialize();

    makeChildChange(ParentChangeState.NotDone);
    final Change childChange = getPendingChildChange(ParentChangeState.NotDone);
    final Change childChangeToCheck = ChangeHelper.getChangeWithCachedContent(childChange);
    rollback(childChange);
    checkOriginalStateAfterRollbackParentChild();

    makeParentChange();
    makeChildChange(ParentChangeState.Pending);

    final Change childChangeToCommit = getPendingChildChange(ParentChangeState.Pending);
    if (childChangeToCommit != null) {
      commit(childChangeToCommit, CHILD_CHANGES_COMMIT_COMMENT);
      checkChildChangeCommittedParentPending();
    }

    Change parentChange = getPendingParentChange();
    if (parentChange != null) {
      commit(parentChange, PARENT_CHANGES_COMMIT_COMMENT);
    }
    checkParentAndChildChangesCommitted();

    if (parentChange == null) {
      if (childChangeToCommit != null) {
        updateTo(1);
        checkOriginalStateAfterUpdate();
        updateTo(0);
        checkParentAndChildChangesCommitted();
      }
    }
    else {
      if (childChangeToCommit != null) {
        updateTo(2);
        checkOriginalStateAfterUpdate();
        updateTo(1);
        checkChildChangeCommitted();
        updateTo(0);
        checkParentAndChildChangesCommitted();
      }
      else {
        updateTo(1);
        checkOriginalStateAfterUpdate();
        updateTo(0);
        checkParentChangeCommitted();
      }
    }

    assertHistory(parentChange, childChangeToCommit != null ? childChangeToCheck : null, false, originalStateChangesCommitted);
  }

  private void assertHistory(Change parentChangeCommitted,
                             Change childChangeCommitted,
                             boolean parentThenChild,
                             boolean originalStateCommited) throws VcsException {
    final RepositoryLocation location = getVcs().getCommittedChangesProvider().getLocationFor(TfsFileUtil.getFilePath(mySandboxRoot));
    final List<TFSChangeList> historyList =
      getVcs().getCommittedChangesProvider().getCommittedChanges(new ChangeBrowserSettings(), location, 0);

    final TFSChangeList childChangelist;
    if (parentChangeCommitted != null) {
      if (originalStateCommited) {
        Assert.assertEquals(childChangeCommitted != null ? 4 : 3, historyList.size());
      }
      else {
        Assert.assertEquals(childChangeCommitted != null ? 3 : 2, historyList.size());
      }
      childChangelist = childChangeCommitted != null ? historyList.get(parentThenChild ? 0 : 1) : null;

      TFSChangeList parentChangelist = historyList.get(parentThenChild ? 1 : 0);
      Assert.assertEquals(PARENT_CHANGES_COMMIT_COMMENT, parentChangelist.getComment());
      Assert.assertEquals(getUsername().toUpperCase(), parentChangelist.getCommitterName());
      ChangeHelper.assertContains(Collections.singletonList(parentChangeCommitted), parentChangelist.getChanges());
    }
    else {
      Assert.assertEquals(originalStateCommited ? 3 : 2, historyList.size());
      childChangelist = historyList.get(0);
    }

    if (childChangeCommitted != null) {
      Assert.assertEquals(CHILD_CHANGES_COMMIT_COMMENT, childChangelist.getComment());
      Assert.assertEquals(getUsername().toUpperCase(), childChangelist.getCommitterName());
      ChangeHelper.assertContains(Collections.singletonList(childChangeCommitted), childChangelist.getChanges());
    }
  }

  /**
   * @return true if initial state changes committed
   */
  private boolean initialize() throws VcsException {
    preparePaths();

    makeOriginalState();
    final List<Change> originalStateChanges = getChanges().getChanges();
    if (!originalStateChanges.isEmpty()) {
      commit(originalStateChanges, INITIAL_STATE_COMMIT_COMMENT);
    }
    checkOriginalStateAfterUpdate();
    return !originalStateChanges.isEmpty();
  }


}
