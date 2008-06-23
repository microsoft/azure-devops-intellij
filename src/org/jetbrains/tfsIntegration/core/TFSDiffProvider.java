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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.DeletedState;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

public class TFSDiffProvider implements DiffProvider {
  private @NotNull final Project myProject;

  public TFSDiffProvider(@NotNull final Project project) {
    myProject = project;
  }

  @Nullable
  public VcsRevisionNumber getLastRevision(final VirtualFile virtualFile) {
    ExtendedItem item = getExtendedItembyVirtualFile(virtualFile);
    return item != null ? new VcsRevisionNumber.Int(item.getLatest()) : VcsRevisionNumber.NULL;
  }

  @Nullable
  public ContentRevision createFileContent(final VcsRevisionNumber vcsRevisionNumber, final VirtualFile virtualFile) {
    if (VcsRevisionNumber.NULL.equals(vcsRevisionNumber)) {
      return null;
    }
    else {
      FilePath path = VcsUtil.getFilePath(virtualFile.getPath());
      final VcsRevisionNumber.Int intRevisionNumber = (VcsRevisionNumber.Int)vcsRevisionNumber;
      try {
        return new TFSContentRevision(path, intRevisionNumber.getValue());
      }
      catch (TfsException e) {
        //noinspection ThrowableInstanceNeverThrown
        AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
      }
      return null;
    }
  }

  @Nullable
  public VcsRevisionNumber getCurrentRevision(final VirtualFile virtualFile) {
    ExtendedItem item = getExtendedItembyVirtualFile(virtualFile);
    return item != null ? new VcsRevisionNumber.Int(item.getLver()) : VcsRevisionNumber.NULL;
  }

  @Nullable
  private ExtendedItem getExtendedItembyVirtualFile(final VirtualFile virtualFile) {
    FilePath localPath = VcsUtil.getFilePath(virtualFile.getPath());
    try {
      WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(VcsUtil.getFilePath(virtualFile.getPath()));
      if (workspace == null) {
        return null;
      }
      //noinspection ConstantConditions
      String serverPath = workspace.findServerPathByLocalPath(localPath);
      TFSVcs.assertTrue(serverPath != null);
      return workspace.getServer().getVCS()
        .getExtendedItem(workspace.getName(), workspace.getOwnerName(), serverPath, DeletedState.NonDeleted);
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
      return null;
    }
  }
}
