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
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.BeanHelper;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.ResultWithFailures;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class UndoPendingChanges {

  public static class UndoPendingChangesResult {
    public final Collection<VcsException> errors;
    public final Collection<ItemPath> undonePaths;

    public UndoPendingChangesResult(final Collection<ItemPath> undonePaths, final Collection<VcsException> errors) {
      this.undonePaths = undonePaths;
      this.errors = errors;
    }
  }

  public static UndoPendingChangesResult execute(final WorkspaceInfo workspace, final Collection<ItemPath> paths, boolean download) {
    if (paths.isEmpty()) {
      return new UndoPendingChangesResult(Collections.<ItemPath>emptyList(), Collections.<VcsException>emptyList());
    }

    // undo changes
    try {
      ResultWithFailures<GetOperation> result =
        workspace.getServer().getVCS().undoPendingChanges(workspace.getName(), workspace.getOwnerName(), paths);

      Collection<VcsException> errors = new ArrayList<VcsException>();
      errors.addAll(BeanHelper.getVcsExceptions(result.getFailures()));

      Collection<ItemPath> undonePaths = new ArrayList<ItemPath>(result.getResult().size());
      for (GetOperation getOperation : result.getResult()) {
        FilePath undonePath = VcsUtil.getFilePath(getOperation.getTlocal());
        undonePaths.add(new ItemPath(undonePath, workspace.findServerPathByLocalPath(undonePath)));
      }

      Collection<VcsException> postProcessingErrors = ApplyGetOperations.execute(workspace, result.getResult(), download);
      errors.addAll(postProcessingErrors);
      return new UndoPendingChangesResult(undonePaths, errors);
    }
    catch (TfsException e) {
      return new UndoPendingChangesResult(Collections.<ItemPath>emptyList(), Collections.singletonList(new VcsException(e)));
    }
  }


}
