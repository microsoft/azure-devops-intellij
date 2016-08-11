// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.project.Project;
//import com.intellij.openapi.util.Comparing;
//import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
//import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.rollback.DefaultRollbackEnvironment;
import com.intellij.openapi.vcs.rollback.RollbackProgressListener;
import com.intellij.openapi.vfs.VirtualFile;
//import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.GetOperation;
//import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.RecursionType;
import org.jetbrains.annotations.NotNull;
//import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
//import org.jetbrains.tfsIntegration.core.tfs.*;
//import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
//import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyProgress;
//import org.jetbrains.tfsIntegration.core.tfs.operations.UndoPendingChanges;
//import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
//import org.jetbrains.tfsIntegration.core.tfs.version.WorkspaceVersionSpec;
//import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
import java.util.List;

public class TFSRollbackEnvironment extends DefaultRollbackEnvironment {

  private final @NotNull Project myProject;

  public TFSRollbackEnvironment(final Project project) {
    myProject = project;
  }

  @SuppressWarnings({"ConstantConditions"})
  public void rollbackChanges(final List<Change> changes,
                              final List<VcsException> vcsExceptions,
                              @NotNull final RollbackProgressListener listener) {
    List<FilePath> localPaths = new ArrayList<FilePath>();

    listener.determinate();
    for (Change change : changes) {
      ContentRevision revision = change.getType() == Change.Type.DELETED ? change.getBeforeRevision() : change.getAfterRevision();
      localPaths.add(revision.getFile());
    }
    undoPendingChanges(localPaths, vcsExceptions, listener, false);
  }

  public void rollbackMissingFileDeletion(final List<FilePath> files,
                                          final List<VcsException> errors,
                                          final RollbackProgressListener listener) {
//    try {
//      WorkstationHelper.processByWorkspaces(files, false, myProject, new WorkstationHelper.VoidProcessDelegate() {
//        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
//          final List<VersionControlServer.GetRequestParams> download = new ArrayList<VersionControlServer.GetRequestParams>();
//          final Collection<String> undo = new ArrayList<String>();
//          StatusProvider.visitByStatus(workspace, paths, false, null, new StatusVisitor() {
//
//            public void unversioned(final @NotNull FilePath localPath,
//                                    final boolean localItemExists,
//                                    final @NotNull ServerStatus serverStatus) throws TfsException {
//              TFSVcs.error("Server returned status Unversioned when rolling back missing file deletion: " + localPath.getPresentableUrl());
//            }
//
//            public void checkedOutForEdit(final @NotNull FilePath localPath,
//                                          final boolean localItemExists,
//                                          final @NotNull ServerStatus serverStatus) {
//              undo.add(serverStatus.targetItem);
//            }
//
//            public void scheduledForAddition(final @NotNull FilePath localPath,
//                                             final boolean localItemExists,
//                                             final @NotNull ServerStatus serverStatus) {
//              undo.add(serverStatus.targetItem);
//            }
//
//            public void scheduledForDeletion(final @NotNull FilePath localPath,
//                                             final boolean localItemExists,
//                                             final @NotNull ServerStatus serverStatus) {
//              TFSVcs.error(
//                "Server returned status ScheduledForDeletion when rolling back missing file deletion: " + localPath.getPresentableUrl());
//            }
//
//            public void outOfDate(final @NotNull FilePath localPath,
//                                  final boolean localItemExists,
//                                  final @NotNull ServerStatus serverStatus) throws TfsException {
//              //noinspection ConstantConditions
//              addForDownload(serverStatus);
//            }
//
//            public void deleted(final @NotNull FilePath localPath,
//                                final boolean localItemExists,
//                                final @NotNull ServerStatus serverStatus) {
//              TFSVcs.error("Server returned status Deleted when rolling back missing file deletion: " + localPath.getPath());
//            }
//
//            public void upToDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
//              throws TfsException {
//              //noinspection ConstantConditions
//              addForDownload(serverStatus);
//            }
//
//            public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
//              throws TfsException {
//              undo.add(serverStatus.targetItem);
//            }
//
//            public void renamedCheckedOut(final @NotNull FilePath localPath,
//                                          final boolean localItemExists,
//                                          final @NotNull ServerStatus serverStatus) throws TfsException {
//              undo.add(serverStatus.targetItem);
//            }
//
//            public void undeleted(final @NotNull FilePath localPath,
//                                  final boolean localItemExists,
//                                  final @NotNull ServerStatus serverStatus) throws TfsException {
//              addForDownload(serverStatus);
//            }
//
//            private void addForDownload(final @NotNull ServerStatus serverStatus) {
//              download.add(new VersionControlServer.GetRequestParams(serverStatus.targetItem, RecursionType.None,
//                                                                     new ChangesetVersionSpec(serverStatus.localVer)));
//            }
//
//
//          }, myProject);
//
//          List<GetOperation> operations = workspace.getServer().getVCS()
//            .get(workspace.getName(), workspace.getOwnerName(), download, myProject, TFSBundle.message("preparing.for.download"));
//          final Collection<VcsException> downloadErrors =
//            ApplyGetOperations.execute(myProject, workspace, operations, ApplyProgress.EMPTY, null, ApplyGetOperations.DownloadMode.FORCE);
//          errors.addAll(downloadErrors);
//
//          final UndoPendingChanges.UndoPendingChangesResult undoResult =
//            UndoPendingChanges.execute(myProject, workspace, undo, false, new ApplyProgress.RollbackProgressWrapper(listener), false);
//          errors.addAll(undoResult.errors);
//        }
//      });
//    }
//    catch (TfsException e) {
//      //noinspection ThrowableInstanceNeverThrown
//      errors.add(new VcsException(e.getMessage(), e));
//    }
  }

  public void rollbackModifiedWithoutCheckout(final List<VirtualFile> files,
                                              final List<VcsException> errors,
                                              final RollbackProgressListener listener) {
//    try {
//      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), false, myProject, new WorkstationHelper.VoidProcessDelegate() {
//        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
//          // query extended items to determine base (local) version
//          //Map<ItemPath, ExtendedItem> extendedItems = workspace.getExtendedItems(paths);
//
//          // query GetOperation-s
//          List<VersionControlServer.GetRequestParams> requests = new ArrayList<VersionControlServer.GetRequestParams>(paths.size());
//          final WorkspaceVersionSpec versionSpec = new WorkspaceVersionSpec(workspace.getName(), workspace.getOwnerName());
//          for (ItemPath e : paths) {
//            requests.add(new VersionControlServer.GetRequestParams(e.getServerPath(), RecursionType.None, versionSpec));
//          }
//          List<GetOperation> operations = workspace.getServer().getVCS()
//            .get(workspace.getName(), workspace.getOwnerName(), requests, myProject, TFSBundle.message("preparing.for.download"));
//          final Collection<VcsException> applyingErrors = ApplyGetOperations
//            .execute(myProject, workspace, operations, new ApplyProgress.RollbackProgressWrapper(listener), null,
//                     ApplyGetOperations.DownloadMode.FORCE);
//          errors.addAll(applyingErrors);
//        }
//      });
//    }
//    catch (TfsException e) {
//      //noinspection ThrowableInstanceNeverThrown
//      errors.add(new VcsException("Cannot undo pending changes", e));
//    }
  }

  public void rollbackIfUnchanged(final VirtualFile file) {
    // TODO: this was commented out by Jetbrains
    /*final List<VcsException> errors = new ArrayList<VcsException>();
    boolean unchanged = false;
    try {
      FilePath path = TfsFileUtil.getFilePath(file);
      String localContent = CurrentContentRevision.create(path).getContent();
      TFSContentRevision currentRevision = TfsUtil.getCurrentRevision(myProject, path, TFSBundle.message("loading.item"));
      unchanged = currentRevision != null && Comparing.equal(localContent, currentRevision.getContent());
    }
    catch (VcsException e) {
      errors.add(e);
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      errors.add(new VcsException(e));
    }

    if (unchanged) {
      undoPendingChanges(Collections.singletonList(TfsFileUtil.getFilePath(file)), errors, RollbackProgressListener.EMPTY, true);
    }
    if (!errors.isEmpty()) {
      AbstractVcsHelper.getInstance(myProject).showErrors(errors, TFSVcs.TFS_NAME);
    }*/
  }

  private void undoPendingChanges(final List<FilePath> localPaths,
                                  final List<VcsException> errors,
                                  @NotNull final RollbackProgressListener listener,
                                  final boolean tolerateNoChangesFailure) {
//    try {
//      WorkstationHelper.processByWorkspaces(localPaths, false, myProject, new WorkstationHelper.VoidProcessDelegate() {
//        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
//          Collection<String> serverPaths = new ArrayList<String>(paths.size());
//          for (ItemPath itemPath : paths) {
//            serverPaths.add(itemPath.getServerPath());
//          }
//          UndoPendingChanges.UndoPendingChangesResult undoResult = UndoPendingChanges
//            .execute(myProject, workspace, serverPaths, false, new ApplyProgress.RollbackProgressWrapper(listener),
//                     tolerateNoChangesFailure);
//          errors.addAll(undoResult.errors);
//          List<VirtualFile> refresh = new ArrayList<VirtualFile>(paths.size());
//          for (ItemPath path : paths) {
//            listener.accept(path.getLocalPath());
//
//            ItemPath undone = undoResult.undonePaths.get(path);
//            FilePath subject = (undone != null ? undone : path).getLocalPath();
//            VirtualFile file = subject.getVirtualFileParent();
//            if (file != null && file.exists()) {
//              refresh.add(file);
//            }
//          }
//          TfsFileUtil.refreshAndMarkDirty(myProject, refresh, true);
//        }
//      });
//    }
//    catch (TfsException e) {
//      //noinspection ThrowableInstanceNeverThrown
//      errors.add(new VcsException("Cannot undo pending changes", e));
//    }
  }


}
