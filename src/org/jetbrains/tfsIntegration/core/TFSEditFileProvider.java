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

import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TFSEditFileProvider implements EditFileProvider {

  public void editFiles(final VirtualFile[] files) throws VcsException {
    // TODO handle orphan paths

    Collection<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.ProcessResult<ResultWithFailures<GetOperation>> processResult = WorkstationHelper
        .processByWorkspaces(TfsFileUtil.getFilePaths(files), false, new WorkstationHelper.ProcessDelegate<ResultWithFailures<GetOperation>>() {
          public ResultWithFailures<GetOperation> executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths)
            throws TfsException {
            return workspace.getServer().getVCS().checkoutForEdit(workspace.getName(), workspace.getOwnerName(), paths);
          }
        });

      if (processResult.results != null) {
        errors.addAll(BeanHelper.getVcsExceptions(processResult.results.getFailures()));

        for (GetOperation getOp : processResult.results.getResult()) {
          String localPath = getOp.getSlocal(); // TODO determine GetOperation local path
          VirtualFile file = VcsUtil.getVirtualFile(localPath);
          if (file != null && file.isValid() && !file.isDirectory()) {
            TfsFileUtil.setReadOnlyInEventDispathThread(file, false);
          }
        }
      }
    }
    catch (TfsException e) {
      throw new VcsException(e);
    }
    if (!errors.isEmpty()) {
      throw TfsUtil.collectExceptions(errors);
    }
  }

  public String getRequestText() {
    return "Would you like to invoke 'CheckOut' command?";

  }
}
