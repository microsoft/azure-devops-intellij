// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.vcs.FilePath;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NotNull;

public interface StatusVisitor {

    void checkedOutForEdit(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void locked(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void scheduledForAddition(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void scheduledForDeletion(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus);

    void renamedCheckedOut(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void unversioned(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void undeleted(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    /* TODO:
    void outOfDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;

    void upToDate(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus)
            throws TfsException;
    */
}
