package org.jetbrains.tfsIntegration.ui.checkoutwizard;

import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

public class CheckoutWizardModel {

  public enum Mode {
    Auto, Manual
  }

  private ServerInfo myServer;

  private Mode myMode = Mode.Auto;

  private WorkspaceInfo myWorkspace;

  private String myNewWorkspaceName;

  private String myServerPath;

  private String myDestinationFolder;


  public ServerInfo getServer() {
    return myServer;
  }

  public Mode getMode() {
    return myMode;
  }

  public WorkspaceInfo getWorkspace() {
    return myWorkspace;
  }

  public String getNewWorkspaceName() {
    return myNewWorkspaceName;
  }

  public String getServerPath() {
    return myServerPath;
  }

  public String getDestinationFolder() {
    return myDestinationFolder;
  }

  public void setServer(ServerInfo server) {
    if (myServer != null && !myServer.getUri().equals(server.getUri())) {
      myWorkspace = null;
      myServerPath = null;
    }
    myServer = server;
  }

  public void setMode(Mode mode) {
    myMode = mode;
  }

  public void setWorkspace(WorkspaceInfo workspace) {
    if (myMode != Mode.Manual) {
      throw new IllegalStateException("Attempt to set workspace in Auto mode");
    }
    myWorkspace = workspace;
  }

  public void setNewWorkspaceName(String newWorkspaceName) {
    //if (myMode != Mode.Auto) {
    //  throw new IllegalStateException("Attempt to set new workspace name in Manual mode");
    //}
    myNewWorkspaceName = newWorkspaceName;
  }

  public void setServerPath(String serverPath) {
    myServerPath = serverPath;
  }

  public void setDestinationFolder(final String destinationFolder) {
    if (myMode != Mode.Auto) {
      throw new IllegalStateException("Attempt to set destination path in Manual mode");
    }
    myDestinationFolder = destinationFolder;
  }

  public boolean isComplete() {
    if (getServer() == null) {
      return false;
    }
    if (getMode() == Mode.Auto) {
      if (getNewWorkspaceName() == null || getNewWorkspaceName().length() == 0) {
        return false;
      }
      if (getDestinationFolder() == null) {
        return false;
      }
    }
    else {
      if (getWorkspace() == null) {
        return false;
      }
    }
    return true;
  }

}
