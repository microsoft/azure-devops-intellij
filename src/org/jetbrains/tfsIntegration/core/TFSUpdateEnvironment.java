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

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.*;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ConflictsEnvironment;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ResolveConflictHelper;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyProgress;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.UpdateSettingsForm;

import java.util.*;

public class TFSUpdateEnvironment implements UpdateEnvironment {
  private final @NotNull TFSVcs myVcs;

  TFSUpdateEnvironment(final @NotNull TFSVcs vcs) {
    myVcs = vcs;
  }

  public void fillGroups(final UpdatedFiles updatedFiles) {
  }

  @NotNull
  public UpdateSession updateDirectories(@NotNull final FilePath[] contentRoots,
                                         final UpdatedFiles updatedFiles,
                                         final ProgressIndicator progressIndicator,
                                         @NotNull final Ref<SequentialUpdatesContext> context) throws ProcessCanceledException {
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    TFSProgressUtil.setProgressText(progressIndicator, "Request update information");
    try {
      final Map<WorkspaceInfo, Collection<Conflict>> workspace2Conflicts = new HashMap<WorkspaceInfo, Collection<Conflict>>();
      List<FilePath> orphanPaths =
        WorkstationHelper.processByWorkspaces(Arrays.asList(contentRoots), true, new WorkstationHelper.VoidProcessDelegate() {
          public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
            VersionSpecBase version = LatestVersionSpec.INSTANCE;
            RecursionType recursionType = RecursionType.Full;
            TFSProjectConfiguration configuration = TFSProjectConfiguration.getInstance(myVcs.getProject());
            if (configuration != null) {
              version = configuration.getUpdateWorkspaceInfo(workspace).getVersion();
              recursionType = configuration.UPDATE_RECURSIVELY ? RecursionType.Full : RecursionType.None;
            }

            // 1. query get operations for contentRoots - to let server know which version we need to report corresponding server conflicts
            List<VersionControlServer.GetRequestParams> requests = new ArrayList<VersionControlServer.GetRequestParams>(paths.size());
            for (ItemPath path : paths) {
              requests.add(new VersionControlServer.GetRequestParams(path.getServerPath(), recursionType, version));
              TFSProgressUtil.checkCanceled(progressIndicator);
            }

            List<GetOperation> operations = workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), requests);
            // execute GetOperation-s, conflicting ones will be skipped
            final Collection<VcsException> applyErrors = ApplyGetOperations
              .execute(myVcs.getProject(), workspace, operations, new ApplyProgress.ProgressIndicatorWrapper(progressIndicator),
                       updatedFiles, ApplyGetOperations.DownloadMode.ALLOW);
            exceptions.addAll(applyErrors);

            Collection<Conflict> conflicts =
              workspace.getServer().getVCS().queryConflicts(workspace.getName(), workspace.getOwnerName(), paths, RecursionType.Full);

            final Collection<Conflict> unresolvedConflicts = ResolveConflictHelper.getUnresolvedConflicts(conflicts);
            if (!unresolvedConflicts.isEmpty()) {
              workspace2Conflicts.put(workspace, unresolvedConflicts);
            }
          }
        });

      if (!workspace2Conflicts.isEmpty()) {
        ResolveConflictHelper resolveConflictHelper = new ResolveConflictHelper(myVcs.getProject(), workspace2Conflicts, updatedFiles);
        ConflictsEnvironment.getConflictsHandler().resolveConflicts(resolveConflictHelper);
      }

      for (FilePath orphanPath : orphanPaths) {
        updatedFiles.getGroupById(FileGroup.UNKNOWN_ID).add(orphanPath.getPresentableUrl(), TFSVcs.getKey(), null);
      }
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      exceptions.add(new VcsException(e));
    }

    // TODO content roots can be renamed while executing
    TfsFileUtil.refreshAndInvalidate(myVcs.getProject(), contentRoots, false);

    return new UpdateSession() {
      @NotNull
      public List<VcsException> getExceptions() {
        return exceptions;
      }

      public void onRefreshFilesCompleted() {
        myVcs.fireRevisionChanged();
      }

      public boolean isCanceled() {
        return false;
      }
    };
  }

  @Nullable
  public Configurable createConfigurable(final Collection<FilePath> files) {
    final Map<WorkspaceInfo, UpdateSettingsForm.WorkspaceSettings> workspacesSettings =
      new HashMap<WorkspaceInfo, UpdateSettingsForm.WorkspaceSettings>();
    final Ref<TfsException> error = new Ref<TfsException>();
    Runnable r = new Runnable() {
      public void run() {
        try {
          WorkstationHelper.processByWorkspaces(files, true, new WorkstationHelper.VoidProcessDelegate() {
            public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
              final Map<FilePath, ExtendedItem> result = workspace.getExtendedItems2(paths);
              Collection<ExtendedItem> items = new ArrayList<ExtendedItem>(result.values());
              for (Iterator<ExtendedItem> i = items.iterator(); i.hasNext();) {
                final ExtendedItem extendedItem = i.next();
                if (extendedItem == null || extendedItem.getSitem() == null) {
                  i.remove();
                }
              }

              if (items.isEmpty()) {
                return;
              }

              // determine common ancestor of all the paths
              ExtendedItem someExtendedItem = items.iterator().next();
              UpdateSettingsForm.WorkspaceSettings workspaceSettings =
                new UpdateSettingsForm.WorkspaceSettings(someExtendedItem.getSitem(), someExtendedItem.getType() == ItemType.Folder);
              for (ExtendedItem extendedItem : items) {
                final String path1 = workspaceSettings.serverPath;
                final String path2 = extendedItem.getSitem();
                if (VersionControlPath.isUnder(path2, path1)) {
                  workspaceSettings = new UpdateSettingsForm.WorkspaceSettings(path2, extendedItem.getType() == ItemType.Folder);
                }
                else if (!VersionControlPath.isUnder(path1, path2)) {
                  workspaceSettings = new UpdateSettingsForm.WorkspaceSettings(VersionControlPath.getCommonAncestor(path1, path2), true);
                }
              }
              workspacesSettings.put(workspace, workspaceSettings);
            }
          });
        }
        catch (TfsException e) {
          error.set(e);
        }
      }
    };

    ProgressManager.getInstance().runProcessWithProgressSynchronously(r, "TFS: preparing for update...", false, myVcs.getProject());

    if (!error.isNull()) {
      //noinspection ThrowableResultOfMethodCallIgnored
      //Messages.showErrorDialog(myVcs.getProject(), error.get().getMessage(), "Update Project");
      return null;
    }
    if (workspacesSettings.isEmpty()) {
      return null;
    }

    return new UpdateConfigurable(myVcs.getProject(), workspacesSettings);
  }

  public boolean validateOptions(final Collection<FilePath> roots) {
    return true;
  }
}
