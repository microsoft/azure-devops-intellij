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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import org.junit.Test;
import org.jetbrains.tfsIntegration.tests.singlechange.SingleChangeTestCase;

import java.io.IOException;

@SuppressWarnings({"HardCodedStringLiteral"})
public class AddedFileInUpToDate extends SingleChangeTestCase {

  private FilePath myAddedFile;

  protected void preparePaths() {
    myAddedFile = getChildPath(mySandboxRoot, "added_file.txt");
  }

  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForAddition(myAddedFile);

    assertFolder(mySandboxRoot, 1);
    assertFile(myAddedFile, FILE_CONTENT, true);
  }

  protected void checkOriginalStateAfterUpdate() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 0);
  }

  protected void checkOriginalStateAfterRollback() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertUnversioned(myAddedFile);

    assertFolder(mySandboxRoot, 1);
    assertFile(myAddedFile, FILE_CONTENT, true);
  }

  protected void checkChildChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFile(myAddedFile, FILE_CONTENT, false);
  }

  protected void makeOriginalState() throws VcsException {
    // nothing here
  }

  protected void makeChildChange() {
    if (myAddedFile.getIOFile().exists()) {
      scheduleForAddition(myAddedFile);
    }
    else {
      createFileInCommand(myAddedFile, FILE_CONTENT);
    }
  }

  protected Change getPendingChildChange() throws VcsException {
    return getChanges().getAddChange(myAddedFile);
  }

  @Test
  public void doTest() throws VcsException, IOException {
    super.doTest();
  }
}
