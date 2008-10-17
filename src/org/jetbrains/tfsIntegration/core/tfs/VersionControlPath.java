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

  public static boolean isUnder(String parent, String child) {
    return child.startsWith(parent);
  }

  public static int compareParentToChild(String path1, String path2) {
    return path1.compareTo(path2);
  }

  public static String getCommonAncestor(final String path1, final String path2) {
    String[] path1components = getPathComponents(path1);
    String[] path2components = getPathComponents(path2);

    int lastEqualIdx = 0;
    while (lastEqualIdx < Math.min(path1components.length, path2components.length) &&
           path1components[lastEqualIdx].equals(path2components[lastEqualIdx])) {
      lastEqualIdx++;
    }

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < lastEqualIdx; i++) {
      if (result.length() > 0) {
        result.append(PATH_SEPARATOR);
      }
      result.append(path1components[i]);
    }
    return result.toString();
  }

  private static String[] getPathComponents(final String serverPath1) {
    return serverPath1.split(PATH_SEPARATOR);
  }

}
