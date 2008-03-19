package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.microsoft.wsdl.types.Guid;
import org.apache.axis2.databinding.ADBBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSConstants;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.TfsExceptionManager;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;

import java.io.*;
import java.net.URI;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.*;


public class VersionControlServer {
  private Repository myRepository;
  private URI myUri;
  private Guid myGuid = new Guid();

  public VersionControlServer(URI uri) {
    myUri = uri;
    try {
      myRepository = new Repository(this, uri);
    }
    catch (Exception e) {
      TFSVcs.LOG.error("Failed to initialize web service stub", e);
      throw new RuntimeException(e);
    }

  }

// ***************************************************
// used by now

  /**
   * @return List<GetOperation or Failure>
   */
  public Map<String, ADBBean> checkoutForEdit(final String workspaceName, final String workspaceOwner, List<String> serverPaths)
    throws TfsException {
    List<ChangeRequest> changeRequests = new ArrayList<ChangeRequest>(serverPaths.size());
    for (String serverPath : serverPaths) {
      ItemSpec itemSpec = new ItemSpec();
      itemSpec.setDid(0);
      itemSpec.setItem(serverPath);
      itemSpec.setRecurse(RecursionType.None);

      ChangeRequest changeRequest = new ChangeRequest();
      changeRequest.setDid(0);
      changeRequest.setEnc(Integer.MIN_VALUE);
      changeRequest.setItem(itemSpec);
      changeRequest.setLock(LockLevel.None); // TODO
      changeRequest.setReq(RequestType.Edit);
      changeRequest.setTarget(null); // TODO
      changeRequest.setTargettype(null); // TODO
      changeRequest.setType(null); // TODO
      changeRequest.setVspec(null); // TODO

      changeRequests.add(changeRequest);
    }
    final ArrayOfChangeRequest arrayOfChangeRequest = new ArrayOfChangeRequest();
    arrayOfChangeRequest.setChangeRequest(changeRequests.toArray(new ChangeRequest[changeRequests.size()]));

    PendChangesResponse response = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<PendChangesResponse>() {
      public PendChangesResponse executeRequest() throws RemoteException {
        return myRepository.PendChanges(workspaceName, workspaceOwner, arrayOfChangeRequest);
      }
    });

    Map<String, ADBBean> results = new HashMap<String, ADBBean>();
    if (response.getPendChangesResult() != null && response.getPendChangesResult().getGetOperation() != null) {
      for (GetOperation getOperation : response.getPendChangesResult().getGetOperation()) {
        results.put(getOperation.getTitem(), getOperation);
      }
    }

    if (response.getFailures() != null && response.getFailures().getFailure() != null) {
      for (Failure failure : response.getFailures().getFailure()) {
        results.put(failure.getItem(), failure);
      }
    }
    return results;
  }

  public Workspace getWorkspace(final String workspaceName, final String workspaceOwner) throws TfsException {
    return WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<Workspace>() {
      public Workspace executeRequest() throws RemoteException {
        return myRepository.QueryWorkspace(workspaceName, workspaceOwner);
      }
    });
  }

  public Workspace updateWorkspace(final String oldWorkspaceName, final String workspaceOwner, final Workspace newWorkspaceDataBean)
    throws TfsException {
    return WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<Workspace>() {
      public Workspace executeRequest() throws RemoteException {
        return myRepository.UpdateWorkspace(oldWorkspaceName, workspaceOwner, newWorkspaceDataBean);
      }
    });
  }

  public Workspace createWorkspace(final Workspace workspaceBean) throws TfsException {
    return WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<Workspace>() {
      public Workspace executeRequest() throws RemoteException {
        return myRepository.CreateWorkspace(workspaceBean);
      }
    });
  }

  public void deleteWorkspace(final String workspaceName, final String workspaceOwner) throws TfsException {
    WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.VoidDelegate() {
      public void executeRequest() throws RemoteException {
        myRepository.DeleteWorkspace(workspaceName, workspaceOwner);
      }
    });
  }

  public List<ExtendedItem> getChildItems(final String workspasceName,
                                          final String ownerName,
                                          final ExtendedItem parent,
                                          final DeletedState deletedState,
                                          final ItemType itemType) throws TfsException {
    List<List<ExtendedItem>> extendedItems =
      getChildItems(workspasceName, ownerName, Collections.singletonList(parent), deletedState, itemType);

    TFSVcs.assertTrue(extendedItems != null && extendedItems.size() == 1);
    return extendedItems.get(0);
  }

  public List<List<ExtendedItem>> getChildItems(final String workspasceName,
                                                final String ownerName,
                                                final List<ExtendedItem> parents,
                                                final DeletedState deletedState,
                                                final ItemType itemType) throws TfsException {
    List<List<ExtendedItem>> result = getExtendedItems(workspasceName, ownerName, parents, deletedState, RecursionType.OneLevel, itemType);
    // remove parent items
    Iterator<ExtendedItem> pIter = parents.iterator();
    Iterator<List<ExtendedItem>> resIter = result.iterator();
    while (resIter.hasNext() && pIter.hasNext()) {
      ExtendedItem parentItem = pIter.next();
      List<ExtendedItem> resList = resIter.next();
      Iterator<ExtendedItem> resListIter = resList.iterator();
      while (resListIter.hasNext()) {
        ExtendedItem childItem = resListIter.next();
        if (parentItem.getSitem().equals(childItem.getSitem())) {
          resListIter.remove();
          break;
        }
      }
    }
    return result;
  }

  public List<List<ExtendedItem>> getExtendedItems(final String workspasceName,
                                                   final String ownerName,
                                                   final List<ExtendedItem> items,
                                                   final DeletedState deletedState,
                                                   final RecursionType recursionType,
                                                   final ItemType itemType) throws TfsException {
    List<ItemSpec> itemSpecList = new ArrayList<ItemSpec>();
    for (ExtendedItem item : items) {
      ItemSpec itemSpec = new ItemSpec();
      itemSpec.setItem(item.getSitem());
      itemSpec.setRecurse(recursionType);
      itemSpecList.add(itemSpec);
    }
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(itemSpecList.toArray(new ItemSpec[itemSpecList.size()]));
    List<List<ExtendedItem>> result = new LinkedList<List<ExtendedItem>>();
    ArrayOfExtendedItem[] extendedItems =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfExtendedItem[]>() {
        public ArrayOfExtendedItem[] executeRequest() throws RemoteException {
          return myRepository.QueryItemsExtended(workspasceName, ownerName, arrayOfItemSpec, deletedState, itemType)
            .getArrayOfExtendedItem();
        }
      });

    TFSVcs.assertTrue(extendedItems != null && extendedItems.length == items.size());
    for (ArrayOfExtendedItem extendedItem : extendedItems) {
      List<ExtendedItem> resultItemsList = new LinkedList<ExtendedItem>();
      ExtendedItem[] resultItems = extendedItem.getExtendedItem();
      if (resultItems != null) {
        resultItemsList.addAll(Arrays.asList(resultItems));
      }
      result.add(resultItemsList);
    }
    return result;
  }

  @Nullable
  public ExtendedItem getExtendedItem(final String workspasceName,
                                      final String ownerName,
                                      final String itemServerPath,
                                      final DeletedState deletedState) throws TfsException {
    ItemSpec itemSpec = new ItemSpec();
    // TODO: is this local path?
    itemSpec.setItem(itemServerPath);
    itemSpec.setRecurse(RecursionType.None);
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(new ItemSpec[]{itemSpec});

    ArrayOfExtendedItem[] extendedItems =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfExtendedItem[]>() {
        public ArrayOfExtendedItem[] executeRequest() throws RemoteException {
          return myRepository.QueryItemsExtended(workspasceName, ownerName, arrayOfItemSpec, deletedState, ItemType.Any)
            .getArrayOfExtendedItem();
        }
      });

    TFSVcs.assertTrue(extendedItems != null && extendedItems.length == 1);
    ExtendedItem[] resultItems = extendedItems[0].getExtendedItem();
    if (resultItems != null) {
      TFSVcs.assertTrue(resultItems.length == 1);
      return resultItems[0];
    }
    return null;
  }

  public Map<String, ExtendedItem> getExtendedItems(final String workspasceName,
                                                    final String ownerName,
                                                    final List<String> itemPaths,
                                                    final DeletedState deletedState) throws TfsException {
    final List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>();
    for (String path : itemPaths) {
      ItemSpec iSpec = new ItemSpec();
      // TODO: is this local path?
      iSpec.setItem(path);
      iSpec.setRecurse(RecursionType.None);
      itemSpecs.add(iSpec);
    }
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(itemSpecs.toArray(new ItemSpec[itemSpecs.size()]));

    ArrayOfExtendedItem[] extendedItems =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfExtendedItem[]>() {
        public ArrayOfExtendedItem[] executeRequest() throws RemoteException {
          return myRepository.QueryItemsExtended(workspasceName, ownerName, arrayOfItemSpec, deletedState, ItemType.Any)
            .getArrayOfExtendedItem();
        }
      });

    TFSVcs.assertTrue(extendedItems != null && extendedItems.length == itemPaths.size());
    Map<String, ExtendedItem> result = new HashMap<String, ExtendedItem>();
    for (int i = 0; i < extendedItems.length; i++) {
      ExtendedItem[] resultItems = extendedItems[i].getExtendedItem();
      ExtendedItem item = null;
      if (resultItems != null) {
        TFSVcs.assertTrue(resultItems.length == 1);
        item = resultItems[0];
      }
      result.put(itemPaths.get(i), item);
    }

    return result;
  }

  public ArrayOfExtendedItem[] getExtendedItems(final String workspasceName,
                                                final String ownerName,
                                                final ItemSpec[] itemSpecs,
                                                final DeletedState deletedState,
                                                final ItemType itemType) throws TfsException {
    final ArrayOfItemSpec is = new ArrayOfItemSpec();
    is.setItemSpec(itemSpecs);
    return WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfExtendedItem[]>() {
      public ArrayOfExtendedItem[] executeRequest() throws RemoteException {
        return myRepository.QueryItemsExtended(workspasceName, ownerName, is, deletedState, itemType).getArrayOfExtendedItem();
      }
    });
  }


  @Nullable
  public ArrayOfExtendedItem[] getExtendedItems(final String workspasceName,
                                                final String ownerName,
                                                final String path,
                                                final DeletedState deletedState,
                                                final ItemType itemType) throws TfsException {
    ItemSpec is = new ItemSpec();
    is.setItem(path);
    is.setRecurse(RecursionType.OneLevel);
    ArrayOfExtendedItem[] items = getExtendedItems(workspasceName, ownerName, new ItemSpec[]{is}, deletedState, itemType);
    if (items.length == 0) {
      return null;
    }
    return items;
  }

  public static void downloadItem(final WorkspaceInfo workspaceInfo, final String downloadKey, OutputStream outputStream)
    throws TfsException {
    final String url = workspaceInfo.getServer().getUri().toASCIIString() + TFSConstants.DOWNLOAD_ASMX + "?" + downloadKey;
    WebServiceHelper.httpGet(url, outputStream);
  }

  private static void downloadItem(final WorkspaceInfo workspaceInfo,
                                   final String downloadKey,
                                   final File destination,
                                   boolean overwriteReadonly) throws TfsException {
    OutputStream fileStream = null;
    try {
      if (overwriteReadonly && destination.isFile() && !destination.canWrite()) {
        FileUtil.setReadOnlyAttribute(destination.getPath(), false);
      }
      fileStream = new FileOutputStream(destination);
      downloadItem(workspaceInfo, downloadKey, fileStream);
    }
    catch (FileNotFoundException e) {
      throw TfsExceptionManager.processException(e);
    }
    catch (IOException e) {
      throw TfsExceptionManager.processException(e);
    }
    finally {
      if (fileStream != null) {
        try {
          fileStream.close();
        }
        catch (IOException e) {
          // ignore
        }
      }
    }
  }

  public static void downloadItem(@NotNull final WorkspaceInfo workspace,
                                  @NotNull final GetOperation operation,
                                  boolean setReadOnly,
                                  boolean overwriteReadonly) throws TfsException {
    File file = new File(operation.getTlocal());
    if (operation.getType() == ItemType.Folder) {
      file.mkdirs();
    }
    else if (operation.getType() == ItemType.File) {
      if (file.getParentFile() != null) {
        file.getParentFile().mkdirs();
      }
      downloadItem(workspace, operation.getDurl(), file, overwriteReadonly);
      if (setReadOnly) {
        file.setReadOnly();
      }
    }
    else {
      TFSVcs.LOG.error("unexpected item type: " + operation.getType() + ", " + file);
    }
  }

  public static LocalVersionUpdate createLocalVersionUpdate(GetOperation getOperation) {
    LocalVersionUpdate result = new LocalVersionUpdate();
    result.setItemid(getOperation.getItemid());
    result.setTlocal(getOperation.getTlocal());
    result.setLver(getOperation.getSver());
    return result;
  }
// ***************************************************
// not used by now

  public LabelResult[] createLabel(VersionControlLabel label, LabelItemSpec[] labelSpecs, LabelChildOption childOption) throws Exception {
    Workspace workspace = getWorkspace(labelSpecs[0].getItemSpec().getItem());
    ArrayOfLabelItemSpec ls = new ArrayOfLabelItemSpec();
    ls.setLabelItemSpec(labelSpecs);
    return myRepository.LabelItem(workspace.getName(), workspace.getOwner(), label, ls, childOption).getLabelItemResult()
      .getLabelResult();
  }

  public LabelResult[] unlabelItem(String labelName, String labelScope, ItemSpec[] itemSpecs, VersionSpec version) throws Exception {
    Workspace workspace = getWorkspace(itemSpecs[0].getItem());
    ArrayOfItemSpec is = new ArrayOfItemSpec();
    is.setItemSpec(itemSpecs);
    return myRepository.UnlabelItem(workspace.getName(), workspace.getOwner(), labelName, labelScope, is, version).getUnlabelItemResult()
      .getLabelResult();
  }

  public Workspace createWorkspace(String name, String owner) throws UnknownHostException, RemoteException {
    return createWorkspace(name, owner, null, new WorkingFolder[0], Workstation.getComputerName());
  }

  @Nullable
  public ArrayOfBranchRelative[] getBranchHistory(ItemSpec[] itemSpecs, VersionSpec version) throws Exception {
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
  public Item getItem(String path) throws Exception {
    return getItem(path, VersionSpecBase.getLatest());
  }

  @Nullable
  public Item getItem(String path, VersionSpec versionSpec) throws Exception {
    return getItem(path, versionSpec, 0, false);
  }

  @Nullable
  public Item getItem(String path, VersionSpec versionSpec, int deletionId, boolean includeDownloadInfo) throws Exception {
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
  public ItemSet getItems(String path, RecursionType recursionType) throws Exception {
    ItemSpec itemSpec = new ItemSpec();
    itemSpec.setRecurse(recursionType);
    itemSpec.setItem(path);
    return getItems(itemSpec, VersionSpecBase.getLatest(), DeletedState.NonDeleted, ItemType.Any, false);
  }

  @Nullable
  public ItemSet getItems(String path, VersionSpec versionSpec, RecursionType recursionType) throws Exception {
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
                          boolean includeDownloadInfo) throws Exception {
    ItemSet[] itemSet = getItems(new ItemSpec[]{itemSpec}, versionSpec, deletedState, itemType, includeDownloadInfo);
    if (itemSet != null) {
      return itemSet[0];
    }
    return null;
  }

  @Nullable
  public ItemSet[] getItems(ItemSpec[] itemSpecs,
                            final VersionSpec versionSpec,
                            final DeletedState deletedState,
                            final ItemType itemType,
                            final boolean includeDownloadInfo) throws TfsException {
    if (itemSpecs.length == 0) {
      return null;
    }

    final Ref<String> workspaceName = new Ref<String>("");
    final Ref<String> workspaceOwner = new Ref<String>("");

    String item = itemSpecs[0].getItem();
    if (!VersionControlPath.isServerItem(item)) {
      WorkspaceInfo info = Workstation.getInstance().findWorkspace(item);
      if (info != null) {
        workspaceName.set(info.getName());
        workspaceOwner.set(info.getOwnerName());
      }
    }
    final ArrayOfItemSpec is = new ArrayOfItemSpec();
    is.setItemSpec(itemSpecs);
    return WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ItemSet[]>() {
      public ItemSet[] executeRequest() throws RemoteException {
        return myRepository
          .QueryItems(workspaceName.get(), workspaceOwner.get(), is, versionSpec, deletedState, itemType, includeDownloadInfo)
          .getItemSet();
      }
    });
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
                                  boolean slotMode) throws Exception {
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

  public Workspace[] queryWorkspaces(final String ownerName, final String computer) throws TfsException {
    Workspace[] workspaces = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<Workspace[]>() {
      public Workspace[] executeRequest() throws RemoteException {
        return myRepository.QueryWorkspaces(ownerName, computer).getWorkspace();
      }
    });

    return workspaces != null ? workspaces : new Workspace[0];
  }

  public Guid getServerGuid() {
    return myGuid;
  }

  private Repository getRepository() {
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

  public Workspace getWorkspace(final @NotNull WorkspaceInfo info) {
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

  public Workspace getWorkspace(String localPath) throws TfsException {
    String path = convertToFullPath(localPath);

    WorkspaceInfo info = Workstation.getInstance().findWorkspace(path);
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
                                  final VersionSpec version,
                                  int deletionId,
                                  RecursionType recursion,
                                  final String user,
                                  final VersionSpec versionFrom,
                                  VersionSpec versionToOrig,
                                  int maxCount,
                                  final boolean includeChanges,
                                  final boolean slotMode,
                                  final boolean includeDownloadInfo) throws TfsException {
    final ItemSpec itemSpec = new ItemSpec();
    itemSpec.setItem(path);
    itemSpec.setRecurse(recursion);
    itemSpec.setDid(deletionId);

    final Ref<String> workspaceName = new Ref<String>("");
    final Ref<String> workspaceOwner = new Ref<String>("");

    if (!VersionControlPath.isServerItem(itemSpec.getItem())) {
      WorkspaceInfo info = Workstation.getInstance().findWorkspace(itemSpec.getItem());
      if (info != null) {
        workspaceName.set(info.getName());
        workspaceOwner.set(info.getOwnerName());
      }
    }

    List<Changeset> changes = new ArrayList<Changeset>();
    int total = maxCount;
    final VersionSpec[] versionTo = new VersionSpec[]{versionToOrig};

    while (total > 0) {
      final int batchMax = Math.min(256, total);
      // todo: our stubs differ from opentf' ones. have to find out difference
      Changeset[] changeSets = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<Changeset[]>() {
        public Changeset[] executeRequest() throws RemoteException {
          return myRepository.QueryHistory(workspaceName.get(), workspaceOwner.get(), itemSpec, version, user, versionFrom, versionTo[0],
                                           batchMax, includeChanges, slotMode, includeDownloadInfo).getChangeset();
        }
      });
      int batchCnt = changeSets.length;
      if (batchCnt < batchMax) break;
      total -= batchCnt;
      Changeset lastChangeset = changes.get(changes.size() - 1);
      versionTo[0] = new ChangesetVersionSpec(lastChangeset.getCset());
    }

    return Collections.enumeration(changes);
  }

  @Nullable
  public GetOperation get(final String workspasceName, final String ownerName, final String path, final VersionSpec versionSpec)
    throws TfsException {
    List<GetOperation> operations = get(workspasceName, ownerName, path, versionSpec, RecursionType.None);
    TFSVcs.assertTrue(operations.size() == 1);
    return operations.get(0);
  }

  public List<GetOperation> get(final String workspasceName,
                                final String ownerName,
                                final String path,
                                final VersionSpec versionSpec,
                                final RecursionType recursionType) throws TfsException {
    final ArrayOfGetRequest arrayOfGetRequests = new ArrayOfGetRequest();
    final GetRequest getRequest = new GetRequest();
    final ItemSpec itemSpec = new ItemSpec();
    itemSpec.setRecurse(recursionType);
    itemSpec.setItem(path);
    getRequest.setItemSpec(itemSpec);
    getRequest.setVersionSpec(versionSpec);
    final GetRequest[] getRequests = new GetRequest[]{getRequest};
    arrayOfGetRequests.setGetRequest(getRequests);

    ArrayOfGetOperation[] operations =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfGetOperation[]>() {
        public ArrayOfGetOperation[] executeRequest() throws RemoteException {
          return myRepository.Get(workspasceName, ownerName, arrayOfGetRequests, true, false).getArrayOfGetOperation();
        }
      });

    if (operations == null) {
      return Collections.emptyList();
    }
    TFSVcs.assertTrue(operations.length == 1);
    ArrayList<GetOperation> result = new ArrayList<GetOperation>();
    TFSVcs.assertTrue(operations[0].getGetOperation() != null);
    result.addAll(Arrays.asList(operations[0].getGetOperation()));
    return result;
  }

  @Nullable
  public GetOperation get(final String workspaceName, final String ownerName, final String path) throws TfsException {
    return get(workspaceName, ownerName, path, VersionSpecBase.getLatest());
  }

  public void updateLocalVersions(final String workspasceName,
                                  final String workspaceOwnerName,
                                  final List<LocalVersionUpdate> localVersions) throws TfsException {
    final ArrayOfLocalVersionUpdate arrayOfLocalVersionUpdate = new ArrayOfLocalVersionUpdate();
    arrayOfLocalVersionUpdate.setLocalVersionUpdate(localVersions.toArray(new LocalVersionUpdate[localVersions.size()]));
    WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.VoidDelegate() {
      public void executeRequest() throws RemoteException {
        myRepository.UpdateLocalVersion(workspasceName, workspaceOwnerName, arrayOfLocalVersionUpdate);
      }
    });
  }

  public Map<String, ADBBean> undoPendingChanges(final String workspaceName, final String workspaceOwner, final List<String> serverPaths)
    throws TfsException {
    List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>(serverPaths.size());
    for (String serverPath : serverPaths) {
      ItemSpec itemSpec = new ItemSpec();
      itemSpec.setDid(0);
      itemSpec.setItem(serverPath);
      itemSpec.setRecurse(RecursionType.None);
      itemSpecs.add(itemSpec);
    }
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(itemSpecs.toArray(new ItemSpec[itemSpecs.size()]));

    UndoPendingChangesResponse response =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<UndoPendingChangesResponse>() {
        public UndoPendingChangesResponse executeRequest() throws RemoteException {
          return myRepository.UndoPendingChanges(workspaceName, workspaceOwner, arrayOfItemSpec);
        }
      });

    Map<String, ADBBean> results = new HashMap<String, ADBBean>();
    if (response.getUndoPendingChangesResult() != null && response.getUndoPendingChangesResult().getGetOperation() != null) {
      for (GetOperation getOperation : response.getUndoPendingChangesResult().getGetOperation()) {
        results.put(getOperation.getTitem(), getOperation);
      }
    }

    if (response.getFailures() != null && response.getFailures().getFailure() != null) {
      for (Failure failure : response.getFailures().getFailure()) {
        results.put(failure.getItem(), failure);
      }
    }
    return results;
  }

  public Map<String, GetOperation> get(final String workspaceName, final String workspaceOwner, final Map<String, VersionSpec> requests)
    throws TfsException {
    List<GetRequest> getRequests = new ArrayList<GetRequest>(requests.size());
    for (String serverPath : requests.keySet()) {
      final GetRequest getRequest = new GetRequest();
      final ItemSpec itemSpec = new ItemSpec();
      itemSpec.setRecurse(RecursionType.None);
      itemSpec.setItem(serverPath);
      getRequest.setItemSpec(itemSpec);
      getRequest.setVersionSpec(requests.get(serverPath));
      getRequests.add(getRequest);
    }
    final ArrayOfGetRequest arrayOfGetRequests = new ArrayOfGetRequest();
    arrayOfGetRequests.setGetRequest(getRequests.toArray(new GetRequest[getRequests.size()]));

    ArrayOfArrayOfGetOperation response =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfArrayOfGetOperation>() {
        public ArrayOfArrayOfGetOperation executeRequest() throws RemoteException {
          return myRepository.Get(workspaceName, workspaceOwner, arrayOfGetRequests, true, false);
        }
      });

    Map<String, GetOperation> results = new HashMap<String, GetOperation>();
    if (response.getArrayOfGetOperation() != null) {
      for (ArrayOfGetOperation array : response.getArrayOfGetOperation()) {
        GetOperation operation = array.getGetOperation()[0];
        results.put(operation.getTitem(), operation);
      }
    }

    return results;
  }
}
