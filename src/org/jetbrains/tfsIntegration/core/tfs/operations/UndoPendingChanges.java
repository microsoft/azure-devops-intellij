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

package org.jetbrains.tfsIntegration.core.tfs.operations;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Failure;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.GetOperation;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ItemType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.*;

public class UndoPendingChanges {

  @NonNls private static final String ITEM_NOT_CHECKED_OUT_FAILURE = "ItemNotCheckedOutException";

  public static class UndoPendingChangesResult {
    public final Collection<VcsException> errors;

    // TODO only local paths are used actually
    public final Map<ItemPath, ItemPath> undonePaths;

    public UndoPendingChangesResult(final Map<ItemPath, ItemPath> undonePaths, final Collection<VcsException> errors) {
      this.undonePaths = undonePaths;
      this.errors = errors;
    }
  }

  public static UndoPendingChangesResult execute(final Project project,
                                                 final WorkspaceInfo workspace,
                                                 final Collection<String> serverPaths,
                                                 final boolean forbidDownload,
                                                 @NotNull ApplyProgress progress,
                                                 boolean tolerateNoChangesFailure) {
    if (serverPaths.isEmpty()) {
      return new UndoPendingChangesResult(Collections.<ItemPath, ItemPath>emptyMap(), Collections.<VcsException>emptyList());
    }

    // undo changes
    try {
      ResultWithFailures<GetOperation> result =
        workspace.getServer().getVCS()
          .undoPendingChanges(workspace.getName(), workspace.getOwnerName(), serverPaths, project, TFSBundle.message("reverting"));

      Collection<Failure> failures = result.getFailures();
      if (tolerateNoChangesFailure) {
        for (Iterator<Failure> i = failures.iterator(); i.hasNext();) {
          if (ITEM_NOT_CHECKED_OUT_FAILURE.equals(i.next().getCode())) {
            i.remove();
          }
        }
      }

      Collection<VcsException> errors = new ArrayList<VcsException>();
      errors.addAll(TfsUtil.getVcsExceptions(failures));

      // TODO fill renamed paths map in ApplyGetOperations
      Map<ItemPath, ItemPath> undonePaths = new HashMap<ItemPath, ItemPath>();
      for (GetOperation getOperation : result.getResult()) {
        if (getOperation.getSlocal() != null && getOperation.getTlocal() != null) {
          @SuppressWarnings({"ConstantConditions"})
          @NotNull FilePath sourcePath =
            VersionControlPath.getFilePath(getOperation.getSlocal(), getOperation.getType() == ItemType.Folder);
          @SuppressWarnings({"ConstantConditions"})
          @NotNull FilePath targetPath =
            VersionControlPath.getFilePath(getOperation.getTlocal(), getOperation.getType() == ItemType.Folder);
          undonePaths.put(new ItemPath(sourcePath, workspace.findServerPathsByLocalPath(sourcePath, false).iterator().next()),
                          new ItemPath(targetPath, workspace.findServerPathsByLocalPath(targetPath, false).iterator().next()));
        }
      }


      final ApplyGetOperations.DownloadMode downloadMode =
        forbidDownload ? ApplyGetOperations.DownloadMode.FORBID : ApplyGetOperations.DownloadMode.FORCE;
      final Collection<VcsException> applyingErrors =
        ApplyGetOperations.execute(project, workspace, result.getResult(), progress, null, downloadMode);
      errors.addAll(applyingErrors);
      return new UndoPendingChangesResult(undonePaths, errors);
    }
    catch (TfsException e) {
      return new UndoPendingChangesResult(Collections.<ItemPath, ItemPath>emptyMap(), Collections.singletonList(new VcsException(e)));
    }
  }


}
