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

package org.jetbrains.tfsIntegration.tests.parentchange;

import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.tfsIntegration.tests.TFSTestCase;

import java.util.Collection;

@SuppressWarnings({"HardCodedStringLiteral"})
public abstract class ParentChangeTestCase extends TFSTestCase {

  protected abstract void preparePaths();

  protected abstract void checkParentChangesPending() throws VcsException;

  protected abstract void checkOriginalState() throws VcsException;

  protected abstract void checkParentChangesCommitted() throws VcsException;

  protected abstract void makeOriginalState() throws VcsException;

  protected abstract void makeParentChanges() throws VcsException;

  protected abstract Collection<Change> getPendingParentChanges() throws VcsException;


  protected void doTest() throws VcsException {
    preparePaths();

    makeOriginalState();
    checkOriginalState();

    // check parent changes
    makeParentChanges();
    checkParentChangesPending();
    updateTo(0);
    checkParentChangesPending();
    rollback(getPendingParentChanges());
    checkOriginalState();

    makeParentChanges();
    commit(getPendingParentChanges(), "parent changes");
    updateTo(1);
    checkOriginalState();
    updateTo(0);
    checkParentChangesCommitted();
  }

}
