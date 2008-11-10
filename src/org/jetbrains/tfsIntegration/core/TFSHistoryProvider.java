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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.*;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Changeset;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;

import java.util.ArrayList;
import java.util.List;

public class TFSHistoryProvider implements VcsHistoryProvider {
  private @NotNull final Project myProject;

  public TFSHistoryProvider(@NotNull Project project) {
    myProject = project;
  }

  public ColumnInfo[] getRevisionColumns(final VcsHistorySession session) {
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
    try {
      final Pair<WorkspaceInfo, ExtendedItem> workspaceAndItem = TfsUtil.getWorkspaceAndExtendedItem(filePath);
      if (workspaceAndItem == null || workspaceAndItem.second == null) {
        return null;
      }

      final List<VcsFileRevision> revisions =
        getRevisions(myProject, workspaceAndItem.second.getSitem(), filePath.isDirectory(), workspaceAndItem.first,
                     LatestVersionSpec.INSTANCE);
      if (revisions.isEmpty()) {
        return null;
      }

      return new VcsHistorySession(revisions) {
        public VcsRevisionNumber calcCurrentRevisionNumber() {
          return TfsUtil.getCurrentRevisionNumber(workspaceAndItem.second);
        }
      };
    }
    catch (TfsException e) {
      throw new VcsException(e);
    }
  }

  public static List<VcsFileRevision> getRevisions(final Project project,
                                                   final String serverPath,
                                                   final boolean isDirectory,
                                                   WorkspaceInfo workspace,
                                                   VersionSpecBase versionTo) throws TfsException {
    List<Changeset> changesets =
      workspace.getServer().getVCS().queryHistory(workspace, serverPath, isDirectory, null, new ChangesetVersionSpec(1), versionTo);

    List<VcsFileRevision> revisions = new ArrayList<VcsFileRevision>(changesets.size());
    for (Changeset changeset : changesets) {
      final Item item = changeset.getChanges().getChange()[0].getItem();
      final FilePath localPath = workspace.findLocalPathByServerPath(item.getItem(), item.getType() == ItemType.Folder);
      revisions.add(
        new TFSFileRevision(project, workspace, localPath, changeset.getDate().getTime(), changeset.getComment(), changeset.getOwner(),
                            changeset.getCset()));
    }
    return revisions;
  }

  @Nullable
  public HistoryAsTreeProvider getTreeHistoryProvider() {
    return null;
  }

  public boolean supportsHistoryForDirectories() {
    return true;
  }

}