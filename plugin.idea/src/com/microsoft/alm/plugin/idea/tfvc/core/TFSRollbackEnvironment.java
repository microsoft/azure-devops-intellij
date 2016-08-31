// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.rollback.DefaultRollbackEnvironment;
import com.intellij.openapi.vcs.rollback.RollbackProgressListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TFSRollbackEnvironment extends DefaultRollbackEnvironment {
    private static final Logger logger = LoggerFactory.getLogger(TFSRollbackEnvironment.class);

    private final Project project;
    private final TFSVcs vcs;

    public TFSRollbackEnvironment(@NotNull final TFSVcs vcs, @NotNull final Project project) {
        logger.info("Initilizing TFSRollbackEnvironment");
        this.vcs = vcs;
        this.project = project;
    }

    public void rollbackChanges(final List<Change> changes,
                                final List<VcsException> vcsExceptions,
                                @NotNull final RollbackProgressListener listener) {
        logger.info("rollbackChanges started");
        final List<FilePath> localPaths = new ArrayList<FilePath>();

        listener.determinate();
        for (final Change change : changes) {
            final ContentRevision revision = change.getType() == Change.Type.DELETED ? change.getBeforeRevision() : change.getAfterRevision();
            localPaths.add(revision.getFile());
        }

        undoPendingChanges(localPaths, vcsExceptions, listener, false);
        logger.info("rollbackChanges ended");
    }

    public void rollbackMissingFileDeletion(final List<FilePath> files,
                                            final List<VcsException> errors,
                                            final RollbackProgressListener listener) {
//    try {
//      WorkstationHelper.processByWorkspaces(files, false, project, new WorkstationHelper.VoidProcessDelegate() {
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
//          }, project);
//
//          List<GetOperation> operations = workspace.getServer().getVCS()
//            .get(workspace.getName(), workspace.getOwnerName(), download, project, TFSBundle.message("preparing.for.download"));
//          final Collection<VcsException> downloadErrors =
//            ApplyGetOperations.execute(project, workspace, operations, ApplyProgress.EMPTY, null, ApplyGetOperations.DownloadMode.FORCE);
//          errors.addAll(downloadErrors);
//
//          final UndoPendingChanges.UndoPendingChangesResult undoResult =
//            UndoPendingChanges.execute(project, workspace, undo, false, new ApplyProgress.RollbackProgressWrapper(listener), false);
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
//      WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), false, project, new WorkstationHelper.VoidProcessDelegate() {
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
//            .get(workspace.getName(), workspace.getOwnerName(), requests, project, TFSBundle.message("preparing.for.download"));
//          final Collection<VcsException> applyingErrors = ApplyGetOperations
//            .execute(project, workspace, operations, new ApplyProgress.RollbackProgressWrapper(listener), null,
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
      TFSContentRevision currentRevision = TfsUtil.getCurrentRevision(project, path, TFSBundle.message("loading.item"));
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
      AbstractVcsHelper.getInstance(project).showErrors(errors, TFSVcs.TFS_NAME);
    }*/
    }

    private void undoPendingChanges(final List<FilePath> localPaths,
                                    final List<VcsException> errors,
                                    @NotNull final RollbackProgressListener listener,
                                    final boolean tolerateNoChangesFailure) {
        logger.info("undoPendingChanges started");
        try {
            // Convert the FilePath objects provided to a simple String list
            final List<String> localFiles = new ArrayList<String>(localPaths.size());
            for (final FilePath path : localPaths) {
                localFiles.add(path.getPath());
            }

            // Call the undo command synchronously
            final ServerContext context = vcs.getServerContext(false);
            final List<String> filesUndone = CommandUtils.undoLocalFiles(context, localFiles);

            // Trigger the accept callback and build up our refresh list
            final List<VirtualFile> refresh = new ArrayList<VirtualFile>(filesUndone.size());
            for (final String path : filesUndone) {
                // Call the accept method on the listener to indicate progress
                final File fileUndone = new File(path);
                listener.accept(fileUndone);

                // Add the parent folder of the file to our refresh list
                final VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(fileUndone);
                final VirtualFile parent = file != null ? file.getParent() : null;
                if (parent != null && parent.exists()) {
                    refresh.add(file);
                }
            }

            // Refresh all the folders that changed
            TfsFileUtil.refreshAndMarkDirty(project, refresh, true);
        } catch (final Throwable e) {
            logger.warn("undoPendingChanges: Errors caught: " + e.getMessage(), e);
            errors.add(new VcsException("Cannot undo pending changes", e));
        }
        logger.info("undoPendingChanges ended");
    }
}
