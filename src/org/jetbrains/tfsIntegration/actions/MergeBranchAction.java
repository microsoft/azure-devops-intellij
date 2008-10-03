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
import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.MergeHelper;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.ui.MergeBranchDialog;

public class MergeBranchAction extends SingleItemAction {

  protected void execute(final @NotNull Project project, final @NotNull WorkspaceInfo workspace, final @NotNull ItemPath itemPath)
    throws TfsException {
    final String title = getActionTitle(itemPath.getLocalPath());
    ExtendedItem extendedItem = TfsUtil.getExtendedItem(itemPath.getLocalPath());

    MergeBranchDialog d =
      new MergeBranchDialog(project, workspace, extendedItem.getTitem(), extendedItem.getType() == ItemType.Folder, title);
    d.show();
    if (d.isOK()) {
      MergeHelper.execute(project, workspace, itemPath.getServerPath(), d.getTargetPath(), d.getFromVersion(), d.getToVersion());
    }
  }

  protected static String getActionTitle(final @NotNull FilePath localPath) {
    return "Merge Changes";
  }
}
