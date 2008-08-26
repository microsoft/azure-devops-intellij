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
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;

public abstract class SingleItemAction extends AnAction {

  protected abstract void execute(final @NotNull Project project, final @NotNull WorkspaceInfo workspace, final @NotNull ItemPath itemPath)
    throws TfsException;

  public void actionPerformed(final AnActionEvent e) {
    final Project project = e.getData(DataKeys.PROJECT);
    VirtualFile[] files = e.getData(DataKeys.VIRTUAL_FILE_ARRAY);

    final FilePath localPath = TfsFileUtil.getFilePath(files[0]);

    try {
      WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(localPath);
      ExtendedItem item = TfsUtil.getExtendedItem(localPath);
      execute(project, workspace, new ItemPath(VcsUtil.getFilePath(item.getLocal(), item.getType() == ItemType.Folder), item.getSitem()));
    }
    catch (TfsException ex) {
      Messages.showErrorDialog(project, ex.getMessage(), e.getPresentation().getText());
    }
  }


  public void update(final AnActionEvent e) {
    VirtualFile[] files = e.getData(DataKeys.VIRTUAL_FILE_ARRAY);
    e.getPresentation().setEnabled(isEnabled(files));
  }


  protected boolean isEnabled(final VirtualFile[] files) {
    if (files == null || files.length != 1) {
      return false;
    }

    try {
      return TfsUtil.getExtendedItem(TfsFileUtil.getFilePath(files[0])) != null;
    }
    catch (TfsException e) {
      // skip error handling
      return false;
    }
  }

}
