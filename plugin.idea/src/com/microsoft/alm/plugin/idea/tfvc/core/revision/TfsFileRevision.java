// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.revision;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
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
    private final ServerContext serverContext;
    private final FilePath localPath;
    private final String serverPath;
    private final int changeset;
    private final String author;
    private final String commitMessage;
    private final String modificationDate;
    private byte[] content;

    public TfsFileRevision(final Project project,
                           final ServerContext serverContext,
                           final String serverPath,
                           final @NotNull FilePath localPath,
                           final int changeset,
                           final String author,
                           final String commitMessage,
                           final String modificationDate) {
        ArgumentHelper.checkNotNull(localPath, "localPath");
        ArgumentHelper.checkNotNull(serverContext, "serverContext");

        this.project = project;
        this.serverContext = serverContext;
        this.localPath = localPath;
        this.serverPath = serverPath;
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
        // TODO: Determine what the right "revision number" is here
        // This is what shows up in the history grid for version
        //return new TfsRevisionNumber(changeset, localPath.getName(), modificationDate);
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

    public String getServerPath() {
        return serverPath;
    }

    public TFSContentRevision createContentRevision() throws VcsException {
        try {
            return TFSContentRevision.create(project, serverContext, localPath, changeset, modificationDate);
        } catch (Exception e) {
            logger.warn("failed to create content revision", e);
            throw new VcsException("Cannot get revision content", e);
        }
    }
}
