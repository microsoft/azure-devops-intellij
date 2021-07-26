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

package com.microsoft.alm.plugin.idea.tfvc.core.revision;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsRevisionNumber;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * Creates a revision object for a file so that comparisons can be done between them
 * <p/>
 * TODO: Used to implement ContenRevision until recently when it was changed to ByteBackedContentRevision
 * TODO: That class does not exist in IntelliJ 14 or 15 so reverting that change back to ContentRevision for now
 */
public abstract class TFSContentRevision implements ContentRevision {

    private final Project project;

    @Nullable
    private byte[] myContent;

    protected TFSContentRevision(final Project project) {
        this.project = project;
    }

    public abstract int getChangeset();

    protected abstract String getFilePath();

    public static TFSContentRevision create(final Project project,
                                            final @NotNull FilePath localPath,
                                            final int changeset,
                                            final String modificationDate) {
        return new TFSContentRevision(project) {

            public int getChangeset() {
                return changeset;
            }

            @NotNull
            public FilePath getFile() {
                return localPath;
            }

            protected String getFilePath() {
                return localPath.getPath();
            }

            public String getModificationDate() {
                return modificationDate;
            }

            @NotNull
            public VcsRevisionNumber getRevisionNumber() {
                return new TfsRevisionNumber(changeset, localPath.getName(), modificationDate);
            }
        };
    }

    /**
     * Creates a revision especially for a renamed file since the original path is needed to display where the file
     * used to reside while the server path is needed to pull down that version of the file from the server for diffs
     *
     * @param project
     * @param orignalPath:     path of the file before it was renamed
     * @param changeset
     * @param modificationDate
     * @param serverPath:      path of the file on the server
     * @return
     */
    public static TFSContentRevision createRenameRevision(final Project project,
                                                          final @NotNull FilePath orignalPath,
                                                          final int changeset,
                                                          final String modificationDate,
                                                          final String serverPath) {
        return new TFSContentRevision(project) {

            public int getChangeset() {
                return changeset;
            }

            @NotNull
            public FilePath getFile() {
                return orignalPath;
            }

            protected String getFilePath() {
                return serverPath;
            }

            public String getModificationDate() {
                return modificationDate;
            }

            @NotNull
            public VcsRevisionNumber getRevisionNumber() {
                return new TfsRevisionNumber(changeset, orignalPath.getName(), modificationDate);
            }
        };
    }

    @Nullable
    public String getContent() throws VcsException {
        FilePath filePath = getFile();
        try {
            // Download the file if required:
            TFSContentStoreFactory.findOrCreate(filePath.getPath(), getChangeset(), getFilePath(), project);
        } catch (IOException e) {
            throw new VcsException(e);
        }

        VirtualFile virtualFile = Objects.requireNonNull(filePath.getVirtualFile());
        try {
            return VfsUtil.loadText(virtualFile);
        } catch (IOException e) {
            throw new VcsException(e);
        }
    }

    @Nullable
    public byte[] doGetContent() throws VcsException {
        if (myContent == null) {
            try {
                myContent = loadContent();
            } catch (TfsException e) {
                throw new VcsException(e);
            } catch (IOException e) {
                throw new VcsException(e);
            }
        }
        return myContent;
    }

    @Nullable
    private byte[] loadContent() throws TfsException, IOException {
        ArgumentHelper.checkNotNull(getFile(), "localPath");
        final TFSContentStore store = TFSContentStoreFactory.findOrCreate(getFile().getPath(), getChangeset(), getFilePath(), project);
        return store.loadContent();
    }

    @NonNls
    public String toString() {
        return "TFSContentRevision [file=" + getFile() + ", revision=" + ((TfsRevisionNumber) getRevisionNumber()).getValue() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TFSContentRevision that = (TFSContentRevision) o;
        return Objects.equals(getFile(), that.getFile())
                && Objects.equals(project, that.project)
                && getChangeset() == that.getChangeset()
                && Objects.equals(getFilePath(), that.getFilePath())
                && Objects.equals(getRevisionNumber(), that.getRevisionNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFile(), project, getChangeset(), getFilePath(), getRevisionNumber());
    }
}
