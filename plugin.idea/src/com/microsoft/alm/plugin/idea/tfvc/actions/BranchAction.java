// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.exceptions.SyncException;
import com.microsoft.alm.plugin.external.models.SyncResults;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import com.microsoft.alm.plugin.idea.tfvc.ui.CreateBranchDialog;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            CreateBranchDialog d = new CreateBranchDialog(
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

            // Get the current workspace
            final Workspace workspace = CommandUtils.getWorkspace(serverContext, actionContext.getProject());
            if (workspace == null) {
                throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.KEY_ERRORS_UNABLE_TO_DETERMINE_WORKSPACE));
            }

            final String targetServerPath = d.getTargetPath();
            logger.info("TargetServerPath from dialog: " + targetServerPath);
            String targetLocalPath = StringUtils.EMPTY;
            if (d.isCreateWorkingCopies()) {
                logger.info("User selected to sync the new branched copies");
                // See if the target path is already mapped
                targetLocalPath = CommandUtils.tryGetLocalPath(serverContext, targetServerPath, workspace.getName());
                logger.info("targetLocalPath: " + targetLocalPath);
                if (StringUtils.isEmpty(targetLocalPath)) {
                    logger.info("Opening the FileChooser dialog for the user to select where the unmapped branch should be mapped to.");
                    final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                    descriptor.setTitle(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_FILE_CHOOSE_TITLE));
                    descriptor.setShowFileSystemRoots(true);
                    final String message = TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_FILE_CHOOSE_DESCRIPTION,
                            targetServerPath, workspace.getName());
                    descriptor.setDescription(message);

                    final VirtualFile selectedFile = FileChooser.chooseFile(descriptor, project, null);
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

//            final ResultWithFailures<GetOperation> createBranchResult = workspace.getServer().getVCS()
//                    .createBranch(workspace.getName(), workspace.getOwnerName(), sourceServerPath, version, targetServerPath, project,
//                            TFSBundle.message("creating.branch"));
//            if (!createBranchResult.getFailures().isEmpty()) {
//                StringBuilder s = new StringBuilder("Failed to create branch:\n");
//                for (Failure failure : createBranchResult.getFailures()) {
//                    s.append(failure.getMessage()).append("\n");
//                }
//                Messages.showErrorDialog(project, s.toString(), "Create Branch");
//                return;
//            }


            // Create the branch
            logger.info("Creating branch... isFolder: " + isFolder);
            final String comment = TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_COMMENT, sourceServerPath);
            CommandUtils.createBranch(serverContext, workingFolder, true, comment, null, sourceServerPath, targetServerPath);

            if (d.isCreateWorkingCopies()) {
                logger.info("Get the latest for the branched folder...");
                final String localPath = targetLocalPath;
                final List<VcsException> errors = new ArrayList<VcsException>();
                ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                    public void run() {
                        logger.info("Syncing: " + localPath);
                        final SyncResults syncResults = CommandUtils.syncWorkspace(serverContext, localPath);
                        if (syncResults.getExceptions().size() > 0) {
                            for (final SyncException se : syncResults.getExceptions()) {
                                errors.add(TFSVcs.convertToVcsException(se));
                            }
                        }
                    }
                }, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_SYNC_PROGRESS), false, project);

                if (!errors.isEmpty()) {
                    logger.info("Errors found");
                    AbstractVcsHelper.getInstance(project).showErrors(errors,
                            TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MESSAGE_TITLE));
                }
            }

            targetLocalPath = CommandUtils.tryGetLocalPath(serverContext, targetServerPath, workspace.getName());
            logger.info("targetLocalPath: " + targetLocalPath);
            if (StringUtils.isNotEmpty(targetLocalPath)) {
                logger.info("Marking the target path dirty in the editor.");
                TfsFileUtil.markDirtyRecursively(project, VersionControlPath.getFilePath(targetLocalPath, isFolder));
            }

            final String message = TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MESSAGE_SUCCESS, sourceServerPath, targetServerPath);
            Messages.showInfoMessage(project, message, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MESSAGE_TITLE));
        } catch (final Throwable t) {
            logger.warn("Branching failed", t);
            final String message = TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MESSAGE_FAILURE, t.getMessage());
            Messages.showErrorDialog(actionContext.getProject(), message, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_BRANCH_MESSAGE_TITLE));
        }
    }
}
