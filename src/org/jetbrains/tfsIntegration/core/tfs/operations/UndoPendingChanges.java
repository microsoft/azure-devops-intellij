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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.project.Project;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.BeanHelper;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.ResultWithFailures;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.util.*;

public class UndoPendingChanges {

  public static class UndoPendingChangesResult {
    public final Collection<VcsException> errors;
    public final Map<ItemPath, ItemPath> undonePaths;

    public UndoPendingChangesResult(final Map<ItemPath, ItemPath> undonePaths, final Collection<VcsException> errors) {
      this.undonePaths = undonePaths;
      this.errors = errors;
    }
  }

  public static UndoPendingChangesResult execute(final Project project,
                                                 final WorkspaceInfo workspace,
                                                 final Collection<ItemPath> paths,
                                                 ApplyGetOperations.DownloadMode downloadMode) {
    if (paths.isEmpty()) {
      return new UndoPendingChangesResult(Collections.<ItemPath, ItemPath>emptyMap(), Collections.<VcsException>emptyList());
    }

    // undo changes
    try {
      ResultWithFailures<GetOperation> result =
        workspace.getServer().getVCS().undoPendingChanges(workspace.getName(), workspace.getOwnerName(), paths);

      Collection<VcsException> errors = new ArrayList<VcsException>();
      errors.addAll(BeanHelper.getVcsExceptions(result.getFailures()));

      Map<ItemPath, ItemPath> undonePaths = new HashMap<ItemPath, ItemPath>();
      for (GetOperation getOperation : result.getResult()) {
        if (getOperation.getSlocal() != null && getOperation.getTlocal() != null) {
          FilePath sourcePath = VcsUtil.getFilePath(getOperation.getSlocal());
          FilePath targetPath = VcsUtil.getFilePath(getOperation.getTlocal());
          undonePaths.put(new ItemPath(sourcePath, workspace.findServerPathsByLocalPath(sourcePath, false).iterator().next()),
                          new ItemPath(targetPath, workspace.findServerPathsByLocalPath(targetPath, false).iterator().next()));
        }
      }

      final Collection<VcsException> applyingErrors =
        ApplyGetOperations.execute(project, workspace, result.getResult(), null, null, downloadMode, ApplyGetOperations.ProcessMode.UNDO);
      errors.addAll(applyingErrors);
      return new UndoPendingChangesResult(undonePaths, errors);
    }
    catch (TfsException e) {
      return new UndoPendingChangesResult(Collections.<ItemPath, ItemPath>emptyMap(), Collections.singletonList(new VcsException(e)));
    }
  }


}
