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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyProgress;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.CreateBranchDialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BranchAction extends SingleItemAction implements DumbAware {

  protected void execute(final @NotNull Project project,
                         final @NotNull WorkspaceInfo workspace,
                         final @NotNull FilePath sourceLocalPath,
                         final @NotNull ExtendedItem sourceExtendedItem) {
    try {
      final String sourceServerPath = sourceExtendedItem.getSitem();
      CreateBranchDialog d = new CreateBranchDialog(project, workspace, sourceServerPath, sourceExtendedItem.getType() == ItemType.Folder);
      d.show();
      if (!d.isOK()) {
        return;
      }

      VersionSpecBase version = d.getVersionSpec();
      if (version == null) {
        Messages.showErrorDialog(project, "Incorrect version specified", "Create Branch");
        return;
      }

      final String targetServerPath = d.getTargetPath();
      if (d.isCreateWorkingCopies()) {
        FilePath targetLocalPath = workspace.findLocalPathByServerPath(targetServerPath, true);
        if (targetLocalPath == null) {
          FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
          d.setTitle("Select Local Folder");
          descriptor.setShowFileSystemRoots(true);
          final String message = MessageFormat
            .format("Branch target folder ''{0}'' is not mapped. Select a local folder to create a mapping in workspace ''{1}''",
                    targetServerPath, workspace.getName());
          descriptor.setDescription(message);

          VirtualFile[] selectedFiles = FileChooser.chooseFiles(project, descriptor);
          if (selectedFiles.length != 1 || selectedFiles[0] == null) {
            return;
          }

          workspace.addWorkingFolderInfo(
            new WorkingFolderInfo(WorkingFolderInfo.Status.Active, TfsFileUtil.getFilePath(selectedFiles[0]), targetServerPath));
          workspace.saveToServer();
        }
      }

      final ResultWithFailures<GetOperation> createBranchResult = workspace.getServer().getVCS()
        .createBranch(workspace.getName(), workspace.getOwnerName(), sourceServerPath, version, targetServerPath);
      if (!createBranchResult.getFailures().isEmpty()) {
        StringBuilder s = new StringBuilder("Failed to create branch:\n");
        for (Failure failure : createBranchResult.getFailures()) {
          s.append(failure.getMessage()).append("\n");
        }
        Messages.showErrorDialog(project, s.toString(), "Create Branch");
        return;
      }

      if (d.isCreateWorkingCopies()) {
        final Ref<Collection<VcsException>> downloadErrors = new Ref<Collection<VcsException>>(Collections.<VcsException>emptyList());
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
          public void run() {
            downloadErrors.set(ApplyGetOperations.execute(project, workspace, createBranchResult.getResult(),
                                                          new ApplyProgress.ProgressIndicatorWrapper(
                                                            ProgressManager.getInstance().getProgressIndicator()), null,
                                                          ApplyGetOperations.DownloadMode.ALLOW));
          }
        }, "Creating target working copies", false, project);

        if (!downloadErrors.get().isEmpty()) {
          AbstractVcsHelper.getInstance(project).showErrors(new ArrayList<VcsException>(downloadErrors.get()), "Create Branch");
        }
      }

      // TODO checkin requires proper configuration
      final Collection<PendingChange> pendingChanges = workspace.getServer().getVCS()
        .queryPendingSetsByServerItems(workspace.getName(), workspace.getOwnerName(), Collections.singletonList(targetServerPath),
                                       RecursionType.Full);
      Collection<String> checkin = new ArrayList<String>();
      for (PendingChange change : pendingChanges) {
        if (new ChangeTypeMask(change.getChg()).contains(ChangeType_type0.Branch)) {
          checkin.add(change.getItem());
        }
      }
      final String comment = MessageFormat.format("Branched from {0}", sourceServerPath);
      final ResultWithFailures<CheckinResult> checkinResult = workspace.getServer().getVCS()
        .checkIn(workspace.getName(), workspace.getOwnerName(), checkin, comment, Collections.<WorkItem, CheckinWorkItemAction>emptyMap(),
                 Collections.<Pair<String, String>>emptyList(), null);

      if (!checkinResult.getFailures().isEmpty()) {
        final List<VcsException> checkinErrors = TfsUtil.getVcsExceptions(checkinResult.getFailures());
        AbstractVcsHelper.getInstance(project).showErrors(checkinErrors, "Create Branch");
      }

      final FilePath targetLocalPath = workspace.findLocalPathByServerPath(targetServerPath, true);
      if (targetLocalPath != null) {
        TfsFileUtil.markDirtyRecursively(project, targetLocalPath);
      }

      String message = MessageFormat.format("''{0}'' branched successfully to ''{1}''.", sourceServerPath, targetServerPath);
      Messages.showInfoMessage(project, message, "Create Branch");
    }
    catch (TfsException ex) {
      String message = "Failed to create branch: " + ex.getMessage();
      Messages.showErrorDialog(project, message, "Create Branch");
    }
  }

}
