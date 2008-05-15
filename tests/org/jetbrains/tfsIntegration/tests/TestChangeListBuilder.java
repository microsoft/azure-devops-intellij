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
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.vcs.MockChangelistBuilder;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;

import java.util.ArrayList;
import java.util.List;

public class TestChangeListBuilder extends MockChangelistBuilder {

  private List<VirtualFile> myUnversionedFiles = new ArrayList<VirtualFile>();
  private List<FilePath> myLocallyDeletedFiles = new ArrayList<FilePath>();
  private List<VirtualFile> myModifiedWithoutCheckoutFiles = new ArrayList<VirtualFile>();
  private List<VirtualFile> myIgnoredFiles = new ArrayList<VirtualFile>();

  public void processUnversionedFile(VirtualFile file) {
    myUnversionedFiles.add(file);
  }

  public void processLocallyDeletedFile(FilePath file) {
    myLocallyDeletedFiles.add(file);
  }

  public void processModifiedWithoutCheckout(VirtualFile file) {
    myModifiedWithoutCheckoutFiles.add(file);
  }

  public void processIgnoredFile(VirtualFile file) {
    myIgnoredFiles.add(file);
  }

  public void processSwitchedFile(VirtualFile file, String branch, final boolean recursive) {
    // TODO
  }

  public List<VirtualFile> getUnversionedFiles() {
    return myUnversionedFiles;
  }

  public List<FilePath> getLocallyDeletedFiles() {
    return myLocallyDeletedFiles;
  }

  public List<VirtualFile> getModifiedWithoutCheckoutFiles() {
    return myModifiedWithoutCheckoutFiles;
  }

  public List<VirtualFile> getIgnoredFiles() {
    return myIgnoredFiles;
  }

  public boolean containsScheduledForAddition(VirtualFile file) {
    return containsScheduledForAddition(TfsFileUtil.getFilePath(file));
  }

  public boolean containsScheduledForAddition(FilePath file) {
    for (Change c : getChanges()) {
      if (c.getBeforeRevision() == null && c.getAfterRevision() != null) {
        if (c.getAfterRevision().getFile().equals(file)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean containsScheduledForDeletion(FilePath file) {
    for (Change c : getChanges()) {
      if (c.getBeforeRevision() != null && c.getAfterRevision() == null) {
        if (c.getBeforeRevision().getFile().equals(file)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean containsRenamedOrMoved(FilePath from, FilePath to) {
    for (Change c : getChanges()) {
      if (c.getBeforeRevision() != null && c.getAfterRevision() != null) {
        if (c.getBeforeRevision().getFile().equals(from) && c.getAfterRevision().getFile().equals(to)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean containsModified(VirtualFile file) {
    return containsModified(TfsFileUtil.getFilePath(file));
  }

  public boolean containsModified(FilePath file) {
    for (Change c : getChanges()) {
      if (c.getBeforeRevision() != null && c.getAfterRevision() != null) {
        if (c.getBeforeRevision().getFile().equals(file) && c.getType() == Change.Type.MODIFICATION) {
          return true;
        }
      }
    }
    return false;
  }

  public int getTotalItems() {
    return getChanges().size() +
           getIgnoredFiles().size() +
           getLocallyDeletedFiles().size() +
           getUnversionedFiles().size() +
           getModifiedWithoutCheckoutFiles().size();
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
          s.append("Add: ").append(change.getAfterRevision().getFile().getPresentableUrl());
        }
        else if (change.getType() == Change.Type.MODIFICATION) {
          s.append("Modified: ").append(change.getAfterRevision().getFile().getPresentableUrl());
        }
        else if (change.getType() == Change.Type.MOVED) {
          s.append("Rename/move: ").append(change.getBeforeRevision().getFile().getPresentableUrl()).append(" -> ").append(change.getAfterRevision().getFile().getPresentableUrl());
        } else {
          s.append("Remove: ").append(change.getBeforeRevision().getFile().getPresentableUrl());
        }
        s.append("\n");
      }
    }

    if (!getLocallyDeletedFiles().isEmpty()) {
      s.append("Locally deleted:\n");
      for (FilePath p : getLocallyDeletedFiles()) {
        s.append("\t").append(p.getPresentableUrl()).append("\n");
      }
    }

    if (!getModifiedWithoutCheckoutFiles().isEmpty()) {
      s.append("Hijacked:\n");
      for (VirtualFile f : getModifiedWithoutCheckoutFiles()) {
        s.append("\t").append(f.getPath()).append("\n");
      }
    }

    if (!getUnversionedFiles().isEmpty()) {
      s.append("Unversioned:\n");
      for (VirtualFile f : getUnversionedFiles()) {
        s.append("\t").append(f.getPath()).append("\n");
      }
    }
    
    return s.toString();
  }

}
