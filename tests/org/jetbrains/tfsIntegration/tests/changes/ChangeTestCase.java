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
public abstract class ChangeTestCase extends TFSTestCase {

  protected enum ParentChangesState {
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

  protected abstract void checkParentChangesPending() throws VcsException;

  protected abstract void checkChildChangePending() throws VcsException;


  protected abstract void makeOriginalState() throws VcsException;

  protected abstract void makeParentChanges() throws VcsException;

  protected abstract void makeChildChange(ParentChangesState parentChangesState) throws VcsException, IOException;

  protected abstract Collection<Change> getPendingParentChanges() throws VcsException;

  @Nullable
  protected abstract Change getPendingChildChange(ParentChangesState parentChangesState) throws VcsException;

  protected boolean shouldTestRollbackChildPendingParent() {
    return true;
  }


  protected void testPendingAndRollback() throws VcsException, IOException {
    boolean originalStateChangesCommitted = initialize();

    makeParentChanges();
    checkParentChangesPending();
    makeChildChange(ParentChangesState.Pending);
    checkParentAndChildChangesPending();
    updateTo(0);
    checkParentAndChildChangesPending();
    rollback(getPendingParentChanges());
    checkChildChangePendingParentRolledBack();
    final Change childChange = getPendingChildChange(ParentChangesState.NotDone);
    if (childChange != null) {
      rollback(childChange);
    }
    checkOriginalStateAfterRollbackParentChild();

    if (shouldTestRollbackChildPendingParent()) {
      makeChildChange(ParentChangesState.NotDone);
      checkChildChangePending();
      makeParentChanges();
      checkParentAndChildChangesPending();
      final Change childChange2 = getPendingChildChange(ParentChangesState.Pending);
      if (childChange2 != null) {
        rollback(childChange2);
        checkParentChangesPendingChildRolledBack();
      }
      rollback(getPendingParentChanges());
      checkOriginalStateAfterRollbackParentChild();
    }

    makeParentChanges();
    makeChildChange(ParentChangesState.Pending);
    Collection<Change> parentAndChildChanges = new ArrayList<Change>(getPendingParentChanges());
    final Change pendingChildChange = getPendingChildChange(ParentChangesState.Pending);
    if (pendingChildChange != null) {
      parentAndChildChanges.add(pendingChildChange);
    }
    rollback(parentAndChildChanges);
    checkOriginalStateAfterRollbackParentChild();
  }


  protected void testCommitParentThenChildChanges() throws VcsException, IOException {
    boolean originalStateChangesCommitted = initialize();

    makeParentChanges();
    final Collection<Change> parentChanges = getPendingParentChanges();
    commit(parentChanges, PARENT_CHANGES_COMMIT_COMMENT);
    checkParentChangesCommitted();
    makeChildChange(ParentChangesState.Committed);
    final Change childChange = getPendingChildChange(ParentChangesState.Committed);
    final Change childChangeToCheck = ChangeHelper.getChangeWithCachedContent(childChange);
    commit(childChange, CHILD_CHANGES_COMMIT_COMMENT);
    checkParentAndChildChangesCommitted();

    updateTo(2);
    checkOriginalStateAfterUpdate();
    updateTo(1);
    checkParentChangesCommitted();
    updateTo(0);
    checkParentAndChildChangesCommitted();

    assertHistory(parentChanges, childChangeToCheck, true, originalStateChangesCommitted);
  }

  protected void testCommitChildThenParentChanges() throws VcsException, IOException {
    boolean originalStateChangesCommitted = initialize();

    makeChildChange(ParentChangesState.NotDone);
    final Change childChange = getPendingChildChange(ParentChangesState.NotDone);
    final Change childChangeToCheck = ChangeHelper.getChangeWithCachedContent(childChange);
    commit(childChange, CHILD_CHANGES_COMMIT_COMMENT);
    checkChildChangeCommitted();
    makeParentChanges();

    final Collection<Change> parentChanges = getPendingParentChanges();
    if (!parentChanges.isEmpty()) {
      commit(parentChanges, PARENT_CHANGES_COMMIT_COMMENT);
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

    assertHistory(parentChanges, childChangeToCheck, false, originalStateChangesCommitted);
  }

  protected void testCommitParentChangesChildPending() throws VcsException, IOException {
    boolean originalStateChangesCommitted = initialize();

    makeParentChanges();
    makeChildChange(ParentChangesState.Pending);
    final Collection<Change> parentChanges = getPendingParentChanges();
    commit(parentChanges, PARENT_CHANGES_COMMIT_COMMENT);
    checkParentChangesCommittedChildPending();
    final Change childChange = getPendingChildChange(ParentChangesState.Committed);
    final Change childChangeToCheck = ChangeHelper.getChangeWithCachedContent(childChange);
    commit(childChange, CHILD_CHANGES_COMMIT_COMMENT);
    checkParentAndChildChangesCommitted();

    updateTo(2);
    checkOriginalStateAfterUpdate();
    updateTo(1);
    checkParentChangesCommitted();
    updateTo(0);
    checkParentAndChildChangesCommitted();

    assertHistory(parentChanges, childChangeToCheck, true, originalStateChangesCommitted);
  }

  protected void testCommitChildChangesParentPending() throws VcsException, IOException {
    boolean originalStateChangesCommitted = initialize();

    makeChildChange(ParentChangesState.NotDone);
    final Change childChange = getPendingChildChange(ParentChangesState.NotDone);
    final Change childChangeToCheck = ChangeHelper.getChangeWithCachedContent(childChange);
    rollback(childChange);
    checkOriginalStateAfterRollbackParentChild();

    makeParentChanges();
    makeChildChange(ParentChangesState.Pending);

    final Change childChangeToCommit = getPendingChildChange(ParentChangesState.Pending);
    if (childChangeToCommit != null) {
      commit(childChangeToCommit, CHILD_CHANGES_COMMIT_COMMENT);
      checkChildChangeCommittedParentPending();
    }

    final Collection<Change> parentChanges = getPendingParentChanges();
    if (!parentChanges.isEmpty()) {
      commit(parentChanges, PARENT_CHANGES_COMMIT_COMMENT);
      checkParentAndChildChangesCommitted();
    }

    if (parentChanges.isEmpty()) {
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
        checkParentChangesCommitted();
      }
    }

    assertHistory(parentChanges, childChangeToCommit != null ? childChangeToCheck : null, false, originalStateChangesCommitted);
  }

  private void assertHistory(Collection<Change> parentChangesCommitted,
                             Change childChangeCommitted,
                             boolean parentThenChild,
                             boolean originalStateCommited) throws VcsException {
    final RepositoryLocation location = getVcs().getCommittedChangesProvider().getLocationFor(TfsFileUtil.getFilePath(mySandboxRoot));
    final List<TFSChangeList> historyList =
      getVcs().getCommittedChangesProvider().getCommittedChanges(new ChangeBrowserSettings(), location, 0);

    final TFSChangeList childChangelist;
    if (!parentChangesCommitted.isEmpty()) {
      if (originalStateCommited) {
        Assert.assertEquals(childChangeCommitted != null ? 4 : 3, historyList.size());
      }
      else {
        Assert.assertEquals(childChangeCommitted != null ? 3 : 2, historyList.size());
      }
      childChangelist = childChangeCommitted != null ? historyList.get(parentThenChild ? 0 : 1) : null;

      TFSChangeList parentChangelist = historyList.get(parentThenChild ? 1 : 0);
      Assert.assertEquals(PARENT_CHANGES_COMMIT_COMMENT, parentChangelist.getComment());
      Assert.assertEquals(getUsername(), parentChangelist.getCommitterName());
      ChangeHelper.assertContains(parentChangesCommitted, parentChangelist.getChanges());
    }
    else {
      Assert.assertEquals(originalStateCommited ? 3 : 2, historyList.size());
      childChangelist = historyList.get(0);
    }

    if (childChangeCommitted != null) {
      Assert.assertEquals(CHILD_CHANGES_COMMIT_COMMENT, childChangelist.getComment());
      Assert.assertEquals(getUsername(), childChangelist.getCommitterName());
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
