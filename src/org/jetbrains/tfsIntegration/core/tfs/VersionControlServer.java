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

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSConstants;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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

  public static final int LOCAL_CONFLICT_REASON_SOURCE = 1;
  public static final int LOCAL_CONFLICT_REASON_TARGET = 3;

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

  /**
   * @param string local or server item
   */
  public static ItemSpec createItemSpec(final String string, final int deletionId, final RecursionType recursionType) {
    ItemSpec itemSpec = new ItemSpec();
    itemSpec.setItem(string);
    itemSpec.setDid(deletionId);
    itemSpec.setRecurse(recursionType);
    return itemSpec;
  }

  /**
   * @param string local or server item
   */
  public static ItemSpec createItemSpec(final String string, final RecursionType recursionType) {
    return createItemSpec(string, Integer.MIN_VALUE, recursionType);
  }

  public List<Item> queryItemsById(final int[] itemIds, final int changeSet, final boolean generateDownloadUrl) throws TfsException {
    final ArrayOfInt arrayOfInt = new ArrayOfInt();
    arrayOfInt.set_int(itemIds);
    ArrayOfItem arrayOfItems = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfItem>() {
      public ArrayOfItem executeRequest() throws RemoteException {
        return myRepository.QueryItemsById(arrayOfInt, changeSet, generateDownloadUrl);
      }
    });
    ArrayList<Item> result = new ArrayList<Item>();
    result.addAll(Arrays.asList(arrayOfItems.getItem()));
    return result;
  }

  @Nullable
  public Item queryItemById(final int itemId, final int changeSet, final boolean generateDownloadUrl) throws TfsException {

    List<Item> items = queryItemsById(new int[]{itemId}, changeSet, generateDownloadUrl);
    if (items.isEmpty()) {
      return null;
    }
    TFSVcs.assertTrue(items.size() == 1);
    return items.get(0);
  }

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

  private static ChangeRequest createChangeRequestTemplate() {
    ItemSpec itemSpec = createItemSpec(null, null);

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
        ChangeRequest changeRequest = createChangeRequestTemplate();
        changeRequest.getItem().setItem(itemPath.getServerPath());
        changeRequest.setReq(RequestType.Edit);

        File file = itemPath.getLocalPath().getIOFile();
        TFSVcs.assertTrue(file.isFile());
        changeRequest.setType(ItemType.File);

        return changeRequest;
      }
    });
  }

  public ResultWithFailures<GetOperation> createBranch(final String workspaceName,
                                                       final String workspaceOwner,
                                                       ItemPath source,
                                                       final VersionSpecBase versionSpec,
                                                       final String targetServerPath) throws TfsException {
    return pendChanges(workspaceName, workspaceOwner, Collections.singletonList(source), new ChangeRequestProvider() {
      public ChangeRequest createChangeRequest(final ItemPath itemPath) {
        ChangeRequest changeRequest = createChangeRequestTemplate();
        changeRequest.getItem().setItem(itemPath.getServerPath());
        changeRequest.getItem().setRecurse(RecursionType.Full);
        changeRequest.setReq(RequestType.Branch);
        changeRequest.setTarget(targetServerPath);
        changeRequest.setVspec(versionSpec);
        return changeRequest;
      }
    });
  }

  public ResultWithFailures<GetOperation> scheduleForAddition(final String workspaceName, final String workspaceOwner, List<ItemPath> paths)
    throws TfsException {
    return pendChanges(workspaceName, workspaceOwner, paths, new ChangeRequestProvider() {
      public ChangeRequest createChangeRequest(final ItemPath itemPath) {
        ChangeRequest changeRequest = createChangeRequestTemplate();
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
                                                              final Collection<ItemPath> paths) throws TfsException {
    return pendChanges(workspaceName, workspaceOwner, paths, new ChangeRequestProvider() {
      public ChangeRequest createChangeRequest(final ItemPath path) {
        ChangeRequest changeRequest = createChangeRequestTemplate();
        changeRequest.getItem().setItem(VersionControlPath.toTfsRepresentation(path.getLocalPath()));
        changeRequest.setReq(RequestType.Delete);
        return changeRequest;
      }
    });
  }

  public ResultWithFailures<GetOperation> rename(final String workspaceName,
                                                 final String workspaceOwner,
                                                 final Map<ItemPath, FilePath> movedPaths) throws TfsException {
    return pendChanges(workspaceName, workspaceOwner, movedPaths.keySet(), new ChangeRequestProvider() {
      public ChangeRequest createChangeRequest(final ItemPath path) {
        ChangeRequest changeRequest = createChangeRequestTemplate();
        changeRequest.getItem().setItem(VersionControlPath.toTfsRepresentation(path.getLocalPath()));
        changeRequest.setReq(RequestType.Rename);
        changeRequest.setTarget(VersionControlPath.toTfsRepresentation(movedPaths.get(path)));
        return changeRequest;
      }
    });
  }


  private ResultWithFailures<GetOperation> pendChanges(final String workspaceName,
                                                       final String workspaceOwner,
                                                       Collection<ItemPath> paths,
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

  public List<Item> getChildItems(final String parentServerItem, final boolean foldersOnly) throws TfsException {
    final ItemSpec itemSpec = new ItemSpec();
    itemSpec.setDid(Integer.MIN_VALUE);
    itemSpec.setItem(parentServerItem);
    itemSpec.setRecurse(RecursionType.OneLevel);

    final ArrayOfItemSpec itemSpecs = new ArrayOfItemSpec();
    itemSpecs.setItemSpec(new ItemSpec[]{itemSpec});

    final ArrayOfItemSet arrayOfItemSet = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfItemSet>() {
      public ArrayOfItemSet executeRequest() throws RemoteException {
        return myRepository.QueryItems(null, null, itemSpecs, LatestVersionSpec.INSTANCE, DeletedState.NonDeleted,
                                       foldersOnly ? ItemType.Folder : ItemType.Any, false);
      }
    });

    TFSVcs.assertTrue(arrayOfItemSet.getItemSet() != null && arrayOfItemSet.getItemSet().length == 1);
    final ItemSet itemSet = arrayOfItemSet.getItemSet()[0];
    if (itemSet.getItems() != null && itemSet.getItems().getItem() != null) {
      List<Item> result = new ArrayList<Item>(itemSet.getItems().getItem().length);
      for (Item item : itemSet.getItems().getItem()) {
        if (!item.getItem().equals(parentServerItem)) {
          result.add(item);
        }
      }
      return result;
    }
    else {
      return Collections.emptyList();
    }
  }

  public List<ExtendedItem> getChildItems(final String workspaceName,
                                          final String ownerName,
                                          final String parentServerPath,
                                          final DeletedState deletedState,
                                          final ItemType itemType) throws TfsException {
    List<List<ExtendedItem>> extendedItems =
      getChildItems(workspaceName, ownerName, Collections.singletonList(parentServerPath), deletedState, itemType);

    TFSVcs.assertTrue(extendedItems != null && extendedItems.size() == 1);
    return extendedItems != null ? extendedItems.get(0) : Collections.<ExtendedItem>emptyList();
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
      itemSpecList.add(createItemSpec(serverPath, recursionType));
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
      itemSpecList.add(createItemSpec(path.getServerPath(), recursionType));
    }
    return getExtendedItems(workspaceName, ownerName, itemSpecList, deletedState, itemType);
  }


  public List<List<ExtendedItem>> getExtendedItems(final String workspaceName,
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
      List<ExtendedItem> currentList = extendedItem.getExtendedItem() != null
                                       ? new ArrayList<ExtendedItem>(Arrays.asList(extendedItem.getExtendedItem()))
                                       : Collections.<ExtendedItem>emptyList();
      result.add(currentList);
    }
    return result;
  }

  @Nullable
  public ExtendedItem getExtendedItem(final String workspaceName,
                                      final String ownerName,
                                      final String itemServerPath,
                                      final DeletedState deletedState) throws TfsException {
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(new ItemSpec[]{createItemSpec(itemServerPath, RecursionType.None)});

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
    TFSVcs.assertTrue(!paths.isEmpty());
    final List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>();
    for (ItemPath itemPath : paths) {
      itemSpecs.add(createItemSpec(itemPath.getServerPath(), RecursionType.None));
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

  public static void downloadItem(final ServerInfo server, final String downloadKey, OutputStream outputStream) throws TfsException {
    final String url = server.getUri().toASCIIString() + TFSConstants.DOWNLOAD_ASMX + "?" + downloadKey;
    WebServiceHelper.httpGet(url, outputStream);
  }

  public List<Changeset> queryHistory(final String workspaceName,
                                      final String workspaceOwner,
                                      String serverPath,
                                      int deletionId,
                                      final String user,
                                      final VersionSpec itemVersion,
                                      final VersionSpec versionFrom,
                                      final VersionSpec versionTo,
                                      int maxCount,
                                      RecursionType recursionType) throws TfsException {
    // TODO: slot mode
    // TODO: include allChangeSets

    final ItemSpec itemSpec = createItemSpec(serverPath, deletionId, recursionType);
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
                                      String serverPath,
                                      int deletionId,
                                      final String user,
                                      final VersionSpec versionFrom,
                                      final VersionSpec versionTo,
                                      int maxCount

  ) throws TfsException {
    // TODO WorkspaceVersionSpec instead of LatestVersionSpec ?!!
    return queryHistory(workspaceName, workspaceOwner, serverPath, deletionId, user, LatestVersionSpec.INSTANCE, versionFrom, versionTo,
                        maxCount, RecursionType.Full);
  }

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
    return get(workspaceName, ownerName, Collections.singletonList(getRequest));
  }


  public static LocalVersionUpdate getLocalVersionUpdate(GetOperation operation) {
    LocalVersionUpdate localVersionUpdate = new LocalVersionUpdate();
    localVersionUpdate.setItemid(operation.getItemid());
    localVersionUpdate.setTlocal(operation.getTlocal());
    localVersionUpdate.setLver(operation.getSver() != Integer.MIN_VALUE ? operation.getSver() : 0); // 0 for scheduled for addition
    return localVersionUpdate;
  }

  public void updateLocalVersionsByGetOperations(final String workspaceName,
                                                 final String workspaceOwnerName,
                                                 final Collection<GetOperation> getOperations) throws TfsException {
    List<LocalVersionUpdate> localVersionUpdates = new ArrayList<LocalVersionUpdate>(getOperations.size());
    for (GetOperation operation : getOperations) {
      localVersionUpdates.add(getLocalVersionUpdate(operation));
    }
    updateLocalVersions(workspaceName, workspaceOwnerName, localVersionUpdates);
  }

  public void updateLocalVersions(final String workspaceName, final String workspaceOwnerName, final Collection<LocalVersionUpdate> updates)
    throws TfsException {
    if (updates.isEmpty()) {
      return;
    }

    final ArrayOfLocalVersionUpdate arrayOfLocalVersionUpdate = new ArrayOfLocalVersionUpdate();
    arrayOfLocalVersionUpdate.setLocalVersionUpdate(updates.toArray(new LocalVersionUpdate[updates.size()]));
    WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<UpdateLocalVersionResponse>() {
      public UpdateLocalVersionResponse executeRequest() throws RemoteException {
        return myRepository.UpdateLocalVersion(workspaceName, workspaceOwnerName, arrayOfLocalVersionUpdate);
      }
    });
  }

  public ResultWithFailures<GetOperation> undoPendingChanges(final String workspaceName,
                                                             final String workspaceOwner,
                                                             final Collection<ItemPath> paths) throws TfsException {
    List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>(paths.size());
    for (ItemPath itemPath : paths) {
      itemSpecs.add(createItemSpec(itemPath.getServerPath(), null));
    }
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(itemSpecs.toArray(new ItemSpec[itemSpecs.size()]));

    UndoPendingChangesResponse response =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<UndoPendingChangesResponse>() {
        public UndoPendingChangesResponse executeRequest() throws RemoteException {
          return myRepository.UndoPendingChanges(workspaceName, workspaceOwner, arrayOfItemSpec);
        }
      });

    GetOperation[] getOperations =
      response.getUndoPendingChangesResult() != null ? response.getUndoPendingChangesResult().getGetOperation() : null;
    Failure[] failures = response.getFailures() != null ? response.getFailures().getFailure() : null;
    return new ResultWithFailures<GetOperation>(getOperations, failures);
  }

  public List<GetOperation> get(final String workspaceName, final String workspaceOwner, final List<GetRequestParams> requests)
    throws TfsException {
    if (requests.isEmpty()) {
      return Collections.emptyList();
    }

    List<GetRequest> getRequests = new ArrayList<GetRequest>(requests.size());
    for (GetRequestParams getRequestParams : requests) {
      final GetRequest getRequest = new GetRequest();
      getRequest.setItemSpec(createItemSpec(getRequestParams.serverPath, getRequestParams.recursionType));
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

    List<GetOperation> results = new ArrayList<GetOperation>();
    for (ArrayOfGetOperation arrayOfGetOperation : response.getArrayOfGetOperation()) {
      if (arrayOfGetOperation.getGetOperation() != null) {
        results.addAll(Arrays.asList(arrayOfGetOperation.getGetOperation()));
      }
    }
    return results;
  }

  public void addLocalConflict(final String workspaceName,
                               final String workspaceOwner,
                               final int itemId,
                               final int versionFrom,
                               final int pendingChangeId,
                               final String sourceLocal,
                               final String targetLocal,
                               final int reason) throws TfsException {

    WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<AddConflictResponse>() {
      public AddConflictResponse executeRequest() throws RemoteException {
        return myRepository
          .AddConflict(workspaceName, workspaceOwner, ConflictType.Local, itemId, versionFrom, pendingChangeId, sourceLocal, targetLocal,
                       reason);
      }
    });
  }


  public List<Conflict> queryConflicts(final String workspaceName,
                                       final String ownerName,
                                       final List<ItemPath> paths,
                                       final RecursionType recursionType) throws TfsException {
    List<ItemSpec> itemSpecList = new ArrayList<ItemSpec>();
    for (ItemPath path : paths) {
      itemSpecList.add(createItemSpec(path.getServerPath(), recursionType));
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

  public Collection<PendingChange> queryPendingSetsByPaths(final String workspaceName,
                                                           final String workspaceOwnerName,
                                                           final Collection<ItemPath> paths,
                                                           final RecursionType recursionType) throws TfsException {
    final Collection<ItemSpec> itemSpecs = new ArrayList<ItemSpec>(paths.size());
    for (ItemPath path : paths) {
      itemSpecs.add(createItemSpec(VersionControlPath.toTfsRepresentation(path.getLocalPath()), recursionType));
    }
    return doQueryPendingSets(workspaceName, workspaceOwnerName, itemSpecs);
  }

  public Collection<PendingChange> queryPendingSetsByServerItems(final String workspaceName,
                                                                 final String workspaceOwnerName,
                                                                 final Collection<String> serverItems,
                                                                 final RecursionType recursionType) throws TfsException {
    final List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>(serverItems.size());
    for (String serverItem : serverItems) {
      itemSpecs.add(createItemSpec(serverItem, recursionType));
    }
    return doQueryPendingSets(workspaceName, workspaceOwnerName, itemSpecs);
  }

  private Collection<PendingChange> doQueryPendingSets(final String workspaceName,
                                                       final String workspaceOwnerName,
                                                       final Collection<ItemSpec> itemSpecs) throws TfsException {
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(itemSpecs.toArray(new ItemSpec[itemSpecs.size()]));

    PendingSet[] pendingSets = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<PendingSet[]>() {
      public PendingSet[] executeRequest() throws RemoteException {
        return myRepository.QueryPendingSets(workspaceName, workspaceOwnerName, workspaceName, workspaceOwnerName, arrayOfItemSpec, false)
          .getQueryPendingSetsResult().getPendingSet();
      }
    });

    if (pendingSets != null) {
      TFSVcs.assertTrue(pendingSets.length == 1);
      return Arrays.asList(pendingSets[0].getPendingChanges().getPendingChange());
    }
    else {
      return Collections.emptyList();
    }
  }

  public ResultWithFailures<CheckinResult> checkIn(final String workspaceName,
                                                   final String workspaceOwnerName,
                                                   final Collection<String> serverItems,
                                                   final String comment) throws TfsException {
    final ArrayOfString serverItemsArray = new ArrayOfString();
    for (String serverItem : serverItems) {
      serverItemsArray.addString(serverItem);
    }
    final Changeset changeset = new Changeset();
    changeset.setCset(0);
    changeset.setDate(new GregorianCalendar());
    changeset.setOwner(workspaceOwnerName);
    changeset.setComment(comment);
    final CheckinNotificationInfo checkinNotificationInfo = new CheckinNotificationInfo();
    final String checkinOptions = "ValidateCheckinOwner"; // TODO checkin options

    CheckInResponse response = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<CheckInResponse>() {
      public CheckInResponse executeRequest() throws RemoteException {
        return myRepository
          .CheckIn(workspaceName, workspaceOwnerName, serverItemsArray, changeset, checkinNotificationInfo, checkinOptions);
      }
    });

    ResultWithFailures<CheckinResult> result = new ResultWithFailures<CheckinResult>();
    if (response.getCheckInResult() != null) {
      result.getResult().add(response.getCheckInResult());
    }

    if (response.getFailures().getFailure() != null) {
      result.getFailures().addAll(Arrays.asList(response.getFailures().getFailure()));
    }
    return result;
  }

  public MergeResponse merge(final String workspaceName,
                             final String ownerName,
                             String sourceServerPath,
                             String targetServerPath,
                             final VersionSpecBase fromVersion,
                             final VersionSpecBase toVersion) throws TfsException {

    final ItemSpec source = createItemSpec(sourceServerPath, RecursionType.Full);
    final ItemSpec target = createItemSpec(sourceServerPath, null);

    return WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<MergeResponse>() {
      public MergeResponse executeRequest() throws RemoteException {
        return myRepository
          .Merge(workspaceName, ownerName, source, target, fromVersion, toVersion, MergeOptions.None.name(), LockLevel.Unchanged);
      }
    });
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
  //    itemSpec.setItem(path.getSelectedPath());
  //    itemSpec.setRecurse(recursionType);
  //    itemSpecList.add(itemSpec);
  //  }
  //  return queryItems(workspaceName, ownerName, itemSpecList, versionSpec, deletedState, itemType, generateDownloadUrl);
  //}

  //public List<List<Item>> queryItems(final String workspaceName,
  //                                   final String ownerName,
  //                                   final List<ItemSpec> itemsSpecs,
  //                                   final VersionSpec versionSpec,
  //                                   final DeletedState deletedState,
  //                                   final ItemType itemType,
  //                                   final boolean generateDownloadUrl) throws TfsException {
  //  final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
  //  arrayOfItemSpec.setItemSpec(itemsSpecs.toArray(new ItemSpec[itemsSpecs.size()]));
  //  List<List<Item>> result = new ArrayList<List<Item>>();
  //  ItemSet[] items = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ItemSet[]>() {
  //    public ItemSet[] executeRequest() throws RemoteException {
  //      return myRepository.QueryItems(workspaceName, ownerName, arrayOfItemSpec, versionSpec, deletedState, itemType, generateDownloadUrl)
  //        .getItemSet();
  //    }
  //  });
  //
  //  TFSVcs.assertTrue(items != null && items.length == itemsSpecs.size());
  //  //noinspection ConstantConditions
  //  for (ItemSet item : items) {
  //    List<Item> resultItemsList = new ArrayList<Item>();
  //    Item[] resultItems = item.getItems().getItem();
  //    if (resultItems != null) {
  //      resultItemsList.addAll(Arrays.asList(resultItems));
  //    }
  //    result.add(resultItemsList);
  //  }
  //  return result;
  //}

  @Nullable
  public Item queryItem(final String workspaceName,
                        final String ownerName,
                        final String itemServerPath,
                        final VersionSpec versionSpec,
                        final DeletedState deletedState,
                        final boolean generateDownloadUrl) throws TfsException {
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(new ItemSpec[]{createItemSpec(itemServerPath, RecursionType.None)});

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


  public List<VersionControlLabel> queryLabels(final String labelName,
                                               final String labelScope,
                                               final String owner,
                                               final boolean includeItems,
                                               final String filterItem,
                                               final VersionSpec versionFilterItem,
                                               final boolean generateDownloadUrls) throws TfsException {
    VersionControlLabel[] labels = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<VersionControlLabel[]>() {
      public VersionControlLabel[] executeRequest() throws RemoteException {
        return myRepository
          .QueryLabels(null, null, labelName, labelScope, owner, filterItem, versionFilterItem, includeItems, generateDownloadUrls)
          .getVersionControlLabel();
      }
    });
    ArrayList<VersionControlLabel> result = new ArrayList<VersionControlLabel>();
    if (labels != null) {
      result.addAll(Arrays.asList(labels));
    }
    return result;
  }

  public Collection<BranchRelative> queryBranches(final String workspaceName,
                                                  final String ownerName,
                                                  final String itemServerPath,
                                                  final VersionSpec versionSpec) throws TfsException {
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(new ItemSpec[]{createItemSpec(itemServerPath, null)});

    ArrayOfArrayOfBranchRelative result =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfArrayOfBranchRelative>() {
        public ArrayOfArrayOfBranchRelative executeRequest() throws RemoteException {
          return myRepository.QueryBranches(workspaceName, ownerName, arrayOfItemSpec, versionSpec);
        }
      });

    TFSVcs.assertTrue(result.getArrayOfBranchRelative().length == 1);
    final BranchRelative[] branches = result.getArrayOfBranchRelative()[0].getBranchRelative();
    return branches != null ? Arrays.asList(branches) : Collections.<BranchRelative>emptyList();
  }

  public Collection<MergeCandidate> queryMergeCandidates(final String workspaceName,
                                                         final String ownerName,
                                                         String sourceServerPath,
                                                         String targetServerPath) throws TfsException {
    final ItemSpec source = createItemSpec(sourceServerPath, RecursionType.Full);
    final ItemSpec target = createItemSpec(targetServerPath, RecursionType.Full);

    final ArrayOfMergeCandidate result =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfMergeCandidate>() {
        public ArrayOfMergeCandidate executeRequest() throws RemoteException {
          return myRepository.QueryMergeCandidates(workspaceName, ownerName, source, target);
        }
      });
    return result.getMergeCandidate() != null ? Arrays.asList(result.getMergeCandidate()) : Collections.<MergeCandidate>emptyList();
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
  //    iSpec.setItem(itemPath.getSelectedPath());
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
//  public VersionControlLabel[] queryLabels(String labelName, String labelScope, String owner, boolean includeItems) throws RemoteException {
//    return myRepository.QueryLabels(null, null, labelName, labelScope, owner, null, VersionSpecBase.getLatest(), includeItems, false)
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
