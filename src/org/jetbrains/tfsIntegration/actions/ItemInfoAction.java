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
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.BranchRelative;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.DeletedState;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.ui.ItemInfoDialog;

import java.text.MessageFormat;
import java.util.Collection;

public class ItemInfoAction extends MappedItemAction {

  protected void execute(final @NotNull Project project, final @NotNull WorkspaceInfo workspace, final @NotNull ItemPath itemPath) {
    try {
      final ExtendedItem item = workspace.getServer().getVCS()
        .getExtendedItem(workspace.getName(), workspace.getOwnerName(), itemPath.getServerPath(), DeletedState.Any);
      if (item == null) {
        final String itemType = itemPath.getLocalPath().isDirectory() ? "Folder" : "File";
        final String message =
          MessageFormat.format("{0} ''{1}'' does not exist on server", itemType, itemPath.getLocalPath().getPresentableUrl());
        Messages.showInfoMessage(project, message, getActionTitle(itemPath.getLocalPath()));
        return;
      }
      if (item.getLver() == Integer.MIN_VALUE) {
        final String itemType = itemPath.getLocalPath().isDirectory() ? "Folder" : "File";
        final String message = MessageFormat.format("{0} ''{1}'' is unversioned", itemType, itemPath.getLocalPath().getPresentableUrl());
        Messages.showInfoMessage(project, message, getActionTitle(itemPath.getLocalPath()));
        return;
      }

      final String serverPath = item.getTitem() != null ? item.getTitem() : item.getSitem();
      final Collection<BranchRelative> branches = workspace.getServer().getVCS()
        .queryBranches(workspace.getName(), workspace.getOwnerName(), serverPath, new ChangesetVersionSpec(item.getLver()));

      ItemInfoDialog d = new ItemInfoDialog(project, workspace, item, branches, getActionTitle(itemPath.getLocalPath()));
      d.show();
    }
    catch (TfsException e) {
      final String itemType = itemPath.getLocalPath().isDirectory() ? "folder" : "file";
      final String message = MessageFormat
        .format("Failed to obtain information on {0} ''{1}''\n{2}", itemType, itemPath.getLocalPath().getPresentableUrl(), e.getMessage());
      Messages.showErrorDialog(project, message, getActionTitle(itemPath.getLocalPath()));
    }

  }

  protected String getActionTitle(@NotNull FilePath localPath) {
    return MessageFormat.format("{0} Information", localPath.isDirectory() ? "Folder" : "File");
  }

}