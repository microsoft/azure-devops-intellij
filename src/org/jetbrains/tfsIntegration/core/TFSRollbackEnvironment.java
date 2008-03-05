package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Collections;
import java.util.List;

public class TFSRollbackEnvironment implements RollbackEnvironment {
  
  public String getRollbackOperationName() {
    return "Revert";
  }

  public List<VcsException> rollbackChanges(final List<Change> changes) {
    return Collections.emptyList();  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List<VcsException> rollbackMissingFileDeletion(final List<FilePath> files) {
    return Collections.emptyList();  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List<VcsException> rollbackModifiedWithoutCheckout(final List<VirtualFile> files) {
    return Collections.emptyList();  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void rollbackIfUnchanged(final VirtualFile file) {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
