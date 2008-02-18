package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.DeletedState;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

import java.rmi.RemoteException;

public class TFSChangeProvider implements ChangeProvider {
  public void getChanges(final VcsDirtyScope dirtyScope, final ChangelistBuilder builder, final ProgressIndicator progress)
    throws VcsException {
    try {
      for (FilePath dirtyFile : dirtyScope.getDirtyFiles()) {
        WorkspaceInfo workspaceInfo = Workstation.getInstance().findWorkspace(dirtyFile.getPath());
        if (workspaceInfo == null) {
          continue;
        }
        String serverPath = workspaceInfo.findServerPathByLocalPath(dirtyFile.getPath());
        ExtendedItem result = workspaceInfo.getServer().getVCS()
          .getExtendedItem(workspaceInfo.getName(), workspaceInfo.getOwnerName(), serverPath, DeletedState.Any);
        if (result == null) {
          builder.processUnversionedFile(dirtyFile.getVirtualFile());
        }
        else {
          // todo: get real content of revisions
          ContentRevision beforeRevision = new CurrentContentRevision(dirtyFile);
          ContentRevision afterRevision = new CurrentContentRevision(dirtyFile);
          builder.processChange(new Change(beforeRevision, afterRevision));
        }
      }
    }
    catch (RemoteException e) {
      throw new VcsException(e);
    }
  }

  public boolean isModifiedDocumentTrackingRequired() {
    return true;
  }
}
