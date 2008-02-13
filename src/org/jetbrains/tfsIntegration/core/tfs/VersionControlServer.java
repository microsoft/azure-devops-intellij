package org.jetbrains.tfsIntegration.core.tfs;

import com.microsoft.wsdl.types.Guid;
import org.apache.axis2.AxisFault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;

import java.net.URI;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;


public class VersionControlServer {
  private Repository myRepository;
  private URI myUri;
  private Guid myGuid = new Guid();

  public VersionControlServer(URI uri) throws AxisFault {
    myUri = uri;
    myRepository = new Repository(this, uri);
  }


// ***************************************************
// used by now



  public Workspace getWorkspace(String workspaceName, String workspaceOwner) throws RemoteException {
    return myRepository.QueryWorkspace(workspaceName, workspaceOwner);
  }

  public Workspace updateWorkspace(String oldWorkspaceName, String workspaceOwner, Workspace newWorkspaceDataBean) throws RemoteException {
    return myRepository.UpdateWorkspace(oldWorkspaceName, workspaceOwner, newWorkspaceDataBean);
  }

  public Workspace createWorkspace(Workspace workspaceBean) throws RemoteException {
    return myRepository.CreateWorkspace(workspaceBean);
  }

  public void deleteWorkspace(String workspaceName, String workspaceOwner) throws RemoteException {
    myRepository.DeleteWorkspace(workspaceName, workspaceOwner);
  }


  public List<ItemInfo> getItems(final ItemInfo item, final DeletedState delState, final ItemType type) throws RemoteException {
    ArrayList<ItemInfo> result = new ArrayList<ItemInfo>();
    ArrayOfExtendedItem[] items = getExtendedItems(item.getFullName(), delState, type);
    if (items.length > 0) {
      for (ExtendedItem i : items[0].getExtendedItem()) {
        result.add(new ItemInfo(item.getWorkspace(), i.getSitem()));
      }
    }
    return result;
  }

  
// ***************************************************
// not used by now

  public LabelResult[] createLabel(VersionControlLabel label, LabelItemSpec[] labelSpecs, LabelChildOption childOption)
    throws RemoteException {
    Workspace workspace = getWorkspace(labelSpecs[0].getItemSpec().getItem());
    ArrayOfLabelItemSpec ls = new ArrayOfLabelItemSpec();
    ls.setLabelItemSpec(labelSpecs);
    return myRepository.LabelItem(workspace.getName(), workspace.getOwner(), label, ls, childOption).getLabelItemResult().getLabelResult();
  }

  public LabelResult[] unlabelItem(String labelName, String labelScope, ItemSpec[] itemSpecs, VersionSpec version) throws RemoteException {
    Workspace workspace = getWorkspace(itemSpecs[0].getItem());
    ArrayOfItemSpec is = new ArrayOfItemSpec();
    is.setItemSpec(itemSpecs);
    return myRepository.UnlabelItem(workspace.getName(), workspace.getOwner(), labelName, labelScope, is, version).getUnlabelItemResult()
      .getLabelResult();
  }

  public Workspace createWorkspace(String name, String owner) throws UnknownHostException, RemoteException {
    return createWorkspace(name, owner, null, new WorkingFolder[0], Workstation.getInstance().getComputerName());
  }

  @Nullable
  public ArrayOfBranchRelative[] getBranchHistory(ItemSpec[] itemSpecs, VersionSpec version) throws RemoteException {
    if (itemSpecs.length == 0) {
      return null;
    }
    ArrayOfItemSpec is = new ArrayOfItemSpec();
    is.setItemSpec(itemSpecs);
    Workspace workspace = getWorkspace(itemSpecs[0].getItem());
    return myRepository.QueryBranches(workspace.getName(), workspace.getOwner(), is, version).getArrayOfBranchRelative();
  }

  public Changeset getChangeset(int changesetId) throws RemoteException {
    return getChangeset(changesetId, false, false);
  }

  public Changeset getChangeset(int changesetId, boolean includeChanges, boolean includeDownloadInfo) throws RemoteException {
    return myRepository.QueryChangeset(changesetId, includeChanges, includeDownloadInfo);
  }

  @Nullable
  public Item getItem(int id, int changeSet) throws RemoteException {
    return getItem(id, changeSet, false);
  }

  @Nullable
  public Item getItem(int id, int changeSet, boolean includeDownloadInfo) throws RemoteException {
    int[] ids = new int[1];
    ids[0] = id;

    Item[] items = getItems(ids, changeSet, includeDownloadInfo);
    if (items.length > 0) {
      return items[0];
    }
    return null;
  }

  public Item[] getItems(int[] ids, int changeSet) throws RemoteException {
    return getItems(ids, changeSet, false);
  }

  @Nullable
  public Item getItem(String path) throws RemoteException {
    return getItem(path, VersionSpecBase.getLatest());
  }

  @Nullable
  public Item getItem(String path, VersionSpec versionSpec) throws RemoteException {
    return getItem(path, versionSpec, 0, false);
  }

  @Nullable
  public Item getItem(String path, VersionSpec versionSpec, int deletionId, boolean includeDownloadInfo) throws RemoteException {
    ItemSpec itemSpec = new ItemSpec();
    itemSpec.setRecurse(RecursionType.None);
    itemSpec.setItem(path);
    //itemSpec.setDid(deletionId); // todo: this line is missed in opentfs, deletionId is unused. why ???
    ItemSet itemSet = getItems(itemSpec, versionSpec, DeletedState.NonDeleted, ItemType.Any, includeDownloadInfo);

    Item[] items = new Item[0];
    if (itemSet != null) {
      items = itemSet.getItems().getItem();
    }
    if (items.length > 0) {
      return items[0];
    }
    return null;
  }

  public Item[] getItems(int[] ids, int changeSet, boolean includeDownloadInfo) throws RemoteException {
    ArrayOfInt ia = new ArrayOfInt();
    ia.set_int(ids);
    return myRepository.QueryItemsById(ia, changeSet, includeDownloadInfo).getItem();
  }

  @Nullable
  public ItemSet getItems(String path, RecursionType recursionType) throws RemoteException {
    ItemSpec itemSpec = new ItemSpec();
    itemSpec.setRecurse(recursionType);
    itemSpec.setItem(path);
    return getItems(itemSpec, VersionSpecBase.getLatest(), DeletedState.NonDeleted, ItemType.Any, false);
  }

  @Nullable
  public ItemSet getItems(String path, VersionSpec versionSpec, RecursionType recursionType) throws RemoteException {
    ItemSpec itemSpec = new ItemSpec();
    itemSpec.setRecurse(recursionType);
    itemSpec.setItem(path);
    return getItems(itemSpec, versionSpec, DeletedState.NonDeleted, ItemType.Any, false);
  }

  @Nullable
  public ItemSet getItems(ItemSpec itemSpec,
                          VersionSpec versionSpec,
                          DeletedState deletedState,
                          ItemType itemType,
                          boolean includeDownloadInfo) throws RemoteException {
    ItemSet[] itemSet = getItems(new ItemSpec[]{itemSpec}, versionSpec, deletedState, itemType, includeDownloadInfo);
    if (itemSet != null) {
      return itemSet[0];
    }
    return null;
  }

  @Nullable
  public ItemSet[] getItems(ItemSpec[] itemSpecs,
                            VersionSpec versionSpec,
                            DeletedState deletedState,
                            ItemType itemType,
                            boolean includeDownloadInfo) throws RemoteException {
    if (itemSpecs.length == 0) {
      return null;
    }

    String workspaceName = "";
    String workspaceOwner = "";

    String item = itemSpecs[0].getItem();
    if (!VersionControlPath.isServerItem(item)) {
      WorkspaceInfo info = Workstation.getInstance().findWorkspaceByLocalPath(item);
      if (info != null) {
        workspaceName = info.getName();
        workspaceOwner = info.getOwnerName();
      }
    }
    ArrayOfItemSpec is = new ArrayOfItemSpec();
    is.setItemSpec(itemSpecs);    
    return myRepository.QueryItems(workspaceName, workspaceOwner, is, versionSpec, deletedState, itemType, includeDownloadInfo)
      .getItemSet();
  }

  @Nullable
  public ArrayOfExtendedItem[] getExtendedItems(String path, DeletedState deletedState, ItemType itemType) throws RemoteException {
    ItemSpec is = new ItemSpec();
    is.setItem(path);
    is.setRecurse(RecursionType.OneLevel);
    ArrayOfExtendedItem[] items = getExtendedItems(new ItemSpec[]{is}, deletedState, itemType);
    if (items.length == 0) {
      return null;
    }
    return items;
  }

  public ArrayOfExtendedItem[] getExtendedItems(ItemSpec[] itemSpecs, DeletedState deletedState, ItemType itemType) throws RemoteException {
    ArrayOfItemSpec is = new ArrayOfItemSpec();
    is.setItemSpec(itemSpecs);
    return myRepository.QueryItemsExtended(null, null, is, deletedState, itemType).getArrayOfExtendedItem();
  }

  public int getLatestChangesetId() throws RemoteException {
    RepositoryProperties properties = myRepository.GetRepositoryProperties(new GetRepositoryProperties());
    return properties.getLcset();
  }

  public Enumeration queryHistory(String path,
                                  VersionSpec version,
                                  int deletionId,
                                  RecursionType recursion,
                                  String user,
                                  VersionSpec versionFrom,
                                  VersionSpec versionTo,
                                  int maxCount,
                                  boolean includeChanges,
                                  boolean slotMode) throws RemoteException {
    return queryHistory(path, version, deletionId, recursion, user, versionFrom, versionTo, maxCount, includeChanges, slotMode, false);
  }

  public VersionControlLabel[] queryLabels(String labelName, String labelScope, String owner, boolean includeItems) throws RemoteException {
    return myRepository.QueryLabels(null, null, labelName, labelScope, owner, null, VersionSpecBase.getLatest(), includeItems, false)
      .getVersionControlLabel();
  }

  public VersionControlLabel[] queryLabels(String labelName,
                                           String labelScope,
                                           String owner,
                                           boolean includeItems,
                                           String filterItem,
                                           VersionSpec versionFilterItem) throws RemoteException {
    return myRepository.QueryLabels(null, null, labelName, labelScope, owner, filterItem, versionFilterItem, includeItems, false)
      .getVersionControlLabel();
  }

  public VersionControlLabel[] queryLabels(String labelName,
                                           String labelScope,
                                           String owner,
                                           boolean includeItems,
                                           String filterItem,
                                           VersionSpec versionFilterItem,
                                           boolean generateDownloadUrls) throws RemoteException {
    return myRepository
      .QueryLabels(null, null, labelName, labelScope, owner, filterItem, versionFilterItem, includeItems, generateDownloadUrls)
      .getVersionControlLabel();
  }

  public ItemSecurity[] getPermissions(String[] items, RecursionType recursion) {
    return getPermissions(null, items, recursion);
  }

  public Shelveset[] queryShelvesets(String shelvesetName, String shelvesetOwner) throws RemoteException {
    return myRepository.QueryShelvesets(shelvesetName, shelvesetOwner).getShelveset();
  }

  public Workspace[] queryWorkspaces(String ownerName, String computer) throws RemoteException {
    return myRepository.QueryWorkspaces(ownerName, computer).getWorkspace();
  }

  public Guid getServerGuid() {
    return myGuid;
  }

  Repository getRepository() {
    return myRepository;
  }

  URI getUri() {
    return myUri;
  }

/////////////////////////////////////////////////////////////////////////////////////////////
// TO IMPLEMENT 

  private static String convertToFullPath(final String localPath) {
    // todo: implement convertion from local to full path
    return localPath;
  }

  public Workspace createWorkspace(String name, String owner, String comment, WorkingFolder[] folders, String computer)
    throws RemoteException {
    // todo: we cannot not set referense to 'this' like in opentfs,
    // because our generated calss for WorkspaceInfo does not have such field.
    // In opentfs referense to 'this' allows get Repository from WorkspaceInfo,
    // Maybe we must create TFSWorkspace class with ref to VersionControlServer?
    Workspace w1 = new Workspace();
    w1.setName(name);
    w1.setOwner(owner);
    w1.setComment(comment);
    w1.getFolders().setWorkingFolder(folders);
    w1.setComputer(computer);
    Workspace w2 = myRepository.CreateWorkspace(w1);
    Workstation.getInstance().addWorkspace(getServerGuid(), myUri, w2);
    return w2;
  }

  public Workspace getWorkspace(WorkspaceInfo info) {
    if (info == null) {
      throw new IllegalArgumentException("workspaceInfo cannot be null!");
    }
    // todo: we cannot not set referense to 'this' like in opentfs,
    // because our generated calss for WorkspaceInfo does not have such field.
    // In opentfs referense to 'this' allows get Repository from WorkspaceInfo,
    // Maybe we must create TFSWorkspace class with ref to VersionControlServer?
    Workspace w = new Workspace();
    w.setName(info.getName());
    w.setOwner(info.getOwnerName());
    w.setComment(info.getComment());
    w.getFolders().setWorkingFolder(new WorkingFolder[0]);
    w.setComputer(info.getComputer());
    return w;
  }

  public Workspace getWorkspace(String localPath) {
    String path = convertToFullPath(localPath);

    WorkspaceInfo info = Workstation.getInstance().findWorkspaceByLocalPath(path);
    if (info == null) {
      throw new IllegalArgumentException("Item not mapped " + path);
    }

    Workspace w = new Workspace();
    // todo: we cannot not set referense to 'this' like in opentfs,
    // because our generated calss for WorkspaceInfo does not have such field.
    // In opentfs referense to 'this' allows get Repository from WorkspaceInfo,
    // Maybe we must create TFSWorkspace class with ref to VersionControlServer?
    w.setName(info.getName());
    w.setOwner(info.getOwnerName());
    w.setComment(info.getComment());
    w.getFolders().setWorkingFolder(new WorkingFolder[0]);
    w.setComputer(Workstation.getInstance().getComputerName());
    return w;
  }


  public ItemSecurity[] getPermissions(String[] identityNames, String[] items, RecursionType recursion) {
    return new ItemSecurity[0];

    // todo: our stubs differ from opentf' ones. have to find out difference
    //return myRepository.QueryItemPermissions(identityNames, items, recursion);
  }

  public Enumeration queryHistory(String path,
                                  VersionSpec version,
                                  int deletionId,
                                  RecursionType recursion,
                                  String user,
                                  VersionSpec versionFrom,
                                  VersionSpec versionToOrig,
                                  int maxCount,
                                  boolean includeChanges,
                                  boolean slotMode,
                                  boolean includeDownloadInfo) throws RemoteException {
    ItemSpec itemSpec = new ItemSpec();
    itemSpec.setItem(path);
    itemSpec.setRecurse(recursion);
    itemSpec.setDid(deletionId);

    String workspaceName = "";
    String workspaceOwner = "";

    if (!VersionControlPath.isServerItem(itemSpec.getItem())) {
      WorkspaceInfo info = Workstation.getInstance().findWorkspaceByLocalPath(itemSpec.getItem());
      if (info != null) {
        workspaceName = info.getName();
        workspaceOwner = info.getOwnerName();
      }
    }

    List<Changeset> changes = new ArrayList<Changeset>();
    int total = maxCount;
    VersionSpec versionTo = versionToOrig;

    while (total > 0) {
      int batchMax = Math.min(256, total);
      // todo: our stubs differ from opentf' ones. have to find out difference
      Changeset[] changeSets = myRepository.QueryHistory(workspaceName, workspaceOwner, itemSpec, version, user, versionFrom, versionTo,
                                                         batchMax, includeChanges, slotMode, includeDownloadInfo).getChangeset();
      int batchCnt = changeSets.length;
      if (batchCnt < batchMax) break;
      total -= batchCnt;
      Changeset lastChangeset = changes.get(changes.size() - 1);
      versionTo = new ChangesetVersionSpec(lastChangeset.getCset());
    }

    return Collections.enumeration(changes);
  }

}