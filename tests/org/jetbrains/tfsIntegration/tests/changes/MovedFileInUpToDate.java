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
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.IOException;

@SuppressWarnings({"HardCodedStringLiteral"})
public class MovedFileInUpToDate extends ChildChangeTestCase {

  private static final String FILE_ORIGINAL = "file_original.txt";

  private FilePath myOriginalFile;
  private FilePath myMovedFile;

  private FilePath mySourceFolder;
  private FilePath myTargetFolder;

  protected void preparePaths() {
    mySourceFolder = getChildPath(mySandboxRoot, "SourceFolder");
    myTargetFolder = getChildPath(mySandboxRoot, "TargetFolder");
    myOriginalFile = getChildPath(mySourceFolder, FILE_ORIGINAL);
    myMovedFile = getChildPath(myTargetFolder, FILE_ORIGINAL);
  }

  protected void checkChildChangePending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalFile, myMovedFile);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySourceFolder, 0);
    assertFolder(myTargetFolder, 1);
    assertFile(myMovedFile, ORIGINAL_CONTENT, false);
  }

  protected void checkOriginalStateAfterUpdate() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySourceFolder, 1);
    assertFolder(myTargetFolder, 0);
    assertFile(myOriginalFile, ORIGINAL_CONTENT, false);
  }

  protected void checkOriginalStateAfterRollback() throws VcsException {
    checkOriginalStateAfterUpdate();
  }

  protected void checkChildChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySourceFolder, 0);
    assertFolder(myTargetFolder, 1);
    assertFile(myMovedFile, ORIGINAL_CONTENT, false);
  }

  protected void makeOriginalState() throws VcsException {
    createDirInCommand(mySourceFolder);
    createDirInCommand(myTargetFolder);
    createFileInCommand(myOriginalFile, ORIGINAL_CONTENT);
  }

  protected void makeChildChange() throws IOException, VcsException {
    moveFileInCommand(myOriginalFile, myTargetFolder);
  }

  @Nullable
  protected Change getPendingChildChange() throws VcsException {
    return getChanges().getMoveChange(myOriginalFile, myMovedFile);
  }

  @Test
  public void doTest() throws VcsException, IOException {
    super.doTest();
  }

}
