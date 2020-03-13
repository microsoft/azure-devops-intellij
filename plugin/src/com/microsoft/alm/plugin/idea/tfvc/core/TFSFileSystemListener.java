// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vfs.LocalFileOperationsHandler;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThrowableConsumer;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.ServerStatus;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusProvider;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusVisitor;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TFVCUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.tfs.model.connector.TfsPath;
import com.microsoft.tfs.model.connector.TfsServerPath;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listener that intercepts file system actions and executes the appropriate TFVC command if needed
 */
public class TFSFileSystemListener implements LocalFileOperationsHandler, Disposable {
    private static final Logger ourLogger = LoggerFactory.getLogger(TFSFileSystemListener.class);

    @NotNull
    private final Project myProject;

    public TFSFileSystemListener(@NotNull Project project) {
        myProject = project;
        LocalFileSystem.getInstance().registerAuxiliaryFileOperationsHandler(this);
    }

    @Override
    public void dispose() {
        LocalFileSystem.getInstance().unregisterAuxiliaryFileOperationsHandler(this);
    }

    @Override
    public boolean delete(@NotNull final VirtualFile virtualFile) {
        long startTime = System.nanoTime();
        ourLogger.trace("Delete command started for file " + virtualFile);

        final TFSVcs vcs = VcsHelper.getTFSVcsByPath(virtualFile);
        // no TFSVcs so not a TFVC project so do nothing
        if (vcs == null) {
            ourLogger.info("Not a TFVC project so not doing a TFVC delete");
            return false;
        }

        if (TFVCUtil.isInvalidTFVCPath(vcs, new LocalFilePath(virtualFile.getPath(), virtualFile.isDirectory()))) {
            ourLogger.warn("Invalid file name for TFVC, so not performing TFVC delete: {}", virtualFile.getPath());
            return false;
        }

        // do nothing with TFVC if the user chooses not to
        final VcsShowConfirmationOption.Value value = vcs.getDeleteConfirmation().getValue();
        if (VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY.equals(value)) {
            ourLogger.info("Don't delete file from TFVC: " + virtualFile.getPath());
            return false;
        }

        ourLogger.info("Deleting file with TFVC: " + virtualFile.getPath());
        final Project currentProject = vcs.getProject();
        TfvcClient tfvcClient = TfvcClient.getInstance(currentProject);
        ServerContext serverContext = vcs.getServerContext(true);

        List<PendingChange> pendingChanges = tfvcClient.getStatusForFiles(
                serverContext,
                Collections.singletonList(virtualFile.getPath()));

        // if 0 pending changes then just delete the file and return
        if (pendingChanges.isEmpty()) {
            ourLogger.info("No changes to file so deleting though TFVC");
            tfvcClient.deleteFilesRecursively(
                    serverContext,
                    Collections.singletonList(TfsFileUtil.createLocalPath(virtualFile)));

            long time = System.nanoTime() - startTime;
            ourLogger.trace("Delete command finished in " + time / 1_000_000_000.0 + "s");

            return true;
        }

        // start with assuming you don't need to revert but look at the pending changes to see if that's incorrect
        final AtomicBoolean revert = new AtomicBoolean(false);
        // assume false until we know we need to delete from TFVC
        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicBoolean isUndelete = new AtomicBoolean(false);
        for (final PendingChange pendingChange : pendingChanges) {
            StatusProvider.visitByStatus(new StatusVisitor() {
                public void scheduledForAddition(final @NotNull FilePath localPath,
                                                 final boolean localItemExists,
                                                 final @NotNull ServerStatus serverStatus) {
                    // revert the file and then let the IDE delete it
                    revert.set(true);
                    success.set(false);
                }

                public void unversioned(final @NotNull FilePath localPath,
                                        final boolean localItemExists,
                                        final @NotNull ServerStatus serverStatus) {
                    // only do something if it's an unversioned delete, the IDE will take care of it otherwise
                    if (pendingChange.getChangeTypes().contains(ServerStatusType.DELETE)) {
                        revert.set(true);
                        success.set(true);
                    }
                }

                public void scheduledForDeletion(final @NotNull FilePath localPath,
                                                 final boolean localItemExists,
                                                 final @NotNull ServerStatus serverStatus) {
                    // already deleted on server so let IDE take care of it
                    success.set(false);
                }

                public void checkedOutForEdit(final @NotNull FilePath localPath,
                                              final boolean localItemExists,
                                              final @NotNull ServerStatus serverStatus) {
                    // revert it and then delete it
                    revert.set(true);
                    success.set(true);
                }

                @Override
                public void locked(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) {
                    // nothing to do if it's locked
                    success.set(false);
                }

                public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
                    // revert it and then delete it
                    revert.set(true);
                    success.set(true);
                }

                public void renamedCheckedOut(final @NotNull FilePath localPath,
                                              final boolean localItemExists,
                                              final @NotNull ServerStatus serverStatus) {
                    // revert it and then delete it
                    revert.set(true);
                    success.set(true);
                }

                public void undeleted(final @NotNull FilePath localPath,
                                      final boolean localItemExists,
                                      final @NotNull ServerStatus serverStatus) {
                    // revert it and it will be deleted
                    revert.set(true);
                    isUndelete.set(true);
                }
            }, pendingChange);
        }

        if (revert.get()) {
            TfsPath filePath = TfsFileUtil.createLocalPath(virtualFile);
            tfvcClient.undoLocalChanges(serverContext, Collections.singletonList(filePath));
        }

        if (success.get() && !isUndelete.get()) {
            ourLogger.info("Deleting file with TFVC after undoing pending changes");
            // PendingChanges will always have at least 1 element or else we wouldn't have gotten this far
            PendingChange pendingChange = pendingChanges.get(0);
            TfsPath itemToDelete = StringUtils.isNotEmpty(pendingChange.getSourceItem())
                    ? new TfsServerPath(pendingChange.getWorkspace(), pendingChange.getSourceItem())
                    : TfsFileUtil.createLocalPath(pendingChange.getLocalItem());
            tfvcClient.deleteFilesRecursively(serverContext, Collections.singletonList(itemToDelete));
        }
        ourLogger.info("File was deleted using TFVC: " + success.get());

        long time = System.nanoTime() - startTime;
        ourLogger.trace("Delete command finished in " + time / 1_000_000_000.0 + "s");

        return success.get();
    }

    @Override
    public boolean move(final VirtualFile virtualFile, final VirtualFile toDirectory) throws IOException {
        ourLogger.info(String.format("Moving file %s to %s", virtualFile.getPath(), toDirectory.getPath()));
        return renameOrMove(virtualFile, Path.combine(toDirectory.getPath(), virtualFile.getName()));
    }

    @Nullable
    @Override
    public File copy(final VirtualFile virtualFile, final VirtualFile virtualFile1, final String s) throws IOException {
        return null;
    }

    @Override
    public boolean rename(final VirtualFile virtualFile, final String s) throws IOException {
        ourLogger.info(String.format("Renaming file %s to %s", virtualFile.getName(), s));
        return renameOrMove(virtualFile, Path.combine(virtualFile.getParent().getPath(), s));
    }

    @Override
    public boolean createFile(final VirtualFile virtualFile, final String s) throws IOException {
        return false;
    }

    @Override
    public boolean createDirectory(final VirtualFile virtualFile, final String s) throws IOException {
        return false;
    }

    @Override
    public void afterDone(final ThrowableConsumer<LocalFileOperationsHandler, IOException> throwableConsumer) {
        // nothing to do
    }

    /**
     * Move and rename logic the same
     *
     * @param oldFile
     * @param newPath
     * @return
     * @throws IOException
     */
    private boolean renameOrMove(final VirtualFile oldFile, final String newPath) throws IOException {
        final TFSVcs vcs = VcsHelper.getTFSVcsByPath(oldFile);
        // no TFSVcs so not a TFVC project so do nothing
        if (vcs == null) {
            ourLogger.info("Not a TFVC project so not doing a TFVC rename/move");
            return false;
        }

        boolean isDirectory = oldFile.isDirectory();
        LocalFilePath oldFilePath = new LocalFilePath(oldFile.getPath(), isDirectory);
        if (TFVCUtil.isInvalidTFVCPath(vcs, oldFilePath)) {
            ourLogger.warn("Invalid old TFVC path, ignore rename or move: {}", oldFilePath);
            return false;
        }

        LocalFilePath newFilePath = new LocalFilePath(newPath, isDirectory);
        if (TFVCUtil.isInvalidTFVCPath(vcs, newFilePath)) {
            ourLogger.warn("Invalid new TFVC path, ignore rename or move: {}", newFilePath);
            return false;
        }

        final String oldPath = oldFile.getPath();
        try {
            // a single file may have 0, 1, or 2 pending changes to it
            // 0 - file has not been touched in the local workspace
            // 1 - file has versioned OR unversioned changes
            // 2 - file has versioned AND unversioned changes (rare but can happen)
            final List<PendingChange> pendingChanges = new ArrayList<>(2);
            pendingChanges.addAll(
                    CommandUtils.getStatusForFiles(
                            myProject,
                            vcs.getServerContext(true),
                            ImmutableList.of(oldPath)));

            // ** Rename logic **
            // If 1 change and it's a candidate add that means it's a new unversioned file so rename thru the file system
            // Anything else can be renamed
            // Deleted files should not be at this point since IDE disables rename option for them
            if (pendingChanges.size() == 1 && pendingChanges.get(0).isCandidate() && pendingChanges.get(0).getChangeTypes().contains(ServerStatusType.ADD)) {
                ourLogger.info("Renaming unversioned file thru file system");
                return false;
            } else {
                ourLogger.info("Renaming file thru tf commandline");
                CommandUtils.renameFile(vcs.getServerContext(true), oldPath, newPath);
                return true;
            }
        } catch (Throwable t) {
            ourLogger.warn("renameOrMove experienced a failure while trying to rename a file", t);
            throw new IOException(t);
        }
    }
}
