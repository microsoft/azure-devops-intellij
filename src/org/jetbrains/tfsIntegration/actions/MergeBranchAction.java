/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.actions;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ConflictsEnvironment;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ResolveConflictHelper;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.MergeResponse;
import org.jetbrains.tfsIntegration.ui.MergeBranchDialog;

import java.text.MessageFormat;
import java.util.*;

public class MergeBranchAction extends SingleItemAction {

  protected void execute(final @NotNull Project project,
                         final @NotNull WorkspaceInfo workspace,
                         final @NotNull FilePath localPath,
                         final @NotNull ExtendedItem extendedItem) throws TfsException {
    final String title = "Merge Branch Changes";

    MergeBranchDialog d =
      new MergeBranchDialog(project, workspace, extendedItem.getSitem(), extendedItem.getType() == ItemType.Folder, title);
    d.show();
    if (!d.isOK()) {
      return;
    }

    if (!workspace.hasLocalPathForServerPath(d.getTargetPath())) {
      String message = MessageFormat.format("No mapping found for ''{0}'' in workspace ''{1}''.", d.getTargetPath(), workspace.getName());
      Messages.showErrorDialog(project, message, title);
      return;
    }

    final MergeResponse mergeResponse = workspace.getServer().getVCS()
      .merge(workspace.getName(), workspace.getOwnerName(), d.getSourcePath(), d.getTargetPath(), d.getFromVersion(), d.getToVersion());

    final List<VcsException> errors = new ArrayList<VcsException>();
    if (mergeResponse.getMergeResult().getGetOperation() != null) {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
        public void run() {
          final Collection<VcsException> applyErrors = ApplyGetOperations
            .execute(project, workspace, Arrays.asList(mergeResponse.getMergeResult().getGetOperation()),
                     ProgressManager.getInstance().getProgressIndicator(), null, ApplyGetOperations.DownloadMode.ALLOW);
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
      FilePath targetLocalPath = workspace.findLocalPathByServerPath(d.getTargetPath(), true);

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

}
