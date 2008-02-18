package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.annotations.NotNull;

public class WorkingFolderInfo {

  public enum Status {
    Active,
    Cloaked
  }

  private @NotNull String myLocalPath;
  private @NotNull String myServerPath;
  private @NotNull Status myStatus;

  public WorkingFolderInfo() {
    this("");
  }

  public WorkingFolderInfo(final String localPath) {
    this(Status.Active, localPath, "");
  }

  public WorkingFolderInfo(final Status status, final String localPath, final String serverPath) {
    myStatus = status;
    myLocalPath = localPath;
    myServerPath = serverPath;
  }

  @NotNull
  public String getLocalPath() {
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

  public void setLocalPath(final @NotNull String localPath) {
    myLocalPath = localPath;
  }

}
