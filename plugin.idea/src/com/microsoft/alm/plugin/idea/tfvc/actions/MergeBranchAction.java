// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.project.DumbAware;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.jetbrains.annotations.NotNull;

public class MergeBranchAction extends SingleItemAction implements DumbAware {
    /*
      protected void execute(final @NotNull Project project,
                             final @NotNull WorkspaceInfo workspace,
                             final @NotNull FilePath localPath,
                             final @NotNull ExtendedItem extendedItem) throws TfsException {
        final String title = "Merge Branch Changes";

        MergeBranchDialog d =
          new MergeBranchDialog(project, workspace, extendedItem.getSitem(), extendedItem.getType() == ItemType.Folder, title);
        if (!d.showAndGet()) {
          return;
        }

        if (!workspace.hasLocalPathForServerPath(d.getTargetPath(), project)) {
          String message = MessageFormat.format("No mapping found for ''{0}'' in workspace ''{1}''.", d.getTargetPath(), workspace.getName());
          Messages.showErrorDialog(project, message, title);
          return;
        }

        final MergeResponse mergeResponse = workspace.getServer().getVCS()
          .merge(workspace.getName(), workspace.getOwnerName(), d.getSourcePath(), d.getTargetPath(), d.getFromVersion(), d.getToVersion(),
                 project, TFSBundle.message("merging"));

        final List<VcsException> errors = new ArrayList<VcsException>();
        if (mergeResponse.getMergeResult().getGetOperation() != null) {
          ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
              final Collection<VcsException> applyErrors = ApplyGetOperations
                .execute(project, workspace, Arrays.asList(mergeResponse.getMergeResult().getGetOperation()),
                         new ApplyProgress.ProgressIndicatorWrapper(ProgressManager.getInstance().getProgressIndicator()), null,
                         ApplyGetOperations.DownloadMode.ALLOW);
              errors.addAll(applyErrors);
            }
          }, title, false, project);
        }

        Collection<Conflict> unresolvedConflicts = ResolveConflictHelper.getUnresolvedConflicts(
          mergeResponse.getConflicts().getConflict() != null
          ? Arrays.asList(mergeResponse.getConflicts().getConflict())
          : Collections.<Conflict>emptyList());

        if (!unresolvedConflicts.isEmpty()) {
          ResolveConflictHelper resolveConflictHelper =
            new ResolveConflictHelper(project, Collections.singletonMap(workspace, unresolvedConflicts), null);
          ConflictsEnvironment.getConflictsHandler().resolveConflicts(resolveConflictHelper);
        }

        if (mergeResponse.getFailures().getFailure() != null) {
          errors.addAll(TfsUtil.getVcsExceptions(Arrays.asList(mergeResponse.getFailures().getFailure())));
        }

        if (errors.isEmpty()) {
          FilePath targetLocalPath = workspace.findLocalPathByServerPath(d.getTargetPath(), true, project);

          for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots()) {
            //noinspection ConstantConditions
            if (targetLocalPath.isUnder(TfsFileUtil.getFilePath(root), false)) {
              TfsFileUtil.refreshAndInvalidate(project, new FilePath[]{targetLocalPath}, true);
              break;
            }
          }

          if (unresolvedConflicts.isEmpty() && mergeResponse.getMergeResult().getGetOperation() == null) {
            String message = MessageFormat.format("No changes to merge from ''{0}'' to ''{1}''.", d.getSourcePath(), d.getTargetPath());
            TfsUtil.showBalloon(project, MessageType.INFO, message);
          }
          else {
            String message = MessageFormat.format("Changes merged successfully from ''{0}'' to ''{1}''.", d.getSourcePath(), d.getTargetPath());
            TfsUtil.showBalloon(project, MessageType.INFO, message);
          }
        }
        else {
          AbstractVcsHelper.getInstance(project).showErrors(errors, TFSVcs.TFS_NAME);
        }
      }
    */
    @Override
    protected void execute(@NotNull SingleItemActionContext actionContext) throws TfsException {

    }
}
