// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeListManagerGate;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.Command;
import com.microsoft.alm.plugin.external.commands.StatusCommand;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.RootsCollection;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusProvider;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Extends the VCS change provider to execture the correct events to find out the local changes in the workspace
 * <p/>
 * TODO (Jetbrains) important cases
 * 1. when folder1 is unversioned and folder1/file1 is scheduled for addition, team explorer effectively shows folder1 as scheduled for addition
 */

public class TFSChangeProvider implements ChangeProvider {
    private static final Logger logger = LoggerFactory.getLogger(TFSChangeProvider.class);

    private final Project myProject;

    public TFSChangeProvider(final Project project) {
        myProject = project;
    }

    public boolean isModifiedDocumentTrackingRequired() {
        return true;
    }

    public void doCleanup(final List<VirtualFile> files) {
    }

    public void getChanges(@NotNull final VcsDirtyScope dirtyScope,
                           @NotNull final ChangelistBuilder builder,
                           @NotNull final ProgressIndicator progress,
                           @NotNull final ChangeListManagerGate addGate) throws VcsException {

        if (myProject.isDisposed()) {
            return;
        }
        if (builder == null) {
            return;
        }

        progress.setText("Processing changes");

        // Get server context for this project
        final ServerContext serverContext = TFSVcs.getInstance(myProject).getServerContext(false);

        // process only roots, filter out child items since requests are recursive anyway
        RootsCollection.FilePathRootsCollection roots = new RootsCollection.FilePathRootsCollection();
        roots.addAll(dirtyScope.getRecursivelyDirtyDirectories());

        final ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
        for (FilePath dirtyFile : dirtyScope.getDirtyFiles()) {
            // workaround for IDEADEV-31511 and IDEADEV-31721
            if (dirtyFile.getVirtualFile() == null || !changeListManager.isIgnoredFile(dirtyFile.getVirtualFile())) {
                roots.add(dirtyFile);
            }
        }

        if (roots.isEmpty()) {
            return;
        }

        final ChangelistBuilderStatusVisitor changelistBuilderStatusVisitor = new ChangelistBuilderStatusVisitor(myProject, serverContext, builder);

        for (final FilePath root : roots) {
            // if we get a change notification in the $tf folder, we need to just ignore it
            if (StringUtils.containsIgnoreCase(root.getPath(), "$tf") ||
                    StringUtils.containsIgnoreCase(root.getPath(), ".tf")) {
                continue;
            }

            List<PendingChange> changes;
            try {
                // TODO: add the ability to pass multiple roots to the command line
                final Command<List<PendingChange>> command = new StatusCommand(serverContext, root.getPath());
                changes = command.runSynchronously();
            } catch (final Throwable t) {
                logger.warn("Failed to get changes from command line. root=" + root.getPath(), t);
                changes = Collections.emptyList();
            }

            // for each change, find out the status of the changes and then add to the list
            for (final PendingChange change : changes) {
                try {
                    StatusProvider.visitByStatus(changelistBuilderStatusVisitor, change);
                } catch (TfsException e) {
                    throw new VcsException(e.getMessage(), e);
                }
            }
        }
    }

}