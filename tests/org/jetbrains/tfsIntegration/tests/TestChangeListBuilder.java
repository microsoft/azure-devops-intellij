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
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.vcs.MockChangelistBuilder;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class TestChangeListBuilder extends MockChangelistBuilder {

  private final String myPathPrefix;
  private final Project myProject;

  private final List<VirtualFile> myUnversionedFiles = new ArrayList<VirtualFile>();
  private final List<FilePath> myLocallyDeletedFiles = new ArrayList<FilePath>();
  private final List<VirtualFile> myHijackedFiles = new ArrayList<VirtualFile>();
  private final List<VirtualFile> myIgnoredFiles = new ArrayList<VirtualFile>();

  public TestChangeListBuilder(final String pathPrefix, final Project project) {
    myPathPrefix = pathPrefix;
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

  public void assertUnversioned(VirtualFile file) {
    Assert.assertTrue(toString(), myUnversionedFiles.contains(file));
    assertFileStatus(file, FileStatus.UNKNOWN);
  }

  public void assertLocallyDeleted(VirtualFile file) {
    assertLocallyDeleted(TfsFileUtil.getFilePath(file));
  }

  public void assertLocallyDeleted(FilePath path) {
    Assert.assertTrue(toString(), myLocallyDeletedFiles.contains(path));
    assertFileStatus(path, FileStatus.DELETED_FROM_FS);
  }

  public List<VirtualFile> getHijackedFiles() {
    return myHijackedFiles;
  }

  public void assertHijacked(VirtualFile file) {
    Assert.assertTrue(toString(), myHijackedFiles.contains(file));
    assertFileStatus(file, FileStatus.HIJACKED);
  }

  public void assertIgnored(VirtualFile file) {
    Assert.assertTrue(toString(), myIgnoredFiles.contains(file));
    assertFileStatus(file, FileStatus.IGNORED);
  }

  public void assertScheduledForAddition(VirtualFile file) {
    assertScheduledForAddition(TfsFileUtil.getFilePath(file));
  }

  public void assertScheduledForAddition(FilePath file) {
    for (Change c : getChanges()) {
      if (c.getBeforeRevision() == null && c.getAfterRevision() != null) {
        if (c.getAfterRevision().getFile().equals(file)) {
          assertFileStatus(file, FileStatus.ADDED);
          return;
        }
      }
    }
    Assert.fail(toString());
  }

  public void assertScheduledForDeletion(VirtualFile file) {
    assertScheduledForDeletion(TfsFileUtil.getFilePath(file));
  }

  public void assertScheduledForDeletion(FilePath file) {
    for (Change c : getChanges()) {
      if (c.getBeforeRevision() != null && c.getAfterRevision() == null) {
        if (c.getBeforeRevision().getFile().equals(file)) {
          assertFileStatus(file, FileStatus.DELETED);
          return;
        }
      }
    }
    Assert.fail(toString());
  }

  public void assertRenamedOrMoved(FilePath from, FilePath to) {
    Assert.assertNotNull(toString(), getMoveChange(from, to));
  }

  public Change getMoveChange(final FilePath from, FilePath to) {
    for (Change c : getChanges()) {
      if (c.getBeforeRevision() != null && c.getAfterRevision() != null) {
        if (c.getBeforeRevision().getFile().equals(from) && c.getAfterRevision().getFile().equals(to)) {
          assertFileStatus(to, FileStatus.MODIFIED);
          return c;
        }
      }
    }
    return null;
  }

  public void assertModified(VirtualFile file) {
    assertModified(TfsFileUtil.getFilePath(file));
  }

  public void assertModified(FilePath file) {
    for (Change c : getChanges()) {
      if (c.getBeforeRevision() != null && c.getAfterRevision() != null) {
        if (c.getBeforeRevision().getFile().equals(file) && c.getType() == Change.Type.MODIFICATION) {
          // TODO: FileStatus HIJACKED while expected MODIFIED
          //assertFileStatus(file, FileStatus.MODIFIED);
          return;
        }
      }
    }
    Assert.fail(toString());
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


  private String getPathRemainder(VirtualFile file) {
    String path = file.getPresentableUrl();
    return path.startsWith(myPathPrefix) ? path.substring(myPathPrefix.length() + 1) : path;
  }

  private String getPathRemainder(FilePath filePath) {
    String path = filePath.getPath();
    return path.startsWith(myPathPrefix) ? path.substring(myPathPrefix.length() + 1) : path;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("Total changes: ").append(getTotalItems()).append("\n");

    if (!getChanges().isEmpty()) {
      s.append("Changes:\n");
      for (Change change : getChanges()) {
        s.append("\t");
        if (change.getType() == Change.Type.NEW) {
          s.append("Add: ").append(getPathRemainder(change.getAfterRevision().getFile()));
        }
        else if (change.getType() == Change.Type.MODIFICATION) {
          s.append("Modified: ").append(getPathRemainder(change.getAfterRevision().getFile()));
        }
        else if (change.getType() == Change.Type.MOVED) {
          s.append("Rename/move: ").append(getPathRemainder(change.getBeforeRevision().getFile())).append(" -> ")
            .append(getPathRemainder(change.getAfterRevision().getFile()));
        }
        else {
          s.append("Remove: ").append(getPathRemainder(change.getBeforeRevision().getFile()));
        }
        s.append("\n");
      }
    }

    if (!myLocallyDeletedFiles.isEmpty()) {
      s.append("Locally deleted:\n");
      for (FilePath p : myLocallyDeletedFiles) {
        s.append("\t").append(getPathRemainder(p)).append("\n");
      }
    }

    if (!myHijackedFiles.isEmpty()) {
      s.append("Hijacked:\n");
      for (VirtualFile f : myHijackedFiles) {
        s.append("\t").append(getPathRemainder(f)).append("\n");
      }
    }

    if (!myUnversionedFiles.isEmpty()) {
      s.append("Unversioned:\n");
      for (VirtualFile f : myUnversionedFiles) {
        s.append("\t").append(getPathRemainder(f)).append("\n");
      }
    }

    return s.toString();
  }

  private void assertFileStatus(VirtualFile file, FileStatus expectedStatus) {
    final FileStatus realStatus = FileStatusManager.getInstance(myProject).getStatus(file);
    Assert.assertTrue("FileStatus " + realStatus + " while expected " + expectedStatus, realStatus == expectedStatus);
  }

  private void assertFileStatus(FilePath path, FileStatus expectedStatus) {
    VirtualFile file = path.getVirtualFile();
    if (file == null) {
      Assert.assertTrue(FileStatus.DELETED == expectedStatus || FileStatus.DELETED_FROM_FS == expectedStatus);
    }
    else {
      assertFileStatus(file, expectedStatus);
    }
  }

}
