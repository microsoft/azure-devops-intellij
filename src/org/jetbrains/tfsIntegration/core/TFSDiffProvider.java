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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.diff.ItemLatestState;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.DeletedState;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ExtendedItem;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Item;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.RecursionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.Collection;

public class TFSDiffProvider implements DiffProvider {
  private @NotNull final Project myProject;

  public TFSDiffProvider(@NotNull final Project project) {
    myProject = project;
  }

  @Nullable
  public ItemLatestState getLastRevision(final VirtualFile virtualFile) {
    final FilePath localPath = TfsFileUtil.getFilePath(virtualFile);
    return getLastRevision(localPath);
  }

  @Nullable
  public ContentRevision createFileContent(final VcsRevisionNumber vcsRevisionNumber, final VirtualFile virtualFile) {
    if (VcsRevisionNumber.NULL.equals(vcsRevisionNumber)) {
      return null;
    }
    else {
      FilePath path = TfsFileUtil.getFilePath(virtualFile);
      try {
        Pair<WorkspaceInfo, ExtendedItem> workspaceAndItem = TfsUtil.getWorkspaceAndExtendedItem(path);
        if (workspaceAndItem == null || workspaceAndItem.second == null) {
          return null;
        }
        final VcsRevisionNumber.Int intRevisionNumber = (VcsRevisionNumber.Int)vcsRevisionNumber;
        return TFSContentRevision
          .create(myProject, workspaceAndItem.first, intRevisionNumber.getValue(), workspaceAndItem.second.getItemid());
      }
      catch (TfsException e) {
        //noinspection ThrowableInstanceNeverThrown
        AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e), TFSVcs.TFS_NAME);
        return null;
      }
    }
  }

  @Nullable
  public VcsRevisionNumber getCurrentRevision(final VirtualFile virtualFile) {
    return TfsUtil.getCurrentRevisionNumber(TfsFileUtil.getFilePath(virtualFile));
  }

  public ItemLatestState getLastRevision(final FilePath localPath) {
    try {
      Collection<WorkspaceInfo> workspaces = Workstation.getInstance().findWorkspaces(localPath, false);
      if (workspaces.isEmpty()) {
        return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
      }
      final WorkspaceInfo workspace = workspaces.iterator().next();
      final ExtendedItem extendedItem = workspace.getServer().getVCS()
        .getExtendedItem(workspace.getName(), workspace.getOwnerName(), localPath, RecursionType.None, DeletedState.Any);
      if (extendedItem == null) {
        return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
      }
      // there may be several extended items for a given name (see VersionControlServer.chooseExtendedItem())
      // so we need to query item by name
      final Item item = workspace.getServer().getVCS()
        .queryItem(workspace.getName(), workspace.getOwnerName(), extendedItem.getSitem(), LatestVersionSpec.INSTANCE, DeletedState.Any,
                   false);
      if (item != null) {
        VcsRevisionNumber.Int revisionNumber = new VcsRevisionNumber.Int(item.getCs());
        return new ItemLatestState(revisionNumber, item.getDid() == Integer.MIN_VALUE, false);
      }
      else {
        return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
      }
    }
    catch (TfsException e) {
      AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
      return new ItemLatestState(VcsRevisionNumber.NULL, false, false);
    }
  }

  public VcsRevisionNumber getLatestCommittedRevision(VirtualFile vcsRoot) {
    // todo.
    return null;
  }
}
