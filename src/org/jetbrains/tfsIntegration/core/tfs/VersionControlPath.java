package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.vcs.FilePath;

/**
 * Created by IntelliJ IDEA.
 * Date: 05.02.2008
 * Time: 1:26:39
 */
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
    return localPath.getPath().replace(PATH_SEPARATOR_CHAR, '\\'); // TODO need this? .replaceAll("[/]*$", "");
  }

}
