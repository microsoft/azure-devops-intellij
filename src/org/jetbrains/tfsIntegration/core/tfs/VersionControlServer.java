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
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSConstants;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemField;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemSerialize;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.ClientServiceClientServiceSoap12Stub;
import org.jetbrains.tfsIntegration.stubs.GroupSecurityServiceGroupSecurityServiceSoap12Stub;
import org.jetbrains.tfsIntegration.stubs.RepositoryRepositorySoap12Stub;
import org.jetbrains.tfsIntegration.stubs.services.authorization.Identity;
import org.jetbrains.tfsIntegration.stubs.services.authorization.QueryMembership;
import org.jetbrains.tfsIntegration.stubs.services.authorization.SearchFactor;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ArrayOfInt;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ArrayOfString;
import org.jetbrains.tfsIntegration.stubs.workitemtracking.clientservices.*;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.*;

public class VersionControlServer {
  //private URI myUri;
  //private Guid myGuid = new Guid();

  //public static final String Upload = "Upload";
  //public static final String Download = "Download";
  //public static final int EncodingBinary = -1;
  @NonNls public static final String WORKSPACE_NAME_FIELD = "wsname";
  @NonNls public static final String WORKSPACE_OWNER_FIELD = "wsowner";
  @NonNls public static final String RANGE_FIELD = "range";
  @NonNls public static final String LENGTH_FIELD = "filelength";
  @NonNls public static final String HASH_FIELD = "hash";
  @NonNls public static final String SERVER_ITEM_FIELD = "item";
  @NonNls public static final String CONTENT_FIELD = "content";

  public static final int LOCAL_CONFLICT_REASON_SOURCE = 1;
  public static final int LOCAL_CONFLICT_REASON_TARGET = 3;

  private final RepositoryRepositorySoap12Stub myRepository;
  private final ClientServiceClientServiceSoap12Stub myWorkItemTrackingClientService;
  private final GroupSecurityServiceGroupSecurityServiceSoap12Stub myGroupSecurityService;

  public VersionControlServer(URI uri) {
    //myUri = uri;
    try {
      final ConfigurationContext configContext = WebServiceHelper.getStubConfigurationContext();
      myRepository = new RepositoryRepositorySoap12Stub(configContext, uri.toString() + TFSConstants.VERSION_CONTROL_ASMX);
      myWorkItemTrackingClientService =
        new ClientServiceClientServiceSoap12Stub(configContext, uri.toString() + TFSConstants.WORK_ITEM_TRACKING_CLIENT_SERVICE_ASMX);
      myGroupSecurityService =
        new GroupSecurityServiceGroupSecurityServiceSoap12Stub(configContext, uri.toString() + TFSConstants.GROUP_SECURITY_SERVICE_ASMX);
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
  public static ItemSpec createItemSpec(final String string, final RecursionType recursionType) {
    return createItemSpec(string, Integer.MIN_VALUE, recursionType);
  }

  public static ItemSpec createItemSpec(final FilePath localPath, final RecursionType recursionType) {
    return createItemSpec(VersionControlPath.toTfsRepresentation(localPath), Integer.MIN_VALUE, recursionType);
  }

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

  private interface ChangeRequestProvider<T> {
    ChangeRequest createChangeRequest(T path);
  }

  private static ChangeRequest createChangeRequestTemplate() {
    ItemSpec itemSpec = createItemSpec((String)null, null);

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
    return pendChanges(workspaceName, workspaceOwner, paths, false, new ChangeRequestProvider<ItemPath>() {
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
                                                       final String sourceServerPath,
                                                       final VersionSpecBase versionSpec,
                                                       final String targetServerPath) throws TfsException {
    return pendChanges(workspaceName, workspaceOwner, Collections.singletonList(sourceServerPath), false,
                       new ChangeRequestProvider<String>() {
                         public ChangeRequest createChangeRequest(final String serverPath) {
                           ChangeRequest changeRequest = createChangeRequestTemplate();
                           changeRequest.getItem().setItem(serverPath);
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
    return pendChanges(workspaceName, workspaceOwner, paths, false, new ChangeRequestProvider<ItemPath>() {
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

  public ResultWithFailures<GetOperation> scheduleForDeletionAndUpateLocalVersion(final String workspaceName,
                                                                                  final String workspaceOwner,
                                                                                  final Collection<FilePath> localPaths)
    throws TfsException {
    return pendChanges(workspaceName, workspaceOwner, localPaths, true, new ChangeRequestProvider<FilePath>() {
      public ChangeRequest createChangeRequest(final FilePath localPath) {
        ChangeRequest changeRequest = createChangeRequestTemplate();
        changeRequest.getItem().setItem(VersionControlPath.toTfsRepresentation(localPath));
        changeRequest.setReq(RequestType.Delete);
        return changeRequest;
      }
    });
  }

  public ResultWithFailures<GetOperation> renameAndUpdateLocalVersion(final String workspaceName,
                                                                      final String workspaceOwner,
                                                                      final Map<FilePath, FilePath> movedPaths) throws TfsException {
    return pendChanges(workspaceName, workspaceOwner, movedPaths.keySet(), true, new ChangeRequestProvider<FilePath>() {
      public ChangeRequest createChangeRequest(final FilePath localPath) {
        ChangeRequest changeRequest = createChangeRequestTemplate();
        changeRequest.getItem().setItem(VersionControlPath.toTfsRepresentation(localPath));
        changeRequest.setReq(RequestType.Rename);
        changeRequest.setTarget(VersionControlPath.toTfsRepresentation(movedPaths.get(localPath)));
        return changeRequest;
      }
    });
  }

  public ResultWithFailures<GetOperation> lockOrUnlockItems(final String workspaceName,
                                                            final String workspaceOwner,
                                                            final LockLevel lockLevel,
                                                            final Collection<ExtendedItem> items) throws TfsException {
    return pendChanges(workspaceName, workspaceOwner, items, false, new ChangeRequestProvider<ExtendedItem>() {
      public ChangeRequest createChangeRequest(final ExtendedItem item) {
        ChangeRequest changeRequest = createChangeRequestTemplate();
        changeRequest.getItem().setItem(item.getSitem());
        changeRequest.setReq(RequestType.Lock);
        changeRequest.setLock(lockLevel);
        return changeRequest;
      }
    });
  }

  private <T> ResultWithFailures<GetOperation> pendChanges(final String workspaceName,
                                                           final String workspaceOwner,
                                                           Collection<T> paths,
                                                           final boolean updateLocalVersion,
                                                           ChangeRequestProvider<T> changeRequestProvider) throws TfsException {
    ResultWithFailures<GetOperation> result = new ResultWithFailures<GetOperation>();
    if (paths.isEmpty()) {
      return result;
    }

    List<ChangeRequest> changeRequests = new ArrayList<ChangeRequest>(paths.size());
    for (T path : paths) {
      changeRequests.add(changeRequestProvider.createChangeRequest(path));
    }

    final ArrayOfChangeRequest arrayOfChangeRequest = new ArrayOfChangeRequest();
    arrayOfChangeRequest.setChangeRequest(changeRequests.toArray(new ChangeRequest[changeRequests.size()]));

    PendChangesResponse response = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<PendChangesResponse>() {
      public PendChangesResponse executeRequest() throws RemoteException {
        final PendChangesResponse response = myRepository.PendChanges(workspaceName, workspaceOwner, arrayOfChangeRequest);
        if (updateLocalVersion && response.getPendChangesResult().getGetOperation() != null) {
          final ArrayOfLocalVersionUpdate arrayOfLocalVersionUpdate = new ArrayOfLocalVersionUpdate();
          List<LocalVersionUpdate> localVersionUpdates =
            new ArrayList<LocalVersionUpdate>(response.getPendChangesResult().getGetOperation().length);
          for (GetOperation getOperation : response.getPendChangesResult().getGetOperation()) {
            localVersionUpdates.add(getLocalVersionUpdate(getOperation));
          }
          arrayOfLocalVersionUpdate.setLocalVersionUpdate(localVersionUpdates.toArray(new LocalVersionUpdate[localVersionUpdates.size()]));
          myRepository.UpdateLocalVersion(workspaceName, workspaceOwner, arrayOfLocalVersionUpdate);
        }
        return response;
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
    final ArrayOfItemSpec itemSpecs = new ArrayOfItemSpec();
    itemSpecs.setItemSpec(new ItemSpec[]{createItemSpec(parentServerItem, RecursionType.OneLevel)});

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

  public static class ExtendedItemsAndPendingChanges {
    public final List<List<ExtendedItem>> extendedItems;
    public final Collection<PendingChange> pendingChanges;

    public ExtendedItemsAndPendingChanges(final Collection<PendingChange> pendingChanges, final List<List<ExtendedItem>> extendedItems) {
      this.pendingChanges = pendingChanges;
      this.extendedItems = extendedItems;
    }
  }

  public ExtendedItemsAndPendingChanges getExtendedItemsAndPendingChanges(final String workspaceName,
                                                                          final String ownerName,
                                                                          final List<ItemSpec> itemsSpecs,
                                                                          final ItemType itemType) throws TfsException {
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(itemsSpecs.toArray(new ItemSpec[itemsSpecs.size()]));
    final Ref<ArrayOfExtendedItem[]> extendedItemsRef = new Ref<ArrayOfExtendedItem[]>();
    final Ref<PendingSet[]> pendingSetsRef = new Ref<PendingSet[]>();
    WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.VoidDelegate() {
      public void executeRequest() throws RemoteException {
        extendedItemsRef.set(myRepository
          .QueryItemsExtended(workspaceName, ownerName, arrayOfItemSpec, DeletedState.NonDeleted, itemType).getArrayOfExtendedItem());
        pendingSetsRef.set(myRepository.QueryPendingSets(workspaceName, ownerName, workspaceName, ownerName, arrayOfItemSpec, false)
          .getQueryPendingSetsResult().getPendingSet());
      }
    });

    TFSVcs.assertTrue(extendedItemsRef.get() != null && extendedItemsRef.get().length == itemsSpecs.size());

    List<List<ExtendedItem>> extendedItems = new ArrayList<List<ExtendedItem>>();
    for (ArrayOfExtendedItem extendedItem : extendedItemsRef.get()) {
      List<ExtendedItem> currentList = extendedItem.getExtendedItem() != null
                                       ? new ArrayList<ExtendedItem>(Arrays.asList(extendedItem.getExtendedItem()))
                                       : Collections.<ExtendedItem>emptyList();
      // no need to chooseExtendedItem() since DeletedState.NonDeleted specified
      extendedItems.add(currentList);
    }

    final Collection<PendingChange> pendingChanges;
    if (pendingSetsRef.get() != null) {
      TFSVcs.assertTrue(pendingSetsRef.get().length == 1);
      pendingChanges = Arrays.asList(pendingSetsRef.get()[0].getPendingChanges().getPendingChange());
    }
    else {
      pendingChanges = Collections.emptyList();
    }

    return new ExtendedItemsAndPendingChanges(pendingChanges, extendedItems);
  }

  @Nullable
  public ExtendedItem getExtendedItem(final String workspaceName,
                                      final String ownerName,
                                      final FilePath localPath,
                                      final RecursionType recursionType,
                                      final DeletedState deletedState) throws TfsException {
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(new ItemSpec[]{createItemSpec(localPath, recursionType)});

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
      return chooseExtendedItem(resultItems);
    }

    return null;
  }

  private static ExtendedItem chooseExtendedItem(ExtendedItem[] extendedItems) {
    // server may report more than one extended item for given local name if item with same name was created and deleted several times

    TFSVcs.assertTrue(extendedItems.length > 0);
    if (extendedItems.length > 1) {
      // choose item that has non-null 'local' field...
      for (ExtendedItem candidate : extendedItems) {
        if (candidate.getLocal() != null) {
          return candidate;
        }
      }

      //  ...or latest one if not found
      ExtendedItem latest = extendedItems[0];
      for (ExtendedItem candidate : extendedItems) {
        if (candidate.getLocal() != null) {
          if (candidate.getLatest() > latest.getLatest()) {
            latest = candidate;
          }
        }
      }
      return latest;
    }
    else {
      return extendedItems[0];
    }
  }

  public Map<FilePath, ExtendedItem> getExtendedItems(final String workspaceName,
                                                      final String ownerName,
                                                      final List<FilePath> paths,
                                                      final DeletedState deletedState) throws TfsException {
    TFSVcs.assertTrue(!paths.isEmpty());
    final List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>();
    for (FilePath path : paths) {
      itemSpecs.add(createItemSpec(path, RecursionType.None));
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
    Map<FilePath, ExtendedItem> result = new HashMap<FilePath, ExtendedItem>();

    //noinspection ConstantConditions
    for (int i = 0; i < extendedItems.length; i++) {
      ExtendedItem[] resultItems = extendedItems[i].getExtendedItem();
      ExtendedItem item = null;
      if (resultItems != null) {
        item = chooseExtendedItem(resultItems);
      }
      result.put(paths.get(i), item);
    }

    return result;
  }

  public static void downloadItem(final ServerInfo server, final String downloadKey, OutputStream outputStream) throws TfsException {
    final String url = server.getUri().toASCIIString() + TFSConstants.DOWNLOAD_ASMX + "?" + downloadKey;
    WebServiceHelper.httpGet(url, outputStream);
  }

  public List<Changeset> queryHistory(final WorkspaceInfo workspace,
                                      final String serverPath,
                                      final boolean recursive,
                                      final String user,
                                      final VersionSpec versionFrom,
                                      final VersionSpec versionTo) throws TfsException {
    final VersionSpec itemVersion = LatestVersionSpec.INSTANCE;
    ItemSpec itemSpec = createItemSpec(serverPath, recursive ? RecursionType.Full : null);
    return queryHistory(workspace.getName(), workspace.getOwnerName(), itemSpec, user, itemVersion, versionFrom, versionTo,
                        Integer.MAX_VALUE);
  }

  public List<Changeset> queryHistory(final String workspaceName,
                                      final String workspaceOwner,
                                      final ItemSpec itemSpec,
                                      final String user,
                                      final VersionSpec itemVersion,
                                      final VersionSpec versionFrom,
                                      final VersionSpec versionTo,
                                      int maxCount) throws TfsException {
    // TODO: slot mode
    // TODO: include allChangeSets

    List<Changeset> allChangeSets = new ArrayList<Changeset>();
    int total = maxCount > 0 ? maxCount : Integer.MAX_VALUE;
    final Ref<VersionSpec> versionToCurrent = new Ref<VersionSpec>(versionTo);

    while (total > 0) {
      final int batchMax = Math.min(256, total);

      Changeset[] currentChangeSets = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<Changeset[]>() {
        public Changeset[] executeRequest() throws RemoteException {
          return myRepository
            .QueryHistory(workspaceName, workspaceOwner, itemSpec, itemVersion, user, versionFrom, versionToCurrent.get(), batchMax, true,
                          false, false).getChangeset();
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
                                                             final Collection<String> serverPaths) throws TfsException {
    List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>(serverPaths.size());
    for (String serverPath : serverPaths) {
      itemSpecs.add(createItemSpec(serverPath, null));
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


  public Collection<Conflict> queryConflicts(final String workspaceName,
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
        return myRepository
          .Resolve(workspaceName, workspasceOwnerName, params.conflictId, params.resolution, params.newPath, params.encoding,
                   params.lockLevel);
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
    final byte[] hash = TfsFileUtil.calculateMD5(file);
    parts.add(new StringPart(HASH_FIELD, new String(Base64.encodeBase64(hash), "UTF-8"))); // TODO: check encoding!
// TODO: handle files too large to fit in a single POST
    parts.add(new StringPart(RANGE_FIELD, String.format("bytes=0-%d/%d", fileLength - 1, fileLength)));
    FilePart filePart = new FilePart(CONTENT_FIELD, SERVER_ITEM_FIELD, file);
    parts.add(filePart);
    filePart.setCharSet(null);
    WebServiceHelper.httpPost(uploadUrl, parts.toArray(new Part[parts.size()]), null);
  }

  public Collection<PendingChange> queryPendingSetsByLocalPaths(final String workspaceName,
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

  // TODO support checkin notes
  public ResultWithFailures<CheckinResult> checkIn(final String workspaceName,
                                                   final String workspaceOwnerName,
                                                   final Collection<String> serverItems,
                                                   final String comment,
                                                   final @NotNull Map<WorkItem, CheckinWorkItemAction> workItemsActions)
    throws TfsException {
    final String serverPathToProject = VersionControlPath.getPathToProject(serverItems.iterator().next());
    for (String serverItem : serverItems) {
      TFSVcs.assertTrue(serverPathToProject.equals(VersionControlPath.getPathToProject(serverItem)));
    }
    final List<CheckinNoteFieldDefinition> fieldDefinitions = queryCheckinNoteDefinition(serverPathToProject);
    final ArrayOfCheckinNoteFieldValue fieldValues = new ArrayOfCheckinNoteFieldValue();
    for (CheckinNoteFieldDefinition fieldDefinition : fieldDefinitions) {
      final CheckinNoteFieldValue fieldValue = new CheckinNoteFieldValue();
      fieldValue.setName(fieldDefinition.getName());
      fieldValue.setVal("");
      fieldValues.addCheckinNoteFieldValue(fieldValue);
    }

    final CheckinNote checkinNote = new CheckinNote();
    checkinNote.setValues(fieldValues);

    final PolicyOverrideInfo policyOverride = new PolicyOverrideInfo();

    final ArrayOfString serverItemsArray = new ArrayOfString();
    for (String serverItem : serverItems) {
      serverItemsArray.addString(serverItem);
    }

    final Changeset changeset = new Changeset();
    changeset.setCset(0);
    changeset.setDate(TfsUtil.getZeroCalendar());
    changeset.setOwner(workspaceOwnerName);
    changeset.setComment(comment);
    changeset.setCheckinNote(checkinNote);
    changeset.setPolicyOverride(policyOverride);
    final CheckinNotificationInfo checkinNotificationInfo = new CheckinNotificationInfo();
    checkinNotificationInfo.setWorkItemInfo(toArrayOfCheckinNotificationWorkItemInfo(workItemsActions));
    final String checkinOptions = CheckinOptions.ValidateCheckinOwner.name(); // TODO checkin options

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

  @Nullable
  private static ArrayOfCheckinNotificationWorkItemInfo toArrayOfCheckinNotificationWorkItemInfo(final @NotNull Map<WorkItem, CheckinWorkItemAction> workItemsActions) {
    if (workItemsActions.size() == 0) {
      return null;
    }
    List<CheckinNotificationWorkItemInfo> checkinNotificationWorkItemInfoArray =
      new ArrayList<CheckinNotificationWorkItemInfo>(workItemsActions.size());
    for (Map.Entry<WorkItem, CheckinWorkItemAction> e : workItemsActions.entrySet()) {
      if (e.getValue() != CheckinWorkItemAction.None) {
        CheckinNotificationWorkItemInfo checkinNotificationWorkItemInfo = new CheckinNotificationWorkItemInfo();
        checkinNotificationWorkItemInfo.setId(e.getKey().getId());
        checkinNotificationWorkItemInfo.setCheckinAction(e.getValue());
        checkinNotificationWorkItemInfoArray.add(checkinNotificationWorkItemInfo);
      }
    }

    ArrayOfCheckinNotificationWorkItemInfo arrayOfCheckinNotificationWorkItemInfo = new ArrayOfCheckinNotificationWorkItemInfo();
    arrayOfCheckinNotificationWorkItemInfo.setCheckinNotificationWorkItemInfo(
      checkinNotificationWorkItemInfoArray.toArray(new CheckinNotificationWorkItemInfo[checkinNotificationWorkItemInfoArray.size()]));
    return arrayOfCheckinNotificationWorkItemInfo;
  }

  public MergeResponse merge(final String workspaceName,
                             final String ownerName,
                             String sourceServerPath,
                             String targetServerPath,
                             final VersionSpecBase fromVersion,
                             final VersionSpecBase toVersion) throws TfsException {

    final ItemSpec source = createItemSpec(sourceServerPath, RecursionType.Full);
    final ItemSpec target = createItemSpec(targetServerPath, null);

    return WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<MergeResponse>() {
      public MergeResponse executeRequest() throws RemoteException {
        return myRepository
          .Merge(workspaceName, ownerName, source, target, fromVersion, toVersion, MergeOptions.None.name(), LockLevel.Unchanged);
      }
    });
  }

  /**
   * @return sorted accorging to 'do' attribute
   */
  private List<CheckinNoteFieldDefinition> queryCheckinNoteDefinition(final String serverPath) throws TfsException {
    final ArrayOfString associatedServerItem = new ArrayOfString();
    associatedServerItem.addString(serverPath);

    final ArrayOfCheckinNoteFieldDefinition result =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfCheckinNoteFieldDefinition>() {
        public ArrayOfCheckinNoteFieldDefinition executeRequest() throws RemoteException {
          return myRepository.QueryCheckinNoteDefinition(associatedServerItem);
        }
      });

    final List<CheckinNoteFieldDefinition> checkinNoteFields = Arrays.asList(result.getCheckinNoteFieldDefinition());
    Collections.sort(checkinNoteFields, new Comparator<CheckinNoteFieldDefinition>() {
      public int compare(final CheckinNoteFieldDefinition o1, final CheckinNoteFieldDefinition o2) {
        return o1.get_do() - o2.get_do();
      }
    });

    return checkinNoteFields;
  }

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
          .QueryItems(workspaceName, ownerName, arrayOfItemSpec, versionSpec, deletedState, ItemType.Any, generateDownloadUrl).getItemSet();
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

  public List<Item> queryItems(final ItemSpec itemSpec, final VersionSpec version) throws TfsException {
    final ArrayOfItemSpec itemSpecs = new ArrayOfItemSpec();
    itemSpecs.setItemSpec(new ItemSpec[]{itemSpec});

    final ArrayOfItemSet arrayOfItemSet = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfItemSet>() {
      public ArrayOfItemSet executeRequest() throws RemoteException {
        return myRepository.QueryItems(null, null, itemSpecs, version, DeletedState.NonDeleted, ItemType.Any, false);
      }
    });

    TFSVcs.assertTrue(arrayOfItemSet.getItemSet() != null && arrayOfItemSet.getItemSet().length == 1);

    final ItemSet itemSet = arrayOfItemSet.getItemSet()[0];
    if (itemSet.getItems() != null && itemSet.getItems().getItem() != null) {
      List<Item> result = new ArrayList<Item>(itemSet.getItems().getItem().length);
      result.addAll(Arrays.asList(itemSet.getItems().getItem()));
      return result;
    }
    else {
      return Collections.emptyList();
    }
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

  public ResultWithFailures<LabelResult> labelItem(final String labelName,
                                                   final String labelComment,
                                                   final List<LabelItemSpec> labelItemSpecs) throws TfsException {
    final VersionControlLabel versionControlLabel = new VersionControlLabel();
    versionControlLabel.setName(labelName);
    versionControlLabel.setComment(labelComment);

    versionControlLabel.setDate(TfsUtil.getZeroCalendar());

    final ArrayOfLabelItemSpec arrayOfLabelItemSpec = new ArrayOfLabelItemSpec();
    arrayOfLabelItemSpec.setLabelItemSpec(labelItemSpecs.toArray(new LabelItemSpec[labelItemSpecs.size()]));

    LabelItemResponse labelItemResponse = WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<LabelItemResponse>() {
      public LabelItemResponse executeRequest() throws RemoteException {
        return myRepository.LabelItem(null, null, versionControlLabel, arrayOfLabelItemSpec, LabelChildOption.Fail);
      }
    });

    ArrayOfLabelResult results = labelItemResponse.getLabelItemResult();
    ArrayOfFailure failures = labelItemResponse.getFailures();

    return new ResultWithFailures<LabelResult>(results == null ? null : results.getLabelResult(),
                                               failures == null ? null : failures.getFailure());
  }

  public Collection<BranchRelative> queryBranches(final String itemServerPath, final VersionSpec versionSpec) throws TfsException {
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(new ItemSpec[]{createItemSpec(itemServerPath, null)});

    ArrayOfArrayOfBranchRelative result =
      WebServiceHelper.executeRequest(myRepository, new WebServiceHelper.Delegate<ArrayOfArrayOfBranchRelative>() {
        public ArrayOfArrayOfBranchRelative executeRequest() throws RemoteException {
          return myRepository.QueryBranches(null, null, arrayOfItemSpec, versionSpec);
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

  // GroupSecurityService

  /**
   * <code>ReadIdentity</code> request to <code>&lt;tfs_server&gt;/Services/v1.0/GroupSecurityService.asmx</code>.
   * <code>SearchFactor</code> is set to "<code>AccountName</code>", <code>QueryMembership</code> is set to "<code>None</code>".
   *
   * @param qualifiedUsername user name including domain (e.g. "MY_DOMAIN\\MY_USER_NAME")
   * @return
   * @throws TfsException
   */
  public Identity readIdentity(String qualifiedUsername) throws TfsException {
    final SearchFactor searchFactor = SearchFactor.AccountName;
    final String factorValue = qualifiedUsername;
    final QueryMembership queryMembership = QueryMembership.None;

    return WebServiceHelper.executeRequest(myGroupSecurityService, new WebServiceHelper.Delegate<Identity>() {
      public Identity executeRequest() throws RemoteException {
        return myGroupSecurityService.ReadIdentity(searchFactor, factorValue, queryMembership);
      }
    });
  }

  // WorkItemTracking
  private static RequestHeader3 generateRequestHeader() {
    RequestHeader requestHeader = new RequestHeader();
    requestHeader.setId("uuid:" + UUID.randomUUID().toString());

    RequestHeader3 requestHeader3 = new RequestHeader3();
    requestHeader3.setRequestHeader(requestHeader);
    return requestHeader3;
  }

  public List<WorkItem> queryWorkItems(Query_type01 query) throws TfsException {
    query.setXmlns("");

    final PsQuery_type1 psQuery_type1 = new PsQuery_type1();
    psQuery_type1.setQuery(query);

    QueryWorkitemsResponse queryWorkitemsResponse =
      WebServiceHelper.executeRequest(myWorkItemTrackingClientService, new WebServiceHelper.Delegate<QueryWorkitemsResponse>() {
        public QueryWorkitemsResponse executeRequest() throws RemoteException {
          return myWorkItemTrackingClientService.QueryWorkitems(psQuery_type1, generateRequestHeader());
        }
      });

    final List<Integer> ids = parseWorkItemsIds(queryWorkitemsResponse);
    Collections.sort(ids);
    return pageWorkitemsByIds(ids);
  }

  private static List<Integer> parseWorkItemsIds(final QueryWorkitemsResponse queryWorkitemsResponse) {
    Id_type0[] ids_type0 = queryWorkitemsResponse.getResultIds().getQueryIds().getId();

    List<Integer> workItemsIdSet = new ArrayList<Integer>();
    if (ids_type0 != null) {
      for (Id_type0 id_type0 : ids_type0) {
        int startIndex = id_type0.getS();

        // end index can be Integer.MIN_VALUE if the attribute is not really present in soap response
        int endIndex = id_type0.getE();

        if (endIndex > startIndex) {
          for (int i = startIndex; i <= endIndex; i++) {
            workItemsIdSet.add(i);
          }
        }
        else {
          workItemsIdSet.add(startIndex);
        }
      }
    }
    return workItemsIdSet;
  }

  private List<WorkItem> pageWorkitemsByIds(Collection<Integer> workItemsIds) throws TfsException {
    if (workItemsIds.isEmpty()) {
      return Collections.emptyList();
    }

    int[] idsAsArray = new int[workItemsIds.size()];
    int i = 0;

    for (final Integer id : workItemsIds) {
      idsAsArray[i++] = id;
    }

    final org.jetbrains.tfsIntegration.stubs.workitemtracking.clientservices.ArrayOfInt workitemIds =
      new org.jetbrains.tfsIntegration.stubs.workitemtracking.clientservices.ArrayOfInt();
    workitemIds.set_int(idsAsArray);

    final org.jetbrains.tfsIntegration.stubs.workitemtracking.clientservices.ArrayOfString workItemFields =
      new org.jetbrains.tfsIntegration.stubs.workitemtracking.clientservices.ArrayOfString();

    List<String> serializedFields = new ArrayList<String>();
    for (WorkItemField field : WorkItemSerialize.FIELDS) {
      serializedFields.add(field.getSerialized());
    }
    workItemFields.setString(serializedFields.toArray(new String[serializedFields.size()]));

    PageWorkitemsByIdsResponse pageWorkitemsByIdsResponse =
      WebServiceHelper.executeRequest(myWorkItemTrackingClientService, new WebServiceHelper.Delegate<PageWorkitemsByIdsResponse>() {
        public PageWorkitemsByIdsResponse executeRequest() throws RemoteException {
          return myWorkItemTrackingClientService
            .PageWorkitemsByIds(workitemIds, workItemFields, null, new GregorianCalendar(), false, null, generateRequestHeader());
        }
      });

    List<WorkItem> workItems = new ArrayList<WorkItem>();
    for (R_type0 row : pageWorkitemsByIdsResponse.getItems().getTable().getRows().getR()) {
      workItems.add(WorkItemSerialize.createFromFields(row.getF()));
    }
    return workItems;
  }

  public void updateWorkItemsAfterCheckin(final String workspaceOwnerName,
                                          final Map<WorkItem, CheckinWorkItemAction> workItems,
                                          final int changeSet) throws TfsException {
    if (workItems.isEmpty()) {
      return;
    }

    String identity = readIdentity(workspaceOwnerName).getDisplayName();
    for (WorkItem workItem : workItems.keySet()) {
      CheckinWorkItemAction checkinWorkItemAction = workItems.get(workItem);
      if (checkinWorkItemAction != CheckinWorkItemAction.None) {
        updateWorkItem(workItem, checkinWorkItemAction, changeSet, identity);
      }
    }
  }

  private void updateWorkItem(WorkItem workItem, CheckinWorkItemAction action, int changeSet, String identity) throws TfsException {
    UpdateWorkItem_type0 updateWorkItem_type0 = new UpdateWorkItem_type0();
    updateWorkItem_type0.setWorkItemID(workItem.getId());
    updateWorkItem_type0.setRevision(workItem.getRevision());
    updateWorkItem_type0.setObjectType("WorkItem");
    updateWorkItem_type0.setComputedColumns(WorkItemSerialize.generateComputedColumnsForUpdateRequest(workItem.getType(), action));
    updateWorkItem_type0
      .setColumns(WorkItemSerialize.generateColumnsForUpdateRequest(workItem.getType(), workItem.getReason(), action, identity));
    updateWorkItem_type0.setInsertText(WorkItemSerialize.generateInsertTextForUpdateRequest(action, changeSet));
    updateWorkItem_type0.setInsertResourceLink(WorkItemSerialize.generateInsertResourceLinkforUpdateRequest(changeSet));

    Package_type00 package_type00 = new Package_type00();
    package_type00.setXmlns("");
    package_type00.setUpdateWorkItem(updateWorkItem_type0);

    final Package_type1 package_type_1 = new Package_type1();
    package_type_1.setPackage(package_type00);

    WebServiceHelper.executeRequest(myWorkItemTrackingClientService, new WebServiceHelper.VoidDelegate() {
      public void executeRequest() throws RemoteException {
        myWorkItemTrackingClientService.Update(package_type_1, null, generateRequestHeader());
      }
    });
  }

}
