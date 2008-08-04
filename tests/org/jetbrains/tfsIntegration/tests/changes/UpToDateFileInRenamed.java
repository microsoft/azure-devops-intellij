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
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.vcsUtil.VcsUtil;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

@SuppressWarnings({"HardCodedStringLiteral"})
public class UpToDateFileInRenamed extends ParentChangeTestCase {

  private static final String FILE_CONTENT = "file content";
  private static final String FOLDERNAME_RENAMED = "Folder_Renamed";

  private FilePath myParentFolderOriginal;
  private FilePath myChildFileOriginal;
  private FilePath myParentFolderRenamed;
  private FilePath mychildFileInRenamedFolder;

  protected void preparePaths() {
    final String filename = "up_to_date.txt";
    final String foldernameOriginal = "Folder_Original";

    myParentFolderOriginal = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), foldernameOriginal));
    myChildFileOriginal = VcsUtil.getFilePath(new File(new File(myParentFolderOriginal.getPath()), filename));
    myParentFolderRenamed = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), FOLDERNAME_RENAMED));
    mychildFileInRenamedFolder = VcsUtil.getFilePath(new File(new File(myParentFolderRenamed.getPath()), filename));
  }

  protected void makeOriginalState() throws VcsException {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    createDirInCommand(myParentFolderOriginal);
    createFileInCommand(myChildFileOriginal, FILE_CONTENT);
    commit(getChanges().getChanges(), "original state");
  }

  protected void checkOriginalState() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentFolderOriginal, 1);
    assertFile(myChildFileOriginal, FILE_CONTENT, false);
  }

  protected void makeParentChanges() throws VcsException {
    rename(myParentFolderOriginal, FOLDERNAME_RENAMED);
  }

  protected void checkParentChangesPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myParentFolderOriginal, myParentFolderRenamed);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentFolderRenamed, 1);
    assertFile(mychildFileInRenamedFolder, FILE_CONTENT, false);
  }

  protected void checkParentChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myParentFolderRenamed, 1);
    assertFile(mychildFileInRenamedFolder, FILE_CONTENT, false);
  }

  protected Collection<Change> getPendingParentChanges() throws VcsException {
    return Collections.singletonList(getChanges().getMoveChange(myParentFolderOriginal, myParentFolderRenamed));
  }

  @Test
  public void doTest() throws VcsException {
    super.doTest();
  }

}
