package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.vcs.FilePath;

public class ItemPath {

  private final FilePath myLocalPath;
  private final String myServerPath;

  public ItemPath(FilePath localPath, String serverPath) {
    myLocalPath = localPath;
    myServerPath = serverPath;
  }

  public FilePath getLocalPath() {
    return myLocalPath;
  }

  public String getServerPath() {
    return myServerPath;
  }
}
