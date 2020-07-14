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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsVFSListener;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.ServerStatus;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.StatusProvider;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TFVCUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TFSFileListener extends VcsVFSListener {
    public static final Logger logger = LoggerFactory.getLogger(TFSFileListener.class);

    public TFSFileListener(Project project, TFSVcs vcs) {
        super(project, vcs);
    }

    @NotNull
    protected String getAddTitle() {
        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_ITEMS);
    }

    @NotNull
    protected String getSingleFileAddTitle() {
        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_ITEM);
    }

    @NotNull
    protected String getSingleFileAddPromptTemplate() {
        // pass {0} as a param because the current {0} in the string needs to be replaced by something or else there is an error
        // the {0} will be replaced higher up by the file name that we don't have here
        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_PROMPT, "{0}");
    }

    @Override
    protected void executeAdd(
            @NotNull List<VirtualFile> addedFiles,
            @NotNull Map<VirtualFile, VirtualFile> copyFromMap) {
        logger.info("executeAdd executing...");
        Application application = ApplicationManager.getApplication();
        if (UndoManager.getInstance(myProject).isUndoInProgress()) {
            logger.info("{} files won't be added into TFS since Undo action is in progress", addedFiles.size());
            return;
        }

        final List<String> filePaths = TfsFileUtil.getFilePathStrings(addedFiles);
        final List<PendingChange> pendingChanges = new ArrayList<>();

        application.invokeAndWait(() -> ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            TfvcClient client = TfvcClient.getInstance(myProject);
            pendingChanges.addAll(
                    client.getStatusForFiles(
                            TFSVcs.getInstance(myProject).getServerContext(true),
                            filePaths));
        }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_SCHEDULING), false, myProject));

        for (final PendingChange pendingChange : pendingChanges) {
            StatusProvider.visitByStatus(new StatusProvider.StatusAdapter() {
                public void checkedOutForEdit(final @NotNull FilePath localPath,
                                              final boolean localItemExists,
                                              final @NotNull ServerStatus serverStatus) {
                    // TODO (JetBrains): add local conflict
                }

                public void scheduledForAddition(final @NotNull FilePath localPath,
                                                 final boolean localItemExists,
                                                 final @NotNull ServerStatus serverStatus) {
                    addedFiles.remove(localPath.getVirtualFile());
                }

                public void scheduledForDeletion(final @NotNull FilePath localPath,
                                                 final boolean localItemExists,
                                                 final @NotNull ServerStatus serverStatus) {
                    // TODO (JetBrains): add local conflict
                }

                public void renamed(final @NotNull FilePath localPath, final boolean localItemExists, final @NotNull ServerStatus serverStatus) {
                    // TODO (JetBrains): add local conflict
                }

                public void renamedCheckedOut(final @NotNull FilePath localPath,
                                              final boolean localItemExists,
                                              final @NotNull ServerStatus serverStatus) {
                    // TODO (JetBrains): add local conflict
                }

                public void undeleted(final @NotNull FilePath localPath,
                                      final boolean localItemExists,
                                      final @NotNull ServerStatus serverStatus) {
                    // TODO (JetBrains): add local conflict
                }

            }, pendingChange);
        }

        removeInvalidTFVCAddedFiles(addedFiles);

        if (!addedFiles.isEmpty()) {
            application.invokeAndWait(() -> super.executeAdd(addedFiles, copyFromMap));
        }
    }

    protected void performDeletion(@NotNull final List<FilePath> filesToDelete) {
        // nothing to do here since we already have taken care of the deleted file
    }

    @Override
    protected void executeDelete() {
        // overriding so we don't do any special logic here for delete since it's already been taken care of
    }

    @Override
    protected void performAdding(@NotNull final Collection<VirtualFile> addedFiles, @NotNull final Map<VirtualFile, VirtualFile> copyFromMap) {
        if (addedFiles.isEmpty()) {
            logger.warn("TFSFileListener was asked to add 0 files under VCS; skipping");
            return;
        }

        final List<VcsException> errors = new ArrayList<>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            CheckinEnvironment checkinEnvironment = Objects.requireNonNull(
                    TFSVcs.getInstance(myProject).getCheckinEnvironment());
            List<VcsException> exceptions = checkinEnvironment.scheduleUnversionedFilesForAddition(
                    new ArrayList<>(addedFiles));
            if (exceptions != null) errors.addAll(exceptions);
        }, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ADD_PROGRESS), false, myProject);

        if (!errors.isEmpty()) {
            AbstractVcsHelper.getInstance(myProject).showErrors(errors, TFSVcs.TFVC_NAME);
        }
    }

    @NotNull
    protected String getDeleteTitle() {
        return StringUtils.EMPTY; // never called
    }

    protected String getSingleFileDeleteTitle() {
        return null;
    }

    protected String getSingleFileDeletePromptTemplate() {
        return null;
    }

    protected void performMoveRename(final List<MovedFileInfo> movedFiles) {
        // Refreshes the files so that the changes show up in the Local Changes tab
        final Collection<FilePath> invalidate = new ArrayList<>(movedFiles.size());
        for (final MovedFileInfo info : movedFiles) {
            invalidate.add(VcsUtil.getFilePath(info.myOldPath));
        }
        TfsFileUtil.markDirtyRecursively(myProject, invalidate);
    }

    protected boolean isDirectoryVersioningSupported() {
        return true;
    }

    private void removeInvalidTFVCAddedFiles(List<VirtualFile> addedFiles) {
        Map<VirtualFile, FilePath> addedFilePaths = addedFiles.stream()
                .collect(Collectors.toMap(vf -> vf, vf -> new LocalFilePath(vf.getPath(), vf.isDirectory())));
        Set<FilePath> invalidPaths = TFVCUtil.collectInvalidTFVCPaths((TFSVcs) myVcs, addedFilePaths.values().stream())
                .collect(Collectors.toSet());

        addedFiles.removeIf(file -> invalidPaths.contains(addedFilePaths.get(file)));
    }
}
