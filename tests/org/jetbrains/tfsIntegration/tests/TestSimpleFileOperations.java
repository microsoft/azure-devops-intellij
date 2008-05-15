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

package org.jetbrains.tfsIntegration.tests;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

@SuppressWarnings({"HardCodedStringLiteral"})
public class TestSimpleFileOperations extends TFSTestCase {

  // none -> create file, don't schedule -> unversioned
  @Test
  public void testNoneToUnversioned() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getUnversionedFiles().contains(file));
    Assert.assertEquals(content, getContent(file));
  }

  // unversioned -> delete -> none
  @Test
  public void testUnversionedToNone() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    deleteFileInCommand(file);

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // unversioned -> delete -> none
  @Test
  public void testUnversionedToNoneExternalDelete() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    deleteFileExternally(file);

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // none -> create file, schedule -> scheduled for addition
  @Test
  public void testNoneToScheduledForAddition() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsScheduledForAddition(file));
    Assert.assertEquals(content, getContent(file));
  }

  // scheduled for addition -> delete -> none
  @Test
  public void testScheduledForAdditionToNone() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    deleteFileInCommand(file);

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // scheduled for addition -> external delete -> locally deleted
  @Test
  public void testScheduledForAdditionToLocallyDeleted() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    deleteFileExternally(file);

    Assert.assertTrue(getChanges().getLocallyDeletedFiles().contains(TfsFileUtil.getFilePath(file)));
  }

  // locally deleted -> removeFromVcs -> none
  @Test
  public void testLocallyDeletedToNone() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    deleteFileExternally(file);

    scheduleForDeletion(file);

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // locally deleted -> rollback -> none
  @Test
  public void testLocallyDeletedRollbackToNone() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath path = TfsFileUtil.getFilePath(file);
    deleteFileExternally(file);
    refreshRecursively(mySandboxRoot);

    getVcs().getRollbackEnvironment().rollbackMissingFileDeletion(Collections.singletonList(TfsFileUtil.getFilePath(file)));

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // scheduled for addition -> rollback -> unversioned
  @Test
  public void testScheduledForAdditionToUnversioned() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);

    rollback(getChanges().getChanges());

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getUnversionedFiles().contains(file));
    Assert.assertEquals(content, getContent(file));
  }

  // unversioned -> schedule for addition -> scheduled for addition
  @Test
  public void testUnversionedToScheduledForAddition() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);

    scheduleForAddition(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsScheduledForAddition(file));
  }

  // unversioned -> rename -> unversioned
  @Test
  public void testUnversionedToUnversionedRename() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    final String newName = "file2.txt";

    rename(file, newName);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getUnversionedFiles().contains(getVirtualFile(mySandboxRoot, newName)));
  }

  // unversioned -> external rename -> unversioned
  @Test
  public void testUnversionedToUnversionedExternalRename() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    final String newName = "file2.txt";

    renameAndClearReadonlyExternally(file, newName);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getUnversionedFiles().contains(getVirtualFile(mySandboxRoot, newName)));
  }

  // scheduled for addition -> rename -> scheduled for addition
  @Test
  public void testScheduledForAdditionToScheduledForAdditionRename() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    final String newName = "file2.txt";

    rename(file, newName);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsScheduledForAddition(getVirtualFile(mySandboxRoot, newName)));
  }

  // scheduled for addition -> check in -> up to date
  @Test
  public void testScheduledForAdditionToUpToDate() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    createFileInCommand(mySandboxRoot, "file.txt", content);

    commit(getChanges().getChanges(), "unittest");

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(0, changes.getTotalItems());
  }

  // up to date -> check out -> modified [edit]
  @Test
  public void testUpToDateToCheckedOut() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");

    editFiles(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsModified(file));
  }

  // modified [edit] -> rollback -> up to date
  @Test
  public void testCheckedOutToUpToDateRollback() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");

    editFiles(file);

    rollback(getChanges().getChanges());

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // modified [edit] -> check in -> up to date
  @Test
  public void testCheckedOutToUpToDateCheckin() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");

    editFiles(file);

    commit(getChanges().getChanges(), "unittest");

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // up to date -> clear readonly by filesystem -> hijacked
  @Test
  public void testUpToDateToHijacked() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");

    clearReadonlyStatusExternally(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getModifiedWithoutCheckoutFiles().contains(file));
  }

  // hijacked -> rollback -> up to date
  @Test
  public void testHijackedToUpToDate() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");

    clearReadonlyStatusExternally(file);
    rollbackHijacked(getChanges().getModifiedWithoutCheckoutFiles());

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // hijacked -> checkout -> modified [edit]
  @Test
  public void testHijackedToCheckedOut() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");

    clearReadonlyStatusExternally(file);
    editFiles(file);

    final TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsModified(file));
  }

  // hijacked -> rename -> modified [rename]
  @Test
  public void testHijackedToRenamedRename() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath before = TfsFileUtil.getFilePath(file);
    commit(getChanges().getChanges(), "unittest");

    clearReadonlyStatusExternally(file);

    final String newName = "file2.txt";
    rename(file, newName);

    final TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    final FilePath after = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), newName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, after));
  }

  // modified [rename] -> rename to original name -> hijacked
  @Test
  public void testRenamedToHijackedRenameBack() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final String originalName = "file.txt";
    final VirtualFile file = createFileInCommand(mySandboxRoot, originalName, content);
    commit(getChanges().getChanges(), "unittest");

    clearReadonlyStatusExternally(file);

    final String newName = "file2.txt";
    rename(file, newName);
    rename(file, originalName);

    final TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getModifiedWithoutCheckoutFiles().contains(file));
  }

  // modified [rename] -> commit -> hijacked
  @Test
  public void testRenamedToHijackedCommit() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final String originalName = "file.txt";
    final VirtualFile file = createFileInCommand(mySandboxRoot, originalName, content);
    commit(getChanges().getChanges(), "unittest");

    clearReadonlyStatusExternally(file);

    final String newName = "file2.txt";
    rename(file, newName);
    commit(getChanges().getChanges(), "unittest");

    final TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(0, changes.getTotalItems());
  }

  // modified [rename] -> rename not to original name -> modified [rename]
  @Test
  public void testRenamedToRenamedRename() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath before = TfsFileUtil.getFilePath(file);
    commit(getChanges().getChanges(), "unittest");

    clearReadonlyStatusExternally(file);

    final String newName = "file2.txt";
    rename(file, newName);

    final String otherName = "file3.txt";
    rename(file, otherName);

    final TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    final FilePath after = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), otherName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, after));
  }

  // up to date -> rename externally -> modified [rename]
  @Test
  public void testUpToDateToRenamed() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(file);

    final String newName = "file2.txt";
    clearReadonlyStatusExternally(file);
    renameFileInCommand(file, newName);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    FilePath after = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), newName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, after));
  }

  // modified [rename] -> revert -> up to date
  @Test
  public void testRenamedToUpToDateRevert() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(file);

    final String newName = "file2.txt";
    clearReadonlyStatusExternally(file);
    renameFileInCommand(file, newName);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    FilePath after = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), newName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, after));

    rollback(getChanges().getChanges());

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // modified [rename] -> commit -> up to date
  @Test
  public void testRenamedToUpToDateCommit() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(file);

    final String newName = "file2.txt";
    clearReadonlyStatusExternally(file);
    renameFileInCommand(file, newName);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    FilePath after = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), newName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, after));

    commit(getChanges().getChanges(), "unittest");

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // up to date -> rename externally -> modified [rename] -> rename to original name
  @Test
  public void testUpToDateToRenamedToHijacked() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final String originalName = "file.txt";
    final VirtualFile file = createFileInCommand(mySandboxRoot, originalName, content);
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(file);

    final String newName = "file2.txt";
    clearReadonlyStatusExternally(file);
    renameFileInCommand(file, newName);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    FilePath after = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), newName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, after));

    renameFileInCommand(file, originalName);
    changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getModifiedWithoutCheckoutFiles().contains(file));
  }

  // hijacked -> delete, schedule for deletion -> scheduled for deletion
  @Test
  public void testHijackedToScheduledForDeletion() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath path = TfsFileUtil.getFilePath(file);
    commit(getChanges().getChanges(), "unittest");

    clearReadonlyStatusExternally(file);

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsScheduledForDeletion(path));
  }


  // hijacked -> delete, schedule for deletion -> scheduled for deletion -> rollback -> up to date
  @Test
  public void testHijackedToScheduledForDeletionToUpToDateRevert() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath path = TfsFileUtil.getFilePath(file);
    commit(getChanges().getChanges(), "unittest");

    clearReadonlyStatusExternally(file);

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsScheduledForDeletion(path));

    rollback(getChanges().getChanges());
    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // up to date -> delete, schedule for deletion -> scheduled for deletion
  @Test
  public void testUpToDateToScheduledForDeletion() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath path = TfsFileUtil.getFilePath(file);
    commit(getChanges().getChanges(), "unittest");

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsScheduledForDeletion(path));
  }

  // scheduled for deletion -> rollback -> up to date
  @Test
  public void testScheduledForDeletionToUpToDate() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath path = TfsFileUtil.getFilePath(file);
    commit(getChanges().getChanges(), "unittest");

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsScheduledForDeletion(path));

    rollback(getChanges().getChanges());
    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // scheduled for deletion -> check in -> none
  @Test
  public void testScheduledForDeletionToNoneCheckin() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath path = TfsFileUtil.getFilePath(file);
    commit(getChanges().getChanges(), "unittest");

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsScheduledForDeletion(path));

    commit(getChanges().getChanges(), "unit test");
    Assert.assertEquals(0, getChanges().getTotalItems());
  }


  // hijacked -> delete, don't schedule for deletion -> locally missing
  @Test
  public void testHijackedToLocallyMissing() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath path = TfsFileUtil.getFilePath(file);
    commit(getChanges().getChanges(), "unittest");

    clearReadonlyStatusExternally(file);

    doNothingSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getLocallyDeletedFiles().contains(path));
  }

  // up to date -> delete, don't schedule for deletion (modify by VCS) -> locally missing
  @Test
  public void testUpToDateToLocallyMissingModifyByVCS() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath path = TfsFileUtil.getFilePath(file);
    commit(getChanges().getChanges(), "unittest");

    doNothingSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    editFiles(file);
    deleteFileInCommand(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getLocallyDeletedFiles().contains(path));
  }

  // locally missing -> rollback -> up to date
  @Test
  public void testLocallyMissingToUpToDate() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath path = TfsFileUtil.getFilePath(file);
    commit(getChanges().getChanges(), "unittest");

    doNothingSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    editFiles(file);
    deleteFileInCommand(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getLocallyDeletedFiles().contains(path));

    getVcs().getRollbackEnvironment().rollbackMissingFileDeletion(Arrays.asList(path));
    Assert.assertEquals(0, getChanges().getTotalItems());
    Assert.assertEquals(content, getContent(VcsUtil.getVirtualFile(path.getPath())));
  }

  // locally missing -> remove from vcs -> scheduled for deletion
  @Test
  public void testLocallyMissingToScheduledForDeletion() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    FilePath path = TfsFileUtil.getFilePath(file);
    commit(getChanges().getChanges(), "unittest");

    doNothingSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    editFiles(file);
    deleteFileInCommand(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getLocallyDeletedFiles().contains(path));

    getVcs().getCheckinEnvironment().scheduleMissingFileForDeletion(Arrays.asList(path));
    Assert.assertEquals(1, getChanges().getTotalItems());
    Assert.assertTrue(changes.getLocallyDeletedFiles().contains(path));
  }

  // modified [edit] -> delete, schedule for deletion -> scheduled for deletion
  @Test
  public void testCheckedOutToScheduledForDeletion() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");
    FilePath path = TfsFileUtil.getFilePath(file);

    editFiles(file);

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);

    deleteFileInCommand(file);

    final TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsScheduledForDeletion(path));
  }

  // modified [edit] -> external delete, don't schedule for deletion -> locally missing
  @Test
  public void testCheckedOutToLocallyMissing() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");
    FilePath path = TfsFileUtil.getFilePath(file);

    editFiles(file);

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);

    deleteFileExternally(file);

    final TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.getLocallyDeletedFiles().contains(path));
  }

  // up to date -> rename, modify by VCS -> modified [edit, rename]
  @Test
  public void testUpToDateToCheckedOutAndRenamed() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(file);

    final String newName = "file2.txt";
    editFiles(file);
    renameFileInCommand(file, newName);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    FilePath after = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), newName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, after));
  }

  // up to date -> rename, modify by VCS -> modified [edit, rename] -> rename to original name -> modified [edit]
  @Test
  public void testUpToDateToCheckedOutAndRenamedToModifiedRenameBack() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final String originalName = "file.txt";
    final VirtualFile file = createFileInCommand(mySandboxRoot, originalName, content);
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(file);

    final String newName = "file2.txt";
    editFiles(file);
    renameFileInCommand(file, newName);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    FilePath after = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), newName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, after));

    renameFileInCommand(file, originalName);

    changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsModified(before));
  }

  // modified [rename, edit] -> rollback -> up to date 
  @Test
  public void testCheckedOutAndRenamedToUpToDateRollback() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(file);

    final String newName = "file2.txt";
    editFiles(file);
    renameFileInCommand(file, newName);

    rollback(getChanges().getChanges());

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // modified [rename, edit] -> check in -> up to date
  @Test
  public void testCheckedOutAndRenamedToUpToDateCheckin() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(file);

    final String newName = "file2.txt";
    editFiles(file);
    renameFileInCommand(file, newName);

    commit(getChanges().getChanges(), "unittest");

    Assert.assertEquals(0, getChanges().getTotalItems());
  }

  // modified [rename, edit] -> rename not to original name -> modified [rename, edit]
  @Test
  public void testCheckedOutAndRenamedToCheckedOutAndRenamedRename() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(file);

    final String newName = "file2.txt";
    editFiles(file);
    renameFileInCommand(file, newName);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    FilePath after = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), newName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, after));

    final String anotherName = "file3.txt";
    renameFileInCommand(file, anotherName);

    changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    FilePath anotherFile = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), anotherName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, anotherFile));
  }

  // modified [rename, edit] -> rename to original name -> modified [edit]
  @Test
  public void testCheckedOutAndRenamedToCheckedOutRenameBack() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final String originalName = "file.txt";
    final VirtualFile file = createFileInCommand(mySandboxRoot, originalName, content);
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(file);

    final String newName = "file2.txt";
    editFiles(file);
    renameFileInCommand(file, newName);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    FilePath after = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), newName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, after));

    renameFileInCommand(file, originalName);

    changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsModified(before));
  }

  // modified [edit] -> rename -> modified [edit, rename]
  @Test
  public void testCheckedOutToCheckedOutRenamed() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String content = "filecontent";
    final VirtualFile file = createFileInCommand(mySandboxRoot, "file.txt", content);
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(file);

    editFiles(file);

    TestChangeListBuilder changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    Assert.assertTrue(changes.containsModified(file));

    final String newName = "file2.txt";
    renameFileInCommand(file, newName);

    changes = getChanges();
    Assert.assertEquals(1, changes.getTotalItems());
    FilePath after = VcsUtil.getFilePath(new File(new File(mySandboxRoot.getPath()), newName));
    Assert.assertTrue(changes.containsRenamedOrMoved(before, after));
  }

}
