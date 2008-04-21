package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
      ContentRevision revision = change.getType() == Change.Type.DELETED ? change.getBeforeRevision() : change.getAfterRevision();
      localPaths.add(revision.getFile());
    }
    return undoPendingChanges(localPaths, true);
  }

  public List<VcsException> rollbackMissingFileDeletion(final List<FilePath> files) {
    try {
      WorkstationHelper.processByWorkspaces(files, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          Map<ItemPath, ServerStatus> local2serverStatus = StatusProvider.determineServerStatus(workspace, paths);
          final List<VersionControlServer.GetRequestParams> getRequests = new ArrayList<VersionControlServer.GetRequestParams>();
          for (Map.Entry<ItemPath, ServerStatus> e : local2serverStatus.entrySet()) {
            e.getValue().visitBy(e.getKey(), new StatusVisitor() {

              public void unversioned(@NotNull final ItemPath path,
                                      final @Nullable ExtendedItem extendedItem,
                                      final boolean localItemExists) throws TfsException {
                TFSVcs.error("Server status Unversioned when rolling back missing file deletion: " + path.getLocalPath().getPath());
              }

              public void checkedOutForEdit(@NotNull final ItemPath path,
                                            final @NotNull ExtendedItem extendedItem,
                                            final boolean localItemExists) {
                addForDownload(path, extendedItem);
              }

              public void scheduledForAddition(@NotNull final ItemPath path,
                                               final @NotNull ExtendedItem extendedItem,
                                               final boolean localItemExists) {
                TFSVcs
                  .error("Server status ScheduledForAddition when rolling back missing file deletion: " + path.getLocalPath().getPath());
              }

              public void scheduledForDeletion(@NotNull final ItemPath path,
                                               final @NotNull ExtendedItem extendedItem,
                                               final boolean localItemExists) {
                TFSVcs
                  .error("Server status ScheduledForDeletion when rolling back missing file deletion: " + path.getLocalPath().getPath());
              }

              public void outOfDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
                throws TfsException {
                addForDownload(path, extendedItem);
              }

              public void deleted(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists) {
                TFSVcs.error("Server status Deleted when rolling back missing file deletion: " + path.getLocalPath().getPath());
              }

              public void upToDate(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem, final boolean localItemExists)
                throws TfsException {
                addForDownload(path, extendedItem);
              }

              public void renamed(final @NotNull ItemPath path, final ExtendedItem extendedItem, final boolean localItemExists)
                throws TfsException {
                addForDownload(path, extendedItem);
              }

              private void addForDownload(final @NotNull ItemPath path, final @NotNull ExtendedItem extendedItem) {
                getRequests.add(new VersionControlServer.GetRequestParams(path.getServerPath(), RecursionType.None,
                                                                          new ChangesetVersionSpec(extendedItem.getLver())));
              }

            }, false);
          }

          List<List<GetOperation>> getOperations =
            workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), getRequests);

          for (List<GetOperation> list : getOperations) {
            VersionControlServer.downloadItem(workspace, list.get(0), true, true);
          }
        }
      });
    }
    catch (TfsException e) {
      return Collections.singletonList(new VcsException("Failed to rollback file", e));
    }
    return Collections.emptyList();
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
            requests.add(new VersionControlServer.GetRequestParams(e.getKey().getServerPath(), RecursionType.None,
                                                                   new ChangesetVersionSpec(e.getValue().getLver())));
          }
          List<List<GetOperation>> operations = workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), requests);

          // update content
          for (List<GetOperation> list : operations) {
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
