package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorkingFolderInfo {

  public enum Status {
    Active,
    Cloaked
  }

  private @NotNull FilePath myLocalPath;
  private @NotNull String myServerPath;
  private @NotNull Status myStatus;

  public WorkingFolderInfo() {
    this(VcsUtil.getFilePath("")); // TODO can do this?
  }

  public WorkingFolderInfo(final FilePath localPath) {
    this(Status.Active, localPath, "");
  }

  public WorkingFolderInfo(final Status status, final FilePath localPath, final String serverPath) {
    myStatus = status;
    myLocalPath = localPath;
    myServerPath = serverPath;
  }

  @NotNull
  public FilePath getLocalPath() {
    return myLocalPath;
  }

  @NotNull
  public String getServerPath() {
    return myServerPath;
  }

  @NotNull
  public Status getStatus() {
    return myStatus;
  }

  public void setStatus(final @NotNull Status status) {
    myStatus = status;
  }

  public void setServerPath(final @NotNull String serverPath) {
    myServerPath = serverPath;
  }

  public void setLocalPath(final @NotNull FilePath localPath) {
    myLocalPath = localPath;
  }

  public WorkingFolderInfo getCopy() {
    WorkingFolderInfo copy = new WorkingFolderInfo(myStatus, myLocalPath, myServerPath);
    return copy;
  }

  @Nullable
  public String getServerPathByLocalPath(final @NotNull FilePath localPath) {
    if (getServerPath().length() > 0 && localPath.isUnder(getLocalPath(), false)) {
      String localPathString = FileUtil.toSystemIndependentName(localPath.getPath());
      String thisPathString = FileUtil.toSystemIndependentName(getLocalPath().getPath());
      String remainder = localPathString.substring(thisPathString.length());
      return getServerPath() + remainder;
    }
    return null;
  }

  @Nullable
  public FilePath getLocalPathByServerPath(final @NotNull String serverPath) {
    if (getServerPath().length() > 0 && serverPath.startsWith(getServerPath())) {
      String remainder = serverPath.substring(getServerPath().length());
      return VcsUtil.getFilePath(getLocalPath().getPath() + remainder);
    }
    return null;
  }
}

