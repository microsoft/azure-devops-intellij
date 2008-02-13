package org.jetbrains.tfsIntegration.core.tfs;

public class WorkingFolderInfo {

  public enum Status {
    Active,
    Cloaked
  }

  private String myLocalPath;
  private String myServerPath;
  private Status myStatus;

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

  public String getLocalPath() {
    return myLocalPath;
  }

  public String getServerPath() {
    return myServerPath;
  }

  public Status getStatus() {
    return myStatus;
  }

  public void setStatus(final Status status) {
    myStatus = status;
  }

  public void setServerPath(final String serverPath) {
    myServerPath = serverPath;
  }

  public void setLocalPath(final String localPath) {
    myLocalPath = localPath;
  }

}
