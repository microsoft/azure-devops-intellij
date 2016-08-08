// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.microsoft.alm.plugin.idea.tfvc.core.revision.TFSContentRevision;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.ServerStatus;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusVisitor;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NotNull;

/**
 * Adds changes to change log as the correct status
 */
class ChangelistBuilderStatusVisitor implements StatusVisitor {
    @NotNull private final Project myProject;
    @NotNull private final ChangelistBuilder myChangelistBuilder;

    public ChangelistBuilderStatusVisitor(final @NotNull Project project,
                                          final @NotNull ChangelistBuilder changelistBuilder) {
        myProject = project;
        myChangelistBuilder = changelistBuilder;
    }

    public void checkedOutForEdit(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException {
        if (localItemExists) {
            TFSContentRevision baseRevision =
                    TFSContentRevision.create(myProject, localPath, serverStatus.localVer);
            myChangelistBuilder.processChange(new Change(baseRevision, CurrentContentRevision.create(localPath)), TFSVcs.getKey());
        } else {
            myChangelistBuilder.processLocallyDeletedFile(localPath);
        }
    }

    public void scheduledForAddition(final @NotNull FilePath localPath,
                                     final boolean localItemExists,
                                     final @NotNull ServerStatus serverStatus) {
        if (localItemExists) {
            myChangelistBuilder.processChange(new Change(null, new CurrentContentRevision(localPath)), TFSVcs.getKey());
        } else {
            myChangelistBuilder.processLocallyDeletedFile(localPath);
        }
    }

    public void scheduledForDeletion(final @NotNull FilePath localPath,
                                     final boolean localItemExists,
                                     final @NotNull ServerStatus serverStatus) {
        TFSContentRevision baseRevision =
                TFSContentRevision.create(myProject, localPath, serverStatus.localVer);
        myChangelistBuilder.processChange(new Change(baseRevision, null), TFSVcs.getKey());
    }

    /* TODO:
    public void unversioned(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
        if (localItemExists) {
            myChangelistBuilder.processUnversionedFile(localPath.getVirtualFile());
        }
    }

    public void outOfDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatusm) {
        upToDate(localPath, localItemExists, serverStatusm);
    }

    public void deleted(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
        if (localItemExists) {
            myChangelistBuilder.processUnversionedFile(localPath.getVirtualFile());
        }
    }

    public void upToDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
        if (localItemExists) {
            //  if (!myWorkspace.isLocal() && TfsFileUtil.isFileWritable(localPath)) {
            myChangelistBuilder.processModifiedWithoutCheckout(localPath.getVirtualFile());
            //     }
        } else {
            myChangelistBuilder.processLocallyDeletedFile(localPath);
        }
    }

    public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException {
        if (localItemExists) {
            sourceItem can 't be null for renamed
            noinspection ConstantConditions
            FilePath beforePath = myWorkspace.findLocalPathByServerPath(serverStatus.sourceItem, serverStatus.isDirectory, myProject);

            noinspection ConstantConditions
            TFSContentRevision before = TFSContentRevision.create(myProject, myWorkspace, beforePath, serverStatus.localVer, serverStatus.itemId);
            ContentRevision after = CurrentContentRevision.create(localPath);
            myChangelistBuilder.processChange(new Change(before, after), TFSVcs.getKey());
        } else {
            myChangelistBuilder.processLocallyDeletedFile(localPath);
        }
    }

    public void renamedCheckedOut(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException {
        if (localItemExists) {
            // sourceItem can't be null for renamed and checked out for edit
            //noinspection ConstantConditions
            FilePath beforePath = myWorkspace.findLocalPathByServerPath(serverStatus.sourceItem, serverStatus.isDirectory, myProject);

            //noinspection ConstantConditions
            TFSContentRevision before = TFSContentRevision.create(myProject, myWorkspace, beforePath, serverStatus.localVer, serverStatus.itemId);
            ContentRevision after = CurrentContentRevision.create(localPath);
            myChangelistBuilder.processChange(new Change(before, after), TFSVcs.getKey());
        } else {
            myChangelistBuilder.processLocallyDeletedFile(localPath);
        }
    }

    public void undeleted(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException {
        checkedOutForEdit(localPath, localItemExists, serverStatus);
    }
    */
}
