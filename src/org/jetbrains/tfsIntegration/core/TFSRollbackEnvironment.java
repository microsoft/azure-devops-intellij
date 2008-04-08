package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Failure;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TFSRollbackEnvironment implements RollbackEnvironment {

  public String getRollbackOperationName() {
    return "Undo Pending Changes";
  }

  @SuppressWarnings({"ConstantConditions"})
  public List<VcsException> rollbackChanges(final List<Change> changes) {
    List<FilePath> localPaths = new ArrayList<FilePath>();
    for (Change change : changes) {
      final FilePath path;
      if (change.getBeforeRevision() != null) {
        path = change.getBeforeRevision().getFile();
      }
      else {
        path = change.getAfterRevision().getFile();
      }
      localPaths.add(path);
    }
    return undoPendingChanges(localPaths, true);
  }

  public List<VcsException> rollbackMissingFileDeletion(final List<FilePath> files) {
    return undoPendingChanges(files, true);
  }

  public List<VcsException> rollbackModifiedWithoutCheckout(final List<VirtualFile> files) {
    try {
      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          // query extended items to determine base version
          Map<ItemPath, ExtendedItem> extendedItems = workspace.getExtendedItems(paths);

          // query GetOperation-s
          List<VersionControlServer.GetRequestParams> requests = new ArrayList<VersionControlServer.GetRequestParams>(extendedItems.size());
          for (Map.Entry<ItemPath, ExtendedItem> e : extendedItems.entrySet()) {
            requests.add(
              new VersionControlServer.GetRequestParams(e.getKey(), RecursionType.None, new ChangesetVersionSpec(e.getValue().getLver())));
          }
          Map<ItemPath, List<GetOperation>> operations =
            workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), requests);

          // update content
          for (List<GetOperation> list : operations.values()) {
            VersionControlServer.downloadItem(workspace, list.get(0), true, true);
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
    undoPendingChanges(Collections.singletonList(TfsFileUtil.getFilePath(file)), false);
  }

  private static List<VcsException> undoPendingChanges(final List<FilePath> localPaths, final boolean updateToBaseVersion) {
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(localPaths, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          List<Failure> failures = OperationHelper.undoPendingChanges(workspace, paths, updateToBaseVersion);
          exceptions.addAll(BeanHelper.getVcsExceptions("Failed to undo pending changes", failures));
        }
      });
      return exceptions;
    }
    catch (TfsException e) {
      return Collections.singletonList(new VcsException("Failed to undo pending changes", e));
    }
  }


}
