/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.WorkspaceNotFoundException;
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

  // TODO replace with getPresentableUri() where needed
  public URI getUri() {
    return myUri;
  }

  String getGuid() {
    return myGuid;
  }

  @Nullable
  public String getQualifiedUsername() {
    Credentials credentials = TFSConfigurationManager.getInstance().getCredentials(getUri());
    return credentials != null ? credentials.getQualifiedUsername() : null;
  }

  public List<WorkspaceInfo> getWorkspacesForCurrentOwnerAndComputer() {
    List<WorkspaceInfo> result = new ArrayList<WorkspaceInfo>();
    final List<WorkspaceInfo> workspaces = getWorkspaces();
    for (WorkspaceInfo workspaceInfo : workspaces) {
      if (workspaceInfo.hasCurrentOwnerAndComputer()) {
        result.add(workspaceInfo);
      }
    }
    return Collections.unmodifiableList(result);
  }

  public List<WorkspaceInfo> getWorkspaces() {
    return Collections.unmodifiableList(myWorkspaceInfos);
  }

  public void deleteWorkspace(WorkspaceInfo workspaceInfo) throws TfsException {
    try {
      getVCS().deleteWorkspace(workspaceInfo.getName(), workspaceInfo.getOwnerName());
    }
    catch (WorkspaceNotFoundException e) {
      // already deleted
    }
    myWorkspaceInfos.remove(workspaceInfo);
    Workstation.getInstance().update();
  }

  @NotNull
  public VersionControlServer getVCS() {
    if (myServer == null) {
      myServer = new VersionControlServer(myUri);
    }
    return myServer;
  }

  public void refreshWorkspacesForCurrentOwner() throws TfsException {
    String owner = getQualifiedUsername();
    if (owner != null) {
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
      Workstation.getInstance().update();
    }
  }

  public void replaceWorkspace(final @NotNull WorkspaceInfo existingWorkspace, final @NotNull WorkspaceInfo newWorkspace) {
    myWorkspaceInfos.set(myWorkspaceInfos.indexOf(existingWorkspace), newWorkspace);
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    return "ServerInfo[uri=" + getUri() + ",guid=" + getGuid() + "," + getWorkspaces().size() + " workspaces]";
  }


}
