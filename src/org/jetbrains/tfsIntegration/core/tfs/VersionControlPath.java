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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;

import java.util.Arrays;

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
    return parent.equals(getCommonAncestor(parent, child));
  }

  public static int compareParentToChild(String path1, String path2) {
    return path1.compareTo(path2);
  }


  public static String getCommonAncestor(final String path1, final String path2) {
    String[] components1 = getPathComponents(path1);
    String[] components2 = getPathComponents(path2);

    int i = 0;
    while (i < Math.min(components1.length, components2.length) && components1[i].equals(components2[i])) {
      i++;
    }
    return StringUtil.join(Arrays.asList(components1).subList(0, i), PATH_SEPARATOR);
  }

  private static String[] getPathComponents(final String serverPath1) {
    return serverPath1.split(PATH_SEPARATOR);
  }

}
