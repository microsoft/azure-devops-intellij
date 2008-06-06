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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.credentials.Credentials;
import org.jetbrains.tfsIntegration.core.credentials.CredentialsManager;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;

import java.util.*;

public class WorkspaceInfo {

  private static final Collection<String> WORKSPACE_NAME_INVALID_CHARS =
    Arrays.asList(new String[]{"\"", "/", ":", "<", ">", "|", "*", "?"});

  private static final Collection<String> WORKSPACE_NAME_INVALID_ENDING_CHARS = Arrays.asList(new String[]{" ", "."});

  // TODO: do we need owner name and computer name here?

  private final ServerInfo myServerInfo;
  private final String myOwnerName;
  private final String myComputer;

  private String myOriginalName;
  private String myComment;
  private Calendar myTimestamp;
  private boolean myLoaded;
  private String myModifiedName;

  private List<WorkingFolderInfo> myWorkingFoldersInfos = new ArrayList<WorkingFolderInfo>();

  public WorkspaceInfo(final @NotNull ServerInfo serverInfo, final @NotNull String owner, final @NotNull String computer) {
    myServerInfo = serverInfo;
    myOwnerName = owner;
    myComputer = computer;
    myTimestamp = Calendar.getInstance();
  }

  public WorkspaceInfo(final @NotNull ServerInfo serverInfo,
                       final @NotNull String name,
                       final String owner,
                       final String computer,
                       final String comment,
                       final Calendar timestamp) {
    this(serverInfo, owner, computer);

    myOriginalName = name;
    myComment = comment;
    myTimestamp = timestamp;
  }

  // TODO: make private
  @NotNull
  public ServerInfo getServer() {
    return myServerInfo;
  }

  public String getOwnerName() {
    return myOwnerName;
  }

  public String getComputer() {
    return myComputer;
  }

  public String getName() {
    return myModifiedName != null ? myModifiedName : myOriginalName;
  }

  public void setName(final String name) {
    checkCurrentOwner();
    myModifiedName = name;
  }

  public String getComment() {
    return myComment;
  }

  public void setComment(final String comment) {
    checkCurrentOwner();
    myComment = comment;
  }

  public Calendar getTimestamp() {
    return myTimestamp;
  }

  public void setTimestamp(final Calendar timestamp) {
    checkCurrentOwner();
    myTimestamp = timestamp;
  }

  public List<WorkingFolderInfo> getWorkingFoldersInfos() throws TfsException {
    loadFromServer();
    return Collections.unmodifiableList(myWorkingFoldersInfos);
  }

  public void loadFromServer() throws TfsException {
    if (hasCurrentOwner()) {
      if (myOriginalName != null && !myLoaded) {
        Workspace workspaceBean = getServer().getVCS().getWorkspace(getName(), getOwnerName());
        fromBean(workspaceBean, this);
        myLoaded = true;
      }
    }
  }

  boolean hasCurrentOwner() {
    Credentials credentials = CredentialsManager.getInstance().getCredentials(getServer().getUri());
    return credentials != null && credentials.getQualifiedUsername().equalsIgnoreCase(getOwnerName());
  }

  private void checkCurrentOwner() {
    if (!hasCurrentOwner()) {
      throw new IllegalStateException("Workspace " + getName() + " has other owner");
    }
  }

  /**
   * @param localPath local path to find server path for
   * @return nearest server path according to one of workspace mappings
   * @throws org.jetbrains.tfsIntegration.exceptions.TfsException in case of error during request to TFS
   */
  @Nullable
  public String findServerPathByLocalPath(final @NotNull FilePath localPath) throws TfsException {
    FilePath mappingPath = null;
    String serverPath = null;
    for (WorkingFolderInfo folderInfo : getWorkingFoldersInfos()) {
      String sPath = folderInfo.getServerPathByLocalPath(localPath);
      if (sPath != null && (mappingPath == null || folderInfo.getLocalPath().isUnder(mappingPath, false))) {
        mappingPath = folderInfo.getLocalPath();
        serverPath = sPath;
      }
    }
    return serverPath;
  }

  @Nullable
  public WorkingFolderInfo findNearestMappingByLocalPath(final @NotNull FilePath localPath) throws TfsException {
    WorkingFolderInfo mapping = null;
    for (WorkingFolderInfo folderInfo : getWorkingFoldersInfos()) {
      if (folderInfo.getServerPathByLocalPath(localPath) != null && 
          (mapping == null || folderInfo.getLocalPath().isUnder(mapping.getLocalPath(), false))) {
        mapping = folderInfo;
      }
    }
    return mapping;
  }

  @Nullable
  public FilePath findLocalPathByServerPath(final @NotNull String serverPath) throws TfsException {
    for (WorkingFolderInfo folderInfo : getWorkingFoldersInfos()) {
      FilePath localPath = folderInfo.getLocalPathByServerPath(serverPath);
      if (localPath != null) {
        return localPath;
      }
    }
    return null;
  }

  public boolean isWorkingFolder(final @NotNull FilePath localPath) throws TfsException {
    for (WorkingFolderInfo folderInfo : getWorkingFoldersInfos()) {
      if (folderInfo.getLocalPath().equals(localPath)) {
        return true;
      }
    }
    return false;
  }

  public void addWorkingFolderInfo(final WorkingFolderInfo workingFolderInfo) {
    myWorkingFoldersInfos.add(workingFolderInfo);
  }

  public void removeWorkingFolderInfo(final WorkingFolderInfo folderInfo) {
    checkCurrentOwner();
    myWorkingFoldersInfos.remove(folderInfo);
  }

  public void saveToServer() throws TfsException {
    checkCurrentOwner();
    if (myOriginalName != null) {
      getServer().getVCS().updateWorkspace(myOriginalName, getOwnerName(), toBean(this));
    }
    else {
      // TODO: refactor
      getServer().getVCS().createWorkspace(toBean(this));
      getServer().addWorkspaceInfo(this);
    }
    myOriginalName = getName();
    Workstation.getInstance().updateCacheFile();
  }

  private static Workspace toBean(WorkspaceInfo info) throws TfsException {
    final ArrayOfWorkingFolder folders = new ArrayOfWorkingFolder();
    List<WorkingFolder> foldersList = new ArrayList<WorkingFolder>(info.getWorkingFoldersInfos().size());
    for (WorkingFolderInfo folderInfo : info.getWorkingFoldersInfos()) {
      foldersList.add(toBean(folderInfo));
    }
    folders.setWorkingFolder(foldersList.toArray(new WorkingFolder[foldersList.size()]));

    Workspace bean = new Workspace();
    bean.setComment(info.getComment());
    bean.setComputer(info.getComputer());
    bean.setFolders(folders);
    bean.setLastAccessDate(info.getTimestamp());
    bean.setName(info.getName());
    bean.setOwner(info.getOwnerName());
    return bean;
  }

  @NotNull
  private static WorkingFolder toBean(final WorkingFolderInfo folderInfo) {
    WorkingFolder bean = new WorkingFolder();
    bean.setItem(folderInfo.getServerPath());
    bean.setLocal(VersionControlPath.toTfsRepresentation(folderInfo.getLocalPath()));
    bean.setType(folderInfo.getStatus() == WorkingFolderInfo.Status.Cloaked ? WorkingFolderType.Cloak : WorkingFolderType.Map);
    return bean;
  }

  @Nullable
  private static WorkingFolderInfo fromBean(WorkingFolder bean) {
    WorkingFolderInfo.Status status =
      WorkingFolderType.Cloak.equals(bean.getType()) ? WorkingFolderInfo.Status.Cloaked : WorkingFolderInfo.Status.Active;
    if (bean.getLocal() != null) {
      return new WorkingFolderInfo(status, VcsUtil.getFilePath(bean.getLocal()), bean.getItem());
    }
    else {
      TFSVcs.LOG.info("null local folder mapping for " + bean.getItem());
      return null;
    }
  }

  static void fromBean(Workspace bean, WorkspaceInfo info) {
    info.myOriginalName = bean.getName();
    info.myComment = bean.getComment();
    info.myTimestamp = bean.getLastAccessDate();
    final WorkingFolder[] folders;
    if (bean.getFolders().getWorkingFolder() != null) {
      folders = bean.getFolders().getWorkingFolder();
    }
    else {
      folders = new WorkingFolder[0];
    }
    List<WorkingFolderInfo> workingFoldersInfos = new ArrayList<WorkingFolderInfo>(folders.length);
    for (WorkingFolder folderBean : folders) {
      WorkingFolderInfo folderInfo = fromBean(folderBean);
      if (folderInfo != null) {
        workingFoldersInfos.add(folderInfo);
      }
    }
    info.myWorkingFoldersInfos = workingFoldersInfos;
  }

  public WorkspaceInfo getCopy() {
    WorkspaceInfo copy = new WorkspaceInfo(myServerInfo, myOwnerName, myComputer);
    copy.myComment = myComment;
    copy.myLoaded = myLoaded;
    copy.myOriginalName = myOriginalName;
    copy.myModifiedName = myModifiedName;
    copy.myTimestamp = myTimestamp;

    for (WorkingFolderInfo workingFolder : myWorkingFoldersInfos) {
      copy.myWorkingFoldersInfos.add(workingFolder.getCopy());
    }
    return copy;
  }

  //public ExtendedItem getExtendedItem(final String serverPath) throws TfsException {
  //  return getServer().getVCS().getExtendedItem(getName(), getOwnerName(), serverPath, DeletedState.Any);
  //}

  public Map<ItemPath, ExtendedItem> getExtendedItems(final List<ItemPath> paths) throws TfsException {
    return getServer().getVCS().getExtendedItems(getName(), getOwnerName(), paths, DeletedState.Any);
  }

  public static boolean isValidName(String name) {
    for (String invalid : WORKSPACE_NAME_INVALID_CHARS) {
      if (name.contains(invalid)) {
        return false;
      }
    }
    for (String invalidEnd : WORKSPACE_NAME_INVALID_ENDING_CHARS) {
      if (name.endsWith(invalidEnd)) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    return "WorkspaceInfo[server=" +
           getServer().getUri() +
           ",name=" +
           getName() +
           ",owner=" +
           getOwnerName() +
           ",computer=" +
           getComputer() +
           ",comment=" +
           getComment() +
           "]";
  }

}
