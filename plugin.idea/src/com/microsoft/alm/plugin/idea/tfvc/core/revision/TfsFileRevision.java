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
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.microsoft.alm.common.utils.ArgumentHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TfsFileRevision implements VcsFileRevision {
    public static final Logger logger = LoggerFactory.getLogger(TfsFileRevision.class);

    private final Project project;
    private final FilePath localPath;
    private final int changeset;
    private final String author;
    private final String commitMessage;
    private final String modificationDate;
    private byte[] content;

    public TfsFileRevision(final Project project,
                           final @NotNull FilePath localPath,
                           final int changeset,
                           final String author,
                           final String commitMessage,
                           final String modificationDate) {
        ArgumentHelper.checkNotNull(localPath, "localPath");

        this.project = project;
        this.localPath = localPath;
        this.changeset = changeset;
        this.author = author;
        this.commitMessage = commitMessage;
        this.modificationDate = modificationDate;
    }

    @Nullable
    @Override
    public String getBranchName() {
        return null;
    }

    @Nullable
    @Override
    public RepositoryLocation getChangedRepositoryPath() {
        return null;
    }

    @Override
    public byte[] loadContent() throws IOException, VcsException {
        return content = createContentRevision().doGetContent();
    }

    @Nullable
    @Override
    public byte[] getContent() throws IOException, VcsException {
        return content;
    }

    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return new VcsRevisionNumber.Int(changeset);
    }

    @Override
    public Date getRevisionDate() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH).parse(modificationDate);
        } catch (ParseException e) {
            logger.warn("Unable to parse date: " + modificationDate);
            return new Date();
        }
    }

    @Nullable
    @Override
    public String getAuthor() {
        return author;
    }

    @Nullable
    @Override
    public String getCommitMessage() {
        return commitMessage;
    }

    public TFSContentRevision createContentRevision() throws VcsException {
        try {
            return TFSContentRevision.create(project, localPath, changeset, modificationDate);
        } catch (Exception e) {
            logger.warn("failed to create content revision", e);
            throw new VcsException("Cannot get revision content", e);
        }
    }
}
