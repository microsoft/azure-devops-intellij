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
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.revision.TFSContentRevision;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.ServerStatus;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusProvider;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds changes to change log as the correct status
 */
class ChangelistBuilderStatusVisitor extends StatusProvider.StatusAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ChangelistBuilderStatusVisitor.class);

    @NotNull
    private final Project project;
    @NotNull
    private final ChangelistBuilder changelistBuilder;

    public ChangelistBuilderStatusVisitor(final @NotNull Project project,
                                          final @NotNull ChangelistBuilder changelistBuilder) {
        this.project = project;
        this.changelistBuilder = changelistBuilder;
    }

    public void checkedOutForEdit(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
        if (localItemExists) {
            TFSContentRevision baseRevision =
                    TFSContentRevision.create(project, localPath, serverStatus.localVer, serverStatus.modicationDate);
            changelistBuilder.processChange(new Change(baseRevision, CurrentContentRevision.create(localPath)), TFSVcs.getKey());
        } else {
            changelistBuilder.processLocallyDeletedFile(localPath);
        }
    }

    public void scheduledForAddition(final @NotNull FilePath localPath,
                                     final boolean localItemExists,
                                     final @NotNull ServerStatus serverStatus) {
        if (localItemExists) {
            changelistBuilder.processChange(new Change(null, new CurrentContentRevision(localPath)), TFSVcs.getKey());
        } else {
            changelistBuilder.processLocallyDeletedFile(localPath);
        }
    }

    public void scheduledForDeletion(final @NotNull FilePath localPath,
                                     final boolean localItemExists,
                                     final @NotNull ServerStatus serverStatus) {
        TFSContentRevision baseRevision =
                TFSContentRevision.create(project, localPath, serverStatus.localVer, serverStatus.modicationDate);
        changelistBuilder.processChange(new Change(baseRevision, null), TFSVcs.getKey());
    }

    public void renamedCheckedOut(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
        if (localItemExists) {
            final ContentRevision before = getPreviousRenamedRevision(localPath, serverStatus.localVer);
            final ContentRevision after = CurrentContentRevision.create(localPath);
            changelistBuilder.processChange(new Change(before, after), TFSVcs.getKey());
        } else {
            changelistBuilder.processLocallyDeletedFile(localPath);
        }
    }

    public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
        if (localItemExists) {
            final ContentRevision before = getPreviousRenamedRevision(localPath, serverStatus.localVer);
            final ContentRevision after = CurrentContentRevision.create(localPath);
            changelistBuilder.processChange(new Change(before, after), TFSVcs.getKey());
        } else {
            changelistBuilder.processLocallyDeletedFile(localPath);
        }
    }

    public void unversioned(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
        if (localItemExists) {
            final VirtualFile filePath = localPath.getVirtualFile();
            if (filePath == null) {
                // for files that were deleted w/o using the VCS
                changelistBuilder.processLocallyDeletedFile(localPath);
            } else {
                changelistBuilder.processUnversionedFile(filePath);
            }
        }
    }

    public void undeleted(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
        checkedOutForEdit(localPath, localItemExists, serverStatus);
    }

    /**
     * Create the previous revision of a file that has been renamed
     *
     * @param localPath
     * @param revision
     * @return
     */
    private TFSContentRevision getPreviousRenamedRevision(final FilePath localPath, final int revision) {
        // find the original name of file by getting the most recent history entry
        final ServerContext serverContext = TFSVcs.getInstance(project).getServerContext(false);
        final ChangeSet lastChangeSet = CommandUtils.getLastHistoryEntryForAnyUser(serverContext, localPath.getPath());

        // check that the history command returned an entry with a change
        if (lastChangeSet != null && !lastChangeSet.getChanges().isEmpty()) {
            final String serverPath = lastChangeSet.getChanges().get(0).getServerItem();
            final String originalPath = CommandUtils.getLocalPathSynchronously(serverContext, serverPath,
                    TfvcWorkspaceLocator.getWorkspaceName(project));

            return TFSContentRevision.createRenameRevision(project,
                    VersionControlPath.getFilePath(originalPath, localPath.isDirectory()),
                    revision,
                    lastChangeSet.getDate(),
                    serverPath);
        }
        return null;
    }
}
