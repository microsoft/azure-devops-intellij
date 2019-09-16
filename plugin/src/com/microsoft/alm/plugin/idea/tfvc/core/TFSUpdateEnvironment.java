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

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.SequentialUpdatesContext;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.microsoft.alm.plugin.external.models.SyncResults;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TFVCUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ConflictsEnvironment;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ResolveConflictHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TFSUpdateEnvironment implements UpdateEnvironment {
    private static final Logger logger = LoggerFactory.getLogger(TFSUpdateEnvironment.class);

    @NotNull private final Project project;

    @NotNull
    private final TFSVcs tfsVcs;

    TFSUpdateEnvironment(@NotNull Project project, final @NotNull TFSVcs vcs) {
        this.project = project;
        tfsVcs = vcs;
    }

    @Override
    public void fillGroups(final UpdatedFiles updatedFiles) {
    }

    @Override
    @NotNull
    public UpdateSession updateDirectories(@NotNull final FilePath[] contentRoots,
                                           final UpdatedFiles updatedFiles,
                                           final ProgressIndicator progressIndicator,
                                           @NotNull final Ref<SequentialUpdatesContext> context) throws ProcessCanceledException {
        logger.info("Update on files initiated...");
        final List<VcsException> exceptions = new ArrayList<VcsException>();
        TFSProgressUtil.setProgressText(progressIndicator, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_UPDATE_STATUS_MSG));

        try {
            boolean needRecursion = false;
            for (final FilePath file : contentRoots) {
                // checks for directories so we know if to perform a recursive update
                needRecursion = file.isDirectory() ? true : needRecursion;
                if (needRecursion)
                    break;
            }

            List<String> filesUpdatePaths = TFVCUtil.filterValidTFVCPaths(project, Arrays.asList(contentRoots));
            final SyncResults results = CommandUtils.syncWorkspace(tfsVcs.getServerContext(false), filesUpdatePaths, needRecursion, false);

            // add the changed files to updatedFiles so user knows what has occurred in the workspace
            // TODO: determine the resolution numbers (probably need to call history on each file to get this)
            for (final String file : results.getDeletedFiles()) {
                updatedFiles.getGroupById(FileGroup.REMOVED_FROM_REPOSITORY_ID).add(file, TFSVcs.getKey(), null);
            }
            for (final String file : results.getNewFiles()) {
                updatedFiles.getGroupById(FileGroup.CREATED_ID).add(file, TFSVcs.getKey(), null);
            }
            for (final String file : results.getUpdatedFiles()) {
                updatedFiles.getGroupById(FileGroup.UPDATED_ID).add(file, TFSVcs.getKey(), null);
            }

            // check and resolve conflicts
            // updatedFiles updated in the helper class
            if (results.doConflictsExists()) {
                logger.info("Conflicts found during update");
                final ResolveConflictHelper conflictHelper = new ResolveConflictHelper(tfsVcs.getProject(), updatedFiles, filesUpdatePaths);
                ConflictsEnvironment.getConflictsHandler().resolveConflicts(tfsVcs.getProject(), conflictHelper);
            }

            if (!results.getExceptions().isEmpty()) {
                for (Exception e : results.getExceptions()) {
                    exceptions.add(TFSVcs.convertToVcsException(e));
                }
            }
        } catch (Exception e) {
            exceptions.add(TFSVcs.convertToVcsException(e));
        }

        // TODO (JetBrains) content roots can be renamed while executing
        TfsFileUtil.refreshAndInvalidate(tfsVcs.getProject(), contentRoots, false);

        return new UpdateSession() {
            @Override
            @NotNull
            public List<VcsException> getExceptions() {
                return exceptions;
            }

            @Override
            public void onRefreshFilesCompleted() {
                // TODO: add in the code that does this
                //myVcs.fireRevisionChanged();
            }

            @Override
            public boolean isCanceled() {
                return false;
            }
        };
    }


    @Override
    @Nullable
    public Configurable createConfigurable(final Collection<FilePath> files) {
//    final Map<WorkspaceInfo, UpdateSettingsForm.WorkspaceSettings> workspacesSettings =
//      new HashMap<WorkspaceInfo, UpdateSettingsForm.WorkspaceSettings>();
//    final Ref<TfsException> error = new Ref<TfsException>();
//    Runnable r = new Runnable() {
//      @Override
//      public void run() {
//        try {
//          WorkstationHelper.processByWorkspaces(files, true, myVcs.getProject(), new WorkstationHelper.VoidProcessDelegate() {
//            @Override
//            public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
//              final Map<FilePath, ExtendedItem> result =
//                workspace.getExtendedItems2(paths, myVcs.getProject(), TFSBundle.message("loading.items"));
//              Collection<ExtendedItem> items = new ArrayList<ExtendedItem>(result.values());
//              for (Iterator<ExtendedItem> i = items.iterator(); i.hasNext();) {
//                final ExtendedItem extendedItem = i.next();
//                if (extendedItem == null || extendedItem.getSitem() == null) {
//                  i.remove();
//                }
//              }
//
//              if (items.isEmpty()) {
//                return;
//              }
//
//              // determine common ancestor of all the paths
//              ExtendedItem someExtendedItem = items.iterator().next();
//              UpdateSettingsForm.WorkspaceSettings workspaceSettings =
//                new UpdateSettingsForm.WorkspaceSettings(someExtendedItem.getSitem(), someExtendedItem.getType() == ItemType.Folder);
//              for (ExtendedItem extendedItem : items) {
//                final String path1 = workspaceSettings.serverPath;
//                final String path2 = extendedItem.getSitem();
//                if (VersionControlPath.isUnder(path2, path1)) {
//                  workspaceSettings = new UpdateSettingsForm.WorkspaceSettings(path2, extendedItem.getType() == ItemType.Folder);
//                }
//                else if (!VersionControlPath.isUnder(path1, path2)) {
//                  workspaceSettings = new UpdateSettingsForm.WorkspaceSettings(VersionControlPath.getCommonAncestor(path1, path2), true);
//                }
//              }
//              workspacesSettings.put(workspace, workspaceSettings);
//            }
//          });
//        }
//        catch (TfsException e) {
//          error.set(e);
//        }
//      }
//    };
//
//    ProgressManager.getInstance().runProcessWithProgressSynchronously(r, "TFS: preparing for update...", false, myVcs.getProject());
//
//    if (!error.isNull()) {
//      //noinspection ThrowableResultOfMethodCallIgnored
//      //Messages.showErrorDialog(myVcs.getProject(), error.get().getMessage(), "Update Project");
//      return null;
//    }
//    if (workspacesSettings.isEmpty()) {
//      return null;
//    }
//
//    return new UpdateConfigurable(myVcs.getProject(), workspacesSettings);
        return null;
    }

    @Override
    public boolean validateOptions(final Collection<FilePath> roots) {
        return true;
    }
}
