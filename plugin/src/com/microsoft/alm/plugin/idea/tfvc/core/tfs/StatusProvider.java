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

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.vcs.FilePath;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// Note: if item is renamed (moved), same local item and pending change reported by server for source and target names (JetBrains)

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
     */
    public static void visitByStatus(final @NotNull StatusVisitor statusVisitor,
                                     PendingChange pendingChange) {
        String localItem = pendingChange.getLocalItem();
        File localFile = new File(localItem);
        ServerStatus status = determineServerStatus(pendingChange);
        if (status != null) {
            status.visitBy(
                    Objects.requireNonNull(VersionControlPath.getFilePath(localItem, localFile.isDirectory())),
                    localFile.exists(),
                    statusVisitor);
        }
    }

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
        } else if (types.contains(ServerStatusType.BRANCH)) {
            // treat branch like an addition
            return new ServerStatus.ScheduledForAddition(pendingChange);
        } else if (types.contains(ServerStatusType.MERGE)) {
            // treat merge like an edit unless the file is gone, then it must have been a delete
            if (Path.fileExists(pendingChange.getLocalItem())) {
                return new ServerStatus.CheckedOutForEdit(pendingChange);
            } else {
                return new ServerStatus.ScheduledForDeletion(pendingChange);
            }
        } else if (types.contains(ServerStatusType.LOCK)) {
            return new ServerStatus.Locked(pendingChange);
        } else {
            logger.error("Unhandled status type: " + Arrays.toString(pendingChange.getChangeTypes().toArray()));
            return null;
        }
    }

    /**
     * An empty {@link StatusVisitor} implementation useful for cases when only some of the overrides are required.
     */
    public static class StatusAdapter implements StatusVisitor {
        @Override
        public void checkedOutForEdit(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) {
        }

        @Override
        public void locked(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) {
        }

        @Override
        public void scheduledForAddition(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) {
        }

        @Override
        public void scheduledForDeletion(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) {
        }

        @Override
        public void renamedCheckedOut(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) {
        }

        @Override
        public void unversioned(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) {
        }

        @Override
        public void renamed(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) {
        }

        @Override
        public void undeleted(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) {
        }
    }
}
