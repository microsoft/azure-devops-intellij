package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NonNls;

import java.io.File;

public final class TFSConstants {

  @NonNls
  private static final String CONFIG_FOLDER_NAME = "tfsIntegration";

  public static File getConfigFolder() {
    return new File(PathManager.getPluginsPath(), TFSConstants.CONFIG_FOLDER_NAME);
  }

}
