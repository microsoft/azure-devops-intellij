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

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.GuiUtils;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.exceptions.BranchAlreadyExistsException;
import com.microsoft.alm.plugin.external.exceptions.SyncException;
import com.microsoft.alm.plugin.external.models.SyncResults;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import com.microsoft.alm.plugin.idea.tfvc.ui.CreateBranchDialog;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * This action allows the user to branch the selected file/folder to a new location.
 */
public class BranchAction extends SingleItemAction {
    private static final Logger logger = LoggerFactory.getLogger(BranchAction.class);

    public BranchAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MSG));
    }

    protected void execute(final @NotNull SingleItemActionContext actionContext) {
        logger.info("executing...");
        try {
            final ServerContext serverContext = actionContext.getServerContext();
            final Project project = actionContext.getProject();
            final String sourceServerPath = actionContext.getItem().getServerItem();
            final boolean isFolder = actionContext.getItem().isFolder();
            final String workingFolder = isFolder ?
                    actionContext.getItem().getLocalItem() :
                    Path.getDirectoryName(actionContext.getItem().getLocalItem());
            logger.info("Working folder: " + workingFolder);
            logger.info("Opening branch dialog for " + sourceServerPath);
            final CreateBranchDialog d = new CreateBranchDialog(
                    project,
                    serverContext,
                    sourceServerPath,
                    isFolder);
            if (!d.showAndGet()) {
                return;
            }

            // For now we are just branching from the Latest
            //VersionSpecBase version = d.getVersionSpec();
            //if (version == null) {
            //    Messages.showErrorDialog(project, "Incorrect version specified", "Create Branch");
            //    return;
            //}

            ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                public void run() {
                    ProgressManager.getInstance().getProgressIndicator().setFraction(.1);
                    // Get the current workspace
                    final Workspace workspace = CommandUtils.getWorkspace(serverContext, actionContext.getProject());
                    if (workspace == null) {
                        throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.KEY_ERRORS_UNABLE_TO_DETERMINE_WORKSPACE));
                    }
                    ProgressManager.getInstance().getProgressIndicator().setFraction(.3);

                    final String targetServerPath = d.getTargetPath();
                    logger.info("TargetServerPath from dialog: " + targetServerPath);
                    String targetLocalPath = StringUtils.EMPTY;
                    if (d.isCreateWorkingCopies()) {
                        logger.info("User selected to sync the new branched copies");
                        // See if the target path is already mapped
                        targetLocalPath = CommandUtils.tryGetLocalPath(serverContext, targetServerPath, workspace.getName());
                        ProgressManager.getInstance().getProgressIndicator().setFraction(.5);
                        logger.info("targetLocalPath: " + targetLocalPath);
                        if (StringUtils.isEmpty(targetLocalPath)) {
                            logger.info("Opening the FileChooser dialog for the user to select where the unmapped branch should be mapped to.");
                            final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                            descriptor.setTitle(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_FILE_CHOOSE_TITLE));
                            descriptor.setShowFileSystemRoots(true);
                            final String message = TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_FILE_CHOOSE_DESCRIPTION,
                                    targetServerPath, workspace.getName());
                            descriptor.setDescription(message);

                            final VirtualFile selectedFile = chooseFileInUi(descriptor, project);
                            if (selectedFile == null) {
                                logger.info("User canceled");
                                return;
                            }

                            targetLocalPath = TfsFileUtil.getFilePath(selectedFile).getPath();
                            logger.info("Adding workspace mapping: " + targetServerPath + " -> " + targetLocalPath);
                            CommandUtils.addWorkspaceMapping(serverContext, workspace.getName(),
                                    targetServerPath, targetLocalPath);
                        }
                    }
                    ProgressManager.getInstance().getProgressIndicator().setFraction(.6);

                    // Create the branch
                    logger.info("Creating branch... isFolder: " + isFolder);
                    final String comment = TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_COMMENT, sourceServerPath);
                    try {
                        CommandUtils.createBranch(serverContext, workingFolder, true, comment, null, sourceServerPath, targetServerPath);
                    } catch (final BranchAlreadyExistsException e) {
                        logger.warn("Branch already exists");
                        IdeaHelper.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                Messages.showErrorDialog(actionContext.getProject(), LocalizationServiceImpl.getInstance().getExceptionMessage(e),
                                        TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MESSAGE_TITLE));
                            }
                        });
                        return;
                    }
                    ProgressManager.getInstance().getProgressIndicator().setFraction(.7);

                    if (d.isCreateWorkingCopies()) {
                        logger.info("Get the latest for the branched folder...");
                        final String localPath = targetLocalPath;
                        final List<VcsException> errors = new ArrayList<VcsException>();

                        logger.info("Syncing: " + localPath);
                        final SyncResults syncResults = CommandUtils.syncWorkspace(serverContext, localPath);
                        ProgressManager.getInstance().getProgressIndicator().setFraction(.8);
                        if (syncResults.getExceptions().size() > 0) {
                            for (final SyncException se : syncResults.getExceptions()) {
                                errors.add(TFSVcs.convertToVcsException(se));
                            }
                        }

                        if (!errors.isEmpty()) {
                            logger.info("Errors found");
                            AbstractVcsHelper.getInstance(project).showErrors(errors,
                                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MESSAGE_TITLE));
                        }
                    }

                    targetLocalPath = CommandUtils.tryGetLocalPath(serverContext, targetServerPath, workspace.getName());
                    ProgressManager.getInstance().getProgressIndicator().setFraction(.9);
                    logger.info("targetLocalPath: " + targetLocalPath);
                    if (StringUtils.isNotEmpty(targetLocalPath)) {
                        logger.info("Refresh target parent directory to display in the editor.");
                        // need to refresh the parent directory to see the new branch show up
                        final File targetFile = new File(targetLocalPath);
                        final FilePath[] parent = {VersionControlPath.getFilePath(targetFile.getParent(), true)};
                        TfsFileUtil.refreshAndInvalidate(project, parent, false);
                    }

                    final String message = TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MESSAGE_SUCCESS, sourceServerPath, targetServerPath);
                    IdeaHelper.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Messages.showInfoMessage(project, message, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MESSAGE_TITLE));
                        }
                    });

                }
            }, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_SYNC_PROGRESS), false, project);

        } catch (final Throwable t) {
            logger.warn("Branching failed", t);
            final String message = TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MESSAGE_FAILURE, t.getMessage());
            Messages.showErrorDialog(actionContext.getProject(), message, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MESSAGE_TITLE));
        }
    }

    private VirtualFile chooseFileInUi(final FileChooserDescriptor descriptor, final Project project) {
        final Ref<VirtualFile> selectedFile = Ref.create();
        try {
            GuiUtils.runOrInvokeAndWait(new Runnable() {
                @Override
                public void run() {
                     selectedFile.set(FileChooser.chooseFile(descriptor, project, null));
                }
            });
        } catch (InvocationTargetException e) {
            logger.error("Error when calling FileChooser", e);
            return null;
        } catch (InterruptedException e) {
            return null;
        }

        return selectedFile.get();
    }
}
