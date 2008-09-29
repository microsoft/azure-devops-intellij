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

package org.jetbrains.tfsIntegration.tests.singlechange;

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
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"HardCodedStringLiteral"})
public abstract class SingleChangeTestCase extends TFSTestCase {

  protected static final String ORIGINAL_CONTENT = "original content";
  protected static final String MODIFIED_CONTENT = "modified content";
  protected static final String FILE_CONTENT = "file_content";
  private static final String CHILD_CHANGE_COMMIT_COMMENT = "child change";
  private static final String INITIAL_STATE_COMMIT_COMMENT = "initial state";


  protected abstract void preparePaths();

  protected abstract void checkChildChangePending() throws VcsException;

  protected abstract void checkOriginalStateAfterUpdate() throws VcsException;

  protected abstract void checkOriginalStateAfterRollback() throws VcsException;

  protected abstract void checkChildChangeCommitted() throws VcsException;


  protected abstract void makeOriginalState() throws VcsException;

  protected abstract void makeChildChange() throws IOException, VcsException;

  @Nullable
  protected abstract Change getPendingChildChange() throws VcsException;


  protected void doTest() throws VcsException, IOException {
    preparePaths();

    makeOriginalState();
    final List<Change> originalStateChanges = getChanges().getChanges();
    if (!originalStateChanges.isEmpty()) {
      commit(originalStateChanges, INITIAL_STATE_COMMIT_COMMENT);
    }
    checkOriginalStateAfterUpdate();

    // check child change
    makeChildChange();
    checkChildChangePending();
    updateTo(0);
    checkChildChangePending();
    rollback(getPendingChildChange());
    checkOriginalStateAfterRollback();

    makeChildChange();
    final Change childChange = getPendingChildChange();
    final Change changeToCheck = ChangeHelper.getChangeWithCachedContent(childChange);
    commit(childChange, CHILD_CHANGE_COMMIT_COMMENT);
    updateTo(1);
    checkOriginalStateAfterUpdate();
    updateTo(0);
    checkChildChangeCommitted();

    assertHistory(changeToCheck, !originalStateChanges.isEmpty());
  }

  private void assertHistory(Change change, boolean originalStateCommited) throws VcsException {
    final RepositoryLocation location = getVcs().getCommittedChangesProvider().getLocationFor(TfsFileUtil.getFilePath(mySandboxRoot));
    final List<TFSChangeList> historyList =
      getVcs().getCommittedChangesProvider().getCommittedChanges(new ChangeBrowserSettings(), location, 0);

    Assert.assertEquals(originalStateCommited ? 3 : 2, historyList.size());
    TFSChangeList changelist = historyList.get(0);

    Assert.assertEquals(CHILD_CHANGE_COMMIT_COMMENT, changelist.getComment());
    Assert.assertEquals(getUsername().toUpperCase(), changelist.getCommitterName());
    ChangeHelper.assertContains(Collections.singletonList(change), changelist.getChanges());
  }

}