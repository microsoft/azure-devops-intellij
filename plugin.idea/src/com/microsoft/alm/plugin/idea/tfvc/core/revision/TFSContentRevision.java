// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.revision;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.external.commands.Command;
import com.microsoft.alm.plugin.external.commands.DownloadCommand;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsRevisionNumber;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Creates a revision object for a file so that comparisons can be done between them
 * <p/>
 * TODO: Used to implement ContenRevision until recently when it was changed to ByteBackedContentRevision
 * TODO: That class does not exist in IntelliJ 14 or 15 so reverting that change back to ContentRevision for now
 */
public abstract class TFSContentRevision implements ContentRevision {

    private final Project myProject;

    @Nullable
    private byte[] myContent;

    protected TFSContentRevision(final Project project) {
        myProject = project;
    }

    protected abstract int getChangeset() throws TfsException;

    public static TFSContentRevision create(final Project project,
                                            final @NotNull FilePath localPath,
                                            final int changeset) {
        return new TFSContentRevision(project) {

            protected int getChangeset() {
                return changeset;
            }

            @NotNull
            public FilePath getFile() {
                return localPath;
            }

            @NotNull
            public VcsRevisionNumber getRevisionNumber() {
                return new TfsRevisionNumber(changeset, localPath.getPath()); //TODO: make this number unique
            }
        };
    }

    @Nullable
    public String getContent() throws VcsException {
        return new String(doGetContent(), getFile().getCharset(myProject));
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
        TFSContentStore store = TFSContentStoreFactory.find(getFile().getPath(), getChangeset());
        if (store == null) {
            ArgumentHelper.checkNotNull(getFile(), "localPath");
            store = TFSContentStoreFactory.create(getFile().getPath(), getChangeset());
            // TODO: pass a context instead of null
            final Command<String> command = new DownloadCommand(null, getFile().getPath(), getChangeset(), store.getTmpFile().getPath());
            command.runSynchronously();
        }
        return store.loadContent();
    }

    @NonNls
    public String toString() {
        return "TFSContentRevision [file=" + getFile() + ", revision=" + ((TfsRevisionNumber) getRevisionNumber()).getValue() + "]";
    }
}
