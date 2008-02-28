package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.credentials.Credentials;
import org.jetbrains.tfsIntegration.core.credentials.CredentialsManager;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Workspace;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

  public List<WorkspaceInfo> getWorkspacesForCurrentOwner() {
    Credentials credentials = CredentialsManager.getInstance().getCredentials(getUri());
    if (credentials != null) {
      List<WorkspaceInfo> result = new ArrayList<WorkspaceInfo>();
      for (WorkspaceInfo workspaceInfo : getWorkspaces()) {
        if (credentials.getQualifiedUsername().equalsIgnoreCase(workspaceInfo.getOwnerName())) {
          result.add(workspaceInfo);
        }
      }
      return Collections.unmodifiableList(result);
    }
    else {
      return Collections.emptyList();
    }
  }

  public List<WorkspaceInfo> getWorkspaces() {
    return Collections.unmodifiableList(myWorkspaceInfos);
  }

  public void deleteWorkspace(WorkspaceInfo workspaceInfo) throws Exception {
    getVCS().deleteWorkspace(workspaceInfo.getName(), workspaceInfo.getOwnerName());
    myWorkspaceInfos.remove(workspaceInfo);
    Workstation.getInstance().updateCacheFile();
  }

  public VersionControlServer getVCS() throws Exception {
    if (myServer == null) {
      myServer = new VersionControlServer(myUri);
    }
    return myServer;
  }

  public void refreshWorkspacesForCurrentOwner() throws Exception {
    Credentials credentials = CredentialsManager.getInstance().getCredentials(getUri());
    if (credentials != null) {
      String owner = credentials.getQualifiedUsername();
      Workspace[] workspaces = getVCS().queryWorkspaces(owner, Workstation.getComputerName());
      for (Iterator<WorkspaceInfo> i = myWorkspaceInfos.iterator(); i.hasNext();) {
        WorkspaceInfo workspaceInfo = i.next();
        if (workspaceInfo.getOwnerName().equalsIgnoreCase(owner)) {
          i.remove();
        }
      }

      for (Workspace workspace : workspaces) {
        WorkspaceInfo workspaceInfo = new WorkspaceInfo(this, owner, Workstation.getComputerName());
        WorkspaceInfo.fromBean(workspace, workspaceInfo);
        addWorkspaceInfo(workspaceInfo);
      }
      Workstation.getInstance().updateCacheFile();
    }
  }

  public void replaceWorkspace(final @NotNull WorkspaceInfo existingWorkspace, final @NotNull WorkspaceInfo newWorkspace) {
    myWorkspaceInfos.add(myWorkspaceInfos.indexOf(existingWorkspace), newWorkspace);
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    return "ServerInfo[uri=" + getUri() + ",guid=" + getGuid() + "," + getWorkspaces().size() + " workspaces]";
  }


}
