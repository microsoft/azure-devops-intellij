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

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.schemas.teamfoundation._2005._06.services.authorization._03.Identity;
import com.microsoft.schemas.teamfoundation._2005._06.services.authorization._03.QueryMembership;
import com.microsoft.schemas.teamfoundation._2005._06.services.authorization._03.SearchFactor;
import com.microsoft.schemas.teamfoundation._2005._06.services.groupsecurity._03.GroupSecurityServiceStub;
import com.microsoft.schemas.teamfoundation._2005._06.services.groupsecurity._03.ReadIdentity;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.*;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ArrayOfInt;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ArrayOfString;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.CheckinOptions;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.MergeOptions;
import com.microsoft.schemas.teamfoundation._2005._06.workitemtracking.clientservices._03.*;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.TFSConstants;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemField;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemSerialize;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.webservice.TfsRequestManager;
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

  private final URI myServerUri;
  private final RepositoryStub myRepository;
  private final ClientService2Stub myWorkItemTrackingClientService;
  private final GroupSecurityServiceStub myGroupSecurityService;

  private static final int ITEMS_IN_GROUP = 200;

  private interface OperationOnCollection<T, U> {
    U execute(Collection<T> items) throws RemoteException;

    U merge(Collection<U> results);
  }

  private interface OperationOnList<T, U> {
    U execute(List<T> items) throws RemoteException;

    U merge(Collection<U> results);
  }

  private <T, U> U execute(final OperationOnCollection<T, U> operation, final Collection<T> items) throws TfsException {
    return execute(new OperationOnList<T, U>() {
      public U execute(List<T> items) throws RemoteException {
        return operation.execute(items);
      }

      public U merge(Collection<U> results) {
        return operation.merge(results);
      }
    }, new ArrayList<T>(items));
  }

  private <T, U> U execute(final OperationOnList<T, U> operation, List<T> items) throws TfsException {
    if (items.isEmpty()) {
      return operation.merge(Collections.<U>emptyList());
    }

    final Collection<U> results = new ArrayList<U>();
    TfsUtil.consumeInParts(items, ITEMS_IN_GROUP, new TfsUtil.Consumer<List<T>, TfsException>() {
      public void consume(final List<T> ts) throws TfsException {
        U result = WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<U>() {
          public U executeRequest() throws RemoteException {
            return operation.execute(ts);
          }
        });
        results.add(result);
      }
    });
    return operation.merge(results);
  }

  public VersionControlServer(URI uri) {
    myServerUri = uri;
    try {
      final ConfigurationContext configContext = WebServiceHelper.getStubConfigurationContext();
      myRepository = new RepositoryStub(configContext, TfsUtil.appendPath(uri, TFSConstants.VERSION_CONTROL_ASMX));
      myWorkItemTrackingClientService =
        new ClientService2Stub(configContext, TfsUtil.appendPath(uri, TFSConstants.WORK_ITEM_TRACKING_CLIENT_SERVICE_ASMX));
      myGroupSecurityService =
        new GroupSecurityServiceStub(configContext, TfsUtil.appendPath(uri, TFSConstants.GROUP_SECURITY_SERVICE_ASMX));
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

  private List<Item> queryItemsById(final int[] itemIds, final int changeSet, final boolean generateDownloadUrl) throws TfsException {
    final ArrayOfInt arrayOfInt = new ArrayOfInt();
    arrayOfInt.set_int(itemIds);
    ArrayOfItem arrayOfItems = WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<ArrayOfItem>() {
      public ArrayOfItem executeRequest() throws RemoteException {
        final QueryItemsById param = new QueryItemsById();
        param.setChangeSet(changeSet);
        param.setItemIds(arrayOfInt);
        param.setGenerateDownloadUrls(generateDownloadUrl);
        return myRepository.queryItemsById(param).getQueryItemsByIdResult();
      }
    });
    ArrayList<Item> result = new ArrayList<Item>();
    ContainerUtil.addAll(result, arrayOfItems.getItem());
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
        if (itemPath.getLocalPath().isDirectory()) {
          changeRequest.getItem().setRecurse(RecursionType.Full);
        }
        changeRequest.setReq(RequestType.Edit);
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
                                                           final ChangeRequestProvider<T> changeRequestProvider) throws TfsException {
    OperationOnCollection<T, ResultWithFailures<GetOperation>> operation =
      new OperationOnCollection<T, ResultWithFailures<GetOperation>>() {
        public ResultWithFailures<GetOperation> execute(Collection<T> items) throws RemoteException {
          ResultWithFailures<GetOperation> result = new ResultWithFailures<GetOperation>();
          List<ChangeRequest> changeRequests = new ArrayList<ChangeRequest>(items.size());
          for (T path : items) {
            changeRequests.add(changeRequestProvider.createChangeRequest(path));
          }

          final ArrayOfChangeRequest arrayOfChangeRequest = new ArrayOfChangeRequest();
          arrayOfChangeRequest.setChangeRequest(changeRequests.toArray(new ChangeRequest[changeRequests.size()]));

          final PendChanges param = new PendChanges();
          param.setOwnerName(workspaceOwner);
          param.setWorkspaceName(workspaceName);
          param.setChanges(arrayOfChangeRequest);
          final PendChangesResponse response = myRepository.pendChanges(param);
          if (updateLocalVersion && response.getPendChangesResult().getGetOperation() != null) {
            final ArrayOfLocalVersionUpdate arrayOfLocalVersionUpdate = new ArrayOfLocalVersionUpdate();
            List<LocalVersionUpdate> localVersionUpdates =
              new ArrayList<LocalVersionUpdate>(response.getPendChangesResult().getGetOperation().length);
            for (GetOperation getOperation : response.getPendChangesResult().getGetOperation()) {
              localVersionUpdates.add(getLocalVersionUpdate(getOperation));
            }
            arrayOfLocalVersionUpdate
              .setLocalVersionUpdate(localVersionUpdates.toArray(new LocalVersionUpdate[localVersionUpdates.size()]));

            final UpdateLocalVersion param2 = new UpdateLocalVersion();
            param2.setOwnerName(workspaceOwner);
            param2.setWorkspaceName(workspaceName);
            param2.setUpdates(arrayOfLocalVersionUpdate);
            myRepository.updateLocalVersion(param2);
          }

          if (response.getPendChangesResult().getGetOperation() != null) {
            ContainerUtil.addAll(result.getResult(), response.getPendChangesResult().getGetOperation());
          }

          if (response.getFailures().getFailure() != null) {
            ContainerUtil.addAll(result.getFailures(), response.getFailures().getFailure());
          }
          return result;
        }

        public ResultWithFailures<GetOperation> merge(Collection<ResultWithFailures<GetOperation>> results) {
          return ResultWithFailures.merge(results);
        }
      };

    return execute(operation, paths);
  }


  public Workspace loadWorkspace(final String workspaceName, final String workspaceOwner, Object projectOrComponent)
    throws TfsException {
    return TfsRequestManager.executeRequest(myServerUri, projectOrComponent, myRepository, new TfsRequestManager.Request<Workspace>(
      TFSBundle.message("load.workspace.0", workspaceName)) {
      @Override
      public Workspace execute(Credentials credentials, URI serverUri, @Nullable ProgressIndicator pi) throws Exception {
        final QueryWorkspace param = new QueryWorkspace();
        param.setOwnerName(workspaceOwner);
        param.setWorkspaceName(workspaceName);
        return myRepository.queryWorkspace(param).getQueryWorkspaceResult();
      }
    });
  }

  public void updateWorkspace(final String oldWorkspaceName,
                              final Workspace newWorkspaceDataBean,
                              Object projectOrComponent)
    throws TfsException {
    TfsRequestManager.executeRequest(myServerUri, projectOrComponent, myRepository, new TfsRequestManager.Request<Object>(
      TFSBundle.message("save.workspace.0", newWorkspaceDataBean.getName())) {
      @Override
      public Workspace execute(Credentials credentials, URI serverUri, @Nullable ProgressIndicator pi) throws Exception {
        final UpdateWorkspace param = new UpdateWorkspace();
        param.setNewWorkspace(newWorkspaceDataBean);
        param.setOldWorkspaceName(oldWorkspaceName);
        param.setOwnerName(credentials.getQualifiedUsername());
        myRepository.updateWorkspace(param).getUpdateWorkspaceResult();
        //noinspection ConstantConditions
        return null;
      }
    });
  }

  public Workspace createWorkspace(final Workspace workspaceBean, Object projectOrComponent) throws TfsException {
    return TfsRequestManager.executeRequest(myServerUri, projectOrComponent, myRepository, new TfsRequestManager.Request<Workspace>(
      TFSBundle.message("create.workspace.0", workspaceBean.getName())) {
      @Override
      public Workspace execute(Credentials credentials, URI serverUri, @Nullable ProgressIndicator pi) throws Exception {
        final CreateWorkspace param = new CreateWorkspace();
        param.setWorkspace(workspaceBean);
        return myRepository.createWorkspace(param).getCreateWorkspaceResult();
      }
    });
  }

  public void deleteWorkspace(final String workspaceName, final String workspaceOwner, Object projectOrComponent) throws TfsException {
    TfsRequestManager.executeRequest(myServerUri, projectOrComponent, myRepository, new TfsRequestManager.Request<Object>(
      TFSBundle.message("delete.workspace.0", workspaceName)) {
      @Override
      public Workspace execute(Credentials credentials, URI serverUri, @Nullable ProgressIndicator pi) throws Exception {
        final DeleteWorkspace param = new DeleteWorkspace();
        param.setOwnerName(workspaceOwner);
        param.setWorkspaceName(workspaceName);
        myRepository.deleteWorkspace(param);
        //noinspection ConstantConditions
        return null;
      }
    });
  }

  public List<Item> getChildItems(final String parentServerItem, final boolean foldersOnly) throws TfsException {
    final ArrayOfItemSpec itemSpecs = new ArrayOfItemSpec();
    itemSpecs.setItemSpec(new ItemSpec[]{createItemSpec(parentServerItem, RecursionType.OneLevel)});

    final ArrayOfItemSet arrayOfItemSet =
      WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<ArrayOfItemSet>() {
        public ArrayOfItemSet executeRequest() throws RemoteException {
          QueryItems param = new QueryItems();
          param.setWorkspaceName(null);
          param.setWorkspaceOwner(null);
          param.setItems(itemSpecs);
          param.setVersion(LatestVersionSpec.INSTANCE);
          param.setDeletedState(DeletedState.NonDeleted);
          param.setItemType(foldersOnly ? ItemType.Folder : ItemType.Any);
          param.setGenerateDownloadUrls(false);
          return myRepository.queryItems(param).getQueryItemsResult();
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
    public final List<ExtendedItem> extendedItems;
    public final Collection<PendingChange> pendingChanges;

    public ExtendedItemsAndPendingChanges(final Collection<PendingChange> pendingChanges, final List<ExtendedItem> extendedItems) {
      this.pendingChanges = pendingChanges;
      this.extendedItems = extendedItems;
    }
  }

  public ExtendedItemsAndPendingChanges getExtendedItemsAndPendingChanges(final String workspaceName,
                                                                          final String ownerName,
                                                                          List<ItemSpec> itemsSpecs,
                                                                          final ItemType itemType) throws TfsException {
    OperationOnCollection<ItemSpec, ExtendedItemsAndPendingChanges> operation =
      new OperationOnCollection<ItemSpec, ExtendedItemsAndPendingChanges>() {
        public ExtendedItemsAndPendingChanges execute(Collection<ItemSpec> items) throws RemoteException {
          final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
          arrayOfItemSpec.setItemSpec(items.toArray(new ItemSpec[items.size()]));
          QueryItemsExtended param = new QueryItemsExtended();
          param.setWorkspaceName(workspaceName);
          param.setWorkspaceOwner(ownerName);
          param.setItems(arrayOfItemSpec);
          param.setDeletedState(DeletedState.NonDeleted);
          param.setItemType(itemType);
          ArrayOfExtendedItem[] extendedItemsArray =
            myRepository.queryItemsExtended(param).getQueryItemsExtendedResult().getArrayOfExtendedItem();

          TFSVcs.assertTrue(extendedItemsArray != null && extendedItemsArray.length == items.size());

          List<ExtendedItem> extendedItems = new ArrayList<ExtendedItem>();
          //noinspection ConstantConditions
          for (ArrayOfExtendedItem extendedItem : extendedItemsArray) {
            if (extendedItem.getExtendedItem() != null) {
              // no need to chooseExtendedItem() since DeletedState.NonDeleted specified
              ContainerUtil.addAll(extendedItems, extendedItem.getExtendedItem());
            }
          }

          QueryPendingSets param2 = new QueryPendingSets();
          param2.setLocalWorkspaceName(workspaceName);
          param2.setLocalWorkspaceOwner(ownerName);
          param2.setQueryWorkspaceName(workspaceName);
          param2.setOwnerName(ownerName);
          param2.setItemSpecs(arrayOfItemSpec);
          param2.setGenerateDownloadUrls(false);
          final PendingSet[] pendingSets = myRepository.queryPendingSets(param2).getQueryPendingSetsResult().getPendingSet();

          final Collection<PendingChange> pendingChanges;
          if (pendingSets != null) {
            TFSVcs.assertTrue(pendingSets.length == 1);
            pendingChanges = Arrays.asList(pendingSets[0].getPendingChanges().getPendingChange());
          }
          else {
            pendingChanges = Collections.emptyList();
          }
          return new ExtendedItemsAndPendingChanges(pendingChanges, extendedItems);
        }

        public ExtendedItemsAndPendingChanges merge(Collection<ExtendedItemsAndPendingChanges> results) {
          List<ExtendedItem> mergedItems = new ArrayList<ExtendedItem>();
          List<PendingChange> mergedPendingChanges = new ArrayList<PendingChange>();
          for (ExtendedItemsAndPendingChanges r : results) {
            mergedItems.addAll(r.extendedItems);
            mergedPendingChanges.addAll(r.pendingChanges);
          }
          return new ExtendedItemsAndPendingChanges(mergedPendingChanges, mergedItems);
        }
      };

    return execute(operation, itemsSpecs);
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
      WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<ArrayOfExtendedItem[]>() {
        public ArrayOfExtendedItem[] executeRequest() throws RemoteException {
          final QueryItemsExtended param = new QueryItemsExtended();
          param.setDeletedState(deletedState);
          param.setItems(arrayOfItemSpec);
          param.setItemType(ItemType.Any);
          param.setWorkspaceName(workspaceName);
          param.setWorkspaceOwner(ownerName);
          return myRepository.queryItemsExtended(param).getQueryItemsExtendedResult().getArrayOfExtendedItem();
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
                                                      List<FilePath> paths,
                                                      final DeletedState deletedState) throws TfsException {
    OperationOnList<FilePath, Map<FilePath, ExtendedItem>> operation = new OperationOnList<FilePath, Map<FilePath, ExtendedItem>>() {
      public Map<FilePath, ExtendedItem> execute(List<FilePath> items) throws RemoteException {
        final List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>();
        for (FilePath path : items) {
          itemSpecs.add(createItemSpec(path, RecursionType.None));
        }
        final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
        arrayOfItemSpec.setItemSpec(itemSpecs.toArray(new ItemSpec[itemSpecs.size()]));
        QueryItemsExtended param = new QueryItemsExtended();
        param.setWorkspaceName(workspaceName);
        param.setWorkspaceOwner(ownerName);
        param.setItems(arrayOfItemSpec);
        param.setDeletedState(deletedState);
        param.setItemType(ItemType.Any);
        ArrayOfExtendedItem[] extendedItems = myRepository.queryItemsExtended(param).getQueryItemsExtendedResult().getArrayOfExtendedItem();

        TFSVcs.assertTrue(extendedItems != null && extendedItems.length == items.size());
        Map<FilePath, ExtendedItem> result = new HashMap<FilePath, ExtendedItem>();

        //noinspection ConstantConditions
        for (int i = 0; i < extendedItems.length; i++) {
          ExtendedItem[] resultItems = extendedItems[i].getExtendedItem();
          ExtendedItem item = null;
          if (resultItems != null) {
            item = chooseExtendedItem(resultItems);
          }
          result.put(items.get(i), item);
        }
        return result;
      }

      public Map<FilePath, ExtendedItem> merge(Collection<Map<FilePath, ExtendedItem>> results) {
        Map<FilePath, ExtendedItem> merged = new HashMap<FilePath, ExtendedItem>();
        for (Map<FilePath, ExtendedItem> r : results) {
          merged.putAll(r);
        }
        return merged;
      }
    };

    return execute(operation, paths);
  }

  public static void downloadItem(Project project, final ServerInfo server, final String downloadKey, OutputStream outputStream)
    throws TfsException {
    final URI serverUri = server.getUri();
    boolean tryProxy = TFSConfigurationManager.getInstance().shouldTryProxy(serverUri);
    final String downloadUrl;
    if (tryProxy) {
      //noinspection ConstantConditions,HardCodedStringLiteral
      downloadUrl = TfsUtil.appendPath(TFSConfigurationManager.getInstance().getProxyUri(serverUri),
                                       TFSConstants.PROXY_DOWNLOAD_ASMX +
                                       "?" +
                                       downloadKey +
                                       "&rid=" +
                                       server.getGuid());
    }
    else {
      downloadUrl = TfsUtil.appendPath(serverUri, TFSConstants.DOWNLOAD_ASMX + "?" + downloadKey);
    }
    TFSVcs.LOG.debug((tryProxy ? "Downloading via proxy: " : "Downloading: ") + downloadUrl);
    try {
      WebServiceHelper.httpGet(serverUri, downloadUrl, outputStream);
    }
    catch (TfsException e) {
      TFSVcs.LOG.warn("Download failed", e);
      if (tryProxy) {
        TFSVcs.LOG.warn("Disabling proxy");
        String messageHtml = TFSBundle
          .message("proxy.failed", server.getPresentableUri(), TFSConfigurationManager.getInstance().getProxyUri(serverUri),
                   StringUtil.trimEnd(e.getMessage(), "."),
                   ApplicationNamesInfo.getInstance().getFullProductName());
        TfsUtil.showBalloon(project, MessageType.WARNING, messageHtml);
        TFSConfigurationManager.getInstance().setProxyInaccessible(server.getUri());
        downloadItem(project, server, downloadKey, outputStream);
      }
      else {
        throw e;
      }
    }
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

      Changeset[] currentChangeSets =
        WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<Changeset[]>() {
          public Changeset[] executeRequest() throws RemoteException {
            QueryHistory param = new QueryHistory();
            param.setWorkspaceName(workspaceName);
            param.setWorkspaceOwner(workspaceOwner);
            param.setItemSpec(itemSpec);
            param.setVersionItem(itemVersion);
            param.setUser(user);
            param.setVersionFrom(versionFrom);
            param.setVersionTo(versionToCurrent.get());
            param.setMaxCount(batchMax);
            param.setIncludeFiles(true);
            param.setGenerateDownloadUrls(false);
            param.setSlotMode(false);
            return myRepository.queryHistory(param).getQueryHistoryResult().getChangeset();
          }
        });

      if (currentChangeSets != null) {
        ContainerUtil.addAll(allChangeSets, currentChangeSets);
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

  public Workspace[] queryWorkspaces(final String computer, Object projectOrComponent) throws TfsException {
    Workspace[] workspaces =
      TfsRequestManager.executeRequest(myServerUri, projectOrComponent, myRepository, new TfsRequestManager.Request<Workspace[]>(
        TFSBundle.message("reload.workspaces")) {
        @Override
        public Workspace[] execute(Credentials credentials, URI serverUri, @Nullable ProgressIndicator pi) throws Exception {
          QueryWorkspaces param = new QueryWorkspaces();
          param.setComputer(computer);
          param.setOwnerName(credentials.getQualifiedUsername());
          return myRepository.queryWorkspaces(param).getQueryWorkspacesResult().getWorkspace();
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

  public void updateLocalVersions(final String workspaceName, final String workspaceOwnerName, Collection<LocalVersionUpdate> updates)
    throws TfsException {
    OperationOnCollection<LocalVersionUpdate, Void> operation = new OperationOnCollection<LocalVersionUpdate, Void>() {
      public Void execute(Collection<LocalVersionUpdate> items) throws RemoteException {
        final ArrayOfLocalVersionUpdate arrayOfLocalVersionUpdate = new ArrayOfLocalVersionUpdate();
        arrayOfLocalVersionUpdate.setLocalVersionUpdate(items.toArray(new LocalVersionUpdate[items.size()]));
        final UpdateLocalVersion param = new UpdateLocalVersion();
        param.setOwnerName(workspaceOwnerName);
        param.setWorkspaceName(workspaceName);
        param.setUpdates(arrayOfLocalVersionUpdate);
        myRepository.updateLocalVersion(param);
        //noinspection ConstantConditions
        return null;
      }

      public Void merge(Collection<Void> results) {
        //noinspection ConstantConditions
        return null;
      }
    };

    execute(operation, updates);
  }

  public ResultWithFailures<GetOperation> undoPendingChanges(final String workspaceName,
                                                             final String workspaceOwner,
                                                             Collection<String> serverPaths) throws TfsException {
    OperationOnCollection<String, ResultWithFailures<GetOperation>> operation =
      new OperationOnCollection<String, ResultWithFailures<GetOperation>>() {
        public ResultWithFailures<GetOperation> execute(Collection<String> items) throws RemoteException {
          List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>(items.size());
          for (String serverPath : items) {
            itemSpecs.add(createItemSpec(serverPath, null));
          }
          final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
          arrayOfItemSpec.setItemSpec(itemSpecs.toArray(new ItemSpec[itemSpecs.size()]));
          final UndoPendingChanges param = new UndoPendingChanges();
          param.setOwnerName(workspaceOwner);
          param.setWorkspaceName(workspaceName);
          param.setItems(arrayOfItemSpec);
          UndoPendingChangesResponse response = myRepository.undoPendingChanges(param);
          GetOperation[] getOperations =
            response.getUndoPendingChangesResult() != null ? response.getUndoPendingChangesResult().getGetOperation() : null;
          Failure[] failures = response.getFailures() != null ? response.getFailures().getFailure() : null;
          return new ResultWithFailures<GetOperation>(getOperations, failures);
        }

        public ResultWithFailures<GetOperation> merge(Collection<ResultWithFailures<GetOperation>> results) {
          return ResultWithFailures.merge(results);
        }
      };

    return execute(operation, serverPaths);
  }

  public List<GetOperation> get(final String workspaceName, final String workspaceOwner, List<GetRequestParams> requests)
    throws TfsException {
    OperationOnList<GetRequestParams, List<GetOperation>> operation = new OperationOnList<GetRequestParams, List<GetOperation>>() {
      public List<GetOperation> execute(List<GetRequestParams> items) throws RemoteException {
        List<GetRequest> getRequests = new ArrayList<GetRequest>(items.size());
        for (GetRequestParams getRequestParams : items) {
          final GetRequest getRequest = new GetRequest();
          getRequest.setItemSpec(createItemSpec(getRequestParams.serverPath, getRequestParams.recursionType));
          getRequest.setVersionSpec(getRequestParams.version);
          getRequests.add(getRequest);
        }
        final ArrayOfGetRequest arrayOfGetRequests = new ArrayOfGetRequest();
        arrayOfGetRequests.setGetRequest(getRequests.toArray(new GetRequest[getRequests.size()]));
        Get param = new Get();
        param.setWorkspaceName(workspaceName);
        param.setOwnerName(workspaceOwner);
        param.setRequests(arrayOfGetRequests);
        param.setForce(true);
        param.setNoGet(false);
        ArrayOfArrayOfGetOperation response = myRepository.get(param).getGetResult();
        TFSVcs.assertTrue(response.getArrayOfGetOperation() != null && response.getArrayOfGetOperation().length == items.size());

        List<GetOperation> results = new ArrayList<GetOperation>();
        for (ArrayOfGetOperation arrayOfGetOperation : response.getArrayOfGetOperation()) {
          if (arrayOfGetOperation.getGetOperation() != null) {
            ContainerUtil.addAll(results, arrayOfGetOperation.getGetOperation());
          }
        }
        return results;
      }

      public List<GetOperation> merge(Collection<List<GetOperation>> results) {
        List<GetOperation> merged = new ArrayList<GetOperation>();
        for (List<GetOperation> r : results) {
          merged.addAll(r);
        }
        return merged;
      }
    };

    return execute(operation, requests);
  }

  public void addLocalConflict(final String workspaceName,
                               final String workspaceOwner,
                               final int itemId,
                               final int versionFrom,
                               final int pendingChangeId,
                               final String sourceLocal,
                               final String targetLocal,
                               final int reason) throws TfsException {

    WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<AddConflictResponse>() {
      public AddConflictResponse executeRequest() throws RemoteException {
        AddConflict param = new AddConflict();
        param.setWorkspaceName(workspaceName);
        param.setOwnerName(workspaceOwner);
        param.setConflictType(ConflictType.Local);
        param.setItemId(itemId);
        param.setVersionFrom(versionFrom);
        param.setPendingChangeId(pendingChangeId);
        param.setSourceLocalItem(sourceLocal);
        param.setTargetLocalItem(targetLocal);
        param.setReason(reason);
        return myRepository.addConflict(param);
      }
    });
  }


  public Collection<Conflict> queryConflicts(final String workspaceName,
                                             final String ownerName,
                                             List<ItemPath> paths,
                                             final RecursionType recursionType) throws TfsException {
    OperationOnCollection<ItemPath, Collection<Conflict>> operation = new OperationOnCollection<ItemPath, Collection<Conflict>>() {
      public Collection<Conflict> execute(Collection<ItemPath> items) throws RemoteException {
        List<ItemSpec> itemSpecList = new ArrayList<ItemSpec>();
        for (ItemPath path : items) {
          itemSpecList.add(createItemSpec(path.getServerPath(), recursionType));
        }
        final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
        arrayOfItemSpec.setItemSpec(itemSpecList.toArray(new ItemSpec[itemSpecList.size()]));

        final QueryConflicts param = new QueryConflicts();
        param.setWorkspaceName(workspaceName);
        param.setOwnerName(ownerName);
        param.setItems(arrayOfItemSpec);
        Conflict[] conflicts = myRepository.queryConflicts(param).getQueryConflictsResult().getConflict();
        return conflicts != null ? Arrays.asList(conflicts) : Collections.<Conflict>emptyList();
      }

      public Collection<Conflict> merge(Collection<Collection<Conflict>> results) {
        return mergeStatic(results);
      }
    };

    return execute(operation, paths);
  }


  private static <T> Collection<T> mergeStatic(Collection<Collection<T>> results) {
    Collection<T> merged = new ArrayList<T>();
    for (Collection<T> r : results) {
      merged.addAll(r);
    }
    return merged;
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
    return WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<ResolveResponse>() {
      public ResolveResponse executeRequest() throws RemoteException {
        Resolve param = new Resolve();
        param.setWorkspaceName(workspaceName);
        param.setOwnerName(workspasceOwnerName);
        param.setConflictId(params.conflictId);
        param.setResolution(params.resolution);
        param.setNewPath(params.newPath);
        param.setEncoding(params.encoding);
        param.setLockLevel(params.lockLevel);
        return myRepository.resolve(param);
      }
    });
  }


  public void uploadItem(final WorkspaceInfo workspaceInfo, PendingChange change) throws TfsException, IOException {
    final String uploadUrl = TfsUtil.appendPath(workspaceInfo.getServer().getUri(), TFSConstants.UPLOAD_ASMX);
    File file = VersionControlPath.getFile(change.getLocal());
    long fileLength = file.length();

    ArrayList<Part> parts = new ArrayList<Part>();
    parts.add(new StringPart(SERVER_ITEM_FIELD, change.getItem(), "UTF-8"));
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
                                                       Collection<ItemSpec> itemSpecs) throws TfsException {
    OperationOnCollection<ItemSpec, Collection<PendingChange>> operation =
      new OperationOnCollection<ItemSpec, Collection<PendingChange>>() {
        public Collection<PendingChange> execute(Collection<ItemSpec> items) throws RemoteException {
          final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
          arrayOfItemSpec.setItemSpec(items.toArray(new ItemSpec[items.size()]));

          QueryPendingSets param = new QueryPendingSets();
          param.setLocalWorkspaceName(workspaceName);
          param.setLocalWorkspaceOwner(workspaceOwnerName);
          param.setQueryWorkspaceName(workspaceName);
          param.setOwnerName(workspaceOwnerName);
          param.setItemSpecs(arrayOfItemSpec);
          param.setGenerateDownloadUrls(false);
          PendingSet[] pendingSets = myRepository.queryPendingSets(param).getQueryPendingSetsResult().getPendingSet();

          return pendingSets != null
                 ? Arrays.asList(pendingSets[0].getPendingChanges().getPendingChange())
                 : Collections.<PendingChange>emptyList();
        }

        public Collection<PendingChange> merge(Collection<Collection<PendingChange>> results) {
          return mergeStatic(results);
        }
      };

    return execute(operation, itemSpecs);
  }

  public ResultWithFailures<CheckinResult> checkIn(final String workspaceName,
                                                   final String workspaceOwnerName,
                                                   Collection<String> serverItems,
                                                   final String comment,
                                                   final @NotNull Map<WorkItem, CheckinWorkItemAction> workItemsActions,
                                                   final List<Pair<String, String>> checkinNotes,
                                                   final @Nullable Pair<String/*comment*/, Map<String/*policyName*/, String/*policyMessage*/>> policyOverride)
    throws TfsException {
    final ArrayOfCheckinNoteFieldValue fieldValues = new ArrayOfCheckinNoteFieldValue();
    for (Pair<String, String> checkinNote : checkinNotes) {
      final CheckinNoteFieldValue fieldValue = new CheckinNoteFieldValue();
      fieldValue.setName(checkinNote.first);
      fieldValue.setVal(checkinNote.second);
      fieldValues.addCheckinNoteFieldValue(fieldValue);
    }

    final CheckinNote checkinNote = new CheckinNote();
    checkinNote.setValues(fieldValues);

    final PolicyOverrideInfo policyOverrideInfo = new PolicyOverrideInfo();
    if (policyOverride != null) {
      policyOverrideInfo.setComment(policyOverride.first);

      ArrayOfPolicyFailureInfo policyFailures = new ArrayOfPolicyFailureInfo();
      for (Map.Entry<String, String> entry : policyOverride.second.entrySet()) {
        PolicyFailureInfo policyFailureInfo = new PolicyFailureInfo();
        policyFailureInfo.setPolicyName(entry.getKey());
        policyFailureInfo.setMessage(entry.getValue());
        policyFailures.addPolicyFailureInfo(policyFailureInfo);
      }
      policyOverrideInfo.setPolicyFailures(policyFailures);
    }

    final Changeset changeset = new Changeset();
    changeset.setCset(0);
    changeset.setDate(TfsUtil.getZeroCalendar());
    changeset.setOwner(workspaceOwnerName);
    changeset.setComment(comment);
    changeset.setCheckinNote(checkinNote);
    changeset.setPolicyOverride(policyOverrideInfo);

    final CheckinNotificationInfo checkinNotificationInfo = new CheckinNotificationInfo();
    checkinNotificationInfo.setWorkItemInfo(toArrayOfCheckinNotificationWorkItemInfo(workItemsActions));
    final CheckinOptions checkinOptions = new CheckinOptions();
    checkinOptions.setCheckinOptions_type0(new CheckinOptions_type0[]{CheckinOptions_type0.ValidateCheckinOwner}); // TODO checkin options

    OperationOnCollection<String, ResultWithFailures<CheckinResult>> operation =
      new OperationOnCollection<String, ResultWithFailures<CheckinResult>>() {
        public ResultWithFailures<CheckinResult> execute(Collection<String> items) throws RemoteException {
          final ArrayOfString serverItemsArray = new ArrayOfString();
          for (String serverItem : items) {
            serverItemsArray.addString(serverItem);
          }

          CheckIn param = new CheckIn();
          param.setWorkspaceName(workspaceName);
          param.setOwnerName(workspaceOwnerName);
          param.setServerItems(serverItemsArray);
          param.setInfo(changeset);
          param.setCheckinNotificationInfo(checkinNotificationInfo);
          param.setCheckinOptions(checkinOptions);
          CheckInResponse response = myRepository.checkIn(param);

          ResultWithFailures<CheckinResult> result = new ResultWithFailures<CheckinResult>();
          if (response.getCheckInResult() != null) {
            result.getResult().add(response.getCheckInResult());
          }

          if (response.getFailures().getFailure() != null) {
            ContainerUtil.addAll(result.getFailures(), response.getFailures().getFailure());
          }
          return result;
        }

        public ResultWithFailures<CheckinResult> merge(Collection<ResultWithFailures<CheckinResult>> results) {
          return ResultWithFailures.merge(results);
        }
      };

    return execute(operation, serverItems);
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

    return WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<MergeResponse>() {
      public MergeResponse executeRequest() throws RemoteException {
        Merge param = new Merge();
        param.setWorkspaceName(workspaceName);
        param.setWorkspaceOwner(ownerName);
        param.setSource(source);
        param.setTarget(target);
        param.setFrom(fromVersion);
        param.setTo(toVersion);
        MergeOptions mergeOptions = new MergeOptions();
        mergeOptions.setMergeOptions_type0(new MergeOptions_type0[]{MergeOptions_type0.None});
        param.setOptions(mergeOptions);
        param.setLockLevel(LockLevel.Unchanged);
        return myRepository.merge(param);
      }
    });
  }

  /**
   * @return sorted accorging to 'do' attribute
   */
  public List<CheckinNoteFieldDefinition> queryCheckinNoteDefinition(final Collection<String> teamProjects) throws TfsException {
    final ArrayOfString associatedServerItem = new ArrayOfString();
    for (String teamProject : teamProjects) {
      associatedServerItem.addString(teamProject);
    }

    final ArrayOfCheckinNoteFieldDefinition result =
      WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<ArrayOfCheckinNoteFieldDefinition>() {
        public ArrayOfCheckinNoteFieldDefinition executeRequest() throws RemoteException {
          QueryCheckinNoteDefinition param = new QueryCheckinNoteDefinition();
          param.setAssociatedServerItem(associatedServerItem);
          return myRepository.queryCheckinNoteDefinition(param).getQueryCheckinNoteDefinitionResult();
        }
      });

    final CheckinNoteFieldDefinition[] definitions = result.getCheckinNoteFieldDefinition();
    if (definitions == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(definitions);
  }

  public Collection<Annotation> queryAnnotations(final String annotationName, final Collection<String> teamProjects) throws TfsException {
    final ArrayOfAnnotation arrayOfAnnotation =
      WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<ArrayOfAnnotation>() {
        public ArrayOfAnnotation executeRequest() throws RemoteException {
          QueryAnnotation param = new QueryAnnotation();
          param.setAnnotationName(annotationName);
          param.setAnnotatedItem(
            teamProjects.size() == 1 && !VersionControlPath.ROOT_FOLDER.equals(teamProjects.iterator().next()) ? teamProjects.iterator()
              .next() : null);
          param.setVersion(0);
          return myRepository.queryAnnotation(param).getQueryAnnotationResult();
        }
      });
    if (arrayOfAnnotation == null || arrayOfAnnotation.getAnnotation() == null) {
      return Collections.emptyList();
    }

    Collection<Annotation> result = new ArrayList<Annotation>();
    for (Annotation annotation : arrayOfAnnotation.getAnnotation()) {
      if (annotationName.equals(annotation.getName())) {
        result.add(annotation);
      }
    }
    return result;
  }

  public void createAnnotation(final String serverItem, final String annotationName, final String annotationValue) throws TfsException {
    WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.VoidDelegate() {
      public void executeRequest() throws RemoteException {
        CreateAnnotation param = new CreateAnnotation();
        param.setAnnotationName(annotationName);
        param.setAnnotationValue(annotationValue);
        param.setAnnotatedItem(serverItem);
        param.setVersion(0);
        param.setOverwrite(true);
        myRepository.createAnnotation(param);
      }
    });
  }

  public void deleteAnnotation(final String serverItem, final String annotationName) throws TfsException {
    WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.VoidDelegate() {
      public void executeRequest() throws RemoteException {
        DeleteAnnotation param = new DeleteAnnotation();
        param.setAnnotationName(annotationName);
        param.setAnnotatedItem(serverItem);
        param.setVersion(0);
        myRepository.deleteAnnotation(param);
      }
    });
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

    ItemSet[] items = WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<ItemSet[]>() {
      public ItemSet[] executeRequest() throws RemoteException {
        QueryItems param = new QueryItems();
        param.setWorkspaceName(workspaceName);
        param.setWorkspaceOwner(ownerName);
        param.setItems(arrayOfItemSpec);
        param.setVersion(versionSpec);
        param.setItemType(ItemType.Any);
        param.setDeletedState(deletedState);
        param.setGenerateDownloadUrls(generateDownloadUrl);
        return myRepository.queryItems(param).getQueryItemsResult().getItemSet();
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

    final ArrayOfItemSet arrayOfItemSet =
      WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<ArrayOfItemSet>() {
        public ArrayOfItemSet executeRequest() throws RemoteException {
          QueryItems param = new QueryItems();
          param.setWorkspaceName(null);
          param.setWorkspaceOwner(null);
          param.setItems(itemSpecs);
          param.setVersion(version);
          param.setDeletedState(DeletedState.NonDeleted);
          param.setItemType(ItemType.Any);
          param.setGenerateDownloadUrls(false);
          return myRepository.queryItems(param).getQueryItemsResult();
        }
      });

    TFSVcs.assertTrue(arrayOfItemSet.getItemSet() != null && arrayOfItemSet.getItemSet().length == 1);

    final ItemSet itemSet = arrayOfItemSet.getItemSet()[0];
    if (itemSet.getItems() != null && itemSet.getItems().getItem() != null) {
      List<Item> result = new ArrayList<Item>(itemSet.getItems().getItem().length);
      ContainerUtil.addAll(result, itemSet.getItems().getItem());
      return result;
    }
    else {
      return Collections.emptyList();
    }
  }


  public Changeset queryChangeset(final int changesetId) throws TfsException {
    return WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<Changeset>() {
      public Changeset executeRequest() throws RemoteException {
        QueryChangeset param = new QueryChangeset();
        param.setChangesetId(changesetId);
        param.setIncludeChanges(true);
        param.setGenerateDownloadUrls(false);
        return myRepository.queryChangeset(param).getQueryChangesetResult();
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
    VersionControlLabel[] labels =
      WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<VersionControlLabel[]>() {
        public VersionControlLabel[] executeRequest() throws RemoteException {
          QueryLabels param = new QueryLabels();
          param.setWorkspaceName(null);
          param.setWorkspaceOwner(null);
          param.setLabelName(labelName);
          param.setLabelScope(labelScope);
          param.setOwner(owner);
          param.setFilterItem(filterItem);
          param.setVersionFilterItem(versionFilterItem);
          param.setIncludeItems(includeItems);
          param.setGenerateDownloadUrls(generateDownloadUrls);
          return myRepository.queryLabels(param).getQueryLabelsResult().getVersionControlLabel();
        }
      });
    ArrayList<VersionControlLabel> result = new ArrayList<VersionControlLabel>();
    if (labels != null) {
      ContainerUtil.addAll(result, labels);
    }
    return result;
  }

  public ResultWithFailures<LabelResult> labelItem(final String labelName, final String labelComment, List<LabelItemSpec> labelItemSpecs)
    throws TfsException {
    final VersionControlLabel versionControlLabel = new VersionControlLabel();
    versionControlLabel.setName(labelName);
    versionControlLabel.setComment(labelComment);

    versionControlLabel.setDate(TfsUtil.getZeroCalendar());

    OperationOnCollection<LabelItemSpec, ResultWithFailures<LabelResult>> operation =
      new OperationOnCollection<LabelItemSpec, ResultWithFailures<LabelResult>>() {
        public ResultWithFailures<LabelResult> execute(Collection<LabelItemSpec> items) throws RemoteException {
          final ArrayOfLabelItemSpec arrayOfLabelItemSpec = new ArrayOfLabelItemSpec();
          arrayOfLabelItemSpec.setLabelItemSpec(items.toArray(new LabelItemSpec[items.size()]));
          LabelItem param = new LabelItem();
          param.setWorkspaceName(null);
          param.setWorkspaceOwner(null);
          param.setLabel(versionControlLabel);
          param.setLabelSpecs(arrayOfLabelItemSpec);
          param.setChildren(LabelChildOption.Fail);
          LabelItemResponse labelItemResponse = myRepository.labelItem(param);
          ArrayOfLabelResult results = labelItemResponse.getLabelItemResult();
          ArrayOfFailure failures = labelItemResponse.getFailures();

          return new ResultWithFailures<LabelResult>(results == null ? null : results.getLabelResult(),
                                                     failures == null ? null : failures.getFailure());
        }

        public ResultWithFailures<LabelResult> merge(Collection<ResultWithFailures<LabelResult>> results) {
          return ResultWithFailures.merge(results);
        }
      };


    return execute(operation, labelItemSpecs);
  }

  public Collection<BranchRelative> queryBranches(final String itemServerPath, final VersionSpec versionSpec) throws TfsException {
    final ArrayOfItemSpec arrayOfItemSpec = new ArrayOfItemSpec();
    arrayOfItemSpec.setItemSpec(new ItemSpec[]{createItemSpec(itemServerPath, null)});

    ArrayOfArrayOfBranchRelative result =
      WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<ArrayOfArrayOfBranchRelative>() {
        public ArrayOfArrayOfBranchRelative executeRequest() throws RemoteException {
          QueryBranches param = new QueryBranches();
          param.setWorkspaceName(null);
          param.setWorkspaceOwner(null);
          param.setItems(arrayOfItemSpec);
          param.setVersion(versionSpec);
          return myRepository.queryBranches(param).getQueryBranchesResult();
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
      WebServiceHelper.executeRequest(myServerUri, myRepository, new WebServiceHelper.Delegate<ArrayOfMergeCandidate>() {
        public ArrayOfMergeCandidate executeRequest() throws RemoteException {
          QueryMergeCandidates param = new QueryMergeCandidates();
          param.setWorkspaceName(workspaceName);
          param.setWorkspaceOwner(ownerName);
          param.setSource(source);
          param.setTarget(target);
          return myRepository.queryMergeCandidates(param).getQueryMergeCandidatesResult();
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

    return WebServiceHelper.executeRequest(myServerUri, myGroupSecurityService, new WebServiceHelper.Delegate<Identity>() {
      public Identity executeRequest() throws RemoteException {
        ReadIdentity param = new ReadIdentity();
        param.setFactor(searchFactor);
        param.setFactorValue(factorValue);
        param.setQueryMembership(queryMembership);
        return myGroupSecurityService.readIdentity(param).getReadIdentityResult();
      }
    });
  }

  // WorkItemTracking

  private static RequestHeaderE generateRequestHeader() {
    RequestHeader requestHeader = new RequestHeader();
    requestHeader.setId("uuid:" + UUID.randomUUID().toString());

    RequestHeaderE requestHeader3 = new RequestHeaderE();
    requestHeader3.setRequestHeader(requestHeader);
    return requestHeader3;
  }

  public List<WorkItem> queryWorkItems(Query_type0E query) throws TfsException {
    final PsQuery_type1 psQuery_type1 = new PsQuery_type1();
    psQuery_type1.setQuery(query);

    QueryWorkitemsResponse queryWorkitemsResponse =
      WebServiceHelper
        .executeRequest(myServerUri, myWorkItemTrackingClientService, new WebServiceHelper.Delegate<QueryWorkitemsResponse>() {
          public QueryWorkitemsResponse executeRequest() throws RemoteException {
            QueryWorkitems param = new QueryWorkitems();
            param.setPsQuery(psQuery_type1);
            return myWorkItemTrackingClientService.queryWorkitems(param, generateRequestHeader());
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

    final com.microsoft.schemas.teamfoundation._2005._06.workitemtracking.clientservices._03.ArrayOfInt workitemIds =
      new com.microsoft.schemas.teamfoundation._2005._06.workitemtracking.clientservices._03.ArrayOfInt();
    workitemIds.set_int(idsAsArray);

    final com.microsoft.schemas.teamfoundation._2005._06.workitemtracking.clientservices._03.ArrayOfString workItemFields =
      new com.microsoft.schemas.teamfoundation._2005._06.workitemtracking.clientservices._03.ArrayOfString();

    List<String> serializedFields = new ArrayList<String>();
    for (WorkItemField field : WorkItemSerialize.FIELDS) {
      serializedFields.add(field.getSerialized());
    }
    workItemFields.setString(ArrayUtil.toStringArray(serializedFields));

    PageWorkitemsByIdsResponse pageWorkitemsByIdsResponse =
      WebServiceHelper
        .executeRequest(myServerUri, myWorkItemTrackingClientService, new WebServiceHelper.Delegate<PageWorkitemsByIdsResponse>() {
          public PageWorkitemsByIdsResponse executeRequest() throws RemoteException {
            PageWorkitemsByIds param = new PageWorkitemsByIds();
            param.setIds(workitemIds);
            param.setColumns(workItemFields);
            param.setLongTextColumns(null);
            param.setAsOfDate(new GregorianCalendar());
            param.setUseMaster(false);
            param.setMetadataHave(null);
            return myWorkItemTrackingClientService.pageWorkitemsByIds(param, generateRequestHeader());
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

    Package_type0 package_type00 = new Package_type0();
    package_type00.setUpdateWorkItem(updateWorkItem_type0);

    final Package_type0E package_type_0 = new Package_type0E();
    package_type_0.setPackage(package_type00);

    WebServiceHelper.executeRequest(myServerUri, myWorkItemTrackingClientService, new WebServiceHelper.VoidDelegate() {
      public void executeRequest() throws RemoteException {
        Update param = new Update();
        param.set_package(package_type_0);
        param.setMetadataHave(null);
        myWorkItemTrackingClientService.update(param, generateRequestHeader());
      }
    });
  }

}
