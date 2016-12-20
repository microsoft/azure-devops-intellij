// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsVFSListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.ServerStatus;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusProvider;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusVisitor;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.operations.ScheduleForDeletion;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TFSFileListener extends VcsVFSListener {
    public static final Logger logger = LoggerFactory.getLogger(TFSFileListener.class);

    public TFSFileListener(Project project, TFSVcs vcs) {
        super(project, vcs);
    }

    protected String getAddTitle() {
        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_ITEMS);
    }

    protected String getSingleFileAddTitle() {
        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_ITEM);
    }

    protected String getSingleFileAddPromptTemplate() {
        // pass {0} as a param because the current {0} in the string needs to be replaced by something or else there is an error
        // the {0} will be replaced higher up by the file name that we don't have here
        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_PROMPT, "{0}");
    }

    protected void executeAdd() {
        logger.info("executeAdd executing...");
        try {
            final List<String> filePaths = TfsFileUtil.getFilePathStrings(myAddedFiles);
            final List<PendingChange> pendingChanges = new ArrayList<PendingChange>();

            ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                public void run() {
                    ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                    pendingChanges.addAll(CommandUtils.getStatusForFiles(TFSVcs.getInstance(myProject).getServerContext(true), filePaths));
                }
            }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_SCHEDULING), false, myProject);

            for (final PendingChange pendingChange : pendingChanges) {
                StatusProvider.visitByStatus(new StatusVisitor() {
                    public void unversioned(final @NotNull FilePath localPath,
                                            final boolean localItemExists,
                                            final @NotNull ServerStatus serverStatus) throws TfsException {
                        // ignore
                    }

                    public void checkedOutForEdit(final @NotNull FilePath localPath,
                                                  final boolean localItemExists,
                                                  final @NotNull ServerStatus serverStatus) {
                        // TODO (Jetbrains): add local conflict
                    }

                    @Override
                    public void locked(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) throws TfsException {
                        // ignore
                    }

                    public void scheduledForAddition(final @NotNull FilePath localPath,
                                                     final boolean localItemExists,
                                                     final @NotNull ServerStatus serverStatus) {
                        myAddedFiles.remove(localPath.getVirtualFile());
                    }

                    public void scheduledForDeletion(final @NotNull FilePath localPath,
                                                     final boolean localItemExists,
                                                     final @NotNull ServerStatus serverStatus) {
                        // TODO (Jetbrains): add local conflict
                    }

                    public void outOfDate(final @NotNull FilePath localPath,
                                          final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) throws TfsException {
                        // TODO (Jetbrains): add local conflict
                    }

                    public void deleted(final @NotNull FilePath localPath,
                                        final boolean localItemExists,
                                        final @NotNull ServerStatus serverStatus) {
                        // ignore
                    }

                    public void upToDate(final @NotNull FilePath localPath,
                                         final boolean localItemExists,
                                         final @NotNull ServerStatus serverStatusm) throws TfsException {
                        // TODO (Jetbrains): add local conflict
                    }

                    public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
                            throws TfsException {
                        // TODO (Jetbrains): add local conflict
                    }

                    public void renamedCheckedOut(final @NotNull FilePath localPath,
                                                  final boolean localItemExists,
                                                  final @NotNull ServerStatus serverStatus) throws TfsException {
                        // TODO (Jetbrains): add local conflict
                    }

                    public void undeleted(final @NotNull FilePath localPath,
                                          final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) throws TfsException {
                        // TODO (Jetbrains): add local conflict
                    }

                }, pendingChange);
            }
        } catch (TfsException e) {
            AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e), TFSVcs.TFVC_NAME);
        }
        if (!myAddedFiles.isEmpty()) {
            super.executeAdd();
        }
    }

    protected void executeDelete() {
        logger.info("executeDelete executing...");

        // choose roots
        // revert all pending schedules for addition recursively
        // throw out all the unversioned items

        final List<String> filePaths = new ArrayList<String>();
        for (final FilePath filePath : myDeletedFiles) {
            filePaths.add(filePath.getPath());
        }
        for (final FilePath filePath : myDeletedWithoutConfirmFiles) {
            filePaths.add(filePath.getPath());
        }


        final List<PendingChange> pendingChanges = new ArrayList<PendingChange>();
        final List<VcsException> exceptions = new ArrayList<VcsException>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
                try {
                    ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                    pendingChanges.addAll(CommandUtils.getStatusForFiles(TFSVcs.getInstance(myProject).getServerContext(true), filePaths));

                    final List<String> revertFilePaths = new ArrayList<String>();
                    final List<PendingChange> revertPendingChanges = new ArrayList<PendingChange>();

                    for (final PendingChange pendingChange : pendingChanges) {
                        if (pendingChange.getChangeTypes().contains(ServerStatusType.ADD) || pendingChange.getChangeTypes().contains(ServerStatusType.UNDELETE)) {
                            // TODO (Jetbrains): assert that only Edit, Encoding can be here
                            logger.info("executeDelete: need to revert " + pendingChange.getLocalItem());
                            revertFilePaths.add(pendingChange.getLocalItem());
                            revertPendingChanges.add(pendingChange);
                            final FilePath localPath = VersionControlPath.getFilePath(pendingChange.getLocalItem(),
                                    (new File(pendingChange.getLocalItem()).isDirectory()));
                            excludeFromFurtherProcessing(localPath);
                        }
                    }

                    if (!revertFilePaths.isEmpty()) {
                        CommandUtils.undoLocalFiles(TFSVcs.getInstance(myProject).getServerContext(true), revertFilePaths);
                        pendingChanges.removeAll(revertPendingChanges);
                    }
                } catch (Exception e) {
                    logger.warn("executeDelete experienced a failure while looking for altered files to delete", e);
                    exceptions.add(TFSVcs.convertToVcsException(e));
                }
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_DELETE_SCHEDULING), false, myProject);


        if (!exceptions.isEmpty()) {
            AbstractVcsHelper.getInstance(myProject).showErrors(exceptions, TFSVcs.TFVC_NAME);
            logger.warn("Errors experienced while rolling back changes to delete a file. Aborting delete.", exceptions);
            return;
        }

        try {
            for (final PendingChange pendingChange : pendingChanges) {
                StatusProvider.visitByStatus(new StatusVisitor() {
                    public void scheduledForAddition(final @NotNull FilePath localPath,
                                                     final boolean localItemExists,
                                                     final @NotNull ServerStatus serverStatus) throws TfsException {
                        // should never get here since already reverted
                        throw new TfsException("Cannot revert an item scheduled for addition: " + localPath.getPresentableUrl());
                    }

                    public void unversioned(final @NotNull FilePath localPath,
                                            final boolean localItemExists,
                                            final @NotNull ServerStatus serverStatus) {
                        // if the file is not an unversioned delete, it doesn't need anything done it it
                        if (!pendingChange.getChangeTypes().contains(ServerStatusType.DELETE)) {
                            excludeFromFurtherProcessing(localPath);
                        }
                    }

                    public void scheduledForDeletion(final @NotNull FilePath localPath,
                                                     final boolean localItemExists,
                                                     final @NotNull ServerStatus serverStatus) {
                        // already deleted
                        excludeFromFurtherProcessing(localPath);
                    }

                    public void checkedOutForEdit(final @NotNull FilePath localPath,
                                                  final boolean localItemExists,
                                                  final @NotNull ServerStatus serverStatus) {
                        // keep for further processing
                    }

                    @Override
                    public void locked(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) throws TfsException {
                        // nothing to do
                    }

                    public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
                            throws TfsException {
                        // keep for further processing
                    }

                    public void renamedCheckedOut(final @NotNull FilePath localPath,
                                                  final boolean localItemExists,
                                                  final @NotNull ServerStatus serverStatus) throws TfsException {
                        // keep for further processing
                    }

                    public void undeleted(final @NotNull FilePath localPath,
                                          final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) throws TfsException {
                        // should never get here since already reverted
                        throw new TfsException("Cannot revert undeleted: " + localPath.getPresentableUrl());
                    }
                }, pendingChange);
            }
        } catch (TfsException e) {
            AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e), TFSVcs.TFVC_NAME);
        }

        // deletes files that were new but not added (VCS can't delete these since they don't exist to it)
        if (!myDeletedFiles.isEmpty() || !myDeletedWithoutConfirmFiles.isEmpty()) {
            super.executeDelete();
        }
    }

    protected void performDeletion(final List<FilePath> filesToDelete) {
        final List<VcsException> errors = new ArrayList<VcsException>();

        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                errors.addAll(ScheduleForDeletion.execute(myProject, filesToDelete));
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_DELETE_SCHEDULING), false, myProject);

        if (!errors.isEmpty()) {
            AbstractVcsHelper.getInstance(myProject).showErrors(errors, TFSVcs.TFVC_NAME);
        }
    }

    private void excludeFromFurtherProcessing(final FilePath localPath) {
        if (!myDeletedFiles.remove(localPath)) {
            myDeletedWithoutConfirmFiles.remove(localPath);
        }
    }

    protected void performAdding(final Collection<VirtualFile> addedFiles, final Map<VirtualFile, VirtualFile> copyFromMap) {
        final List<VcsException> errors = new ArrayList<VcsException>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                errors.addAll(TFSVcs.getInstance(myProject).getCheckinEnvironment().scheduleUnversionedFilesForAddition(new ArrayList(addedFiles)));
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_PROGRESS), false, myProject);

        if (!errors.isEmpty()) {
            AbstractVcsHelper.getInstance(myProject).showErrors(errors, TFSVcs.TFVC_NAME);
        }
    }

    protected String getDeleteTitle() {
        return "Do you want to schedule these items for deletion from TFS?";
    }

    protected String getSingleFileDeleteTitle() {
        return null;
    }

    protected String getSingleFileDeletePromptTemplate() {
        return null;
    }

    protected void performMoveRename(final List<MovedFileInfo> movedFiles) {
        final Map<FilePath, FilePath> movedPaths = new HashMap<FilePath, FilePath>(movedFiles.size());
        for (MovedFileInfo movedFileInfo : movedFiles) {
            movedPaths.put(VcsUtil.getFilePath(movedFileInfo.myOldPath), VcsUtil.getFilePath(movedFileInfo.myNewPath));
        }
        final List<VcsException> errors = new ArrayList<VcsException>();
        final Map<FilePath, FilePath> scheduleMove = new HashMap<FilePath, FilePath>();
        // TODO: finish renames
//    try {
//      WorkstationHelper.processByWorkspaces(movedPaths.keySet(), false, myProject, new WorkstationHelper.VoidProcessDelegate() {
//
//        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
//          // TODO (Jetbrains) simplify this
//          StatusProvider.visitByStatus(workspace, paths, false, null, new StatusVisitor() {
//
//            public void unversioned(final @NotNull FilePath localPath,
//                                    final boolean localItemExists,
//                                    final @NotNull ServerStatus serverStatus) throws TfsException {
//              // ignore
//            }
//
//            public void checkedOutForEdit(final @NotNull FilePath localPath,
//                                          final boolean localItemExists,
//                                          final @NotNull ServerStatus serverStatus) {
//              scheduleMove.put(localPath, movedPaths.get(localPath));
//            }
//
//            public void scheduledForAddition(final @NotNull FilePath localPath,
//                                             final boolean localItemExists,
//                                             final @NotNull ServerStatus serverStatus) {
//              scheduleMove.put(localPath, movedPaths.get(localPath));
//            }
//
//            public void scheduledForDeletion(final @NotNull FilePath localPath,
//                                             final boolean localItemExists,
//                                             final @NotNull ServerStatus serverStatus) {
//              TFSVcs.error("Cannot rename a file that does not exist on local machine: " + localPath.getPresentableUrl());
//            }
//
//            public void outOfDate(final @NotNull FilePath localPath,
//                                  final boolean localItemExists,
//                                  final @NotNull ServerStatus serverStatus) throws TfsException {
//              scheduleMove.put(localPath, movedPaths.get(localPath));
//            }
//
//            public void deleted(final @NotNull FilePath localPath,
//                                final boolean localItemExists,
//                                final @NotNull ServerStatus serverStatus) {
//              // ignore
//            }
//
//            public void upToDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
//              throws TfsException {
//              scheduleMove.put(localPath, movedPaths.get(localPath));
//            }
//
//            public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
//              throws TfsException {
//              scheduleMove.put(localPath, movedPaths.get(localPath));
//            }
//
//            public void renamedCheckedOut(final @NotNull FilePath localPath,
//                                          final boolean localItemExists,
//                                          final @NotNull ServerStatus serverStatus) throws TfsException {
//              scheduleMove.put(localPath, movedPaths.get(localPath));
//            }
//
//            public void undeleted(final @NotNull FilePath localPath,
//                                  final boolean localItemExists,
//                                  final @NotNull ServerStatus serverStatus) throws TfsException {
//              scheduleMove.put(localPath, movedPaths.get(localPath));
//            }
//          }, myProject);
//
//          final ResultWithFailures<GetOperation> renameResult =
//            workspace.getServer().getVCS()
//              .renameAndUpdateLocalVersion(workspace.getName(), workspace.getOwnerName(), scheduleMove, myProject,
//                                           TFSBundle.message("renaming"));
//          errors.addAll(TfsUtil.getVcsExceptions(renameResult.getFailures()));
//
//          Collection<FilePath> invalidate = new ArrayList<FilePath>(renameResult.getResult().size());
//          for (GetOperation getOperation : renameResult.getResult()) {
//            invalidate.add(VersionControlPath.getFilePath(getOperation.getTlocal(), getOperation.getType() == ItemType.Folder));
//            //invalidate.add(VcsUtil.getFilePath(getOperation.getSlocal()));
//          }
//          TfsFileUtil.markDirtyRecursively(myProject, invalidate);
//        }
//      });
//    }
//    catch (TfsException e) {
//      errors.add(new VcsException(e));
//    }
        if (!errors.isEmpty()) {
            AbstractVcsHelper.getInstance(myProject).showErrors(errors, TFSVcs.TFVC_NAME);
        }
    }

    protected boolean isDirectoryVersioningSupported() {
        return true;
    }
}
