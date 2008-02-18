package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA.
 * Date: 05.02.2008
 * Time: 1:26:39
 */
public class VersionControlPath {
  public static final String PATH_SEPARATOR = "/";
  public static final String ROOT_FOLDER = "$/";

  public static boolean isServerItem(final String item) {
    return item.startsWith(ROOT_FOLDER);
  }

  public static String getNormalizedPath(final @NotNull String localPath) {
    return localPath.replaceAll("[\\\\]", PATH_SEPARATOR).replaceAll("[/]*$", "");
  }

}
