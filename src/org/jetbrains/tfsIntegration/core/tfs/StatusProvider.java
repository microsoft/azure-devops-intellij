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
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSProgressUtil;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusProvider {

  public static void visitByStatus(final @NotNull WorkspaceInfo workspace,
                                   final @NotNull List<ItemPath> paths,
                                   final @Nullable ProgressIndicator progressIndicator,
                                   final @NotNull StatusVisitor statusVisitor) throws TfsException {
    if (paths.isEmpty()) {
      return;
    }
    // TODO: query pending changes for all items here to handle rename/move

    Map<ItemPath, ServerStatus> statuses = determineServerStatus(workspace, paths);

    for (Map.Entry<ItemPath, ServerStatus> entry : statuses.entrySet()) {
      ServerStatus status = entry.getValue();
      boolean localItemExists = localItemExists(entry.getKey());
      //System.out
      //  .println(entry.getKey().getLocalPath().getPath() + ": " + status + (localItemExists ? ", exists locally" : ", missing locally"));
      status.visitBy(entry.getKey(), statusVisitor, localItemExists);
      TFSProgressUtil.checkCanceled(progressIndicator);
    }
  }

  public static Map<ItemPath, ServerStatus> determineServerStatus(WorkspaceInfo workspace, List<ItemPath> paths) throws TfsException {
    Map<ItemPath, ExtendedItem> extendedItems = workspace.getExtendedItems(paths);
    Map<ItemPath, ServerStatus> result = new HashMap<ItemPath, ServerStatus>(extendedItems.size());

    for (Map.Entry<ItemPath, ExtendedItem> entry : extendedItems.entrySet()) {
      final ServerStatus serverStatus;
      if (workspace.isWorkingFolder(entry.getKey().getLocalPath())) {
        // TODO mapping root folder should be always reported as up to date?
        // TODO creating status instance with improper extended item bean
        serverStatus = new ServerStatus.UpToDate(entry.getValue());
      }
      else {
        serverStatus = determineServerStatus(entry.getValue());
      }
      result.put(entry.getKey(), serverStatus);
    }
    return result;
  }

  public static ServerStatus determineServerStatus(final ExtendedItem item) {
    if (item == null || (item.getLocal() == null && ChangeType.fromString(item.getChg()).isEmpty())) {
      // report not downloaded items as unversioned
      return new ServerStatus.Unversioned(item);
    }
    else {
      ChangeType change = ChangeType.fromString(item.getChg());
      if (item.getDid() != Integer.MIN_VALUE) {
        TFSVcs.assertTrue(change.isEmpty());
        return new ServerStatus.Deleted(item);
      }
      else {
        if (change.isEmpty()) {
          TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
          if (item.getLver() < item.getLatest()) {
            return new ServerStatus.OutOfDate(item);
          }
          else {
            return new ServerStatus.UpToDate(item);
          }
        }
        else if (change.contains(ChangeType.Value.Add)) {
          TFSVcs.assertTrue(change.contains(ChangeType.Value.Edit) || item.getType() == ItemType.Folder);
          TFSVcs.assertTrue(change.contains(ChangeType.Value.Encoding));
          TFSVcs.assertTrue(item.getLatest() == Integer.MIN_VALUE);
          TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
          return new ServerStatus.ScheduledForAddition(item);
        }
        else if (change.contains(ChangeType.Value.Delete)) {
//          TFSVcs.assertTrue(change.containsOnly(ChangeType.Value.Delete)); // NOTE: may come with "Lock" change 
          //TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
          //TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
          //TFSVcs.assertTrue(item.getLocal() == null);
          return new ServerStatus.ScheduledForDeletion(item);
        }
        else if (change.contains(ChangeType.Value.Edit) && !change.contains(ChangeType.Value.Rename)) {
          TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
          TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
          TFSVcs.assertTrue(item.getLocal() != null);
          return new ServerStatus.CheckedOutForEdit(item);
        }
        else if (change.contains(ChangeType.Value.Rename) && !change.contains(ChangeType.Value.Edit)) {
          return new ServerStatus.Renamed(item);
        }
        else if (change.contains(ChangeType.Value.Rename) && change.contains(ChangeType.Value.Edit)) {
          TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
          TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
          TFSVcs.assertTrue(item.getLocal() != null);
          return new ServerStatus.RenamedCheckedOut(item);
        }
      }
    }

    TFSVcs.LOG.assertTrue(false, "Uncovered case for item " + item.getLocal() != null ? item.getLocal() : item.getTitem());
    return null;
  }

  private static boolean localItemExists(ItemPath itemPath) {
    VirtualFile file = itemPath.getLocalPath().getVirtualFile();
    return file != null && file.isValid() && file.exists();
  }

  public static boolean isFileWritable(ItemPath itemPath) {
    VirtualFile file = itemPath.getLocalPath().getVirtualFile();
    return file.isWritable() && !file.isDirectory();
  }

}
