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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ExtendedItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;

public abstract class SingleItemAction extends AnAction {

  private static final Collection<FileStatus> ALLOWED_STATUSES =
    Arrays.asList(FileStatus.HIJACKED, FileStatus.MODIFIED, FileStatus.NOT_CHANGED, FileStatus.OBSOLETE);

  protected abstract void execute(final @NotNull Project project,
                                  final @NotNull WorkspaceInfo workspace,
                                  final @NotNull FilePath localPath,
                                  final @NotNull ExtendedItem extendedItem) throws TfsException;

  protected Collection<FileStatus> getAllowedStatuses() {
    return ALLOWED_STATUSES;
  }

  public void actionPerformed(final AnActionEvent e) {
    final Project project = e.getData(PlatformDataKeys.PROJECT);
    VirtualFile file = VcsUtil.getOneVirtualFile(e);

    // checked by isEnabled()
    //noinspection ConstantConditions
    final FilePath localPath = TfsFileUtil.getFilePath(file);

    String actionTitle = StringUtil.trimEnd(e.getPresentation().getText(), "...");
    try {
      Pair<WorkspaceInfo, ExtendedItem> workspaceAndItem =
        TfsUtil.getWorkspaceAndExtendedItem(localPath, project, TFSBundle.message("loading.item"));
      if (workspaceAndItem == null) {
        final String itemType = localPath.isDirectory() ? "folder" : "file";
        final String message = MessageFormat.format("No mapping found for {0} ''{1}''", itemType, localPath.getPresentableUrl());
        Messages.showErrorDialog(project, message, actionTitle);
        return;
      }
      if (workspaceAndItem.second == null) {
        final String itemType = localPath.isDirectory() ? "Folder" : "File";
        final String message = MessageFormat.format("{0} ''{1}'' is unversioned", itemType, localPath.getPresentableUrl());
        Messages.showErrorDialog(project, message, actionTitle);
        return;
      }
      //noinspection ConstantConditions
      execute(project, workspaceAndItem.first, localPath, workspaceAndItem.second);
    }
    catch (TfsException ex) {
      Messages.showErrorDialog(project, ex.getMessage(), actionTitle);
    }
  }

  public void update(final AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled(e.getData(PlatformDataKeys.PROJECT), VcsUtil.getOneVirtualFile(e)));
  }

  protected final boolean isEnabled(final Project project, final VirtualFile file) {
    if (file == null) {
      return false;
    }
    final FileStatus status = FileStatusManager.getInstance(project).getStatus(file);
    return getAllowedStatuses().contains(status);
  }

}
