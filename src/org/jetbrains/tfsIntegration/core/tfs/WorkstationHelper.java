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

package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.*;

// TODO: rename this class
public class WorkstationHelper {

  private WorkstationHelper() {
  }

  public interface VoidProcessDelegate {
    void executeRequest(WorkspaceInfo workspace, List<ItemPath> paths) throws TfsException;
  }

  /**
   * @param localPaths paths of local items
   * @param processor  operation processor
   * @return local paths for which workspace was not found (orphan paths)
   * @throws TfsException in case error occurs
   */
  // TODO process orphan paths in every caller
  public static List<FilePath> processByWorkspaces(Collection<FilePath> localPaths,
                                                    boolean considerChildMappings,
                                                    VoidProcessDelegate processor) throws TfsException {
    List<FilePath> orphanPaths = new ArrayList<FilePath>();
    Map<WorkspaceInfo, List<FilePath>> workspace2localPaths = new HashMap<WorkspaceInfo, List<FilePath>>();
    for (FilePath localPath : localPaths) {
      Collection<WorkspaceInfo> workspaces = Workstation.getInstance().findWorkspace(localPath, considerChildMappings);
      if (!workspaces.isEmpty()) {
        for (WorkspaceInfo workspace : workspaces) {
          List<FilePath> workspaceLocalPaths = workspace2localPaths.get(workspace);
          if (workspaceLocalPaths == null) {
            workspaceLocalPaths = new ArrayList<FilePath>();
            workspace2localPaths.put(workspace, workspaceLocalPaths);
          }
          workspaceLocalPaths.add(localPath);
        }
      }
      else {
        orphanPaths.add(localPath);
      }
    }

    for (WorkspaceInfo workspace : workspace2localPaths.keySet()) {
      List<FilePath> currentLocalPaths = workspace2localPaths.get(workspace);
      List<ItemPath> currentItemPaths = new ArrayList<ItemPath>(currentLocalPaths.size());
      for (FilePath localPath : currentLocalPaths) {
        Collection<String> serverPaths = workspace.findServerPathsByLocalPath(localPath, considerChildMappings);
        if (!considerChildMappings) {
          // optimization + actual isDirectory flag
          currentItemPaths.add(new ItemPath(localPath, serverPaths.iterator().next()));
        }
        else {
          for (String serverPath : serverPaths) {
            // isDirectory = true since (child) mappings can be set for folders, not for files
            //noinspection ConstantConditions
            currentItemPaths.add(new ItemPath(workspace.findLocalPathByServerPath(serverPath, true), serverPath));
          }
        }
      }
      processor.executeRequest(workspace, currentItemPaths);
    }
    return orphanPaths;
  }

}
