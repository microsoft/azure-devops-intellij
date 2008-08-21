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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.BranchRelative;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.ui.MergeBranchDialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

public class MergeBranchAction extends MappedItemAction {


  protected void execute(final @NotNull Project project, final @NotNull WorkspaceInfo workspace, final @NotNull ItemPath itemPath) {
    final String title = getActionTitle(itemPath.getLocalPath());
    try {
      Collection<Item> targetBranches = new ArrayList<Item>();
      final Collection<BranchRelative> branches = workspace.getServer().getVCS()
        .queryBranches(workspace.getName(), workspace.getOwnerName(), itemPath.getServerPath(), LatestVersionSpec.INSTANCE);

      BranchRelative subject = null;
      for (BranchRelative branch : branches) {
        if (branch.getReqstd()) {
          subject = branch;
          break;
        }
      }

      for (BranchRelative branch : branches) {
        if (branch.getRelfromid() == subject.getReltoid()) {
          targetBranches.add(branch.getBranchToItem());
        }
        else if (branch.getReltoid() == subject.getRelfromid()) {
          targetBranches.add(branch.getBranchFromItem());
        }
      }

      if (!targetBranches.isEmpty()) {
        MergeBranchDialog d = new MergeBranchDialog(project, workspace, itemPath.getServerPath(), targetBranches, title);
        d.show();
        if (d.isOK()) {
          //MergeHelper.execute(itemPath.getServerPath(), d.getTargetPath(), d.getFromVersion(), d.getToVersion());

        }
      }
      else {
        String message = MessageFormat.format("No target branches found for ''{0}", itemPath.getServerPath());
        Messages.showWarningDialog(project, message, title);
      }
    }
    catch (TfsException e) {
      Messages.showWarningDialog(project, e.getMessage(), title);
    }
  }

  protected String getActionTitle(final @NotNull FilePath localPath) {
    return "Merge Changes";
  }
}
