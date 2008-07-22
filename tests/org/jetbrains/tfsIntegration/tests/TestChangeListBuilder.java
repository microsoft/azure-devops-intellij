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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.vcs.MockChangelistBuilder;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class TestChangeListBuilder extends MockChangelistBuilder {

  private final VirtualFile myRootPath;
  private final Project myProject;

  private final List<VirtualFile> myUnversionedFiles = new ArrayList<VirtualFile>();
  private final List<FilePath> myLocallyDeletedFiles = new ArrayList<FilePath>();
  private final List<VirtualFile> myHijackedFiles = new ArrayList<VirtualFile>();
  private final List<VirtualFile> myIgnoredFiles = new ArrayList<VirtualFile>();

  public TestChangeListBuilder(final VirtualFile rootPath, final Project project) {
    myRootPath = rootPath;
    myProject = project;
  }

  public void processUnversionedFile(VirtualFile file) {
    myUnversionedFiles.add(file);
  }

  public void processLocallyDeletedFile(FilePath file) {
    myLocallyDeletedFiles.add(file);
  }

  public void processModifiedWithoutCheckout(VirtualFile file) {
    myHijackedFiles.add(file);
  }

  public void processIgnoredFile(VirtualFile file) {
    myIgnoredFiles.add(file);
  }

  public void processSwitchedFile(VirtualFile file, String branch, final boolean recursive) {
    // TODO
  }

  public void assertUnversioned(FilePath file) {
    assertUnversioned(file.getIOFile().getPath());
  }

  public void assertUnversioned(VirtualFile file) {
    Assert.assertTrue(toString(), myUnversionedFiles.contains(file));
    assertFileStatus(file, FileStatus.UNKNOWN);
  }

  public void assertUnversioned(String file) {
    Assert.assertTrue(toString(), myUnversionedFiles.contains(VcsUtil.getVirtualFile(file)));
    assertFileStatus(file, FileStatus.UNKNOWN);
  }

  public void assertLocallyDeleted(VirtualFile file) {
    assertLocallyDeleted(TfsFileUtil.getFilePath(file));
  }

  public void assertLocallyDeleted(String file) {
    assertLocallyDeleted(VcsUtil.getFilePath(file));
  }

  public void assertLocallyDeleted(FilePath path) {
    Assert.assertTrue(toString(), myLocallyDeletedFiles.contains(path));
    assertFileStatus(path, FileStatus.DELETED_FROM_FS);
  }

  public List<VirtualFile> getHijackedFiles() {
    return myHijackedFiles;
  }

  public void assertHijacked(FilePath file) {
    assertHijacked(file.getIOFile().getPath());
  }

  public void assertHijacked(VirtualFile file) {
    Assert.assertTrue(toString(), myHijackedFiles.contains(file));
    assertFileStatus(file, FileStatus.HIJACKED);
  }

  public void assertHijacked(String file) {
    Assert.assertTrue(toString(), myHijackedFiles.contains(VcsUtil.getVirtualFile(file)));
    assertFileStatus(file, FileStatus.HIJACKED);
  }

  public void assertIgnored(VirtualFile file) {
    Assert.assertTrue(toString(), myIgnoredFiles.contains(file));
    assertFileStatus(file, FileStatus.IGNORED);
  }

  public void assertIgnored(String file) {
    Assert.assertTrue(toString(), myIgnoredFiles.contains(VcsUtil.getVirtualFile(file)));
    assertFileStatus(file, FileStatus.IGNORED);
  }

  public void assertScheduledForAddition(VirtualFile file) {
    assertScheduledForAddition(TfsFileUtil.getFilePath(file));
  }

  public void assertScheduledForAddition(String file) {
    assertScheduledForAddition(VcsUtil.getFilePath(file));
  }

  public void assertScheduledForAddition(FilePath file) {
    if (ChangeHelper.containsAdded(getChanges(), file)) {
      assertFileStatus(file, FileStatus.ADDED);
      return;
    }
    Assert.fail(toString());
  }

  public void assertScheduledForDeletion(VirtualFile file) {
    assertScheduledForDeletion(TfsFileUtil.getFilePath(file));
  }

  public void assertScheduledForDeletion(String file) {
    assertScheduledForDeletion(VcsUtil.getFilePath(file));
  }

  public void assertScheduledForDeletion(FilePath file) {
    if (ChangeHelper.containsDeleted(getChanges(), file)) {
      assertFileStatus(file, FileStatus.DELETED);
      return;
    }
    Assert.fail(toString());
  }

  public void assertRenamedOrMoved(FilePath from, FilePath to) {
    Assert.assertNotNull(toString(), getMoveChange(from, to));
  }

  public void assertRenamedOrMoved(FilePath from, FilePath to, String originalContent, String modifiedContent) throws VcsException {
    final Change moveChange = getMoveChange(from, to);
    Assert.assertNotNull(toString(), moveChange);
    Assert.assertEquals(moveChange.getBeforeRevision().getContent(), originalContent);
    Assert.assertEquals(moveChange.getAfterRevision().getContent(), modifiedContent);
  }


  public void assertRenamedOrMoved(String from, String to) {
    Assert
      .assertNotNull("from=" + from + ", to= " + to + "\n" + toString(), getMoveChange(VcsUtil.getFilePath(from), VcsUtil.getFilePath(to)));
  }

  @Nullable
  public Change getMoveChange(final String from, String to) {
    return getMoveChange(VcsUtil.getFilePath(from), VcsUtil.getFilePath(to));
  }

  @Nullable
  public Change getMoveChange(final FilePath from, FilePath to) {
    return ChangeHelper.getMoveChange(getChanges(), from, to);
  }

  public Change getModificationChange(final FilePath file) {
    return ChangeHelper.getModificationChange(getChanges(), file);
  }

  public void assertModified(VirtualFile file) {
    assertModified(TfsFileUtil.getFilePath(file));
  }

  public void assertModified(String file) {
    assertModified(VcsUtil.getFilePath(file));
  }

  public void assertModified(FilePath file) {
    if (ChangeHelper.containsModified(getChanges(), file)) {
      // TODO: FileStatus HIJACKED while expected MODIFIED
      //assertFileStatus(file, FileStatus.MODIFIED);
      return;
    }
    Assert.fail(toString());
  }

  public void assertModified(FilePath file, String originalContent, String modifiedContent) throws VcsException {
    final Change change = ChangeHelper.getModificationChange(getChanges(), file);
    Assert.assertNotNull(change);
    Assert.assertEquals(originalContent, change.getBeforeRevision().getContent());
    Assert.assertEquals(modifiedContent, change.getAfterRevision().getContent());
  }

  public List<FilePath> getLocallyDeleted() {
    return myLocallyDeletedFiles;
  }

  public void assertTotalItems(int n) {
    Assert.assertEquals(toString(), n, getTotalItems());
  }

  private int getTotalItems() {
    return getChanges().size() + myUnversionedFiles.size() + myLocallyDeletedFiles.size() + myHijackedFiles.size() + myIgnoredFiles.size();
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("Total changes: ").append(getTotalItems()).append("\n");

    if (!getChanges().isEmpty()) {
      s.append(ChangeHelper.toString(getChanges(), myRootPath));
    }

    if (!myLocallyDeletedFiles.isEmpty()) {
      s.append("Locally deleted:\n");
      for (FilePath p : myLocallyDeletedFiles) {
        s.append("\t").append(ChangeHelper.getPathRemainder(p, myRootPath)).append("\n");
      }
    }

    if (!myHijackedFiles.isEmpty()) {
      s.append("Hijacked:\n");
      for (VirtualFile f : myHijackedFiles) {
        s.append("\t").append(ChangeHelper.getPathRemainder(f, myRootPath)).append("\n");
      }
    }

    if (!myUnversionedFiles.isEmpty()) {
      s.append("Unversioned:\n");
      for (VirtualFile f : myUnversionedFiles) {
        s.append("\t").append(ChangeHelper.getPathRemainder(f, myRootPath)).append("\n");
      }
    }

    return s.toString();
  }

  private void assertFileStatus(@Nullable VirtualFile file, FileStatus expectedStatus) {
    if (file == null) {
      Assert.assertTrue("expected status: " + expectedStatus,
                        FileStatus.DELETED == expectedStatus || FileStatus.DELETED_FROM_FS == expectedStatus);
    }
    else {
      final FileStatus realStatus = FileStatusManager.getInstance(myProject).getStatus(file);
      Assert.assertTrue("FileStatus " + realStatus + " while expected " + expectedStatus, realStatus == expectedStatus);
    }
  }

  private void assertFileStatus(String file, FileStatus expectedStatus) {
    assertFileStatus(VcsUtil.getVirtualFile(file), expectedStatus);
  }

  private void assertFileStatus(FilePath path, FileStatus expectedStatus) {
    path.refresh();
    assertFileStatus(path.getIOFile().getPath(), expectedStatus);
  }

  public Change getAddChange(final FilePath file) {
    return ChangeHelper.getAddChange(getChanges(), file);
  }
}
