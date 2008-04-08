package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NonNls;

import java.io.File;

public final class TFSConstants {

  @NonNls private static final String CONFIG_FOLDER_NAME = "tfsIntegration";

  @NonNls public static final String VERSION_CONTROL_ASMX = "VersionControl/v1.0/repository.asmx";
  @NonNls public static final String SERVER_STATUS_ASMX = "Services/v1.0/ServerStatus.asmx";
  @NonNls public static final String REGISTRATION_ASMX = "Services/v1.0/Registration.asmx";
  @NonNls public static final String DOWNLOAD_ASMX = "VersionControl/v1.0/item.asmx";
  @NonNls public static final String UPLOAD_ASMX = "VersionControl/v1.0/upload.asmx";


  public static File getConfigFolder() {
    return new File(PathManager.getPluginsPath(), TFSConstants.CONFIG_FOLDER_NAME);
  }

}
