// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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

        final List<ServerStatusType> types = pendingChange.getChangeTypes();

        // check the list of statuses for how to process the pending change
        if (types.contains(ServerStatusType.ADD)) {
            return new ServerStatus.ScheduledForAddition(pendingChange);
        } else if (types.contains(ServerStatusType.EDIT) && types.contains(ServerStatusType.RENAME)) {
            return new ServerStatus.RenamedCheckedOut(pendingChange);
        } else if (types.contains(ServerStatusType.EDIT)) {
            return new ServerStatus.CheckedOutForEdit(pendingChange);
        } else if (types.contains(ServerStatusType.RENAME)) {
            return new ServerStatus.Renamed(pendingChange);
        } else if (types.contains(ServerStatusType.DELETE)) {
            return new ServerStatus.ScheduledForDeletion(pendingChange);
        } else if (types.contains(ServerStatusType.UNDELETE)) {
            return new ServerStatus.Undeleted(pendingChange);
        } else {
            logger.error("Unhandled status type: " + Arrays.toString(pendingChange.getChangeTypes().toArray()));
            return null;
        }

        // TODO: other scenarios that need to be considered that Jetbrains had
        // ServerStatus.OutOfDate(pendingChange);
        // ServerStatus.UpToDate(pendingChange);
    }
}
