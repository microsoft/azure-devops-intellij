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

public class VersionControlPath {
  public static final char PATH_SEPARATOR_CHAR = '/'; // TODO IDEA constant for this
  public static final String PATH_SEPARATOR = "" + PATH_SEPARATOR_CHAR;
  public static final String ROOT_FOLDER = "$/";

  public static boolean isServerItem(final String item) {
    return item.startsWith(ROOT_FOLDER);
  }

  //public static String toServerPath(final @NotNull String localPath) {
  //  return localPath.replaceAll("[\\\\]", PATH_SEPARATOR).replaceAll("[/]*$", "");
  //}

  // TODO consider FileUtil.toSystemDependentPath()

  public static String toTfsRepresentation(FilePath localPath) {
    return toTfsRepresentation(localPath.getPath());
  }

  public static String toTfsRepresentation(String localPath) {
    return localPath.replace(PATH_SEPARATOR_CHAR, '\\'); // TODO need this? .replaceAll("[/]*$", "");
  }

  public static String getPathToProject(final String serverPath) {
    int secondSlashPos = serverPath.indexOf("/", ROOT_FOLDER.length());
    return serverPath.substring(0, secondSlashPos);
  }
}
