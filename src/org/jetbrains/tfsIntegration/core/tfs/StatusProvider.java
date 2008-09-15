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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSProgressUtil;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;

import java.util.*;

public class StatusProvider {

  public static void visitByStatus(final @NotNull WorkspaceInfo workspace,
                                   final List<ItemPath> roots,
                                   boolean recursive,
                                   final @Nullable ProgressIndicator progress,
                                   final @NotNull StatusVisitor statusVisitor) throws TfsException {
    if (roots.isEmpty()) {
      return;
    }

    List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>(roots.size());
    for (ItemPath root : roots) {
      final VirtualFile file = root.getLocalPath().getVirtualFile();
      RecursionType recursionType =
        recursive && (file == null || !file.exists() || file.isDirectory()) ? RecursionType.Full : RecursionType.None;
      itemSpecs.add(VersionControlServer.createItemSpec(root.getLocalPath(), recursionType));
    }

    VersionControlServer.ExtendedItemsAndPendingChanges extendedItemsAndPendingChanges = workspace.getServer().getVCS()
      .getExtendedItemsAndPendingChanges(workspace.getName(), workspace.getOwnerName(), itemSpecs, DeletedState.Any, ItemType.Any);

    Map<Integer, PendingChange> pendingChanges = new HashMap<Integer, PendingChange>(extendedItemsAndPendingChanges.pendingChanges.size());
    for (PendingChange pendingChange : extendedItemsAndPendingChanges.pendingChanges) {
      pendingChanges.put(pendingChange.getItemid(), pendingChange);
    }

    TFSProgressUtil.checkCanceled(progress);


    Set<Integer> processedExtendedItemsIds = new HashSet<Integer>();

    for (int i = 0; i < roots.size(); i++) {
      ItemPath root = roots.get(i);

      Map<Integer, ExtendedItem> extendedItems =
        new HashMap<Integer, ExtendedItem>(extendedItemsAndPendingChanges.extendedItems.get(i).size());
      for (ExtendedItem extendedItem : extendedItemsAndPendingChanges.extendedItems.get(i)) {
        extendedItems.put(extendedItem.getItemid(), extendedItem);
      }

      Collection<FilePath> localItems = new HashSet<FilePath>();
      localItems.add(root.getLocalPath());
      if (recursive) {
        addExistingFilesRecursively(localItems, root.getLocalPath().getVirtualFile());
      }

      // first process all local items given
      for (FilePath localItem : localItems) {
        final String localPath = VersionControlPath.toTfsRepresentation(localItem.getPath());

        ExtendedItem extendedItem = null;
        PendingChange pendingChange = null;
        for (PendingChange candidate : pendingChanges.values()) {
          if (localPath.equals(candidate.getLocal())) {
            pendingChanges.remove(candidate.getItemid());
            extendedItem = extendedItems.remove(candidate.getItemid());
            if (extendedItem == null) {
              int tt = 0;
            }
            TFSVcs.assertTrue(extendedItem != null, "pending change without extended item for " + candidate.getLocal());
            pendingChange = candidate;
            break;
          }
        }

        if (extendedItem == null) {
          for (ExtendedItem candidate : extendedItems.values()) {
            if (localPath.equals(candidate.getLocal())) {
              extendedItem = extendedItems.remove(candidate.getItemid());
              break;
            }
          }
        }

        if (extendedItem != null) {
          if (processedExtendedItemsIds.contains(extendedItem.getItemid())) {
            // same extended item can be reported several times for different roots in case of rename
            continue;
          }
          else {
            processedExtendedItemsIds.add(extendedItem.getItemid());
          }
        }

        final boolean localItemExists = TfsFileUtil.localItemExists(localItem);
        if (!localItemExists && extendedItem != null) {
          // if path is the original one from dirtyScope, it may have invalid 'isDirectory' status
          localItem = VcsUtil.getFilePathForDeletedFile(localPath, extendedItem.getType() == ItemType.Folder);
        }
        determineServerStatus(pendingChange, extendedItem).visitBy(localItem, localItemExists, statusVisitor);
      }

      if (recursive) {
        // then care about locally deleted
        for (ExtendedItem extendedItem : extendedItems.values()) {
          if (processedExtendedItemsIds.contains(extendedItem.getItemid())) {
            continue;
          }
          else {
            processedExtendedItemsIds.add(extendedItem.getItemid());
          }

          PendingChange pendingChange = pendingChanges.get(extendedItem.getItemid());
          if (pendingChange != null || extendedItem.getLocal() != null) {
            FilePath localPath = VcsUtil.getFilePath(pendingChange != null ? pendingChange.getLocal() : extendedItem.getLocal(),
                                                     extendedItem.getType() == ItemType.Folder);
            determineServerStatus(pendingChange, extendedItem).visitBy(localPath, false, statusVisitor);
          }
        }
      }

      TFSProgressUtil.checkCanceled(progress);
    }
  }

  private static void addExistingFilesRecursively(final @NotNull Collection<FilePath> result, final @Nullable VirtualFile root) {
    if (root != null && root.exists()) {
      result.add(TfsFileUtil.getFilePath(root));
      if (root.isDirectory()) {
        for (VirtualFile child : root.getChildren()) {
          addExistingFilesRecursively(result, child);
        }
      }
    }
  }

  private static ServerStatus determineServerStatus(final @Nullable PendingChange pendingChange, final @Nullable ExtendedItem item) {
    if (item == null || (item.getLocal() == null && EnumMask.fromString(ChangeType.class, item.getChg()).isEmpty())) {
      // report not downloaded items as unversioned
      return new ServerStatus.Unversioned(item);
    }
    else {
      EnumMask<ChangeType> change = EnumMask.fromString(ChangeType.class, item.getChg());
      change.remove(ChangeType.Lock, ChangeType.Branch);

      // in spite of extendedItem.getChg() is not empty, pending change can be
      if (change.isEmpty() ? pendingChange != null : pendingChange == null) {
        int t = 0;
      }
      TFSVcs.assertTrue(change.isEmpty() ? pendingChange == null : pendingChange != null, "pending change exists or missing unexpecteldy");
      //if (item.getDid() != Integer.MIN_VALUE) {
      //  TFSVcs.assertTrue(change.isEmpty());
      //  return new ServerStatus.Deleted(item);
      //}
      //else {
      if (change.isEmpty()) {
        TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
        if (item.getLver() < item.getLatest()) {
          return new ServerStatus.OutOfDate(item);
        }
        else {
          return new ServerStatus.UpToDate(item);
        }
      }
      else if (change.contains(ChangeType.Merge)) {
        if (item.getLatest() == Integer.MIN_VALUE) {
          return new ServerStatus.ScheduledForAddition(pendingChange);
        }
        else if (change.contains(ChangeType.Rename)) {
          return new ServerStatus.RenamedCheckedOut(pendingChange);
        }
        else {
          return new ServerStatus.CheckedOutForEdit(pendingChange);
        }
      }
      else if (change.contains(ChangeType.Add)) {
        TFSVcs.assertTrue(change.contains(ChangeType.Edit) || item.getType() == ItemType.Folder);
        TFSVcs.assertTrue(change.contains(ChangeType.Encoding));
        TFSVcs.assertTrue(item.getLatest() == Integer.MIN_VALUE);
        TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
        return new ServerStatus.ScheduledForAddition(pendingChange);
      }
      else if (change.contains(ChangeType.Delete)) {
//          TFSVcs.assertTrue(change.containsOnly(ChangeType.Value.Delete)); // NOTE: may come with "Lock" change 
        //TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
        //TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
        //TFSVcs.assertTrue(item.getLocal() == null);
        return new ServerStatus.ScheduledForDeletion(pendingChange);
      }
      else if (change.contains(ChangeType.Edit) && !change.contains(ChangeType.Rename)) {
        TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
        TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
        TFSVcs.assertTrue(item.getLocal() != null);
        return new ServerStatus.CheckedOutForEdit(pendingChange);
      }
      else if (change.contains(ChangeType.Rename) && !change.contains(ChangeType.Edit)) {
        return new ServerStatus.Renamed(pendingChange);
      }
      else if (change.contains(ChangeType.Rename) && change.contains(ChangeType.Edit)) {
        TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
        TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
        TFSVcs.assertTrue(item.getLocal() != null);
        return new ServerStatus.RenamedCheckedOut(pendingChange);
      }
      //}
    }

    TFSVcs.LOG.error("Uncovered case for item " + (item.getLocal() != null ? item.getLocal() : item.getTitem()));
    //noinspection ConstantConditions
    return null;
  }

}
