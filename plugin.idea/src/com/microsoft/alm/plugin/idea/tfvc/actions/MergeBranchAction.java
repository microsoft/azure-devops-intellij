// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsRunnable;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.MergeResults;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.VersionControlPath;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ConflictsEnvironment;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.conflicts.ResolveConflictHelper;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import com.microsoft.alm.plugin.idea.tfvc.ui.MergeBranchDialog;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class MergeBranchAction extends SingleItemAction implements DumbAware {

    protected MergeBranchAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_MERGE_BRANCH_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_MERGE_BRANCH_MSG));
    }

    @Override
    protected void execute(@NotNull final SingleItemActionContext actionContext) throws TfsException {
        logger.info("executing...");
        final String title = TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_MERGE_BRANCH_TITLE);
        final ServerContext serverContext = actionContext.getServerContext();
        final Project project = actionContext.getProject();
        final String sourceServerPath = actionContext.getItem().getServerItem();
        final boolean isFolder = actionContext.getItem().isFolder();
        final String workingFolder = isFolder ?
                actionContext.getItem().getLocalItem() :
                Path.getDirectoryName(actionContext.getItem().getLocalItem());

        // Create the branch provider and prepopulate it
        InternalBranchListProvider branchListProvider = new InternalBranchListProvider(serverContext, workingFolder);
        branchListProvider.getBranches(sourceServerPath);

        final MergeBranchDialog d = new MergeBranchDialog(project, serverContext, sourceServerPath, isFolder,
                title, branchListProvider);
        if (!d.showAndGet()) {
            logger.info("User canceled");
            return;
        }

        try {
            VcsUtil.runVcsProcessWithProgress(new VcsRunnable() {
                @Override
                public void run() throws VcsException {
                    // Get the current workspace
                    final Workspace workspace = CommandUtils.getWorkspace(serverContext, actionContext.getProject());
                    if (workspace == null) {
                        logger.info("Workspace not found");
                        throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_ERRORS_UNABLE_TO_DETERMINE_WORKSPACE));
                    }

                    // Make sure we have a mapping for the target
                    final String targetServerPath = d.getTargetPath();
                    logger.info("targetServerPath: " + targetServerPath);
                    final String targetLocalPath = CommandUtils.tryGetLocalPath(serverContext, targetServerPath, workspace.getName());
                    logger.info("targetLocalPath: " + targetLocalPath);
                    if (StringUtils.isEmpty(targetLocalPath)) {
                        logger.info("Target path not mapped in current workspace.");
                        throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_MERGE_BRANCH_ERRORS_NO_MAPPING_FOUND, d.getTargetPath(), workspace.getName()));
                    }

                    // Perform the merge operation
                    final MergeResults mergeResults = CommandUtils.merge(serverContext, workingFolder, sourceServerPath, targetServerPath, null, true);

                    // Check to see if there is anything to actually do. If not, return.
                    if (mergeResults.noChangesToMerge()) {
                        logger.info("No changes to merge.");
                        throw new VcsException(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_MERGE_BRANCH_ERRORS_NO_CHANGES_TO_MERGE, d.getSourcePath(), d.getTargetPath()));
                    }

                    if (mergeResults.doConflictsExists()) {
                        logger.info("Conflicts found; launching the conflict resolution ui...");
                        ResolveConflictHelper resolveConflictHelper =
                                new ResolveConflictHelper(project, null, Collections.singletonList(targetLocalPath));
                        try {
                            ConflictsEnvironment.getConflictsHandler().resolveConflicts(project, resolveConflictHelper);
                        } catch (TfsException e) {
                            throw TFSVcs.convertToVcsException(e);
                        }
                    }

                    // Refresh the virtual files inside IntelliJ
                    logger.info("Refreshing the virtual files...");
                    final FilePath targetFilePath = VersionControlPath.getFilePath(targetLocalPath, isFolder);
                    for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots()) {
                        if (targetFilePath.isUnder(TfsFileUtil.getFilePath(root), false)) {
                            TfsFileUtil.refreshAndInvalidate(project, new FilePath[]{targetFilePath}, true);
                            break;
                        }
                    }
                }
            }, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_MERGE_BRANCH_PROGRESS_MERGING), false, project);

            // All done
            final String message = TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_MERGE_BRANCH_SUCCESS, d.getSourcePath(), d.getTargetPath());
            Messages.showInfoMessage(project, message, message);
        } catch (Throwable t) {
            Messages.showErrorDialog(project, t.getMessage(), title);
        }
    }

    private static class InternalBranchListProvider implements MergeBranchDialog.BranchListProvider {
        private final String workingFolder;
        private final ServerContext serverContext;

        // This class has an internal cache to make successive calls to the getBranches method fast
        // This is not meant to be a reliable or extensive cache.
        private String lastSourceUsed = null;
        private List<String> lastBranchesRetrieved = null;

        public InternalBranchListProvider(final ServerContext serverContext, final String workingFolder) {
            this.workingFolder = workingFolder;
            this.serverContext = serverContext;
        }

        @Override
        public List<String> getBranches(final String source) {
            ArgumentHelper.checkNotEmptyString(source, "source");
            // If the source is the same as the last time we were called, we return the same result
            if (!StringUtils.equalsIgnoreCase(lastSourceUsed, source)) {
                // Not the same so get the list of branches for this new source
                lastBranchesRetrieved = CommandUtils.getBranches(serverContext, workingFolder, source);
                lastSourceUsed = source;
            }
            return lastBranchesRetrieved;
        }
    }
}
