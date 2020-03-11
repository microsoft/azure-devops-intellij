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

package com.microsoft.alm.plugin.idea.tfvc.core.tfs.operations;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.TfvcClient;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.ServerStatus;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusProvider;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusVisitor;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfsPath;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deletes a list of files through TFVC
 */
public class ScheduleForDeletion {
    public static final Logger logger = LoggerFactory.getLogger(ScheduleForDeletion.class);

    public static Collection<VcsException> execute(final Project project, final List<FilePath> files) {
        // choose roots
        // find if changes need reverted and revert them
        // schedule roots for deletion using their original names

        final Collection<VcsException> errors = new ArrayList<VcsException>();

        final List<String> filePaths = new ArrayList<String>(files.size());
        for (final FilePath filePath : files) {
            filePaths.add(filePath.getPath());
        }

        try {
            final List<PendingChange> pendingChanges = new ArrayList<PendingChange>();
            final List<String> revert = new ArrayList<String>();
            final Set<String> scheduleForDeletion = new HashSet<String>();
            final ServerContext context = TFSVcs.getInstance(project).getServerContext(true);

            for (final String path : filePaths) {
                final List<PendingChange> fileChanges = CommandUtils.getStatusForFiles(project, context, ImmutableList.of(path));

                // deleting a file that has no changes
                if (fileChanges.isEmpty()) {
                    scheduleForDeletion.add(path);
                } else {
                    pendingChanges.addAll(fileChanges);
                }
            }

            for (final PendingChange pendingChange : pendingChanges) {
                StatusProvider.visitByStatus(new StatusVisitor() {

                    public void unversioned(final @NotNull FilePath localPath, final boolean localItemExists,
                                            final @NotNull ServerStatus serverStatus) throws TfsException {
                        // if an unversioned delete, IDE has already deleted the file and now TFVC has to delete it
                        if (pendingChange.getChangeTypes().contains(ServerStatusType.DELETE)) {
                            logger.info("ScheduleForDeletion: unversioned deleted file " + localPath.getPath());
                            scheduleForDeletion.add(StringUtils.isNotEmpty(pendingChange.getSourceItem()) ? pendingChange.getSourceItem() : pendingChange.getLocalItem());
                        } else {
                            // do nothing because file isn't recognized
                            logger.info("ScheduleForDeletion: do nothing for unversioned file " + localPath.getPath());
                        }
                    }

                    public void checkedOutForEdit(final @NotNull FilePath localPath, final boolean localItemExists,
                                                  final @NotNull ServerStatus serverStatus) throws TfsException {
                        logger.info("ScheduleForDeletion: checkedOutForEdit file " + localPath.getPath());
                        revert.add(pendingChange.getLocalItem());
                        scheduleForDeletion.add(pendingChange.getLocalItem());
                    }

                    @Override
                    public void locked(@NotNull FilePath localPath, boolean localItemExists, @NotNull ServerStatus serverStatus) throws TfsException {
                        // nothing to do
                    }

                    public void scheduledForAddition(final @NotNull FilePath localPath, final boolean localItemExists,
                                                     final @NotNull ServerStatus serverStatus) {
                        logger.info("ScheduleForDeletion: scheduledForAddition file " + localPath.getPath());
                        revert.add(pendingChange.getLocalItem());
                    }

                    public void scheduledForDeletion(final @NotNull FilePath localPath, final boolean localItemExists,
                                                     final @NotNull ServerStatus serverStatus) {
                        logger.warn("ScheduleForDeletion: " + localPath + " already is deleted");
                    }

                    public void renamed(final @NotNull FilePath localPath, final boolean localItemExists,
                                        final @NotNull ServerStatus serverStatus) throws TfsException {
                        logger.info("ScheduleForDeletion: renamed file " + localPath.getPath());
                        // revert local path but delete the source path since that is the original path before the rename
                        revert.add(pendingChange.getLocalItem());
                        scheduleForDeletion.add(pendingChange.getSourceItem());
                    }

                    public void renamedCheckedOut(final @NotNull FilePath localPath, final boolean localItemExists,
                                                  final @NotNull ServerStatus serverStatus) throws TfsException {
                        logger.info("ScheduleForDeletion: renamedCheckedOut file " + localPath.getPath());
                        // revert local path but delete the source path since that is the original path before the rename
                        revert.add(pendingChange.getLocalItem());
                        scheduleForDeletion.add(pendingChange.getSourceItem());
                    }

                    public void undeleted(final @NotNull FilePath localPath, final boolean localItemExists,
                                          final @NotNull ServerStatus serverStatus) throws TfsException {
                        logger.info("ScheduleForDeletion: undeleted file " + localPath.getPath());
                        revert.add(pendingChange.getLocalItem());
                    }
                }, pendingChange);
            }

            if (!revert.isEmpty()) {
                List<TfsPath> pathsForUndo = revert.stream().map(TfsLocalPath::new).collect(Collectors.toList());
                TfvcClient.getInstance(project).undoLocalChanges(context, pathsForUndo);
            }

            final List<String> confirmedDeletedFiles = new ArrayList<String>();
            if (!scheduleForDeletion.isEmpty()) {
                // a workspace is needed since some paths are server paths, all changes will have the same workspace so
                // just get it from the first change (list isn't empty since deletes were found)
                // if no pending changes exist a workspace isn't needed b/c a local path is being used
                final String workspace;
                if (!pendingChanges.isEmpty()) {
                    workspace = pendingChanges.get(0).getWorkspace();
                } else {
                    workspace = StringUtils.EMPTY;
                }
                confirmedDeletedFiles.addAll(CommandUtils.deleteFiles(context,
                        new ArrayList<String>(scheduleForDeletion), workspace, false));
            }

            for (final FilePath expectedDeletedFile : files) {
                if (confirmedDeletedFiles.contains(expectedDeletedFile.getPath())) {
                    TfsFileUtil.markFileDirty(project, expectedDeletedFile);
                }
            }

        } catch (Throwable t) {
            logger.warn("executeDelete experienced a failure while looking for altered files to delete", t);
            errors.add(TFSVcs.convertToVcsException(t));
        }
        return errors;
    }
}
