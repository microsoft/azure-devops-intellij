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

import java.io.IOException;

@SuppressWarnings({"HardCodedStringLiteral"})
public abstract class ChildChangeTestCase extends TFSTestCase {

  protected static final String ORIGINAL_CONTENT = "original content";
  protected static final String MODIFIED_CONTENT = "modified content";
  protected static final String FILE_CONTENT = "file_content";

  
  protected abstract void preparePaths();

  protected abstract void checkChildChangePending() throws VcsException;

  protected abstract void checkOriginalStateAfterUpdate() throws VcsException;

  protected abstract void checkOriginalStateAfterRollback() throws VcsException;

  protected abstract void checkChildChangeCommitted() throws VcsException;


  protected abstract void makeOriginalState() throws VcsException;

  protected abstract void makeChildChange() throws IOException, VcsException;

  @Nullable
  protected abstract Change getChildChange() throws VcsException;


  protected void doTest() throws VcsException, IOException {
    preparePaths();

    makeOriginalState();
    checkOriginalStateAfterUpdate();

    // check child change
    makeChildChange();
    checkChildChangePending();
    updateTo(0);
    checkChildChangePending();
    rollback(getChildChange());
    checkOriginalStateAfterRollback();

    makeChildChange();
    commit(getChildChange(), "child change");
    updateTo(1);
    checkOriginalStateAfterUpdate();
    updateTo(0);
    checkChildChangeCommitted();
  }

}