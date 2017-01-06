// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileOperationsHandler;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThrowableConsumer;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Listener that intercepts file system actions and executes the appropriate TFVC command if needed
 */
public class TFSFileSystemListener implements LocalFileOperationsHandler, Disposable {
    public static final Logger logger = LoggerFactory.getLogger(TFSFileSystemListener.class);

    private final Project project;

    public TFSFileSystemListener(final Project project) {
        this.project = project;

        LocalFileSystem.getInstance().registerAuxiliaryFileOperationsHandler(this);
    }

    @Override
    public void dispose() {
        LocalFileSystem.getInstance().unregisterAuxiliaryFileOperationsHandler(this);
    }

    @Override
    public boolean delete(final VirtualFile virtualFile) throws IOException {
        return false;
    }

    @Override
    public boolean move(final VirtualFile virtualFile, final VirtualFile toDirectory) throws IOException {
        logger.info(String.format("Moving file %s to %s", virtualFile.getPath(), toDirectory.getPath()));
        return renameOrMove(virtualFile.getPath(), Path.combine(toDirectory.getPath(), virtualFile.getName()));
    }

    @Nullable
    @Override
    public File copy(final VirtualFile virtualFile, final VirtualFile virtualFile1, final String s) throws IOException {
        return null;
    }

    @Override
    public boolean rename(final VirtualFile virtualFile, final String s) throws IOException {
        logger.info(String.format("Renaming file %s to %s", virtualFile.getName(), s));
        return renameOrMove(virtualFile.getPath(), Path.combine(virtualFile.getParent().getPath(), s));
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
     * @param oldPath
     * @param newPath
     * @return
     * @throws IOException
     */
    private boolean renameOrMove(final String oldPath, final String newPath) throws IOException {
        try {
            // a single file may have 0, 1, or 2 pending changes to it
            // 0 - file has not been touched in the local workspace
            // 1 - file has versioned OR unversioned changes
            // 2 - file has versioned AND unversioned changes (rare but can happen)
            final List<PendingChange> pendingChanges = new ArrayList<PendingChange>(2);
            pendingChanges.addAll(CommandUtils.getStatusForFiles(TFSVcs.getInstance(project).getServerContext(true),
                    ImmutableList.of(oldPath)));

            // ** Rename logic **
            // If 1 change and it's an add that means it's a new unversioned file so rename thru the file system
            // Anything else can be renamed
            // Deleted files should not be at this point since IDE disables rename option for them
            if (pendingChanges.size() == 1 && pendingChanges.get(0).getChangeTypes().contains(ServerStatusType.ADD)) {
                logger.info("Renaming unversioned file thru file system");
                return false;
            } else {
                logger.info("Renaming file thru tf commandline");
                CommandUtils.renameFile(TFSVcs.getInstance(project).getServerContext(true), oldPath, newPath);
                return true;
            }
        } catch (Throwable t) {
            logger.warn("renameOrMove experienced a failure while trying to rename a file", t);
            throw new IOException(t);
        }
    }
}
