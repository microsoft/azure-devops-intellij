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
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.DeletedState;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.BranchRelative;
import org.jetbrains.tfsIntegration.ui.ItemInfoDialog;

import java.text.MessageFormat;

public class ItemInfoAction extends SingleSelectionAction {

  protected void actionPerformed(final Project project, final VirtualFile file) {
    final String title = MessageFormat.format("{0} Information", file.isDirectory() ? "Folder" : "File");
    try {
      final FilePath localPath = TfsFileUtil.getFilePath(file);
      final WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(localPath);
      if (workspace == null) {
        final String itemType = file.isDirectory() ? "folder" : "file";
        final String message = MessageFormat.format("No mapping found for {0} ''{1}''", itemType, file.getPresentableUrl());
        Messages.showInfoMessage(project, message, title);
        return;
      }
      final ExtendedItem item = workspace.getServer().getVCS()
        .getExtendedItem(workspace.getName(), workspace.getOwnerName(), workspace.findServerPathByLocalPath(localPath), DeletedState.Any);
      if (item == null) {
        final String itemType = file.isDirectory() ? "Folder" : "File";
        final String message = MessageFormat.format("{0} ''{1}'' does not exist on server", itemType, file.getPresentableUrl());
        Messages.showInfoMessage(project, message, title);
        return;
      }
      if (item.getLver() == Integer.MIN_VALUE) {
        final String itemType = file.isDirectory() ? "Folder" : "File";
        final String message = MessageFormat.format("{0} ''{1}'' is unversioned", itemType, file.getPresentableUrl());
        Messages.showInfoMessage(project, message, title);
        return;
      }

      final String serverPath = item.getTitem() != null ? item.getTitem() : item.getSitem();
      final BranchRelative[] branches = workspace.getServer().getVCS()
        .queryBranches(workspace.getName(), workspace.getOwnerName(), serverPath, new ChangesetVersionSpec(item.getLver()));

      ItemInfoDialog d = new ItemInfoDialog(project, workspace, item, branches, title);
      d.show();
    }
    catch (TfsException e) {
      final String itemType = file.isDirectory() ? "folder" : "file";
      final String message =
        MessageFormat.format("Failed to obtain information on {0} ''{1}''\n{2}", itemType, file.getPresentableUrl(), e.getMessage());
      Messages.showInfoMessage(project, message, title);
    }
  }

}