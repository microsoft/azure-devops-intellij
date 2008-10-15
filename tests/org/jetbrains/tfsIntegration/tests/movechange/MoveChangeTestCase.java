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

package org.jetbrains.tfsIntegration.tests.movechange;

import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.tfsIntegration.tests.TFSTestCase;
import org.jetbrains.tfsIntegration.tests.TestChangeListBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"HardCodedStringLiteral"})
public abstract class MoveChangeTestCase extends TFSTestCase {

  protected enum ChangeStatus {
    NotDone,
    Pending,
    Committed,
    RolledBack
  }

  protected abstract void preparePaths();

  protected abstract void makeOriginalState() throws VcsException;

  protected abstract void makeChange(String changeId) throws VcsException;

  @Nullable
  protected abstract Change getChange(TestChangeListBuilder changes, String changeId);

  protected abstract void checkChangesStatus(Map<String, ChangeStatus> changesStatus);

  protected void testPendRollback(String[] changesSequence) throws VcsException {
    preparePaths();
    makeOriginalState();
    Map<String, ChangeStatus> changesStatus = new HashMap<String, ChangeStatus>(changesSequence.length);
    for (String changeId : changesSequence) {
      changesStatus.put(changeId, ChangeStatus.NotDone);
    }
    checkChangesStatus(changesStatus);

    // pend one by one
    for (String changeId : changesSequence) {
      makeChange(changeId);
      changesStatus.put(changeId, ChangeStatus.Pending);
      checkChangesStatus(changesStatus);
      updateTo(0);
      checkChangesStatus(changesStatus);
    }

    // rollback one by one
    for (int i = changesSequence.length - 1; i >= 0; i--) {
      rollback(getChange(getChanges(), changesSequence[i]));
      changesStatus.put(changesSequence[i], ChangeStatus.RolledBack);
      checkChangesStatus(changesStatus);
      updateTo(0);
      checkChangesStatus(changesStatus);
    }
  }

  protected void testCommit(String[] changesSequence) throws VcsException {
    preparePaths();
    makeOriginalState();

    Map<String, ChangeStatus> changesStatus = new HashMap<String, ChangeStatus>(changesSequence.length);
    // commit one by one
    for (String changeId : changesSequence) {
      makeChange(changeId);
      commit(getChange(getChanges(), changeId), changeId);
      changesStatus.put(changeId, ChangeStatus.Committed);
      checkChangesStatus(changesStatus);
      updateTo(0);
      checkChangesStatus(changesStatus);
    }

    for (String changeId : changesSequence) {
      changesStatus.put(changeId, ChangeStatus.NotDone);
    }

    for (int i = 0; i < changesSequence.length; i++) {
      updateTo(changesSequence.length - i);
      checkChangesStatus(changesStatus);
      changesStatus.put(changesSequence[i], ChangeStatus.Committed);
    }

    updateTo(0);
    checkChangesStatus(changesStatus);
  }

}
