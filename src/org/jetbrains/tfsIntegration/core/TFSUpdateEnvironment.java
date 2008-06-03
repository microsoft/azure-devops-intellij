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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyUpdateGetOperations;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;
import org.jetbrains.tfsIntegration.ui.UpdatePanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TFSUpdateEnvironment implements UpdateEnvironment {
  private final TFSVcs myVcs;

  TFSUpdateEnvironment(final TFSVcs tfsVcs) {
    myVcs = tfsVcs;
  }

  public void fillGroups(final UpdatedFiles updatedFiles) {
  }

  @NotNull
  public UpdateSession updateDirectories(@NotNull final FilePath[] contentRoots,
                                         final UpdatedFiles updatedFiles,
                                         final ProgressIndicator progressIndicator) throws ProcessCanceledException {

    final List<VcsException> exceptions = new ArrayList<VcsException>();

    try {
      List<FilePath> orphanPaths =
        WorkstationHelper.processByWorkspaces(Arrays.asList(contentRoots), new WorkstationHelper.VoidProcessDelegate() {
          public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
            VersionSpecBase version = LatestVersionSpec.INSTANCE;
            RecursionType recursionType = RecursionType.Full;
            TFSProjectConfiguration configuration = TFSProjectConfiguration.getInstance(myVcs.getProject());
            if (configuration != null) {
              version = configuration.getUpdateWorkspaceInfo(workspace).getVersion();
              recursionType = configuration.UPDATE_RECURSIVELY ? RecursionType.Full : RecursionType.None;
            }
            TFSProgressUtil.setProgressText(progressIndicator, "Request update information");

            List<VersionControlServer.GetRequestParams> requests = new ArrayList<VersionControlServer.GetRequestParams>(paths.size());
            for (ItemPath path : paths) {
              requests.add(new VersionControlServer.GetRequestParams(path.getServerPath(), recursionType, version));
              TFSProgressUtil.checkCanceled(progressIndicator);
            }
            // query get operations for contentRoots
            List<List<GetOperation>> result = workspace.getServer().getVCS().get(workspace.getName(), workspace.getOwnerName(), requests);

            List<GetOperation> getOperations = new ArrayList<GetOperation>();
            for (List<GetOperation> list : result) {
              getOperations.addAll(list);
            }

            final Collection<VcsException> applyErrors = ApplyUpdateGetOperations
              .execute(workspace, getOperations, progressIndicator, updatedFiles, ApplyUpdateGetOperations.ConflictAction.AddLocalConflict,
                       false);
            exceptions.addAll(applyErrors);
          }
        });
      for (FilePath orphanPath : orphanPaths) {
        updatedFiles.getGroupById(FileGroup.UNKNOWN_ID).add(FileUtil.toSystemIndependentName(orphanPath.getPath()));
      }
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      exceptions.add(new VcsException("Update failed", e));
    }

    return new TFSUpdateSession(myVcs, contentRoots, exceptions, updatedFiles);
  }

  @Nullable
  public Configurable createConfigurable(final Collection<FilePath> files) {
    if (files.isEmpty()) {
      return null;
    }

    return new UpdateConfigurable(myVcs.getProject()) {
      public String getDisplayName() {
        return "Update";
      }

      protected AbstractUpdatePanel createPanel() {
        return new UpdatePanel(myVcs, files);
      }
    };

  }
}
