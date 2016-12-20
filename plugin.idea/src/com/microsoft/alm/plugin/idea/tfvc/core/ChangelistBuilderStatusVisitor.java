// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

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
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusVisitor;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds changes to change log as the correct status
 */
class ChangelistBuilderStatusVisitor implements StatusVisitor {
    private static final Logger logger = LoggerFactory.getLogger(ChangelistBuilderStatusVisitor.class);

    @NotNull
    private final Project project;
    @NotNull
    private final ChangelistBuilder changelistBuilder;
    private final ServerContext serverContext;

    public ChangelistBuilderStatusVisitor(final @NotNull Project project,
                                          final ServerContext serverContext,
                                          final @NotNull ChangelistBuilder changelistBuilder) {
        this.project = project;
        this.changelistBuilder = changelistBuilder;
        this.serverContext = serverContext;
    }

    public void checkedOutForEdit(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException {
        if (localItemExists) {
            TFSContentRevision baseRevision =
                    TFSContentRevision.create(project, serverContext, localPath, serverStatus.localVer, serverStatus.modicationDate);
            changelistBuilder.processChange(new Change(baseRevision, CurrentContentRevision.create(localPath)), TFSVcs.getKey());
        } else {
            changelistBuilder.processLocallyDeletedFile(localPath);
        }
    }

    @Override
    public void locked(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) throws TfsException {
        // Nothing to do
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
                TFSContentRevision.create(project, serverContext, localPath, serverStatus.localVer, serverStatus.modicationDate);
        changelistBuilder.processChange(new Change(baseRevision, null), TFSVcs.getKey());
    }

    public void renamedCheckedOut(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException {
        if (localItemExists) {
            final ContentRevision before = getPreviousRenamedRevision(localPath, serverStatus.localVer);
            final ContentRevision after = CurrentContentRevision.create(localPath);
            changelistBuilder.processChange(new Change(before, after), TFSVcs.getKey());
        } else {
            changelistBuilder.processLocallyDeletedFile(localPath);
        }
    }

    public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException {
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

    public void undeleted(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException {
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
        final ChangeSet lastChangeSet = CommandUtils.getLastHistoryEntryForAnyUser(serverContext, localPath.getPath());

        // check that the history command returned an entry with a change
        if (lastChangeSet != null && !lastChangeSet.getChanges().isEmpty()) {
            final String serverPath = lastChangeSet.getChanges().get(0).getServerItem();
            final String originalPath = CommandUtils.getLocalPathSynchronously(null, serverPath,
                    CommandUtils.getWorkspaceName(null, project));

            return TFSContentRevision.createRenameRevision(project,
                    serverContext,
                    VersionControlPath.getFilePath(originalPath, localPath.isDirectory()),
                    revision,
                    lastChangeSet.getDate(),
                    serverPath);
        }
        return null;
    }

    /* TODO:

    public void outOfDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatusm) {
        upToDate(localPath, localItemExists, serverStatusm);
    }

    public void upToDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
        if (localItemExists) {
            //  if (!myWorkspace.isLocal() && TfsFileUtil.isFileWritable(localPath)) {
            changelistBuilder.processModifiedWithoutCheckout(localPath.getVirtualFile());
            //     }
        } else {
            changelistBuilder.processLocallyDeletedFile(localPath);
        }
    }
    */
}
