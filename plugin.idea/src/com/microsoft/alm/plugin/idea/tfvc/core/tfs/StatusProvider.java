// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

// Note: if item is renamed (moved), same local item and pending change reported by server for source and target names (Jetbrains)

/**
 * Determines a file's state and provides it to the local changes menu
 */
public class StatusProvider {
    public static final Logger logger = LoggerFactory.getLogger(StatusProvider.class);

    /**
     * Finds the status of the file and then uses that status to determine how to add it to the local changes list
     *
     * @param statusVisitor
     * @param pendingChange
     * @throws TfsException
     */
    public static void visitByStatus(final @NotNull StatusVisitor statusVisitor,
                                     PendingChange pendingChange) throws TfsException {
        determineServerStatus(pendingChange).visitBy(
                VersionControlPath.getFilePath(pendingChange.getLocalItem(), (new File(pendingChange.getLocalItem()).isDirectory())),
                true, //TODO: I made everything a local item for now
                statusVisitor);
    }

    /*
    TODO:
    private static void addExistingFilesRecursively(final @NotNull Collection<FilePath> result, final @Nullable VirtualFile root) {
            if (root != null && root.exists()) {
                VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor() {
                    @Override
                    public boolean visitFile(@NotNull VirtualFile file) {
                        result.add(TfsFileUtil.getFilePath(file));
                        return true;
                    }
                });
            }
        }
     */

    /**
     * Determine which state the change is in so we know how to handle it
     *
     * @param pendingChange
     * @return ServerState
     */
    private static ServerStatus determineServerStatus(final @Nullable PendingChange pendingChange) {
        if (pendingChange.isCandidate()) {
            return ServerStatus.Unversioned.INSTANCE;
        }

        switch (pendingChange.getChangeType()) {
            case ADD:
                return new ServerStatus.ScheduledForAddition(pendingChange);
            case DELETE:
                return new ServerStatus.ScheduledForDeletion(pendingChange);
            case EDIT:
                return new ServerStatus.CheckedOutForEdit(pendingChange);
            case RENAME:
                return new ServerStatus.Renamed(pendingChange);
            case UNDELETE:
                return new ServerStatus.Undeleted(pendingChange);
            case RENAME_EDIT:
                return new ServerStatus.RenamedCheckedOut(pendingChange);
            default:
                logger.error("Unhandled status type: " + pendingChange.getChangeType().name());
                return null;
        }

        // TODO: other scenarios that need to be considered that Jetbrains had
        // ServerStatus.OutOfDate(pendingChange);
        // ServerStatus.UpToDate(pendingChange);
    }
}
