// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.exceptions.SyncException;
import com.microsoft.alm.plugin.external.models.SyncResults;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.ui.CreateBranchDialog;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class BranchAction extends SingleItemAction implements DumbAware {

    protected void execute(final @NotNull SingleItemActionContext actionContext) {
        try {
            final ServerContext serverContext = actionContext.getServerContext();
            final Project project = actionContext.getProject();
            final String sourceServerPath = actionContext.getItem().getServerItem();
            final boolean isFolder = actionContext.getItem().isFolder();
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
            // TODO do we need to check for null workspace here?

            final String targetServerPath = d.getTargetPath();
            if (d.isCreateWorkingCopies()) {
                // See if the target path is already mapped
                String targetLocalPath = CommandUtils.tryGetLocalPath(serverContext, targetServerPath, workspace.getName());
                if (targetLocalPath == null) {
                    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                    descriptor.setTitle("Select Local Folder");
                    descriptor.setShowFileSystemRoots(true);
                    final String message = MessageFormat
                            .format("Branch target folder ''{0}'' is not mapped. Select a local folder to create a mapping in workspace ''{1}''",
                                    targetServerPath, workspace.getName());
                    descriptor.setDescription(message);

                    final VirtualFile selectedFile = FileChooser.chooseFile(descriptor, project, null);
                    if (selectedFile == null) {
                        return;
                    }

                    targetLocalPath = TfsFileUtil.getFilePath(selectedFile).getPath();
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
            final String comment = MessageFormat.format("Branched from {0}", sourceServerPath);
            CommandUtils.createBranch(serverContext, true, comment, null, sourceServerPath, targetServerPath);

            if (d.isCreateWorkingCopies()) {
                // Get the latest for the branched folder
                final List<VcsException> errors = new ArrayList<VcsException>();
                ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                    public void run() {
                        final SyncResults syncResults = CommandUtils.syncWorkspace(serverContext, targetServerPath);
                        if (syncResults.getExceptions().size() > 0) {
                            for (final SyncException se : syncResults.getExceptions()) {
                                errors.add(TFSVcs.convertToVcsException(se));
                            }
                        }
                    }
                }, "Syncing target branch location", false, project);

                if (!errors.isEmpty()) {
                    AbstractVcsHelper.getInstance(project).showErrors(errors, "Create Branch");
                }
            }

            final String targetLocalPath = CommandUtils.tryGetLocalPath(serverContext, targetServerPath, workspace.getName());
            if (targetLocalPath != null) {
                TfsFileUtil.markDirtyRecursively(project, new LocalFilePath(targetLocalPath, isFolder));
            }

            final String message = MessageFormat.format("''{0}'' branched successfully to ''{1}''.", sourceServerPath, targetServerPath);
            Messages.showInfoMessage(project, message, "Create Branch");
        } catch (final Throwable t) {
            final String message = "Failed to create branch: " + t.getMessage();
            Messages.showErrorDialog(actionContext.getProject(), message, "Create Branch");
        }
    }
}
