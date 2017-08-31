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

    public static class Locked extends ServerStatus {
        protected Locked(final @NotNull PendingChange pendingChange) {
            super(pendingChange);
        }

        public void visitBy(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull StatusVisitor statusVisitor)
                throws TfsException {
            statusVisitor.locked(localPath, localItemExists, this);
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
}
