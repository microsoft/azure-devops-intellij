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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.history.*;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.WorkspaceVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Changeset;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.DeletedState;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;

import java.util.ArrayList;
import java.util.List;

public class TFSHistoryProvider implements VcsHistoryProvider {
  private @NotNull final Project myProject;

  public TFSHistoryProvider(@NotNull Project project) {
    myProject = project;
  }

  public ColumnInfo[] getRevisionColumns() {
    return ColumnInfo.EMPTY_ARRAY;
  }

  public AnAction[] getAdditionalActions(final FileHistoryPanel panel) {
    return new AnAction[0];
  }

  public boolean isDateOmittable() {
    return false;
  }

  @Nullable
  @NonNls
  public String getHelpId() {
    return null;
  }

  @Nullable
  public VcsHistorySession createSessionFor(final FilePath filePath) throws VcsException {
    final FilePath committedPath = ChangesUtil.getCommittedPath(myProject, filePath);
    try {
      WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(committedPath);
      if (workspace == null) {
        return null;
      }
      
      final List<VcsFileRevision> revisions = getRevisions(committedPath, workspace);
      if (revisions == null) {
        return null;
      }

      return new VcsHistorySession(revisions) {
        public VcsRevisionNumber calcCurrentRevisionNumber() {
          try {
            WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(committedPath);
            //noinspection ConstantConditions
            String serverPath = workspace.findServerPathByLocalPath(committedPath);
            TFSVcs.assertTrue(serverPath != null);
            ExtendedItem item = workspace.getServer().getVCS()
              .getExtendedItem(workspace.getName(), workspace.getOwnerName(), serverPath, DeletedState.NonDeleted);
            if (item != null) {
              return new VcsRevisionNumber.Int(item.getLver());
            }
          }
          catch (TfsException e) {
            AbstractVcsHelper.getInstance(myProject).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
          }
          return VcsRevisionNumber.NULL;
        }
      };
    }
    catch (TfsException e) {
      throw new VcsException(e.getMessage(), e);
    }
  }

  private static List<VcsFileRevision> getRevisions(final FilePath committedPath, WorkspaceInfo workspace) throws TfsException {
    List<Changeset> changesets = workspace.getServer().getVCS().queryHistory(workspace.getName(), workspace.getOwnerName(), new ItemPath(
      committedPath, workspace.findServerPathByLocalPath(committedPath)), Integer.MIN_VALUE, null, new WorkspaceVersionSpec(
      workspace.getName(), workspace.getOwnerName()), new ChangesetVersionSpec(1), LatestVersionSpec.INSTANCE, Integer.MAX_VALUE,
                                                      RecursionType.None);
    List<VcsFileRevision> revisions = new ArrayList<VcsFileRevision>(changesets.size());
    for (Changeset changeset : changesets) {
      final FilePath localPath = workspace.findLocalPathByServerPath(changeset.getChanges().getChange()[0].getItem().getItem());
      revisions.add(
        new TFSFileRevision(localPath, changeset.getDate().getTime(), changeset.getComment(), changeset.getOwner(), changeset.getCset()));
    }
    return revisions;
  }

  @Nullable
  public HistoryAsTreeProvider getTreeHistoryProvider() {
    return null;
  }
}