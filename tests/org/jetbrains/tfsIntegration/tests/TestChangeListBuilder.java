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
  
}
