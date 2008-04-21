package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import org.apache.axis2.databinding.ADBBean;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSConstants;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.TfsExceptionManager;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;

import java.io.*;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.*;

public class VersionControlServer {
  private Repository myRepository;
  //private URI myUri;
  //private Guid myGuid = new Guid();

  //public static final String Upload = "Upload";
  //public static final String Download = "Download";
  //public static final int EncodingBinary = -1;
  public static final String WORKSPACE_NAME_FIELD = "wsname";
  public static final String WORKSPACE_OWNER_FIELD = "wsowner";
  public static final String RANGE_FIELD = "range";
  public static final String LENGTH_FIELD = "filelength";
  public static final String HASH_FIELD = "hash";
  public static final String SERVER_ITEM_FIELD = "item";
  public static final String CONTENT_FIELD = "content";

  public VersionControlServer(URI uri) {
    //myUri = uri;
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

  public static class GetRequestParams {
    public final String serverPath;
    public final RecursionType recursionType;
    public final VersionSpec version;

    public GetRequestParams(final String serverPath, final RecursionType recursionType, final VersionSpec version) {
      this.serverPath = serverPath;
      this.recursionType = recursionType;
      this.version = version;
    }
  }

  private interface ChangeRequestProvider {
    ChangeRequest createChangeRequest(ItemPath itemPath);
  }

  private static ChangeRequest createChangeRequestTemplate(ItemPath itemPath) {
    ItemSpec itemSpec = new ItemSpec();
    itemSpec.setDid(Integer.MIN_VALUE);
    itemSpec.setRecurse(null);

    ChangeRequest changeRequest = new ChangeRequest();
    changeRequest.setDid(Integer.MIN_VALUE);
    changeRequest.setEnc(Integer.MIN_VALUE);
    changeRequest.setItem(itemSpec);
    changeRequest.setLock(null);
    changeRequest.setTarget(null); // TODO
    changeRequest.setTargettype(null); // TODO
    changeRequest.setVspec(null); // TODO

    return changeRequest;
  }

  public ResultWithFailures<GetOperation> checkoutForEdit(final String workspaceName, final String workspaceOwner, List<ItemPath> paths)
    throws TfsException {
    return pendChanges(workspaceName, workspaceOwner, paths, new ChangeRequestProvider() {
      public ChangeRequest createChangeRequest(final ItemPath itemPath) {
        ChangeRequest changeRequest = createChangeRequestTemplate(itemPath);
        changeRequest.getItem().setItem(itemPath.getServerPath());
        changeRequest.setReq(RequestType.Edit);

        File file = itemPath.getLocalPath().getIOFile();
        changeRequest.setType(file.isFile() ? ItemType.File : ItemType.Folder);

        return changeRequest;
      }
    });
  }

  public ResultWithFailures<GetOperation> scheduleForAddition(final String workspaceName, final String workspaceOwner, List<ItemPath> paths)
    throws TfsException {
    return pendChanges(workspaceName, workspaceOwner, paths, new ChangeRequestProvider() {
      public ChangeRequest createChangeRequest(final ItemPath itemPath) {
        ChangeRequest changeRequest = createChangeRequestTemplate(itemPath);
        changeRequest.getItem().setItem(VersionControlPath.toTfsRepresentation(itemPath.getLocalPath()));
        changeRequest.setReq(RequestType.Add);

        File file = itemPath.getLocalPath().getIOFile();
        changeRequest.setType(file.isFile() ? ItemType.File : ItemType.Folder);

        // TODO: determine encoding by file content
        changeRequest.setEnc(1251);
        return changeRequest;
      }
    });
  }

  public ResultWithFailures<GetOperation> scheduleForDeletion(final String workspaceName,
                                                              final String workspaceOwner,
                                                              final List<ItemPath> paths) throws TfsException {
    return pendChanges(workspaceName, workspaceOwner, paths, new ChangeRequestProvider() {
      public ChangeRequest createChangeRequest(final ItemPath itemPath) {
        ChangeRequest changeRequest = createChangeRequestTemplate(itemPath);
        changeRequest.getItem().setItem(VersionControlPath.toTfsRepresentation(itemPath.getLocalPath()));
        changeRequest.setReq(RequestType.Delete);
        return changeRequest;
      }
    });
  }


  private ResultWithFailures<GetOperation> pendChanges(final String workspaceName,
                                                       final String workspaceOwner,
                                                       List<ItemPath> paths,
                                                       ChangeRequestProvider changeRequestProvider) throws TfsException {
    ResultWithFailures<GetOperation> result = new ResultWithFailures<GetOperation>();
    if (paths.isEmpty()) {
      return result;
    }

    List<ChangeRequest> changeRequests = new ArrayList<ChangeRequest>(paths.size());
    for (ItemPath itemPath : paths) {
      changeRequests.add(changeRequestProvider.createChangeRequest(itemPath));
    }

    final ArrayOfChangeRequest arrayOfChangeRequest = new ArrayOfChangeRequest();
    arrayOfChangeRequest.setChangeRequest(changeRequests.toArray(new ChangeRequest[changeRequests.size()]));

    PendChangesResponse response = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<PendChangesResponse>() {
      public PendChangesResponse executeRequest() throws RemoteException {
        return myRepository.PendChanges(workspaceName, workspaceOwner, arrayOfChangeRequest);
      }
    });

    if (response.getPendChangesResult().getGetOperation() != null) {
      result.getResult().addAll(Arrays.asList(response.getPendChangesResult().getGetOperation()));
    }

    if (response.getFailures().getFailure() != null) {
      result.getFailures().addAll(Arrays.asList(response.getFailures().getFailure()));
    }
    return result;
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

  public List<ExtendedItem> getChildItems(final String workspaceName,
                                          final String ownerName,
                                          final String parentServerPath,
                                          final DeletedState deletedState,
                                          final ItemType itemType) throws TfsException {
    List<List<ExtendedItem>> extendedItems =
      getChildItems(workspaceName, ownerName, Collections.singletonList(parentServerPath), deletedState, itemType);

    TFSVcs.assertTrue(extendedItems != null && extendedItems.size() == 1);
    return extendedItems != null ? extendedItems.get(0) : null;
  }

  private List<List<ExtendedItem>> getChildItems(final String workspaceName,
                                                 final String ownerName,
                                                 final List<String> parentsServerPaths,
                                                 final DeletedState deletedState,
                                                 final ItemType itemType) throws TfsException {
    List<List<ExtendedItem>> result =
      getExtendedItemsByStrings(workspaceName, ownerName, parentsServerPaths, deletedState, RecursionType.OneLevel, itemType);
    // remove parent items
    Iterator<String> pIter = parentsServerPaths.iterator();
    Iterator<List<ExtendedItem>> resIter = result.iterator();
    while (resIter.hasNext() && pIter.hasNext()) {
      String parentServerPath = pIter.next();
      List<ExtendedItem> resList = resIter.next();
      Iterator<ExtendedItem> resListIter = resList.iterator();
      while (resListIter.hasNext()) {
        ExtendedItem childItem = resListIter.next();
        if (parentServerPath.equals(childItem.getSitem())) {
          resListIter.remove();
          break;
        }
      }
    }
    return result;
  }

  public List<List<ExtendedItem>> getExtendedItemsByStrings(final String workspaceName,
                                                            final String ownerName,
                                                            final List<String> parentServerPaths,
                                                            final DeletedState deletedState,
                                                            final RecursionType recursionType,
                                                            final ItemType itemType) throws TfsException {
    List<ItemSpec> itemSpecList = new ArrayList<ItemSpec>();
    for (String serverPath : parentServerPaths) {
      ItemSpec itemSpec = new ItemSpec();
      itemSpec.setItem(serverPath);
      itemSpec.setRecurse(recursionType);
      itemSpecList.add(itemSpec);
    }
    return getExtendedItems(workspaceName, ownerName, itemSpecList, deletedState, itemType);
  }

  public List<List<ExtendedItem>> getExtendedItems(final String workspaceName,
                                                   final String ownerName,
                                                   final List<ItemPath> paths,
                                                   final DeletedState deletedState,
                                                   final RecursionType recursionType,
                                                   final ItemType itemType) throws TfsException {
    List<ItemSpec> itemSpecList = new ArrayList<ItemSpec>();
    for (ItemPath path : paths) {
      ItemSpec itemSpec = new ItemSpec();
      itemSpec.setItem(path.getServerPath());
      itemSpec.setRecurse(recursionType);
      itemSpecList.add(itemSpec);
    }
    return getExtendedItems(workspaceName, ownerName, itemSpecList, deletedState, itemType);
  }


  private List<List<ExtendedItem>> getExtendedItems(final String workspaceName,
                                                    final String ownerName,
                                                    final List<ItemSpec> itemsSpecs,
                                                    final DeletedState deletedState,
                                                    final ItemType itemType) throws TfsException {
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(itemsSpecs.toArray(new ItemSpec[itemsSpecs.size()]));
    List<List<ExtendedItem>> result = new ArrayList<List<ExtendedItem>>();
    ArrayOfExtendedItem[] extendedItems =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfExtendedItem[]>() {
        public ArrayOfExtendedItem[] executeRequest() throws RemoteException {
          return myRepository.QueryItemsExtended(workspaceName, ownerName, arrayOfItemSpec, deletedState, itemType)
            .getArrayOfExtendedItem();
        }
      });

    TFSVcs.assertTrue(extendedItems != null && extendedItems.length == itemsSpecs.size());
    //noinspection ConstantConditions
    for (ArrayOfExtendedItem extendedItem : extendedItems) {
      List<ExtendedItem> resultItemsList = new ArrayList<ExtendedItem>();
      ExtendedItem[] resultItems = extendedItem.getExtendedItem();
      if (resultItems != null) {
        resultItemsList.addAll(Arrays.asList(resultItems));
      }
      result.add(resultItemsList);
    }
    return result;
  }

  @Nullable
  public ExtendedItem getExtendedItem(final String workspaceName,
                                      final String ownerName,
                                      final String itemServerPath,
                                      final DeletedState deletedState) throws TfsException {
    ItemSpec itemSpec = new ItemSpec();
    itemSpec.setItem(itemServerPath);
    itemSpec.setRecurse(RecursionType.None);
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(new ItemSpec[]{itemSpec});

    ArrayOfExtendedItem[] extendedItems =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfExtendedItem[]>() {
        public ArrayOfExtendedItem[] executeRequest() throws RemoteException {
          return myRepository.QueryItemsExtended(workspaceName, ownerName, arrayOfItemSpec, deletedState, ItemType.Any)
            .getArrayOfExtendedItem();
        }
      });

    TFSVcs.assertTrue(extendedItems != null && extendedItems.length == 1);
    //noinspection ConstantConditions
    ExtendedItem[] resultItems = extendedItems[0].getExtendedItem();
    if (resultItems != null) {
      TFSVcs.assertTrue(resultItems.length == 1);
      return resultItems[0];
    }
    return null;
  }

  public Map<ItemPath, ExtendedItem> getExtendedItems(final String workspaceName,
                                                      final String ownerName,
                                                      final List<ItemPath> paths,
                                                      final DeletedState deletedState) throws TfsException {
    final List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>();
    for (ItemPath itemPath : paths) {
      ItemSpec iSpec = new ItemSpec();
      iSpec.setItem(itemPath.getServerPath());
      iSpec.setRecurse(RecursionType.None);
      itemSpecs.add(iSpec);
    }
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(itemSpecs.toArray(new ItemSpec[itemSpecs.size()]));

    ArrayOfExtendedItem[] extendedItems =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfExtendedItem[]>() {
        public ArrayOfExtendedItem[] executeRequest() throws RemoteException {
          return myRepository.QueryItemsExtended(workspaceName, ownerName, arrayOfItemSpec, deletedState, ItemType.Any)
            .getArrayOfExtendedItem();
        }
      });

    TFSVcs.assertTrue(extendedItems != null && extendedItems.length == paths.size());
    Map<ItemPath, ExtendedItem> result = new HashMap<ItemPath, ExtendedItem>();
    //noinspection ConstantConditions
    for (int i = 0; i < extendedItems.length; i++) {
      ExtendedItem[] resultItems = extendedItems[i].getExtendedItem();
      ExtendedItem item = null;
      if (resultItems != null) {
        TFSVcs.assertTrue(resultItems.length == 1);
        item = resultItems[0];
      }
      result.put(paths.get(i), item);
    }

    return result;
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
      if (overwriteReadonly && destination.isFile()) {
        TfsFileUtil.setReadOnlyInEventDispatchThread(destination.getPath(), false);
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
    TFSVcs.assertTrue(operation.getType() == ItemType.Folder || operation.getType() == ItemType.File);

    if (ChangeType.fromString(operation.getChg()).contains(ChangeType.Value.Rename)) {
      TFSVcs.assertTrue(ChangeType.fromString(operation.getChg()).containsOnly(ChangeType.Value.Rename));
      TFSVcs.assertTrue(!operation.getSlocal().equals(operation.getTlocal()));

      File source = new File(operation.getSlocal());
      File target = new File(operation.getTlocal());
      boolean renameSuccessful = source.renameTo(target);
      if (!renameSuccessful) {
        TFSVcs.LOG.warn("Failed to rename " + source);
      }
    }
    else {

      File targetFile = new File(operation.getTlocal());
      if (operation.getType() == ItemType.Folder) {
        targetFile.mkdirs();
      }
      else {
        TFSVcs.assertTrue(operation.getDurl() != null);
        if (targetFile.getParentFile() != null) {
          targetFile.getParentFile().mkdirs();
        }
        downloadItem(workspace, operation.getDurl(), targetFile, overwriteReadonly);
        if (setReadOnly && targetFile.isFile()) {
          targetFile.setReadOnly();
        }
      }
    }
  }

  public static LocalVersionUpdate createLocalVersionUpdate(GetOperation getOperation) {
    LocalVersionUpdate result = new LocalVersionUpdate();
    result.setItemid(getOperation.getItemid());
    result.setTlocal(getOperation.getTlocal());
    result.setLver(getOperation.getSver());
    return result;
  }

  public List<Changeset> queryHistory(final String workspaceName,
                                      final String workspaceOwner,
                                      ItemPath rootPath,
                                      int deletionId,
                                      final String user,
                                      final VersionSpec itemVersion,
                                      final VersionSpec versionFrom,
                                      final VersionSpec versionTo,
                                      int maxCount,
                                      RecursionType recursionType) throws TfsException {
    // TODO: slot mode
    // TODO: include allChangeSets

    final ItemSpec itemSpec = new ItemSpec();

    itemSpec.setRecurse(recursionType);
    itemSpec.setItem(rootPath.getServerPath());
    itemSpec.setDid(deletionId);
    List<Changeset> allChangeSets = new ArrayList<Changeset>();
    int total = maxCount > 0 ? maxCount : Integer.MAX_VALUE;
    final Ref<VersionSpec> versionToCurrent = new Ref<VersionSpec>(versionTo);

    while (total > 0) {
      final int batchMax = Math.min(256, total);

      Changeset[] currentChangeSets = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<Changeset[]>() {
        public Changeset[] executeRequest() throws RemoteException {
          return myRepository.QueryHistory(workspaceName, workspaceOwner, itemSpec, itemVersion, user, versionFrom, versionToCurrent.get(),
                                           batchMax, true, false, false).getChangeset();
        }
      });

      if (currentChangeSets != null) {
        allChangeSets.addAll(Arrays.asList(currentChangeSets));
      }


      if (currentChangeSets == null || currentChangeSets.length < batchMax) {
        break;
      }

      total -= currentChangeSets.length;
      Changeset lastChangeSet = currentChangeSets[currentChangeSets.length - 1];
      versionToCurrent.set(new ChangesetVersionSpec(lastChangeSet.getCset()));
    }
    return allChangeSets;
  }

  public List<Changeset> queryHistory(final String workspaceName,
                                      final String workspaceOwner,
                                      ItemPath rootPath,
                                      int deletionId,
                                      final String user,
                                      final VersionSpec versionFrom,
                                      final VersionSpec versionTo,
                                      int maxCount

  ) throws TfsException {
    return queryHistory(workspaceName, workspaceOwner, rootPath, deletionId, user, LatestVersionSpec.INSTANCE, versionFrom, versionTo,
                        maxCount, RecursionType.Full);
  }

  public Changeset getChangeset(int changesetId) throws RemoteException {
    return getChangeset(changesetId, false, false);
  }

  public Changeset getChangeset(int changesetId, boolean includeChanges, boolean includeDownloadInfo) throws RemoteException {
    return myRepository.QueryChangeset(changesetId, includeChanges, includeDownloadInfo);
  }

  public int getLatestChangesetId() throws RemoteException {
    RepositoryProperties properties = myRepository.GetRepositoryProperties(new GetRepositoryProperties());
    return properties.getLcset();
  }

  //public Enumeration queryHistory(String path,
  //                                VersionSpec version,
  //                                int deletionId,
  //                                RecursionType recursion,
  //                                String user,
  //                                VersionSpec versionFrom,
  //                                VersionSpec versionTo,
  //                                int maxCount,
  //                                boolean includeChanges,
  //                                boolean slotMode) throws Exception {
  //  return queryHistory(path, version, deletionId, recursion, user, versionFrom, versionTo, maxCount, includeChanges, slotMode, false);
  //}

  //public VersionControlLabel[] queryLabels(String labelName, String labelScope, String owner, boolean includeItems) throws RemoteException {
  //  return myRepository.QueryLabels(null, null, labelName, labelScope, owner, null, VersionSpecBase.getLatest(), includeItems, false)
  //    .getVersionControlLabel();
  //}
  //
  //public VersionControlLabel[] queryLabels(String labelName,
  //                                         String labelScope,
  //                                         String owner,
  //                                         boolean includeItems,
  //                                         String filterItem,
  //                                         VersionSpec versionFilterItem) throws RemoteException {
  //  return myRepository.QueryLabels(null, null, labelName, labelScope, owner, filterItem, versionFilterItem, includeItems, false)
  //    .getVersionControlLabel();
  //}
  //
  //public VersionControlLabel[] queryLabels(String labelName,
  //                                         String labelScope,
  //                                         String owner,
  //                                         boolean includeItems,
  //                                         String filterItem,
  //                                         VersionSpec versionFilterItem,
  //                                         boolean generateDownloadUrls) throws RemoteException {
  //  return myRepository
  //    .QueryLabels(null, null, labelName, labelScope, owner, filterItem, versionFilterItem, includeItems, generateDownloadUrls)
  //    .getVersionControlLabel();
  //}

  //public ItemSecurity[] getPermissions(String[] items, RecursionType recursion) {
  //  return getPermissions(null, items, recursion);
  //}

  public Workspace[] queryWorkspaces(final String ownerName, final String computer) throws TfsException {
    Workspace[] workspaces = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<Workspace[]>() {
      public Workspace[] executeRequest() throws RemoteException {
        return myRepository.QueryWorkspaces(ownerName, computer).getWorkspace();
      }
    });

    return workspaces != null ? workspaces : new Workspace[0];
  }

  @Nullable
  public GetOperation get(final String workspaceName, final String ownerName, final String path, final VersionSpec versionSpec)
    throws TfsException {
    List<GetOperation> operations = get(workspaceName, ownerName, path, versionSpec, RecursionType.None);
    TFSVcs.assertTrue(operations.size() == 1);
    return operations.get(0);
  }

  public List<GetOperation> get(final String workspaceName,
                                final String ownerName,
                                final String path,
                                final VersionSpec versionSpec,
                                final RecursionType recursionType) throws TfsException {
    final GetRequestParams getRequest = new GetRequestParams(path, recursionType, versionSpec);
    List<List<GetOperation>> result = get(workspaceName, ownerName, Collections.singletonList(getRequest));
    return result.get(0);
  }

  public void updateLocalVersions(final String workspaceName, final String workspaceOwnerName, final List<LocalVersionUpdate> localVersions)
    throws TfsException {
    final ArrayOfLocalVersionUpdate arrayOfLocalVersionUpdate = new ArrayOfLocalVersionUpdate();
    arrayOfLocalVersionUpdate.setLocalVersionUpdate(localVersions.toArray(new LocalVersionUpdate[localVersions.size()]));
    WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.VoidDelegate() {
      public void executeRequest() throws RemoteException {
        myRepository.UpdateLocalVersion(workspaceName, workspaceOwnerName, arrayOfLocalVersionUpdate);
      }
    });
  }

  public Map<String, ADBBean> undoPendingChanges(final String workspaceName, final String workspaceOwner, final List<ItemPath> paths)
    throws TfsException {
    List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>(paths.size());
    for (ItemPath itemPath : paths) {
      ItemSpec itemSpec = new ItemSpec();
      itemSpec.setDid(Integer.MIN_VALUE);
      itemSpec.setItem(itemPath.getServerPath());
      itemSpec.setRecurse(null);
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

  public List<List<GetOperation>> get(final String workspaceName, final String workspaceOwner, final List<GetRequestParams> requests)
    throws TfsException {
    List<GetRequest> getRequests = new ArrayList<GetRequest>(requests.size());
    for (GetRequestParams getRequestParams : requests) {
      final GetRequest getRequest = new GetRequest();
      final ItemSpec itemSpec = new ItemSpec();
      itemSpec.setRecurse(getRequestParams.recursionType);
      itemSpec.setItem(getRequestParams.serverPath);
      getRequest.setItemSpec(itemSpec);
      getRequest.setVersionSpec(getRequestParams.version);
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

    TFSVcs.assertTrue(response.getArrayOfGetOperation() != null && response.getArrayOfGetOperation().length == requests.size());

    List<List<GetOperation>> results = new ArrayList<List<GetOperation>>(response.getArrayOfGetOperation().length);
    for (ArrayOfGetOperation arrayOfGetOperation : response.getArrayOfGetOperation()) {
      results.add(Arrays.asList(arrayOfGetOperation.getGetOperation()));
    }
    return results;
  }

  public AddConflictResponse addLocalConflict(final String workspaceName,
                                              final String workspaceOwner,
                                              final int itemId,
                                              final int versionFrom,
                                              final String sourceLocal) throws TfsException {
    final ConflictType conflictType = ConflictType.Local;
    final int pendingChangeId = 0;
    final int reason = 1; // TODO: ???
    return WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<AddConflictResponse>() {
      public AddConflictResponse executeRequest() throws RemoteException {
        return myRepository
          .AddConflict(workspaceName, workspaceOwner, conflictType, itemId, versionFrom, pendingChangeId, sourceLocal, sourceLocal, reason);
      }
    });
  }

  public List<Conflict> queryConflicts(final String workspaceName,
                                       final String ownerName,
                                       final List<FilePath> paths,
                                       final RecursionType recursionType) throws TfsException {
    List<ItemSpec> itemSpecList = new ArrayList<ItemSpec>();
    for (FilePath path : paths) {
      ItemSpec itemSpec = new ItemSpec();
      itemSpec.setItem(path.getPath());
      itemSpec.setRecurse(recursionType);
      itemSpecList.add(itemSpec);
    }
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(itemSpecList.toArray(new ItemSpec[itemSpecList.size()]));
    List<Conflict> result = new LinkedList<Conflict>();
    Conflict[] conflicts = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<Conflict[]>() {
      public Conflict[] executeRequest() throws RemoteException {
        return myRepository.QueryConflicts(workspaceName, ownerName, arrayOfItemSpec).getConflict();
      }
    });
    if (conflicts != null) {
      result.addAll(Arrays.asList(conflicts));
    }
    return result;
  }


  public static class ResolveConflictParams {
    public final int conflictId;
    public final Resolution resolution;
    public final LockLevel lockLevel;
    public final int encoding;
    public final String newPath;

    public ResolveConflictParams(final int conflictId,
                                 final Resolution resolution,
                                 final LockLevel lockLevel,
                                 final int encoding,
                                 final String newPath) {
      this.conflictId = conflictId;
      this.resolution = resolution;
      this.lockLevel = lockLevel;
      this.encoding = encoding;
      this.newPath = newPath;
    }
  }

  public ResolveResponse resolveConflict(final String workspaceName, final String workspasceOwnerName, final ResolveConflictParams params)
    throws TfsException {
    return WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ResolveResponse>() {
      public ResolveResponse executeRequest() throws RemoteException {
        return myRepository.Resolve(workspaceName, workspasceOwnerName, params.conflictId, params.resolution, params.newPath,
                                    params.encoding, params.lockLevel);
      }
    });
  }


  public void uploadItem(final WorkspaceInfo workspaceInfo, PendingChange change) throws TfsException, IOException {

    final String uploadUrl = workspaceInfo.getServer().getUri().toASCIIString() + TFSConstants.UPLOAD_ASMX;
    File file = new File(change.getLocal());
    long fileLength = file.length();

    ArrayList<Part> parts = new ArrayList<Part>();
    parts.add(new StringPart(SERVER_ITEM_FIELD, change.getItem()));
    parts.add(new StringPart(WORKSPACE_NAME_FIELD, workspaceInfo.getName()));
    parts.add(new StringPart(WORKSPACE_OWNER_FIELD, workspaceInfo.getOwnerName()));
    parts.add(new StringPart(LENGTH_FIELD, Long.toString(fileLength)));
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    change.getHash().writeTo(os);
    parts.add(new StringPart(HASH_FIELD, new String(Base64.encodeBase64(os.toByteArray()), "UTF-8"))); // TODO: check encoding!
    // TODO: handle files to large to fit in a single POST
    parts.add(new StringPart(RANGE_FIELD, String.format("bytes=0-%d/%d", fileLength - 1, fileLength)));
    FilePart filePart = new FilePart(CONTENT_FIELD, SERVER_ITEM_FIELD, file);
    parts.add(filePart);
    filePart.setCharSet(null);
    WebServiceHelper.httpPost(uploadUrl, parts.toArray(new Part[parts.size()]), null);
  }

  public Map<ItemPath, PendingChange> queryPendingSets(final String workspaceName,
                                                       final String workspaceOwnerName,
                                                       final List<ItemPath> paths) throws TfsException {
    final List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>();
    for (ItemPath itemPath : paths) {
      ItemSpec iSpec = new ItemSpec();
      iSpec.setItem(itemPath.getServerPath());
      iSpec.setRecurse(RecursionType.None);
      itemSpecs.add(iSpec);
    }
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(itemSpecs.toArray(new ItemSpec[itemSpecs.size()]));

    PendingSet[] pendingSets = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<PendingSet[]>() {
      public PendingSet[] executeRequest() throws RemoteException {
        return myRepository.QueryPendingSets(workspaceName, workspaceOwnerName, workspaceName, workspaceOwnerName, arrayOfItemSpec, false)
          .getQueryPendingSetsResult().getPendingSet();
      }
    });

    Map<ItemPath, PendingChange> result = new HashMap<ItemPath, PendingChange>();
    if (pendingSets != null) {
      TFSVcs.assertTrue(pendingSets.length == 1);
      TFSVcs.assertTrue(pendingSets[0].getPendingChanges().getPendingChange().length == paths.size());
      for (int i = 0; i < pendingSets[0].getPendingChanges().getPendingChange().length; i++) {
        PendingChange pendingChange = pendingSets[0].getPendingChanges().getPendingChange()[i];
        result.put(paths.get(i), pendingChange);
      }
    }
    return result;
  }

  //public List<PendingChange> queryPendingSetsRecursive(final String workspaceName,
  //                                                     final String workspaceOwnerName,
  //                                                     final List<ItemPath> roots) throws TfsException {
  //  final List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>();
  //  for (ItemPath root : roots) {
  //    ItemSpec itemSpec = new ItemSpec();
  //    itemSpec.setItem(root.getServerPath());
  //    itemSpec.setRecurse(RecursionType.Full);
  //    itemSpecs.add(itemSpec);
  //  }
  //  final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
  //  arrayOfItemSpec.setItemSpec(itemSpecs.toArray(new ItemSpec[itemSpecs.size()]));
  //
  //  PendingSet[] pendingSets = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<PendingSet[]>() {
  //    public PendingSet[] executeRequest() throws RemoteException {
  //      return myRepository.QueryPendingSets(workspaceName, workspaceOwnerName, workspaceName, workspaceOwnerName, arrayOfItemSpec, false)
  //        .getQueryPendingSetsResult().getPendingSet();
  //    }
  //  });
  //
  //  if (pendingSets != null) {
  //    return Arrays.asList(pendingSets[0].getPendingChanges().getPendingChange());
  //  }
  //  return Collections.emptyList();
  //}

  public ResultWithFailures<CheckinResult> checkIn(final String workspaceName,
                                                   final String workspaceOwnerName,
                                                   final List<ItemPath> paths,
                                                   final String comment) throws TfsException {

    ResultWithFailures<CheckinResult> result = new ResultWithFailures<CheckinResult>();

    CheckInResponse response = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<CheckInResponse>() {
      public CheckInResponse executeRequest() throws RemoteException {
        final ArrayOfString serverItems = new ArrayOfString();
        for (ItemPath path : paths) {
          serverItems.addString(path.getServerPath());
        }
        final Changeset changeset = new Changeset();
        changeset.setCset(0);
        changeset.setDate(new GregorianCalendar());
        changeset.setOwner(workspaceOwnerName);
        changeset.setComment(comment);
        final CheckinNotificationInfo checkinNotificationInfo = new CheckinNotificationInfo();
        final String checkinOptions = "ValidateCheckinOwner"; // TODO checkin options

        return myRepository.CheckIn(workspaceName, workspaceOwnerName, serverItems, changeset, checkinNotificationInfo, checkinOptions);
      }
    });

    if (response.getCheckInResult() != null) {
      result.getResult().add(response.getCheckInResult());
    }

    if (response.getFailures().getFailure() != null) {
      result.getFailures().addAll(Arrays.asList(response.getFailures().getFailure()));
    }
    return result;
  }

  //public List<List<Item>> queryItems(final String workspaceName,
  //                                   final String ownerName,
  //                                   final List<ItemPath> paths,
  //                                   final VersionSpec versionSpec,
  //                                   final DeletedState deletedState,
  //                                   final RecursionType recursionType,
  //                                   final ItemType itemType,
  //                                   final boolean generateDownloadUrl) throws TfsException {
  //  List<ItemSpec> itemSpecList = new ArrayList<ItemSpec>();
  //  for (ItemPath path : paths) {
  //    ItemSpec itemSpec = new ItemSpec();
  //    itemSpec.setItem(path.getServerPath());
  //    itemSpec.setRecurse(recursionType);
  //    itemSpecList.add(itemSpec);
  //  }
  //  return queryItems(workspaceName, ownerName, itemSpecList, versionSpec, deletedState, itemType, generateDownloadUrl);
  //}


  private List<List<Item>> queryItems(final String workspaceName,
                                      final String ownerName,
                                      final List<ItemSpec> itemsSpecs,
                                      final VersionSpec versionSpec,
                                      final DeletedState deletedState,
                                      final ItemType itemType,
                                      final boolean generateDownloadUrl) throws TfsException {
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(itemsSpecs.toArray(new ItemSpec[itemsSpecs.size()]));
    List<List<Item>> result = new ArrayList<List<Item>>();
    ItemSet[] items = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ItemSet[]>() {
      public ItemSet[] executeRequest() throws RemoteException {
        return myRepository.QueryItems(workspaceName, ownerName, arrayOfItemSpec, versionSpec, deletedState, itemType, generateDownloadUrl)
          .getItemSet();
      }
    });

    TFSVcs.assertTrue(items != null && items.length == itemsSpecs.size());
    //noinspection ConstantConditions
    for (ItemSet item : items) {
      List<Item> resultItemsList = new ArrayList<Item>();
      Item[] resultItems = item.getItems().getItem();
      if (resultItems != null) {
        resultItemsList.addAll(Arrays.asList(resultItems));
      }
      result.add(resultItemsList);
    }
    return result;
  }

  @Nullable
  public Item queryItem(final String workspaceName,
                        final String ownerName,
                        final String itemServerPath,
                        final VersionSpec versionSpec,
                        final DeletedState deletedState,
                        final boolean generateDownloadUrl) throws TfsException {
    ItemSpec itemSpec = new ItemSpec();
    itemSpec.setItem(itemServerPath);
    itemSpec.setRecurse(RecursionType.None);
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(new ItemSpec[]{itemSpec});

    ItemSet[] items = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ItemSet[]>() {
      public ItemSet[] executeRequest() throws RemoteException {
        return myRepository
          .QueryItems(workspaceName, ownerName, arrayOfItemSpec, versionSpec, deletedState, ItemType.Any, generateDownloadUrl)
          .getItemSet();
      }
    });

    TFSVcs.assertTrue(items != null && items.length == 1);
    //noinspection ConstantConditions
    Item[] resultItems = items[0].getItems().getItem();
    if (resultItems != null) {
      TFSVcs.assertTrue(resultItems.length == 1);
      return resultItems[0];
    }
    return null;
  }


  public Changeset queryChangeset(final int changesetId) throws TfsException {
    return WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<Changeset>() {
      public Changeset executeRequest() throws RemoteException {
        return myRepository.QueryChangeset(changesetId, true, false);
      }
    });
  }

  //public Map<ItemPath, Item> queryItems(final String workspaceName,
  //                                      final String ownerName,
  //                                      final List<ItemPath> paths,
  //                                      final VersionSpec versionSpec,
  //                                      final DeletedState deletedState,
  //                                      final boolean generateDownloadUrl) throws TfsException {
  //  final List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>();
  //  for (ItemPath itemPath : paths) {
  //    ItemSpec iSpec = new ItemSpec();
  //    iSpec.setItem(itemPath.getServerPath());
  //    iSpec.setRecurse(RecursionType.None);
  //    itemSpecs.add(iSpec);
  //  }
  //  final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
  //  arrayOfItemSpec.setItemSpec(itemSpecs.toArray(new ItemSpec[itemSpecs.size()]));
  //
  //  ItemSet[] items = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ItemSet[]>() {
  //    public ItemSet[] executeRequest() throws RemoteException {
  //      return myRepository
  //        .QueryItems(workspaceName, ownerName, arrayOfItemSpec, versionSpec, deletedState, ItemType.Any, generateDownloadUrl)
  //        .getItemSet();
  //    }
  //  });
  //
  //  TFSVcs.assertTrue(items != null && items.length == paths.size());
  //  Map<ItemPath, Item> result = new HashMap<ItemPath, Item>();
  //  //noinspection ConstantConditions
  //  for (int i = 0; i < items.length; i++) {
  //    Item[] resultItems = items[i].getItems().getItem();
  //    Item item = null;
  //    if (resultItems != null) {
  //      TFSVcs.assertTrue(resultItems.length == 1);
  //      item = resultItems[0];
  //    }
  //    result.put(paths.get(i), item);
  //  }
  //
  //  return result;
  //}

}

// ***************************************************
// not used by now
//
//  public LabelResult[] createLabel(VersionControlLabel label, LabelItemSpec[] labelSpecs, LabelChildOption childOption) throws Exception {
//    Workspace workspace = getWorkspace(labelSpecs[0].getItemSpec().getItem());
//    ArrayOfLabelItemSpec ls = new ArrayOfLabelItemSpec();
//    ls.setLabelItemSpec(labelSpecs);
//    return myRepository.LabelItem(workspace.getName(), workspace.getOwner(), label, ls, childOption).getLabelItemResult()
//      .getLabelResult();
//  }
//
//  public LabelResult[] unlabelItem(String labelName, String labelScope, ItemSpec[] itemSpecs, VersionSpec version) throws Exception {
//    Workspace workspace = getWorkspace(itemSpecs[0].getItem());
//    ArrayOfItemSpec is = new ArrayOfItemSpec();
//    is.setItemSpec(itemSpecs);
//    return myRepository.UnlabelItem(workspace.getName(), workspace.getOwner(), labelName, labelScope, is, version).getUnlabelItemResult()
//      .getLabelResult();
//  }
//
//  public Workspace createWorkspace(String name, String owner) throws UnknownHostException, RemoteException {
//    return createWorkspace(name, owner, null, new WorkingFolder[0], Workstation.getComputerName());
//  }
//
//  @Nullable
//  public ArrayOfBranchRelative[] getBranchHistory(ItemSpec[] itemSpecs, VersionSpec version) throws Exception {
//    if (itemSpecs.length == 0) {
//      return null;
//    }
//    ArrayOfItemSpec is = new ArrayOfItemSpec();
//    is.setItemSpec(itemSpecs);
//    Workspace workspace = getWorkspace(itemSpecs[0].getItem());
//    return myRepository.QueryBranches(workspace.getName(), workspace.getOwner(), is, version).getArrayOfBranchRelative();
//  }
//
//  public Changeset getChangeset(int changesetId) throws RemoteException {
//    return getChangeset(changesetId, false, false);
//  }
//
//  public Changeset getChangeset(int changesetId, boolean includeChanges, boolean includeDownloadInfo) throws RemoteException {
//    return myRepository.QueryChangeset(changesetId, includeChanges, includeDownloadInfo);
//  }
//
//  public int getLatestChangesetId() throws RemoteException {
//    RepositoryProperties properties = myRepository.GetRepositoryProperties(new GetRepositoryProperties());
//    return properties.getLcset();
//  }
//
//  public Enumeration queryHistory(String path,
//                                  VersionSpec version,
//                                  int deletionId,
//                                  RecursionType recursion,
//                                  String user,
//                                  VersionSpec versionFrom,
//                                  VersionSpec versionTo,
//                                  int maxCount,
//                                  boolean includeChanges,
//                                  boolean slotMode) throws Exception {
//    return queryHistory(path, version, deletionId, recursion, user, versionFrom, versionTo, maxCount, includeChanges, slotMode, false);
//  }
//
//  public VersionControlLabel[] queryLabels(String labelName, String labelScope, String owner, boolean includeItems) throws RemoteException {
//    return myRepository.QueryLabels(null, null, labelName, labelScope, owner, null, VersionSpecBase.getLatest(), includeItems, false)
//      .getVersionControlLabel();
//  }
//
//  public VersionControlLabel[] queryLabels(String labelName,
//                                           String labelScope,
//                                           String owner,
//                                           boolean includeItems,
//                                           String filterItem,
//                                           VersionSpec versionFilterItem) throws RemoteException {
//    return myRepository.QueryLabels(null, null, labelName, labelScope, owner, filterItem, versionFilterItem, includeItems, false)
//      .getVersionControlLabel();
//  }
//
//  public VersionControlLabel[] queryLabels(String labelName,
//                                           String labelScope,
//                                           String owner,
//                                           boolean includeItems,
//                                           String filterItem,
//                                           VersionSpec versionFilterItem,
//                                           boolean generateDownloadUrls) throws RemoteException {
//    return myRepository
//      .QueryLabels(null, null, labelName, labelScope, owner, filterItem, versionFilterItem, includeItems, generateDownloadUrls)
//      .getVersionControlLabel();
//  }
//
//  public ItemSecurity[] getPermissions(String[] items, RecursionType recursion) {
//    return getPermissions(null, items, recursion);
//  }
//
//  public Shelveset[] queryShelvesets(String shelvesetName, String shelvesetOwner) throws RemoteException {
//    return myRepository.QueryShelvesets(shelvesetName, shelvesetOwner).getShelveset();
//  }
//
//
//  public Guid getServerGuid() {
//    return myGuid;
//  }
//
//  private Repository getRepository() {
//    return myRepository;
//  }
//
//  URI getUri() {
//    return myUri;
//  }
//
///////////////////////////////////////////////////////////////////////////////////////////////
//// TO IMPLEMENT
//
//  private static String convertToFullPath(final String localPath) {
//    // todo: implement convertion from local to full path
//    return localPath;
//  }
//
//  public Workspace createWorkspace(String name, String owner, String comment, WorkingFolder[] folders, String computer)
//    throws RemoteException {
//    // todo: we cannot not set referense to 'this' like in opentfs,
//    // because our generated calss for WorkspaceInfo does not have such field.
//    // In opentfs referense to 'this' allows get Repository from WorkspaceInfo,
//    // Maybe we must create TFSWorkspace class with ref to VersionControlServer?
//    Workspace w1 = new Workspace();
//    w1.setName(name);
//    w1.setOwner(owner);
//    w1.setComment(comment);
//    w1.getFolders().setWorkingFolder(folders);
//    w1.setComputer(computer);
//    Workspace w2 = myRepository.CreateWorkspace(w1);
//    Workstation.getInstance().addWorkspace(getServerGuid(), myUri, w2);
//    return w2;
//  }
//
//  public Workspace getWorkspace(final @NotNull WorkspaceInfo info) {
//    // todo: we cannot not set referense to 'this' like in opentfs,
//    // because our generated calss for WorkspaceInfo does not have such field.
//    // In opentfs referense to 'this' allows get Repository from WorkspaceInfo,
//    // Maybe we must create TFSWorkspace class with ref to VersionControlServer?
//    Workspace w = new Workspace();
//    w.setName(info.getName());
//    w.setOwner(info.getOwnerName());
//    w.setComment(info.getComment());
//    w.getFolders().setWorkingFolder(new WorkingFolder[0]);
//    w.setComputer(info.getComputer());
//    return w;
//  }
//
//  public Workspace getWorkspace(String localPath) throws TfsException {
//    String path = convertToFullPath(localPath);
//
//    WorkspaceInfo info = null; // TODO !!! Workstation.getInstance().findWorkspace(path);
//    if (info == null) {
//      throw new IllegalArgumentException("Item not mapped " + path);
//    }
//
//    Workspace w = new Workspace();
//    // todo: we cannot not set referense to 'this' like in opentfs,
//    // because our generated calss for WorkspaceInfo does not have such field.
//    // In opentfs referense to 'this' allows get Repository from WorkspaceInfo,
//    // Maybe we must create TFSWorkspace class with ref to VersionControlServer?
//    w.setName(info.getName());
//    w.setOwner(info.getOwnerName());
//    w.setComment(info.getComment());
//    w.getFolders().setWorkingFolder(new WorkingFolder[0]);
//    w.setComputer(Workstation.getInstance().getComputerName());
//    return w;
//  }
//
//
//  public ItemSecurity[] getPermissions(String[] identityNames, String[] items, RecursionType recursion) {
//    return new ItemSecurity[0];
//
//    // todo: our stubs differ from opentf' ones. have to find out difference
//    //return myRepository.QueryItemPermissions(identityNames, items, recursion);
//  }
//
//  public Enumeration queryHistory(String path,
//                                  final VersionSpec version,
//                                  int deletionId,
//                                  RecursionType recursion,
//                                  final String user,
//                                  final VersionSpec versionFrom,
//                                  VersionSpec versionToOrig,
//                                  int maxCount,
//                                  final boolean includeChanges,
//                                  final boolean slotMode,
//                                  final boolean includeDownloadInfo) throws TfsException {
//    final ItemSpec itemSpec = new ItemSpec();
//    itemSpec.setItem(path);
//    itemSpec.setRecurse(recursion);
//    itemSpec.setDid(deletionId);
//
//    final Ref<String> workspaceName = new Ref<String>("");
//    final Ref<String> workspaceOwner = new Ref<String>("");
//
//    if (!VersionControlPath.isServerItem(itemSpec.getItem())) {
//      WorkspaceInfo info = null; // TODO !!!  Workstation.getInstance().findWorkspace(itemSpec.getItem());
//      if (info != null) {
//        workspaceName.set(info.getName());
//        workspaceOwner.set(info.getOwnerName());
//      }
//    }
//
//    List<Changeset> changes = new ArrayList<Changeset>();
//    int total = maxCount;
//    final VersionSpec[] versionTo = new VersionSpec[]{versionToOrig};
//
//    while (total > 0) {
//      final int batchMax = Math.min(256, total);
//      // todo: our stubs differ from opentf' ones. have to find out difference
//      Changeset[] changeSets = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<Changeset[]>() {
//        public Changeset[] executeRequest() throws RemoteException {
//          return myRepository.QueryHistory(workspaceName.get(), workspaceOwner.get(), itemSpec, version, user, versionFrom, versionTo[0],
//                                           batchMax, includeChanges, slotMode, includeDownloadInfo).getChangeset();
//        }
//      });
//      int batchCnt = changeSets.length;
//      if (batchCnt < batchMax) break;
//      total -= batchCnt;
//      Changeset lastChangeset = changes.get(changes.size() - 1);
//      versionTo[0] = new ChangesetVersionSpec(lastChangeset.getCset());
//    }
//
//    return Collections.enumeration(changes);
//  }
