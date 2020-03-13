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

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeListManagerGate;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.RootsCollection;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusProvider;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TFVCUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Extends the VCS change provider to execture the correct events to find out the local changes in the workspace
 * <p/>
 * TODO (JetBrains) important cases
 * 1. when folder1 is unversioned and folder1/file1 is scheduled for addition, team explorer effectively shows folder1 as scheduled for addition
 */

public class TFSChangeProvider implements ChangeProvider {
    private static final Logger logger = LoggerFactory.getLogger(TFSChangeProvider.class);

    @NotNull
    private final TFSVcs myVcs;

    public TFSChangeProvider(@NotNull TFSVcs vcs) {
        myVcs = vcs;
    }

    public boolean isModifiedDocumentTrackingRequired() {
        return false;
    }

    public void doCleanup(final List<VirtualFile> files) {
    }

    public void getChanges(@NotNull final VcsDirtyScope dirtyScope,
                           @NotNull final ChangelistBuilder builder,
                           @NotNull final ProgressIndicator progress,
                           @NotNull final ChangeListManagerGate addGate) {
        Project project = myVcs.getProject();
        if (project.isDisposed()) {
            return;
        }

        progress.setText("Processing changes");

        // process only roots, filter out child items since requests are recursive anyway
        RootsCollection.FilePathRootsCollection roots = new RootsCollection.FilePathRootsCollection();
        roots.addAll(dirtyScope.getRecursivelyDirtyDirectories());

        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        for (FilePath dirtyFile : dirtyScope.getDirtyFiles()) {
            // workaround for IDEADEV-31511 and IDEADEV-31721
            if (dirtyFile.getVirtualFile() == null || !changeListManager.isIgnoredFile(dirtyFile.getVirtualFile())) {
                roots.add(dirtyFile);
            }
        }

        if (roots.isEmpty()) {
            return;
        }

        final List<String> pathsToProcess = TFVCUtil.filterValidTFVCPaths(project, roots);
        if (pathsToProcess.isEmpty()) {
            return;
        }

        List<PendingChange> changes;
        try {
            ServerContext serverContext = myVcs.getServerContext(true);
            changes = TfvcClient.getInstance(project).getStatusForFiles(serverContext, pathsToProcess);
        } catch (final Throwable t) {
            logger.error("Failed to get changes from command line. roots=" + StringUtils.join(pathsToProcess, ", "), t);
            changes = Collections.emptyList();
        }

        // for each change, find out the status of the changes and then add to the list
        final ChangelistBuilderStatusVisitor changelistBuilderStatusVisitor = new ChangelistBuilderStatusVisitor(project, builder);
        for (final PendingChange change : changes) {
            StatusProvider.visitByStatus(changelistBuilderStatusVisitor, change);
        }
    }
}