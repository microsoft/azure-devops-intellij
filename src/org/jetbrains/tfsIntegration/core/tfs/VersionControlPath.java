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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;

public class VersionControlPath {
  private static final char PATH_SEPARATOR_CHAR = '/';
  public static final String PATH_SEPARATOR = "" + PATH_SEPARATOR_CHAR;
  public static final String ROOT_FOLDER = "$" + PATH_SEPARATOR;

  public static String toSystemDependent(@NotNull FilePath localPath) {
    return FileUtil.toSystemDependentName(localPath.getPath());
  }

  @Nullable
  public static String toSystemDependent(@Nullable String localPath) {
    return localPath != null ? FileUtil.toSystemDependentName(localPath) : null;
  }

  @Nullable
  public static FilePath getFilePath(@Nullable String localPath, boolean isDirectory) {
    return localPath != null ? VcsUtil.getFilePath(FileUtil.toSystemDependentName(localPath), isDirectory) : null;
  }

  @Nullable
  public static VirtualFile getVirtualFile(@NotNull String localPath) {
    return VcsUtil.getVirtualFile(FileUtil.toSystemDependentName(localPath));
  }

  public static File getFile(String localPath) {
    return new File(FileUtil.toSystemDependentName(localPath));
  }

  public static String getPathToProject(final String serverPath) {
    int secondSlashPos = serverPath.indexOf("/", ROOT_FOLDER.length());
    return serverPath.substring(0, secondSlashPos);
  }

  public static boolean isUnder(String parent, String child) {
    return parent.equals(getCommonAncestor(parent, child));
  }

  /**
   * Not to be used for UI because files and subfolders at one level are compared lexicographically and may be mixed.
   *
   * @see #compareParentToChild(String, boolean, String, boolean)
   */
  public static int compareParentToChild(String path1, String path2) {
    return path1.compareTo(path2);
  }

  /**
   * At the same level files go before subfolders regardless of the names.
   */
  public static int compareParentToChild(String path1, boolean isDirectory1, String path2, boolean isDrectory2) {
    String[] pathComponents1 = getPathComponents(path1);
    String[] pathComponents2 = getPathComponents(path2);

    final int minLength = Math.min(pathComponents1.length, pathComponents2.length);

    // first compare all the levels except last one
    for (int i = 0; i < minLength - 1; i++) {
      String s1 = pathComponents1[i];
      String s2 = pathComponents2[i];
      if (!s1.equals(s2)) {
        return s1.compareTo(s2);
      }
    }

    // compare last level
    if (pathComponents1.length == pathComponents2.length) {
      if (isDirectory1 == isDrectory2) {
        return pathComponents1[pathComponents1.length - 1].compareTo(pathComponents2[pathComponents2.length - 1]);
      }
      else {
        return isDirectory1 ? 1 : -1;
      }
    }
    else {
      if (pathComponents1.length == minLength && !isDirectory1) {
        return -1;
      }
      else if (pathComponents2.length == minLength && !isDrectory2) {
        return 1;
      }
      else {
        if (pathComponents1[minLength - 1].equals(pathComponents2[minLength - 1])) {
          return pathComponents1.length - pathComponents2.length;
        }
        else {
          return pathComponents1[minLength - 1].compareTo(pathComponents2[minLength - 1]);
        }
      }
    }
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

  public static String getLastComponent(final String serverPath) {
    return serverPath.substring(serverPath.lastIndexOf(PATH_SEPARATOR) + 1);
  }

  public static String[] getPathComponents(final String serverPath) {
    return serverPath.split(PATH_SEPARATOR);
  }

}
