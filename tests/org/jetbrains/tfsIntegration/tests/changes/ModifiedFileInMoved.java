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
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

@SuppressWarnings({"HardCodedStringLiteral"})
public class ModifiedFileInMoved extends ChangeTestCase {
  private FilePath myOriginalParentFolder;
  private FilePath myMovedParentFolder;

  private FilePath myFileInOriginalFolder;
  private FilePath myFileInMovedFolder;

  private FilePath mySubfolder1;
  private FilePath mySubfolder2;

  protected void preparePaths() {
    final String folderName = "Folder_Original";
    myOriginalParentFolder = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), folderName));
    mySubfolder1 = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), "Subfolder1"));
    mySubfolder2 = VcsUtil.getFilePath(new File(mySubfolder1.getIOFile(), "Subfolder2"));
    myMovedParentFolder = VcsUtil.getFilePath(new File(mySubfolder2.getIOFile(), folderName));
    myFileInOriginalFolder = VcsUtil.getFilePath(new File(myOriginalParentFolder.getIOFile(), "file.txt"));
    myFileInMovedFolder = VcsUtil.getFilePath(new File(myMovedParentFolder.getIOFile(), "file.txt"));
  }

  protected void checkParentChangesPendingChildRolledBack() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myMovedParentFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangePendingParentRolledBack() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertModified(myFileInOriginalFolder, ORIGINAL_CONTENT, MODIFIED_CONTENT);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myOriginalParentFolder, 1);
    assertFile(myFileInOriginalFolder, MODIFIED_CONTENT, true);
  }

  protected void checkParentAndChildChangesPending() throws VcsException {
    getChanges().assertTotalItems(2);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myMovedParentFolder);
    getChanges().assertModified(myFileInMovedFolder, ORIGINAL_CONTENT, MODIFIED_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, MODIFIED_CONTENT, true);
  }

  protected void checkOriginalStateAfterRollbackParentChild() throws VcsException {
    checkOriginalState();
  }

  protected void checkOriginalStateAfterUpdate() throws VcsException {
    checkOriginalState();
  }

  private void checkOriginalState() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myOriginalParentFolder, 1);
    assertFile(myFileInOriginalFolder, ORIGINAL_CONTENT, false);
  }

  protected void checkParentChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, ORIGINAL_CONTENT, false);
  }

  protected void checkChildChangeCommitted() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 2);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 0);
    assertFolder(myOriginalParentFolder, 1);
    assertFile(myFileInOriginalFolder, MODIFIED_CONTENT, false);
  }

  protected void checkParentAndChildChangesCommitted() throws VcsException {
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, MODIFIED_CONTENT, false);
  }

  protected void checkParentChangesCommittedChildPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertModified(myFileInMovedFolder, ORIGINAL_CONTENT, MODIFIED_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, MODIFIED_CONTENT, true);
  }

  protected void checkChildChangeCommittedParentPending() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myOriginalParentFolder, myMovedParentFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(mySubfolder1, 1);
    assertFolder(mySubfolder2, 1);
    assertFolder(myMovedParentFolder, 1);
    assertFile(myFileInMovedFolder, MODIFIED_CONTENT, false);
  }

  protected void makeOriginalState() throws VcsException {
    createDirInCommand(myOriginalParentFolder);
    createDirInCommand(mySubfolder1);
    createDirInCommand(mySubfolder2);
    createFileInCommand(myFileInOriginalFolder, ORIGINAL_CONTENT);
    commit();
  }

  protected void makeParentChanges() throws VcsException {
    moveFileInCommand(myOriginalParentFolder, mySubfolder2);
  }

  protected void makeChildChange(boolean parentChangeMade) throws VcsException, IOException {
    editFiles(parentChangeMade ? myFileInMovedFolder : myFileInOriginalFolder);
    setFileContent(parentChangeMade ? myFileInMovedFolder : myFileInOriginalFolder, MODIFIED_CONTENT);
  }

  protected Collection<Change> getPendingParentChanges() throws VcsException {
    final Change moveChange = getChanges().getMoveChange(myOriginalParentFolder, myMovedParentFolder);
    return moveChange != null ? Collections.singletonList(moveChange) : Collections.<Change>emptyList();
  }

  @Nullable
  protected Change getPendingChildChange(boolean parentChangesMade) throws VcsException {
    return getChanges().getModificationChange(parentChangesMade ? myFileInMovedFolder : myFileInOriginalFolder);
  }

  @Test
  public void doTestPendingAndRollback() throws VcsException, IOException {
    super.doTestPendingAndRollback();
  }

  @Test
  public void testCommitParentThenChildChanges() throws VcsException, IOException {
    super.testCommitParentThenChildChanges();
  }

  @Test
  public void testCommitChildThenParentChanges() throws VcsException, IOException {
    super.testCommitChildThenParentChanges();
  }

  @Test
  public void testCommitParentChangesChildPending() throws VcsException, IOException {
    super.testCommitParentChangesChildPending();
  }

  @Test
  public void testCommitChildChangesParentPending() throws VcsException, IOException {
    super.testCommitChildChangesParentPending();
  }
}