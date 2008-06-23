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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.junit.Test;

import java.io.File;

@SuppressWarnings({"HardCodedStringLiteral", "UnusedDeclaration"})
public class TestSimpleFolderOperations extends TFSTestCase {

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

    changes.assertTotalItems(4);
    changes.assertUnversioned(pendingChanges.folderUnversioned);
    changes.assertUnversioned(pendingChanges.fileUnversioned);
    changes.assertUnversioned(pendingChanges.fileToAdd);
    changes.assertUnversioned(pendingChanges.folderToAdd);
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

  // pending changes -> rename root
  @Test
  public void testPendingChangesRenameRoot() throws Exception {
    final PendingChanges pendingChanges = new PendingChanges();
    FilePath rootBefore = TfsFileUtil.getFilePath(pendingChanges.rootfolder);
    final String newName = pendingChanges.rootfolder.getName() + "_Renamed";
    renameFileInCommand(pendingChanges.rootfolder, newName);

    TestChangeListBuilder changes = getChanges();

    changes.assertTotalItems(14);
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(pendingChanges.fileMissing));
    changes.assertHijacked(pendingChanges.fileHijacked);
    changes.assertUnversioned(pendingChanges.fileUnversioned);
    changes.assertModified(pendingChanges.fileCheckedOut);
    changes.assertRenamedOrMoved(pendingChanges.fileRenamedOriginal, TfsFileUtil.getFilePath(pendingChanges.fileRenamed));
    changes.assertRenamedOrMoved(pendingChanges.fileCheckedOutAndRenamedOriginal,
                                 TfsFileUtil.getFilePath(pendingChanges.fileCheckedOutAndRenamed));
    changes.assertScheduledForAddition(pendingChanges.fileToAdd);
    changes.assertScheduledForDeletion(TfsFileUtil.getFilePath(pendingChanges.fileToDelete));
    changes.assertScheduledForAddition(pendingChanges.folderToAdd);
    changes.assertScheduledForDeletion(TfsFileUtil.getFilePath(pendingChanges.folderToDelete));
    changes.assertRenamedOrMoved(pendingChanges.folderRenamedOriginal, TfsFileUtil.getFilePath(pendingChanges.folderRenamed));
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(pendingChanges.folderMissing));
    changes.assertRenamedOrMoved(rootBefore, TfsFileUtil.getFilePath(pendingChanges.rootfolder));
  }

  // pending changes -> rename root -> revert root only
  @Test
  public void testPendingChangesRenameRootRevert() throws Exception {
    final PendingChanges pendingChanges = new PendingChanges();
    FilePath rootBefore = TfsFileUtil.getFilePath(pendingChanges.rootfolder);
    final String newName = pendingChanges.rootfolder.getName() + "_Renamed";
    renameFileInCommand(pendingChanges.rootfolder, newName);

    TestChangeListBuilder changes = getChanges();

    changes.assertTotalItems(14);
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(pendingChanges.fileMissing));
    changes.assertHijacked(pendingChanges.fileHijacked);
    changes.assertUnversioned(pendingChanges.fileUnversioned);
    changes.assertModified(pendingChanges.fileCheckedOut);
    changes.assertRenamedOrMoved(pendingChanges.fileRenamedOriginal, TfsFileUtil.getFilePath(pendingChanges.fileRenamed));
    changes.assertRenamedOrMoved(pendingChanges.fileCheckedOutAndRenamedOriginal,
                                 TfsFileUtil.getFilePath(pendingChanges.fileCheckedOutAndRenamed));
    changes.assertScheduledForAddition(pendingChanges.fileToAdd);
    changes.assertScheduledForDeletion(TfsFileUtil.getFilePath(pendingChanges.fileToDelete));
    changes.assertScheduledForAddition(pendingChanges.folderToAdd);
    changes.assertScheduledForDeletion(TfsFileUtil.getFilePath(pendingChanges.folderToDelete));
    changes.assertRenamedOrMoved(pendingChanges.folderRenamedOriginal, TfsFileUtil.getFilePath(pendingChanges.folderRenamed));
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(pendingChanges.folderMissing));
    final FilePath rootAfter = TfsFileUtil.getFilePath(pendingChanges.rootfolder);
    changes.assertRenamedOrMoved(rootBefore, rootAfter);

    rollback(changes.getMoveChange(rootBefore, rootAfter));

    changes = getChanges();

    changes.assertTotalItems(13);
    changes.assertLocallyDeleted(replaceInPath(pendingChanges.fileMissing, rootAfter.getName(), rootBefore.getName()));
    changes.assertHijacked(replaceInPath(pendingChanges.fileHijacked, rootAfter.getName(), rootBefore.getName()));
    changes.assertUnversioned(replaceInPath(pendingChanges.fileUnversioned, rootAfter.getName(), rootBefore.getName()));
    changes.assertModified(replaceInPath(pendingChanges.fileCheckedOut, rootAfter.getName(), rootBefore.getName()));
    changes.assertRenamedOrMoved(replaceInPath(pendingChanges.fileRenamedOriginal, rootAfter.getName(), rootBefore.getName()),
                                 replaceInPath(pendingChanges.fileRenamed, rootAfter.getName(),
                                                                                                       rootBefore.getName()));
    changes.assertRenamedOrMoved(
      replaceInPath(pendingChanges.fileCheckedOutAndRenamedOriginal, rootAfter.getName(), rootBefore.getName()),
      replaceInPath(pendingChanges.fileCheckedOutAndRenamed, rootAfter.getName(), rootBefore.getName()));
    changes.assertScheduledForAddition(replaceInPath(pendingChanges.fileToAdd, rootAfter.getName(), rootBefore.getName()));
    changes.assertScheduledForDeletion(replaceInPath(pendingChanges.fileToDelete, rootAfter.getName(), rootBefore.getName()));
    changes.assertScheduledForAddition(replaceInPath(pendingChanges.folderToAdd, rootAfter.getName(), rootBefore.getName()));
    changes.assertScheduledForDeletion(replaceInPath(pendingChanges.folderToDelete, rootAfter.getName(), rootBefore.getName()));
    changes.assertRenamedOrMoved(replaceInPath(pendingChanges.folderRenamedOriginal, rootAfter.getName(), rootBefore.getName()),
                                 replaceInPath(pendingChanges.folderRenamed, rootAfter.getName(), rootBefore.getName()));
    changes.assertLocallyDeleted(replaceInPath(pendingChanges.folderMissing, rootAfter.getName(), rootBefore.getName()));
  }

  // pending changes -> rename root -> commit root
  @Test
  public void testPendingChangesRenameRootCommit() throws Exception {
    final PendingChanges pendingChanges = new PendingChanges();
    FilePath rootBefore = TfsFileUtil.getFilePath(pendingChanges.rootfolder);
    final String newName = pendingChanges.rootfolder.getName() + "_Renamed";
    renameFileInCommand(pendingChanges.rootfolder, newName);

    TestChangeListBuilder changes = getChanges();

    changes.assertTotalItems(14);
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(pendingChanges.fileMissing));
    changes.assertHijacked(pendingChanges.fileHijacked);
    changes.assertUnversioned(pendingChanges.fileUnversioned);
    changes.assertModified(pendingChanges.fileCheckedOut);
    changes.assertRenamedOrMoved(pendingChanges.fileRenamedOriginal, TfsFileUtil.getFilePath(pendingChanges.fileRenamed));
    changes.assertRenamedOrMoved(pendingChanges.fileCheckedOutAndRenamedOriginal,
                                 TfsFileUtil.getFilePath(pendingChanges.fileCheckedOutAndRenamed));
    changes.assertScheduledForAddition(pendingChanges.fileToAdd);
    changes.assertScheduledForDeletion(TfsFileUtil.getFilePath(pendingChanges.fileToDelete));
    changes.assertScheduledForAddition(pendingChanges.folderToAdd);
    changes.assertScheduledForDeletion(TfsFileUtil.getFilePath(pendingChanges.folderToDelete));
    changes.assertRenamedOrMoved(pendingChanges.folderRenamedOriginal, TfsFileUtil.getFilePath(pendingChanges.folderRenamed));
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(pendingChanges.folderMissing));
    final FilePath rootAfter = TfsFileUtil.getFilePath(pendingChanges.rootfolder);
    changes.assertRenamedOrMoved(rootBefore, rootAfter);

    commit(changes.getMoveChange(rootBefore, rootAfter), "test");

    changes = getChanges();

    changes.assertTotalItems(13);
    changes.assertLocallyDeleted(TfsFileUtil.getFilePath(pendingChanges.fileMissing));
    changes.assertHijacked(pendingChanges.fileHijacked);
    changes.assertUnversioned(pendingChanges.fileUnversioned);
    changes.assertModified(pendingChanges.fileCheckedOut);
    changes.assertRenamedOrMoved(replaceInPath(pendingChanges.fileRenamedOriginal, rootBefore.getName(), rootAfter.getName()),
                                 replaceInPath(pendingChanges.fileRenamed, rootBefore.getName(), rootAfter.getName()));
    changes.assertRenamedOrMoved(
      replaceInPath(pendingChanges.fileCheckedOutAndRenamedOriginal, rootBefore.getName(), rootAfter.getName()),
      replaceInPath(pendingChanges.fileCheckedOutAndRenamed, rootBefore.getName(), rootAfter.getName()));
    changes.assertScheduledForAddition(pendingChanges.fileToAdd);
    changes.assertScheduledForDeletion(replaceInPath(pendingChanges.fileToDelete, rootBefore.getName(), rootAfter.getName()));
    changes.assertScheduledForAddition(pendingChanges.folderToAdd);
    changes.assertScheduledForDeletion(replaceInPath(pendingChanges.folderToDelete, rootBefore.getName(), rootAfter.getName()));
    changes.assertRenamedOrMoved(replaceInPath(pendingChanges.folderRenamedOriginal, rootBefore.getName(), rootAfter.getName()),
                                 replaceInPath(pendingChanges.folderRenamed, rootBefore.getName(), rootAfter.getName()));
    changes.assertLocallyDeleted(replaceInPath(pendingChanges.folderMissing, rootBefore.getName(), rootAfter.getName()));
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


  private class PendingChanges {
    public final VirtualFile rootfolder;
    public final VirtualFile fileUnversioned;
    public final VirtualFile fileUpToDate;
    public final FilePath fileRenamedOriginal;
    public final VirtualFile fileToDelete;
    public final VirtualFile fileCheckedOut;
    public final VirtualFile fileHijacked;
    public final FilePath fileCheckedOutAndRenamedOriginal;
    public final VirtualFile fileMissing;
    public final VirtualFile fileToAdd;
    public final VirtualFile fileCheckedOutAndRenamed;
    public final VirtualFile folderUpToDate;
    public final VirtualFile folderToAdd;
    public final VirtualFile folderToDelete;
    public final FilePath folderRenamedOriginal;
    public final VirtualFile folderRenamed;
    public final VirtualFile folderMissing;
    public final VirtualFile folderUnversioned;
    public final VirtualFile fileRenamed;

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

      rootfolder = createDirInCommand(mySandboxRoot, rootFolderName);
      commit();

      doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
      fileUnversioned = createFileInCommand(rootfolder, filenameUnversioned, "");
      folderUnversioned = createDirInCommand(rootfolder, foldernameUnversioned);

      doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
      fileUpToDate = createFileInCommand(rootfolder, filenameUpToDate, "");
      fileRenamed = createFileInCommand(rootfolder, filenameRenamedOriginalName, "");
      fileToDelete = createFileInCommand(rootfolder, filenameToDelete, "");
      fileCheckedOut = createFileInCommand(rootfolder, filenameCheckedOut, "");
      fileHijacked = createFileInCommand(rootfolder, filenameHijacked, "");
      fileCheckedOutAndRenamed = createFileInCommand(rootfolder, filenameCheckedOutAndRenamed, "");
      fileMissing = createFileInCommand(rootfolder, filenameMissing, "");

      folderUpToDate = createDirInCommand(rootfolder, foldernameUpToDate);
      folderToDelete = createDirInCommand(rootfolder, foldernameToDelete);
      folderRenamed = createDirInCommand(rootfolder, foldernameRenamedOriginalName);
      folderMissing = createDirInCommand(rootfolder, foldernameMissing);
      commit();

      folderToAdd = createDirInCommand(rootfolder, foldernameToAdd);

      fileToAdd = createFileInCommand(rootfolder, filenameToAdd, "");

      doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
      deleteFileInCommand(fileToDelete);
      deleteFileInCommand(folderToDelete);

      clearReadonlyStatusExternally(fileHijacked, fileRenamed);

      final String fileRenamedNewName = "renamed.txt";
      rename(fileRenamed, fileRenamedNewName);
      fileRenamedOriginal = VcsUtil.getFilePath(new File(new File(rootfolder.getPath()), filenameRenamedOriginalName));

      final String folderRenamedNewName = "Renamed";
      rename(folderRenamed, folderRenamedNewName);
      folderRenamedOriginal = VcsUtil.getFilePath(new File(new File(rootfolder.getPath()), foldernameRenamedOriginalName));

      editFiles(fileCheckedOut);
      editFiles(fileCheckedOutAndRenamed);

      final String fileCheckedOutAndRenamedNewName = "checkedOutAndRenamed.txt";
      rename(fileCheckedOutAndRenamed, fileCheckedOutAndRenamedNewName);
      fileCheckedOutAndRenamedOriginal = VcsUtil.getFilePath(new File(new File(rootfolder.getPath()), filenameCheckedOutAndRenamed));

      deleteFileExternally(fileMissing);
      deleteFileExternally(folderMissing);

      TestChangeListBuilder changes = getChanges();
      changes.assertTotalItems(13);
      changes.assertLocallyDeleted(TfsFileUtil.getFilePath(fileMissing));
      changes.assertHijacked(fileHijacked);
      changes.assertUnversioned(fileUnversioned);
      changes.assertModified(fileCheckedOut);
      changes.assertRenamedOrMoved(fileRenamedOriginal, TfsFileUtil.getFilePath(fileRenamed));
      changes.assertRenamedOrMoved(fileCheckedOutAndRenamedOriginal, TfsFileUtil.getFilePath(fileCheckedOutAndRenamed));
      changes.assertScheduledForAddition(fileToAdd);
      changes.assertScheduledForDeletion(TfsFileUtil.getFilePath(fileToDelete));
      changes.assertScheduledForAddition(folderToAdd);
      changes.assertScheduledForDeletion(TfsFileUtil.getFilePath(folderToDelete));
      changes.assertRenamedOrMoved(folderRenamedOriginal, TfsFileUtil.getFilePath(folderRenamed));
      changes.assertLocallyDeleted(TfsFileUtil.getFilePath(folderMissing));
    }
  }

  private static String replaceInPath(FilePath path, String folderFrom, String folderTo) {
    return FileUtil.toSystemIndependentName(path.getPath()).replace("/" + folderFrom + "/", "/" + folderTo + "/");
  }

  private static String replaceInPath(VirtualFile path, String folderFrom, String folderTo) {
    return FileUtil.toSystemIndependentName(path.getPath()).replace("/" + folderFrom + "/", "/" + folderTo + "/");
  }

}
