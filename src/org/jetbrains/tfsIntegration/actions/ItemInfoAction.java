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

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.BranchRelative;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ExtendedItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.ItemInfoDialog;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;

public class ItemInfoAction extends SingleItemAction implements DumbAware {

  private static final Collection<FileStatus> ALLOWED_STATUSES =
    Arrays.asList(FileStatus.HIJACKED, FileStatus.MODIFIED, FileStatus.NOT_CHANGED, FileStatus.OBSOLETE, FileStatus.ADDED);

  protected Collection<FileStatus> getAllowedStatuses() {
    return ALLOWED_STATUSES;
  }

  protected void execute(final @NotNull Project project,
                         final @NotNull WorkspaceInfo workspace,
                         final @NotNull FilePath localPath,
                         final @NotNull ExtendedItem extendedItem) throws TfsException {
    //noinspection ConstantConditions
    if (extendedItem.getLver() == Integer.MIN_VALUE) {
      final String itemType = localPath.isDirectory() ? "Folder" : "File";
      final String message = MessageFormat.format("{0} ''{1}'' is unversioned", itemType, localPath.getPresentableUrl());
      Messages.showInfoMessage(project, message, getActionTitle(localPath.isDirectory()));
      return;
    }

    final String serverPath = extendedItem.getTitem() != null ? extendedItem.getTitem() : extendedItem.getSitem();
    final Collection<BranchRelative> branches = workspace.getServer().getVCS()
      .queryBranches(serverPath, new ChangesetVersionSpec(extendedItem.getLver()), project, TFSBundle.message("loading.branches"));

    ItemInfoDialog d = new ItemInfoDialog(project, workspace, extendedItem, branches, getActionTitle(localPath.isDirectory()));
    d.show();
  }

  private static String getActionTitle(boolean isDirectory) {
    return MessageFormat.format("{0} Information", isDirectory ? "Folder" : "File");
  }

}
