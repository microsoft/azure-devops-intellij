package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.axis2.databinding.ADBBean;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlServer;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkstationHelper;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;

import java.util.*;

public class TFSRollbackEnvironment implements RollbackEnvironment {

  public String getRollbackOperationName() {
    return "Undo Pending Changes";
  }

  public List<VcsException> rollbackChanges(final List<Change> changes) {
    List<String> localPaths = new ArrayList<String>();
    for (Change change : changes) {
      final String path;
      if (change.getBeforeRevision() != null) {
        path = change.getBeforeRevision().getFile().getPath();
      }
      else {
        path = change.getAfterRevision().getFile().getPath();
      }
      localPaths.add(path);
    }
    return undoPendingChanges(localPaths, true);
  }

  public List<VcsException> rollbackMissingFileDeletion(final List<FilePath> files) {
    List<String> localPaths = new ArrayList<String>();
    for (FilePath path : files) {
      localPaths.add(path.getPath());
    }
    return undoPendingChanges(localPaths, true);
  }

  public List<VcsException> rollbackModifiedWithoutCheckout(final List<VirtualFile> files) {
    List<String> localPaths = new ArrayList<String>();
    for (VirtualFile file : files) {
      localPaths.add(file.getPath());
    }
    try {
      WorkstationHelper.processByWorkspaces(localPaths, new WorkstationHelper.VoidDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<String> serverPaths) throws TfsException {
          // query extended items to determine base version
          Map<String, ExtendedItem> extendedItems = workspace.getExtendedItems(serverPaths);

          // query GetOperation-s
          Map<String, VersionSpec> requests = new HashMap<String, VersionSpec>();
          for (Map.Entry<String, ExtendedItem> e : extendedItems.entrySet()) {
            requests.put(e.getKey(), new ChangesetVersionSpec(e.getValue().getLver()));
          }
          Map<String, GetOperation> operations =
            workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), requests);

          // update content
          for (GetOperation operation : operations.values()) {
            VersionControlServer.downloadItem(workspace, operation, true, true);
          }
        }
      });

      return Collections.emptyList();
    }
    catch (TfsException e) {
      return Collections.singletonList(new VcsException("Failed to undo pending changes", e));
    }
  }

  public void rollbackIfUnchanged(final VirtualFile file) {
    undoPendingChanges(Collections.singletonList(file.getPath()), false);
  }

  private static List<VcsException> undoPendingChanges(final List<String> localPaths, final boolean updateToBaseVersion) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(localPaths, new WorkstationHelper.VoidDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<String> serverPaths) throws TfsException {
          // undo changes
          Map<String, ADBBean> results =
            workspace.getServer().getVCS().undoPendingChanges(workspace.getName(), workspace.getOwnerName(), serverPaths);
          List<LocalVersionUpdate> updateLocalVersions = new ArrayList<LocalVersionUpdate>();

          // TODO: we should update local version when reverting scheduled for deletion folder

          // update content
          for (ADBBean resultBean : results.values()) {
            if (resultBean instanceof GetOperation) {
              GetOperation getOperation = (GetOperation)resultBean;
              if (updateToBaseVersion && getOperation.getDurl() != null) {
                VersionControlServer.downloadItem(workspace, getOperation, true, true);
                if (getOperation.getLver() == Integer.MIN_VALUE) {
                  updateLocalVersions.add(VersionControlServer.createLocalVersionUpdate(getOperation));
                }
              }
            }
            else {
              Failure failure = (Failure)resultBean;
              errors.add(new VcsException("Failed to undo pending changes for " + failure.getLocal() + ": " + failure.getMessage()));
            }
          }

          // update local versions
          if (!updateLocalVersions.isEmpty()) {
            workspace.getServer().getVCS().updateLocalVersions(workspace.getName(), workspace.getOwnerName(), updateLocalVersions);
          }
        }
      });
      return errors;
    }
    catch (TfsException e) {
      return Collections.singletonList(new VcsException("Failed to undo pending changes", e));
    }
  }


}
