// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.vcs.FilePath;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Date;

public abstract class ServerStatus {
    public final int localVer;
    public final boolean isDirectory;
    @Nullable
    public final String sourceItem;
    @Nullable
    public final String targetItem;
    public final String modicationDate;

    /**
     * Types of statuses that are found on the server and how to process them
     *
     * @param localVer
     * @param isDirectory
     * @param sourceItem
     * @param targetItem
     */
    protected ServerStatus(final int localVer,
                           final boolean isDirectory,
                           final String sourceItem,
                           final String targetItem,
                           final String modicationDate) {
        this.localVer = localVer;
        this.isDirectory = isDirectory;
        this.sourceItem = sourceItem;
        this.targetItem = targetItem;
        this.modicationDate = modicationDate;
    }

    protected ServerStatus(final @NotNull PendingChange pendingChange) {
        this(Integer.parseInt(pendingChange.getVersion()), new File(pendingChange.getLocalItem()).isDirectory(), pendingChange.getServerItem(), pendingChange.getLocalItem(), pendingChange.getDate());
    }

    public abstract void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
            throws TfsException;

    public String toString() {
        return getClass().getName().substring(getClass().getName().lastIndexOf("$") + 1);
    }


    public static class CheckedOutForEdit extends ServerStatus {
        protected CheckedOutForEdit(final @NotNull PendingChange pendingChange) {
            super(pendingChange);
        }

        public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
                throws TfsException {
            statusVisitor.checkedOutForEdit(localPath, localItemExists, this);
        }
    }

    public static class ScheduledForAddition extends ServerStatus {
        protected ScheduledForAddition(final @NotNull PendingChange pendingChange) {
            super(pendingChange);
        }

        public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
                throws TfsException {
            statusVisitor.scheduledForAddition(localPath, localItemExists, this);
        }
    }

    public static class ScheduledForDeletion extends ServerStatus {
        protected ScheduledForDeletion(final @NotNull PendingChange pendingChange) {
            super(pendingChange);
        }

        public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
                throws TfsException {
            statusVisitor.scheduledForDeletion(localPath, localItemExists, this);
        }
    }

    public static class RenamedCheckedOut extends ServerStatus {
        protected RenamedCheckedOut(final @NotNull PendingChange pendingChange) {
            super(pendingChange);
        }

        public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
                throws TfsException {
            statusVisitor.renamedCheckedOut(localPath, localItemExists, this);
        }
    }

    public static class Renamed extends ServerStatus {
        protected Renamed(final @NotNull PendingChange pendingChange) {
            super(pendingChange);
        }

        public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
                throws TfsException {
            statusVisitor.renamed(localPath, localItemExists, this);
        }
    }

    public static class Unversioned extends ServerStatus {

        public static final ServerStatus INSTANCE = new Unversioned();

        private Unversioned() {
            super(0, false, null, null, new Date().toString()); // use now date for unversioned since it doesn't matter there are no previous versions
        }

        public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
                throws TfsException {
            statusVisitor.unversioned(localPath, localItemExists, this);
        }
    }

    public static class Undeleted extends ServerStatus {
        protected Undeleted(final @NotNull PendingChange pendingChange) {
            super(pendingChange);
        }

        public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
                throws TfsException {
            statusVisitor.undeleted(localPath, localItemExists, this);
        }
    }

    /* TODO:
    public static class OutOfDate extends ServerStatus {
        protected OutOfDate(final @NotNull ExtendedItem extendedItem) {
            super(extendedItem);
        }

        public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
                throws TfsException {
            statusVisitor.outOfDate(localPath, localItemExists, this);
        }
    }

    public static class UpToDate extends ServerStatus {
        protected UpToDate(final @NotNull ExtendedItem extendedItem) {
            super(extendedItem);
        }

        public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
                throws TfsException {
            statusVisitor.upToDate(localPath, localItemExists, this);
        }
    }
    */
}
