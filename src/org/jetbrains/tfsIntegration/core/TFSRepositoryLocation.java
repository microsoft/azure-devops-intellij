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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RepositoryLocation;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TFSRepositoryLocation implements RepositoryLocation {

  private final Map<WorkspaceInfo, List<FilePath>> myPathsByWorkspaces;

  public TFSRepositoryLocation(final Map<WorkspaceInfo, List<FilePath>> pathsByWorkspaces) {
    myPathsByWorkspaces = pathsByWorkspaces;
  }

  public Map<WorkspaceInfo, List<FilePath>> getPathsByWorkspaces() {
    return myPathsByWorkspaces;
  }

  public String getKey() {
    return toString();
  }

  public String toPresentableString() {
    if (myPathsByWorkspaces.size() == 1 && myPathsByWorkspaces.values().iterator().next().size() == 1) {
      return myPathsByWorkspaces.values().iterator().next().iterator().next().getPresentableUrl();
    }
    else {
      return "Multiple paths";
    }
  }

  public String toString() {
    // IDEA needs this!
    StringBuilder s = new StringBuilder();
    for (Collection<FilePath> paths : myPathsByWorkspaces.values()) {
      for (FilePath path : paths) {
        s.append(path.getPath());
      }
    }
    return s.toString();
  }
}
