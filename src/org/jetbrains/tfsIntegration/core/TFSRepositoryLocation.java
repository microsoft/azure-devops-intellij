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

import com.intellij.openapi.vcs.RepositoryLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

public class TFSRepositoryLocation implements RepositoryLocation {

  private final @NotNull WorkspaceInfo myWorkspace;
  private final @NotNull String myServerPath;

  public TFSRepositoryLocation(final @NotNull String serverPath, final @NotNull WorkspaceInfo workspace) {
    myServerPath = serverPath;
    myWorkspace = workspace;
  }

  public String toPresentableString() {
    return getServerPath();
  }

  @NotNull
  public WorkspaceInfo getWorkspace() {
    return myWorkspace;
  }

  @NotNull
  public String getServerPath() {
    return myServerPath;
  }

  public String toString() {
    // IDEA needs this!
    return myServerPath;
  }
}
