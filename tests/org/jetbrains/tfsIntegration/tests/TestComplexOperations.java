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
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.junit.Test;

@SuppressWarnings({"HardCodedStringLiteral", "UnusedDeclaration"})
public class TestComplexOperations extends TFSTestCase {

  // none -> create structure, don't schedule -> unversioned
  @Test
  public void testNoneToUnversioned() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);

    final String folderName = "folderA";
    final String subfolderName1 = "subfolder1";
    final String subfolderName2 = "subfolder2";
    final String subfolderName3 = "subfolder3";
    final String filename1 = "file1.txt";
    final String filename2 = "file2.txt";
    final String filename3 = "file3.txt";
    final String filename4 = "file4.txt";

    VirtualFile folder = createDirInCommand(mySandboxRoot, folderName);
    VirtualFile subfolder1 = createDirInCommand(folder, subfolderName1);
    VirtualFile subfolder2 = createDirInCommand(folder, subfolderName2);
    VirtualFile subfolder3 = createDirInCommand(folder, subfolderName3);
    VirtualFile file1 = createFileInCommand(subfolder1, filename1, "");
    VirtualFile file2 = createFileInCommand(subfolder1, filename2, "");
    VirtualFile file3 = createFileInCommand(subfolder2, filename3, "");
    VirtualFile file4 = createFileInCommand(subfolder2, filename4, "");

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(8);
    changes.assertUnversioned(folder);
    changes.assertUnversioned(subfolder1);
    changes.assertUnversioned(subfolder2);
    changes.assertUnversioned(subfolder3);
    changes.assertUnversioned(file1);
    changes.assertUnversioned(file2);
    changes.assertUnversioned(file3);
    changes.assertUnversioned(file4);
  }

  // unversioned -> delete root -> none
  @Test
  public void testUnversionedToNoneDeleteRoot() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);

    final String folderName = "folderA";
    final String subfolderName1 = "subfolder1";
    final String subfolderName2 = "subfolder2";
    final String subfolderName3 = "subfolder3";
    final String filename1 = "file1.txt";
    final String filename2 = "file2.txt";
    final String filename3 = "file3.txt";
    final String filename4 = "file4.txt";

    VirtualFile folder = createDirInCommand(mySandboxRoot, folderName);
    VirtualFile subfolder1 = createDirInCommand(folder, subfolderName1);
    VirtualFile subfolder2 = createDirInCommand(folder, subfolderName2);
    VirtualFile subfolder3 = createDirInCommand(folder, subfolderName3);
    VirtualFile file1 = createFileInCommand(subfolder1, filename1, "");
    VirtualFile file2 = createFileInCommand(subfolder1, filename2, "");
    VirtualFile file3 = createFileInCommand(subfolder2, filename3, "");
    VirtualFile file4 = createFileInCommand(subfolder2, filename4, "");

    deleteFileInCommand(folder);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(0);
  }

  // unversioned -> external delete root -> none
  @Test
  public void testUnversionedToNoneExternalDeleteRoot() throws Exception {
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String folderName = "folderA";
    final String subfolderName1 = "subfolder1";
    final String subfolderName2 = "subfolder2";
    final String subfolderName3 = "subfolder3";
    final String filename1 = "file1.txt";
    final String filename2 = "file2.txt";
    final String filename3 = "file3.txt";
    final String filename4 = "file4.txt";

    VirtualFile folder = createDirInCommand(mySandboxRoot, folderName);
    VirtualFile subfolder1 = createDirInCommand(folder, subfolderName1);
    VirtualFile subfolder2 = createDirInCommand(folder, subfolderName2);
    VirtualFile subfolder3 = createDirInCommand(folder, subfolderName3);
    VirtualFile file1 = createFileInCommand(subfolder1, filename1, "");
    VirtualFile file2 = createFileInCommand(subfolder1, filename2, "");
    VirtualFile file3 = createFileInCommand(subfolder2, filename3, "");
    VirtualFile file4 = createFileInCommand(subfolder2, filename4, "");

    deleteFileExternally(folder);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(0);
  }

  // none -> create structure, schedule for addition -> scheduled for addition
  @Test
  public void testNoneToScheduledForAddition() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String folderName = "folderA";
    final String subfolderName1 = "subfolder1";
    final String subfolderName2 = "subfolder2";
    final String subfolderName3 = "subfolder3";
    final String filename1 = "file1.txt";
    final String filename2 = "file2.txt";
    final String filename3 = "file3.txt";
    final String filename4 = "file4.txt";

    VirtualFile folder = createDirInCommand(mySandboxRoot, folderName);
    VirtualFile subfolder1 = createDirInCommand(folder, subfolderName1);
    VirtualFile subfolder2 = createDirInCommand(folder, subfolderName2);
    VirtualFile subfolder3 = createDirInCommand(folder, subfolderName3);
    VirtualFile file1 = createFileInCommand(subfolder1, filename1, "");
    VirtualFile file2 = createFileInCommand(subfolder1, filename2, "");
    VirtualFile file3 = createFileInCommand(subfolder2, filename3, "");
    VirtualFile file4 = createFileInCommand(subfolder2, filename4, "");

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(8);
    changes.assertScheduledForAddition(TfsFileUtil.getFilePath(folder));
    changes.assertScheduledForAddition(TfsFileUtil.getFilePath(subfolder1));
    changes.assertScheduledForAddition(TfsFileUtil.getFilePath(subfolder2));
    changes.assertScheduledForAddition(TfsFileUtil.getFilePath(subfolder3));
    changes.assertScheduledForAddition(TfsFileUtil.getFilePath(file1));
    changes.assertScheduledForAddition(TfsFileUtil.getFilePath(file2));
    changes.assertScheduledForAddition(TfsFileUtil.getFilePath(file3));
    changes.assertScheduledForAddition(TfsFileUtil.getFilePath(file4));
  }

  // scheduled for addition -> delete root -> none
  @Test
  public void testScheduledForAdditionToNoneDeleteRoot() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String folderName = "folderA";
    final String subfolderName1 = "subfolder1";
    final String subfolderName2 = "subfolder2";
    final String subfolderName3 = "subfolder3";
    final String filename1 = "file1.txt";
    final String filename2 = "file2.txt";
    final String filename3 = "file3.txt";
    final String filename4 = "file4.txt";

    VirtualFile folder = createDirInCommand(mySandboxRoot, folderName);
    VirtualFile subfolder1 = createDirInCommand(folder, subfolderName1);
    VirtualFile subfolder2 = createDirInCommand(folder, subfolderName2);
    VirtualFile subfolder3 = createDirInCommand(folder, subfolderName3);
    VirtualFile file1 = createFileInCommand(subfolder1, filename1, "");
    VirtualFile file2 = createFileInCommand(subfolder1, filename2, "");
    VirtualFile file3 = createFileInCommand(subfolder2, filename3, "");
    VirtualFile file4 = createFileInCommand(subfolder2, filename4, "");

    deleteFileInCommand(folder);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(0);
  }

  // scheduled for addition -> external delete -> locally deleted
  @Test
  public void testScheduledForAdditionToLocallyDeleted() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final String folderName = "folderA";
    final String subfolderName1 = "subfolder1";
    final String subfolderName2 = "subfolder2";
    final String subfolderName3 = "subfolder3";
    final String filename1 = "file1.txt";
    final String filename2 = "file2.txt";
    final String filename3 = "file3.txt";
    final String filename4 = "file4.txt";

    VirtualFile folder = createDirInCommand(mySandboxRoot, folderName);
    VirtualFile subfolder1 = createDirInCommand(folder, subfolderName1);
    VirtualFile subfolder2 = createDirInCommand(folder, subfolderName2);
    VirtualFile subfolder3 = createDirInCommand(folder, subfolderName3);
    VirtualFile file1 = createFileInCommand(subfolder1, filename1, "");
    VirtualFile file2 = createFileInCommand(subfolder1, filename2, "");
    VirtualFile file3 = createFileInCommand(subfolder2, filename3, "");
    VirtualFile file4 = createFileInCommand(subfolder2, filename4, "");

    deleteFileExternally(folder);

    TestChangeListBuilder changes = getChanges();
    changes.assertTotalItems(8);
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(folder));
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(subfolder1));
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(subfolder2));
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(subfolder3));
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(file1));
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(file2));
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(file3));
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(file4));
  }

  // pending changes -> delete root externally, don't schedule for deletion -> locally missing
  @Test
  public void testPendingChangesToLocallyMissing() throws Exception {
    final PendingChanges pendingChanges = new PendingChanges();
    deleteFileExternally(pendingChanges.rootfolder);

    TestChangeListBuilder changes = getChanges();

    changes.assertTotalItems(14);
    changes.assertLocallyDeleted(pendingChanges.fileCheckedOut);
    changes.assertLocallyDeleted(pendingChanges.fileCheckedOutAndRenamed);
    changes.assertLocallyDeleted(pendingChanges.fileHijacked);
    changes.assertLocallyDeleted(pendingChanges.fileMissing);
    changes.assertLocallyDeleted(pendingChanges.fileRenamed);
    changes.assertLocallyDeleted(pendingChanges.fileToAdd);
    changes.assertLocallyDeleted(pendingChanges.fileUpToDate);
    changes.assertLocallyDeleted(pendingChanges.folderMissing);
    changes.assertLocallyDeleted(pendingChanges.folderRenamed);
    changes.assertLocallyDeleted(pendingChanges.folderToAdd);
    changes.assertLocallyDeleted(pendingChanges.folderUpToDate);

    changes.assertScheduledForDeletion(pendingChanges.fileToDelete);
    changes.assertScheduledForDeletion(pendingChanges.folderToDelete);
  }

  // pending changes -> delete root externally, don't schedule for deletion -> locally missing -> revert
  @Test
  public void testPendingChangesToLocallyMissingToNone() throws Exception {
    final PendingChanges pendingChanges = new PendingChanges();
    deleteFileExternally(pendingChanges.rootfolder);

    TestChangeListBuilder changes = getChanges();

    changes.assertTotalItems(14);
    changes.assertLocallyDeleted(pendingChanges.fileCheckedOut);
    changes.assertLocallyDeleted(pendingChanges.fileCheckedOutAndRenamed);
    changes.assertLocallyDeleted(pendingChanges.fileHijacked);
    changes.assertLocallyDeleted(pendingChanges.fileMissing);
    changes.assertLocallyDeleted(pendingChanges.fileRenamed);
    changes.assertLocallyDeleted(pendingChanges.fileToAdd);
    changes.assertLocallyDeleted(pendingChanges.fileUpToDate);
    changes.assertScheduledForDeletion(pendingChanges.fileToDelete);

    changes.assertLocallyDeleted(pendingChanges.folderMissing);
    changes.assertLocallyDeleted(pendingChanges.folderRenamed);
    changes.assertLocallyDeleted(pendingChanges.folderToAdd);
    changes.assertLocallyDeleted(pendingChanges.folderUpToDate);
    changes.assertScheduledForDeletion(pendingChanges.folderToDelete);

    rollbackAll(changes);

    changes = getChanges();
    changes.assertTotalItems(0);
  }

  // pending changes -> commit
  @Test
  public void testCommitPendingChanges() throws Exception {
    final PendingChanges pendingChanges = new PendingChanges();

    commit();

    TestChangeListBuilder changes = getChanges();

    changes.assertTotalItems(5);
    changes.assertLocallyDeleted(pendingChanges.folderMissing);
    changes.assertLocallyDeleted(pendingChanges.fileMissing);
    changes.assertHijacked(pendingChanges.fileHijacked);
    changes.assertUnversioned(pendingChanges.folderUnversioned);
    changes.assertUnversioned(pendingChanges.fileUnversioned);
  }

  // pending changes -> revert
  @Test
  public void testPendingChangesRevert() throws Exception {
    final PendingChanges pendingChanges = new PendingChanges();

    rollbackAll(getChanges());

    TestChangeListBuilder changes = getChanges();

    changes.assertTotalItems(5);
    changes.assertUnversioned(pendingChanges.folderUnversioned);
    changes.assertUnversioned(pendingChanges.fileUnversioned);
    changes.assertUnversioned(pendingChanges.fileToAdd);
    changes.assertUnversioned(pendingChanges.folderToAdd);
    changes.assertHijacked(pendingChanges.fileRenamedOriginal);
  }

  // pending changes -> delete, schedule for deletion
  @Test
  public void testPendingChangesDeleteRoot() throws Exception {
    final PendingChanges pendingChanges = new PendingChanges();
    deleteFileInCommand(pendingChanges.rootfolder);

    TestChangeListBuilder changes = getChanges();

    changes.assertTotalItems(1);
    changes.assertScheduledForDeletion(pendingChanges.rootfolder);
  }

  // pending changes -> delete, schedule for deletion -> revert
  @Test
  public void testPendingChangesDeleteRootRevert() throws Exception {
    final PendingChanges pendingChanges = new PendingChanges();
    deleteFileInCommand(pendingChanges.rootfolder);

    TestChangeListBuilder changes = getChanges();

    changes.assertTotalItems(1);
    changes.assertScheduledForDeletion(pendingChanges.rootfolder);

    rollback(changes.getChanges());
    changes = getChanges();
    changes.assertTotalItems(0);
  }

  // pending changes -> rename root -> revert root only
  @Test
  public void testPendingChangesRenameRootRevert() throws Exception {
    final PendingChanges pendingChanges = new PendingChanges();
    FilePath rootBefore = pendingChanges.rootfolder;
    final String newName = pendingChanges.rootfolder.getName() + "_Renamed";
    renameFileInCommand(pendingChanges.rootfolder, newName);
    final FilePath rootAfter = getChildPath(rootBefore.getParentPath(), newName);
    pendingChanges.assertChanges(getChanges(), rootAfter, true);

    TestChangeListBuilder changes = getChanges();
    rollback(changes.getMoveChange(rootBefore, rootAfter));
    pendingChanges.assertChanges(getChanges());
  }

  // pending changes -> rename root -> commit root
  @Test
  public void testPendingChangesRenameRootCommit() throws Exception {
    final PendingChanges pendingChanges = new PendingChanges();
    FilePath rootBefore = pendingChanges.rootfolder;
    final String newName = pendingChanges.rootfolder.getName() + "_Renamed";
    renameFileInCommand(pendingChanges.rootfolder, newName);

    TestChangeListBuilder changes = getChanges();

    final FilePath rootAfter = getChildPath(rootBefore.getParentPath(), newName);

    commit(changes.getMoveChange(rootBefore, rootAfter), "test");

    pendingChanges.assertChanges(getChanges(), rootAfter, false);
  }

  // package.subpackage.Class -> rename package, modify Class -> revert changes
  @Test
  public void testRenamePackageRevert() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);

    final String rootPackageName = "rootpackage";
    final VirtualFile rootPackage = createDirInCommand(mySandboxRoot, rootPackageName);

    final String subPackageName = "subpackage";
    final VirtualFile subPackage = createDirInCommand(mySandboxRoot, subPackageName);

    final String fileName = "ClassA.java";
    final VirtualFile file = createFileInCommand(subPackage, fileName, "content");

    commit();

    assertFileStatus(rootPackage, FileStatus.NOT_CHANGED);
    assertFileStatus(subPackage, FileStatus.NOT_CHANGED);
    assertFileStatus(file, FileStatus.NOT_CHANGED);

    FilePath rootPackageOriginal = TfsFileUtil.getFilePath(rootPackage);
    final String rootPackageRenamedName = rootPackageName + "_renamed";
    rename(rootPackage, rootPackageRenamedName);
    editFiles(file);

    TestChangeListBuilder changes = getChanges();

    changes.assertTotalItems(2);
    changes.assertModified(file);
    changes.assertRenamedOrMoved(rootPackageOriginal, TfsFileUtil.getFilePath(rootPackage));

    final FilePath rootPackageRenamed = TfsFileUtil.getFilePath(rootPackage);
    rollback(getChanges().getMoveChange(rootPackageOriginal, rootPackageRenamed));

    changes = getChanges();
    changes.assertTotalItems(1);
    changes.assertModified(file);
  }

  // pending changes -> move root -> revert
  @Test
  public void testPendingChangesMoveRootRevert() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "anotherfolder");
    final VirtualFile subfolder = createDirInCommand(folder, "subfolder");
    commit(getChanges().getChanges(), "test - folders created");

    final PendingChanges pendingChanges = new PendingChanges();
    FilePath rootBefore = pendingChanges.rootfolder;

    moveFileInCommand(pendingChanges.rootfolder.getVirtualFile(), subfolder);

    final FilePath rootAfter = getChildPath(subfolder, pendingChanges.rootfolder.getName());
    final TestChangeListBuilder changes = getChanges();
    pendingChanges.assertChanges(changes, rootAfter, true);

    rollback(changes.getMoveChange(rootBefore, rootAfter));
    pendingChanges.assertChanges(getChanges());
  }

  // pending changes -> move root -> commit
  @Test
  public void testPendingChangesMoveRootCommit() throws Exception {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    final VirtualFile folder = createDirInCommand(mySandboxRoot, "anotherfolder");
    final VirtualFile subfolder = createDirInCommand(folder, "subfolder");
    commit(getChanges().getChanges(), "test - folders created");

    final PendingChanges pendingChanges = new PendingChanges();
    FilePath rootBefore = pendingChanges.rootfolder;

    moveFileInCommand(pendingChanges.rootfolder.getVirtualFile(), subfolder);

    final FilePath rootAfter = getChildPath(subfolder, pendingChanges.rootfolder.getName());
    final TestChangeListBuilder changes = getChanges();
    pendingChanges.assertChanges(changes, rootAfter, true);

    commit(changes.getMoveChange(rootBefore, rootAfter), "test");
    pendingChanges.assertChanges(getChanges(), rootAfter, false);
  }

  private class PendingChanges {
    public final FilePath rootfolder;
    public final FilePath fileUnversioned;
    public final FilePath fileUpToDate;
    public final FilePath fileRenamedOriginal;
    public final FilePath fileToDelete;
    public final FilePath fileCheckedOut;
    public final FilePath fileHijacked;
    public final FilePath fileCheckedOutAndRenamedOriginal;
    public final FilePath fileMissing;
    public final FilePath fileToAdd;
    public final FilePath fileCheckedOutAndRenamed;
    public final FilePath folderUpToDate;
    public final FilePath folderToAdd;
    public final FilePath folderToDelete;
    public final FilePath folderRenamedOriginal;
    public final FilePath folderRenamed;
    public final FilePath folderMissing;
    public final FilePath folderUnversioned;
    public final FilePath fileRenamed;

    public PendingChanges() throws Exception {
      doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
      final String filenameUpToDate = "upToDate.txt";
      final String filenameToAdd = "ScheduledForAddition.txt";
      final String filenameRenamedOriginalName = "renamed_original.txt";
      final String filenameToDelete = "scheduledForDeletion.txt";
      final String filenameCheckedOut = "checkedOut.txt";
      final String filenameHijacked = "hijacked.txt";
      final String filenameCheckedOutAndRenamed = "checkedOutAndRenamed_original.txt";
      final String filenameMissing = "missing.txt";
      final String filenameUnversioned = "unversioned.txt";

      final String rootFolderName = "rootFolder";
      final String foldernameToAdd = "ScheduledForAddition";
      final String foldernameUpToDate = "UpToDate";
      final String foldernameToDelete = "ScheduledForDeletion";
      final String foldernameRenamedOriginalName = "Renamed_original";
      final String foldernameMissing = "Missing";
      final String foldernameUnversioned = "Unversioned";

      rootfolder = TfsFileUtil.getFilePath(createDirInCommand(mySandboxRoot, rootFolderName));
      commit();

      doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
      fileUnversioned = TfsFileUtil.getFilePath(createFileInCommand(rootfolder, filenameUnversioned, ""));
      folderUnversioned = TfsFileUtil.getFilePath(createDirInCommand(rootfolder, foldernameUnversioned));

      doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
      fileUpToDate = TfsFileUtil.getFilePath(createFileInCommand(rootfolder, filenameUpToDate, ""));
      fileRenamedOriginal = TfsFileUtil.getFilePath(createFileInCommand(rootfolder, filenameRenamedOriginalName, ""));
      fileToDelete = TfsFileUtil.getFilePath(createFileInCommand(rootfolder, filenameToDelete, ""));
      fileCheckedOut = TfsFileUtil.getFilePath(createFileInCommand(rootfolder, filenameCheckedOut, ""));
      fileHijacked = TfsFileUtil.getFilePath(createFileInCommand(rootfolder, filenameHijacked, ""));
      fileCheckedOutAndRenamedOriginal = TfsFileUtil.getFilePath(createFileInCommand(rootfolder, filenameCheckedOutAndRenamed, ""));
      fileMissing = TfsFileUtil.getFilePath(createFileInCommand(rootfolder, filenameMissing, ""));

      folderUpToDate = TfsFileUtil.getFilePath(createDirInCommand(rootfolder, foldernameUpToDate));
      folderToDelete = TfsFileUtil.getFilePath(createDirInCommand(rootfolder, foldernameToDelete));
      folderRenamedOriginal = TfsFileUtil.getFilePath(createDirInCommand(rootfolder, foldernameRenamedOriginalName));
      folderMissing = TfsFileUtil.getFilePath(createDirInCommand(rootfolder, foldernameMissing));
      commit();

      folderToAdd = TfsFileUtil.getFilePath(createDirInCommand(rootfolder, foldernameToAdd));

      fileToAdd = TfsFileUtil.getFilePath(createFileInCommand(rootfolder, filenameToAdd, ""));

      doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
      deleteFileInCommand(fileToDelete);
      deleteFileInCommand(folderToDelete);

      clearReadonlyStatusExternally(fileHijacked, fileRenamedOriginal);

      final String fileRenamedNewName = "renamed.txt";
      rename(fileRenamedOriginal, fileRenamedNewName);
      fileRenamed = getChildPath(rootfolder, fileRenamedNewName);

      final String folderRenamedNewName = "Renamed";
      rename(folderRenamedOriginal, folderRenamedNewName);
      folderRenamed = getChildPath(rootfolder, folderRenamedNewName);

      editFiles(fileCheckedOut);
      editFiles(fileCheckedOutAndRenamedOriginal);

      final String fileCheckedOutAndRenamedNewName = "checkedOutAndRenamed.txt";
      rename(fileCheckedOutAndRenamedOriginal, fileCheckedOutAndRenamedNewName);
      fileCheckedOutAndRenamed = getChildPath(rootfolder, fileCheckedOutAndRenamedNewName);

      deleteFileExternally(fileMissing);
      deleteFileExternally(folderMissing);

      assertChanges(getChanges());
    }

    public void assertChanges(TestChangeListBuilder changes) throws VcsException {
      assertChanges(changes, rootfolder, false);
    }

    public void assertChanges(TestChangeListBuilder changes, FilePath newRootFolder, boolean pending) throws VcsException {
      if (!newRootFolder.equals(rootfolder) && pending) {
        changes.assertTotalItems(14);
        changes.assertRenamedOrMoved(rootfolder, newRootFolder);
      }
      else {
        changes.assertTotalItems(13);
      }

      changes.assertLocallyDeleted(replaceInPath(fileMissing, rootfolder, newRootFolder));
      changes.assertHijacked(replaceInPath(fileHijacked, rootfolder, newRootFolder));
      changes.assertUnversioned(replaceInPath(fileUnversioned, rootfolder, newRootFolder));
      changes.assertModified(replaceInPath(fileCheckedOut, rootfolder, newRootFolder));
      changes.assertScheduledForAddition(replaceInPath(fileToAdd, rootfolder, newRootFolder));
      changes.assertScheduledForDeletion(replaceInPath(fileToDelete, rootfolder, newRootFolder));
      changes.assertScheduledForAddition(replaceInPath(folderToAdd, rootfolder, newRootFolder));
      changes.assertScheduledForDeletion(replaceInPath(folderToDelete, rootfolder, newRootFolder));
      changes.assertLocallyDeleted(replaceInPath(folderMissing, rootfolder, newRootFolder));

      if (pending) {
        changes.assertRenamedOrMoved(fileRenamedOriginal, replaceInPath(fileRenamed, rootfolder, newRootFolder));
        changes.assertRenamedOrMoved(fileCheckedOutAndRenamedOriginal, replaceInPath(fileCheckedOutAndRenamed, rootfolder, newRootFolder));
        changes.assertRenamedOrMoved(folderRenamedOriginal, replaceInPath(folderRenamed, rootfolder, newRootFolder));
      }
      else {
        changes.assertRenamedOrMoved(replaceInPath(fileRenamedOriginal, rootfolder, newRootFolder),
                                     replaceInPath(fileRenamed, rootfolder, newRootFolder));
        changes.assertRenamedOrMoved(replaceInPath(fileCheckedOutAndRenamedOriginal, rootfolder, newRootFolder),
                                     replaceInPath(fileCheckedOutAndRenamed, rootfolder, newRootFolder));
        changes.assertRenamedOrMoved(replaceInPath(folderRenamedOriginal, rootfolder, newRootFolder),
                                     replaceInPath(folderRenamed, rootfolder, newRootFolder));
      }
    }
  }

}
