// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
        logger.info("Initializing TFSRollbackEnvironment");
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

        undoPendingChanges(localPaths, vcsExceptions, listener);
        logger.info("rollbackChanges ended");
    }

    public void rollbackMissingFileDeletion(final List<FilePath> files,
                                            final List<VcsException> errors,
                                            final RollbackProgressListener listener) {
        logger.info("rollbackMissingFileDeletion started");
        for (final FilePath file : files) {
            try {
                // get the file from the server so it's restored locally
                CommandUtils.forceGetFile(vcs.getServerContext(false), file.getPath());
            } catch (final Throwable t) {
                logger.warn("Exception hit while rolling back deleted file: " + file.getPath(), t);
                errors.add(new VcsException(t.getMessage(), t));
            }
        }
        logger.info("rollbackMissingFileDeletion ended");
    }

    public void rollbackModifiedWithoutCheckout(final List<VirtualFile> files,
                                                final List<VcsException> errors,
                                                final RollbackProgressListener listener) {
//TODO: implement once we have server workspace where you can checkout files for edit
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
        // This is optional to implement and not supported by TFVC.
        // This is called when the user performs an "undo" that returns a file to a
        // state in which it was checked out or last saved.
    }

    private void undoPendingChanges(final List<FilePath> localPaths,
                                    final List<VcsException> errors,
                                    @NotNull final RollbackProgressListener listener) {
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
