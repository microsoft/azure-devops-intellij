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

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ItemPath itemPath = (ItemPath)o;

    if (myLocalPath != null ? !myLocalPath.equals(itemPath.myLocalPath) : itemPath.myLocalPath != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myLocalPath != null ? myLocalPath.hashCode() : 0);
    return result;
  }

  public String toString() {
    return "local: " + getLocalPath() + ", server: " + getServerPath();
  }
}
