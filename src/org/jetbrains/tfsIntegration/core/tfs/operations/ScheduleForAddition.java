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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ScheduleForAddition {

  public static Collection<VcsException> execute(Project project, WorkspaceInfo workspace, List<ItemPath> paths) {
    try {
      ResultWithFailures<GetOperation> serverResults =
        workspace.getServer().getVCS().scheduleForAddition(workspace.getName(), workspace.getOwnerName(), paths);
      for (GetOperation getOp : serverResults.getResult()) {
        String localPath = getOp.getTlocal(); // TODO determine GetOperation local path
        VirtualFile file = VcsUtil.getVirtualFile(localPath);
        TfsFileUtil.invalidateFile(project, file);
      }
      return TfsUtil.getVcsExceptions(serverResults.getFailures());
    }
    catch (TfsException e) {
      return Collections.singletonList(new VcsException(e));
    }

  }

}
