// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.revision.TFSContentRevision;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class TFSChangeList implements CommittedChangeList {
    public static final Logger logger = LoggerFactory.getLogger(TFSChangeList.class);
    private static final SimpleDateFormat TFVC_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private TFSVcs myVcs;
    private int changeSetId;
    private String author;
    private String changeSetDate;
    private int previousChangeSetId;
    private String previousChangeSetDate;
    private @NotNull
    String comment;
    private String workspaceName;
    private List<Change> changes;
    private final List<FilePath> addedFiles = new ArrayList<FilePath>();
    private final List<FilePath> deletedFiles = new ArrayList<FilePath>();
    private final List<FilePath> renamedFiles = new ArrayList<FilePath>();
    private final List<FilePath> editedFiles = new ArrayList<FilePath>();

    public TFSChangeList(final TFSVcs vcs, final DataInput stream) {
        this.myVcs = vcs;
        readFromStream(stream);
    }

    public TFSChangeList(final List<FilePath> addedFiles, final List<FilePath> deletedFiles, final List<FilePath> renamedFiles,
                         final List<FilePath> editedFiles, final int changeSetId, final String author, final String comment,
                         final String changeSetDate, final int previousChangeSetId, final String previousChangeSetDate,
                         final TFSVcs vcs, final String workspaceName) {
        this.addedFiles.addAll(addedFiles);
        this.deletedFiles.addAll(deletedFiles);
        this.renamedFiles.addAll(renamedFiles);
        this.editedFiles.addAll(editedFiles);

        this.changeSetId = changeSetId;
        this.author = author;
        this.comment = comment != null ? comment : StringUtils.EMPTY;
        this.changeSetDate = changeSetDate;
        this.previousChangeSetId = previousChangeSetId;
        this.previousChangeSetDate = previousChangeSetDate;
        this.myVcs = vcs;
        this.workspaceName = workspaceName;
    }

    public String getCommitterName() {
        return author;
    }

    public Date getCommitDate() {
        Date formattedDate = null;
        try {
            formattedDate = TFVC_DATE_FORMAT.parse(changeSetDate);
        } catch (final ParseException e) {
            logger.warn("Exception hit while parsing changeset date", e);
            if (formattedDate == null) {
                // need date to be set to something because null is not handled by IntelliJ
                logger.warn("Date could not be established so defaulting to now");
                formattedDate = Calendar.getInstance().getTime();
            }
        }
        return formattedDate;
    }

    public long getNumber() {
        return changeSetId;
    }

    @Nullable
    @Override
    public String getBranch() {
        return null;
    }

    public AbstractVcs getVcs() {
        return myVcs;
    }

    /**
     * Initializes the changes from each changeset
     * <p>
     * NOTE: the tf tool shows a rename as 2 changes ('rename' and 'delete source rename') but there is no way to reliably
     * tell which 'rename' change goes with which 'delete source rename' change if there are multiple renames in a changeset.
     * To deal with this, we will treat the 'delete source rename' changes as deletes and 'rename' changes as adds. This
     * is what JetBrains did as well so it doesn't differ from their experience.
     *
     * @return
     */
    public Collection<Change> getChanges() {
        if (changes == null) {
            changes = new ArrayList<Change>();
            logger.debug("Initializing the changes for the changeset: " + changeSetId);

            for (final FilePath path : addedFiles) {
                changes.add(new Change(null, TFSContentRevision.create(myVcs.getProject(), path, changeSetId, changeSetDate)));
            }
            for (final FilePath path : deletedFiles) {
                changes.add(new Change(TFSContentRevision.create(myVcs.getProject(), path, previousChangeSetId, previousChangeSetDate), null));
            }
            for (final FilePath path : renamedFiles) {
                // treated like an add (more on why above)
                changes.add(new Change(null, TFSContentRevision.create(myVcs.getProject(), path, changeSetId, changeSetDate)));
            }
            for (final FilePath path : editedFiles) {
                final TFSContentRevision before = TFSContentRevision.create(myVcs.getProject(), path, previousChangeSetId, previousChangeSetDate);
                final TFSContentRevision after = TFSContentRevision.create(myVcs.getProject(), path, changeSetId, changeSetDate);
                changes.add(new Change(before, after));
            }
        }
        return changes;
    }

    public Collection<Change> getChangesWithMovedTrees() {
        return getChanges();
    }

    @Override
    public boolean isModifiable() {
        return true;
    }

    @Override
    public void setDescription(final String newMessage) {
        comment = newMessage != null ? newMessage : StringUtils.EMPTY;
    }

    @NotNull
    public String getName() {
        return comment;
    }

    @NotNull
    public String getComment() {
        return comment;
    }

    /**
     * Save object to stream so it can be cached
     *
     * @param stream
     * @throws IOException
     */
    void writeToStream(final DataOutput stream) throws IOException {
        writePaths(stream, addedFiles);
        writePaths(stream, deletedFiles);
        writePaths(stream, renamedFiles);
        writePaths(stream, editedFiles);
        stream.writeInt(changeSetId);
        stream.writeUTF(author);
        stream.writeUTF(comment);
        stream.writeUTF(changeSetDate);
        stream.writeInt(previousChangeSetId);
        stream.writeUTF(previousChangeSetDate);
        stream.writeUTF(workspaceName);
    }

    /**
     * Reading cached changelist values from the stream and repopulating the data into the changelist
     *
     * @param stream
     */
    private void readFromStream(final DataInput stream) {
        try {
            readPaths(stream, addedFiles);
            readPaths(stream, deletedFiles);
            readPaths(stream, renamedFiles);
            readPaths(stream, editedFiles);
            changeSetId = stream.readInt();
            author = stream.readUTF();
            comment = stream.readUTF();
            changeSetDate = stream.readUTF();
            previousChangeSetId = stream.readInt();
            previousChangeSetDate = stream.readUTF();
            workspaceName = stream.readUTF();
        } catch (final IOException e) {
            logger.warn("Error reading changelist from stream", e);

            // default Strings to empty to keep NPE from happening
            author = StringUtils.isEmpty(author) ? StringUtils.EMPTY : author;
            comment = StringUtils.isEmpty(comment) ? StringUtils.EMPTY : comment;
            changeSetDate = StringUtils.isEmpty(changeSetDate) ? StringUtils.EMPTY : changeSetDate;
            previousChangeSetDate = StringUtils.isEmpty(previousChangeSetDate) ? StringUtils.EMPTY : previousChangeSetDate;
            workspaceName = StringUtils.isEmpty(workspaceName) ? StringUtils.EMPTY : workspaceName;

            AbstractVcsHelper.getInstance(myVcs.getProject()).showError(new VcsException(e), TFSVcs.TFVC_NAME);
        }
    }

    private static void writePaths(final DataOutput stream, final Collection<FilePath> paths) throws IOException {
        stream.writeInt(paths.size());
        for (FilePath path : paths) {
            writePath(stream, path);
        }
    }

    private static void writePath(final DataOutput stream, final FilePath path) throws IOException {
        stream.writeUTF(path.getPath());
        stream.writeBoolean(path.isDirectory());
    }

    private static void readPaths(final DataInput stream, final Collection<FilePath> paths) throws IOException {
        int count = stream.readInt();
        for (int i = 0; i < count; i++) {
            paths.add(readPath(stream));
        }
    }

    private static FilePath readPath(final DataInput stream) throws IOException {
        return VcsUtil.getFilePath(stream.readUTF(), stream.readBoolean());
    }

    // NOTE: toString() is used by IDEA for context menu 'Copy' action in Repository view
    public String toString() {
        return comment;
    }

    // NOTE: equals() and hashCode() used by IDEA to maintain changed files tree state
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final TFSChangeList that = (TFSChangeList) o;

        if (changeSetId != that.changeSetId) return false;
        if (author != null ? !author.equals(that.author) : that.author != null) return false;
        if (changeSetDate != null ? !changeSetDate.equals(that.changeSetDate) : that.changeSetDate != null)
            return false;
        if (workspaceName != null ? !workspaceName.equals(that.workspaceName) : that.workspaceName != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = changeSetId;
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (changeSetDate != null ? changeSetDate.hashCode() : 0);
        result = 31 * result + (workspaceName != null ? workspaceName.hashCode() : 0);
        return result;
    }
}