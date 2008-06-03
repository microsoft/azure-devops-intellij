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
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;

public class ItemPath {

  private final @NotNull FilePath myLocalPath;
  private final String myServerPath;

  public ItemPath(final @NotNull FilePath localPath, String serverPath) {
    myLocalPath = localPath;
    myServerPath = serverPath;
  }

  public @NotNull FilePath getLocalPath() {
    return myLocalPath;
  }

  public String getServerPath() {
    return myServerPath;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ItemPath itemPath = (ItemPath)o;

    return FileUtil.pathsEqual(getLocalPath().getPath(), itemPath.getLocalPath().getPath());
  }

  public int hashCode() {
    return getLocalPath().hashCode();
  }

  public String toString() {
    return "local: " + getLocalPath() + ", server: " + getServerPath();
  }
}
