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
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

@SuppressWarnings({"HardCodedStringLiteral"})
public class TestSimpleFolderOperations extends TFSTestCase {

  // none -> create folder, don't schedule -> unversioned
  @Test
  public void testNoneToUnversioned() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertUnversioned(folder);
  }

  // unversioned -> delete -> none
  @Test
  public void testUnversionedToNoneDelete() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    deleteFileInCommand(folder);

    getChanges().assertTotalItems(0);
  }

  // unversioned -> delete -> none
  @Test
  public void testUnversionedToNoneExternalDelete() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    deleteFileExternally(folder);

    getChanges().assertTotalItems(0);
  }

  // none -> create folder, schedule -> scheduled for addition
  @Test
  public void testNoneToScheduledForAddition() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertScheduledForAddition(folder);
  }

  // scheduled for addition -> delete -> none
  @Test
  public void testScheduledForAdditionToNoneDelete() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    deleteFileInCommand(folder);

    getChanges().assertTotalItems(0);
  }

  // scheduled for addition -> external delete -> locally deleted
  @Test
  public void testScheduledForAdditionToLocallyDeleted() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    deleteFileExternally(folder);

    getChanges().assertLocallyDeleted(TfsFileUtil.getFilePath(folder));
  }

  // locally deleted -> removeFromVcs -> none
  @Test
  public void testLocallyDeletedToNone() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    deleteFileExternally(folder);

    scheduleForDeletion(folder);

    getChanges().assertTotalItems(0);
  }

  // locally deleted -> rollback -> none
  @Test
  public void testLocallyDeletedRollbackToNone() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    deleteFileExternally(folder);

    getVcs().getRollbackEnvironment().rollbackMissingFileDeletion(Collections.singletonList(TfsFileUtil.getFilePath(folder)));

    getChanges().assertTotalItems(0);
  }

  // scheduled for addition -> rollback -> unversioned
  @Test
  public void testScheduledForAdditionToUnversioned() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");

    rollback(getChanges().getChanges());

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertUnversioned(folder);
  }

  // unversioned -> schedule for addition -> scheduled for addition
  @Test
  public void testUnversionedToScheduledForAddition() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");

    scheduleForAddition(folder);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertScheduledForAddition(folder);
  }

  // unversioned -> rename -> unversioned
  @Test
  public void testUnversionedToUnversionedRename() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    final String newName = "Folder_renamed";

    rename(folder, newName);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertUnversioned(getVirtualFile(mySandboxRoot, newName));
  }

  // unversioned -> external rename -> unversioned
  @Test
  public void testUnversionedToUnversionedExternalRename() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    final String newName = "Folder_renamed";

    clearReadonlyAndRenameExternally(folder, newName);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertUnversioned(getVirtualFile(mySandboxRoot, newName));
  }

  // scheduled for addition -> rename -> scheduled for addition
  @Test
  public void testScheduledForAdditionToScheduledForAdditionRename() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    final String newName = "Folder_renamed";

    rename(folder, newName);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertScheduledForAddition(getVirtualFile(mySandboxRoot, newName));
  }

  // scheduled for addition -> check in -> up to date
  @Test
  public void testScheduledForAdditionToUpToDate() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    createDirInCommand(mySandboxRoot, "Folder");

    commit(getChanges().getChanges(), "unittest");

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(0);
  }

  // modified [rename] -> rename to original name -> up to date
  @Test
  public void testRenamedToUpToDateRenameBack() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String originalName = "Folder";
    final VirtualFile folder = createDirInCommand(mySandboxRoot, originalName);
    commit(getChanges().getChanges(), "unittest");

    final String newName = "Folder_renamed";
    rename(folder, newName);
    rename(folder, originalName);

    final TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(0);
  }

  // modified [rename] -> rename not to original name -> modified [rename]
  @Test
  public void testRenamedToRenamedRename() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    FilePath before = TfsFileUtil.getFilePath(folder);
    commit(getChanges().getChanges(), "unittest");

    final String newName = "Folder_renamed";
    rename(folder, newName);

    final String otherName = "Folder_3";
    rename(folder, otherName);

    final TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    final FilePath after = getChildPath(mySandboxRoot, otherName);
    changes.assertRenamedOrMoved(before, after);
  }

  // up to date -> rename -> modified [rename]
  @Test
  public void testUpToDateToRenamed() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(folder);

    final String newName = "Folder_renamed";
    rename(folder, newName);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    FilePath after = getChildPath(mySandboxRoot, newName);
    changes.assertRenamedOrMoved(before, after);
  }

  // modified [rename] -> revert -> up to date
  @Test
  public void testRenamedToUpToDateRevert() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(folder);

    final String newName = "Folder_renamed";
    rename(folder, newName);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    FilePath after = getChildPath(mySandboxRoot, newName);
    changes.assertRenamedOrMoved(before, after);

    rollback(getChanges().getChanges());

    getChanges().assertTotalItems(0);
  }

  // modified [rename] -> commit -> up to date
  @Test
  public void testRenamedToUpToDateCommit() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    commit(getChanges().getChanges(), "unittest");
    FilePath before = TfsFileUtil.getFilePath(folder);

    final String newName = "Folder_renamed";
    rename(folder, newName);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    FilePath after = getChildPath(mySandboxRoot, newName);
    changes.assertRenamedOrMoved(before, after);

    commit(getChanges().getChanges(), "unittest");

    getChanges().assertTotalItems(0);
  }

  // up to date -> delete, schedule for deletion -> scheduled for deletion
  @Test
  public void testUpToDateToScheduledForDeletion() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    FilePath path = TfsFileUtil.getFilePath(folder);
    commit(getChanges().getChanges(), "unittest");

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(folder);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertScheduledForDeletion(path);
  }


  // up to date -> delete, schedule for deletion -> scheduled for deletion -> rollback -> up to date
  @Test
  public void testUpToDateToScheduledForDeletionToUpToDateRevert() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    FilePath path = TfsFileUtil.getFilePath(folder);
    commit(getChanges().getChanges(), "unittest");

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(folder);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertScheduledForDeletion(path);

    rollback(getChanges().getChanges());
    getChanges().assertTotalItems(0);
  }

  // scheduled for deletion -> check in -> none
  @Test
  public void testScheduledForDeletionToNoneCheckin() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    FilePath path = TfsFileUtil.getFilePath(folder);
    commit(getChanges().getChanges(), "unittest");

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(folder);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertScheduledForDeletion(path);

    commit(getChanges().getChanges(), "unit test");
    getChanges().assertTotalItems(0);
  }

  // up to date -> delete, don't schedule for deletion (modify by VCS) -> locally missing
  @Test
  public void testUpToDateToLocallyMissingModifyByVCS() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    FilePath path = TfsFileUtil.getFilePath(folder);
    commit(getChanges().getChanges(), "unittest");

    doNothingSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(folder);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertLocallyDeleted(path);
  }

  // locally missing -> rollback -> up to date
  @Test
  public void testLocallyMissingToUpToDate() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    FilePath path = TfsFileUtil.getFilePath(folder);
    commit(getChanges().getChanges(), "unittest");

    doNothingSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(folder);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertLocallyDeleted(path);

    getVcs().getRollbackEnvironment().rollbackMissingFileDeletion(Arrays.asList(path));
    refreshAll();
    getChanges().assertTotalItems(0);
  }

  // locally missing -> remove from vcs -> scheduled for deletion
  @Test
  public void testLocallyMissingToScheduledForDeletion() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    FilePath path = TfsFileUtil.getFilePath(folder);
    commit(getChanges().getChanges(), "unittest");

    doNothingSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    deleteFileInCommand(folder);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertLocallyDeleted(path);

    getVcs().getCheckinEnvironment().scheduleMissingFileForDeletion(Arrays.asList(path));
    getChanges().assertTotalItems(1);
    changes.assertLocallyDeleted(path);
  }

  // modified [rename] -> external delete, don't schedule for deletion -> locally missing (modified name)
  @Test
  public void testRenamedToLocallyMissing() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    commit(getChanges().getChanges(), "unittest");

    final String newName = "Folder_renamed";
    rename(folder, newName);

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);

    deleteFileExternally(folder);

    final TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    FilePath after = getChildPath(mySandboxRoot, newName);
    changes.assertLocallyDeleted(after);
  }

  // modified [rename] -> delete, schedule for deletion -> schedule for deletion (original name)
  @Test
  public void testRenamedToScheduledForDeletion() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "Folder");
    FilePath before = TfsFileUtil.getFilePath(folder);
    commit(getChanges().getChanges(), "unittest");

    final String newName = "Folder_renamed";
    rename(folder, newName);

    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);

    deleteFileInCommand(folder);

    final TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertScheduledForDeletion(before);
  }

}