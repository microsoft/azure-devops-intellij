package org.jetbrains.tfsIntegration.core.tfs;

import org.apache.axis2.AxisFault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.credentials.CredentialsManager;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerInfo {

  private final URI myUri;
  private final String myGuid;
  private VersionControlServer myServer;

  private List<WorkspaceInfo> myWorkspaceInfos = new ArrayList<WorkspaceInfo>();

  public ServerInfo(URI uri, String guid) {
    myUri = uri;
    myGuid = guid;
  }

  public void addWorkspaceInfo(final @NotNull WorkspaceInfo workspaceInfo) {
    myWorkspaceInfos.add(workspaceInfo);
  }

  public URI getUri() {
    return myUri;
  }

  public String getGuid() {
    return myGuid;
  }

  public String getUsername() {
    return CredentialsManager.getInstance().getCredentials(getUri()).getUserName();
  }

  public List<WorkspaceInfo> getWorkspaceInfos() {
    return Collections.unmodifiableList(myWorkspaceInfos);
  }

  public WorkspaceInfo getWorkspace(final @NotNull String workspaceName) {
    for (WorkspaceInfo workspaceInfo : getWorkspaceInfos()) {
      if (workspaceInfo.getName().equals(workspaceName)) {
        return workspaceInfo;
      }
    }
    return null;
  }

  public void deleteWorkspace(WorkspaceInfo workspaceInfo) throws RemoteException {
    getVCS().deleteWorkspace(workspaceInfo.getName(), workspaceInfo.getOwnerName());
    myWorkspaceInfos.remove(workspaceInfo);
    Workstation.getInstance().updateCacheFile();
  }

  //void setWorkspaceInfos(List<WorkspaceInfo> workspaceInfos) {
  //  myWorkspaceInfos = workspaceInfos;
  //}
  //
  public VersionControlServer getVCS() throws AxisFault {
    if (myServer == null) {
      myServer = new VersionControlServer(myUri);
    }
    return myServer;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    return "ServerInfo[uri=" + getUri() + ",guid=" + getGuid() + "," + getWorkspaceInfos().size() + " workspaces]";
  }


}
