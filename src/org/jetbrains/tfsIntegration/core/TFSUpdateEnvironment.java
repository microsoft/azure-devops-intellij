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
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ConflictsEnvironment;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ResolveConflictHelper;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;

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
              .execute(myVcs.getProject(), workspace, operations, progressIndicator, updatedFiles, ApplyGetOperations.DownloadMode.ALLOW)
              ;
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
        updatedFiles.getGroupById(FileGroup.UNKNOWN_ID).add(orphanPath.getPresentableUrl());
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
    if (files.isEmpty()) {
      return null;
    }
    final Ref<Boolean> mappingFound = new Ref<Boolean>(false);
    try {
      WorkstationHelper.processByWorkspaces(files, true, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          mappingFound.set(true);
        }
      });
    }
    catch (TfsException e) {
      return null;
    }

    return mappingFound.get() ? new UpdateConfigurable(myVcs.getProject(), files) : null;
  }

  public boolean validateOptions(final Collection<FilePath> roots) {
    return true;
  }
}
