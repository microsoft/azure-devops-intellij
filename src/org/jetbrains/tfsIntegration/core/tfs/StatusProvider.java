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
                                   final @NotNull List<ItemPath> paths, final @Nullable ProgressIndicator progressIndicator, final @NotNull StatusVisitor statusVisitor) throws TfsException {

    // TODO: query pending changes for all items here to handle rename/move

    Map<ItemPath, ServerStatus> statuses = determineServerStatus(workspace, paths);

    for (Map.Entry<ItemPath, ServerStatus> entry : statuses.entrySet()) {
      ServerStatus status = entry.getValue();
      boolean localItemExists = localItemExists(entry.getKey());
      System.out
        .println(entry.getKey().getLocalPath().getPath() + ": " + status + (localItemExists ? ", exists locally" : ", missing locally"));
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

  private static ServerStatus determineServerStatus(final ExtendedItem item) {
    if (item == null) {
      return new ServerStatus.Unversioned();
    }
    else {
      ChangeType change = ChangeType.fromString(item.getChg());
      if (item.getDid() != Integer.MIN_VALUE) {
        TFSVcs.assertTrue(change.isEmpty());
        return new ServerStatus.Deleted(item);
      }
      else {
        if (change.isEmpty()) {
          if (item.getLocal() == null) {
            TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
            return new ServerStatus.NotDownloaded(item);
          }
          else {
            TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
            if (item.getLver() < item.getLatest()) {
              return new ServerStatus.OutOfDate(item);
            }
            else {
              return new ServerStatus.UpToDate(item);
            }
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
          TFSVcs.assertTrue(change.containsOnly(ChangeType.Value.Delete));
          //TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
          //TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
          //TFSVcs.assertTrue(item.getLocal() == null);
          return new ServerStatus.ScheduledForDeletion(item);
        }
        else if (change.containsOnly(ChangeType.Value.Edit) || change.containsOnly(ChangeType.Value.Edit, ChangeType.Value.Encoding)) {
          TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
          TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
          TFSVcs.assertTrue(item.getLocal() != null);
          return new ServerStatus.CheckedOutForEdit(item);
        }
      }
    }

    TFSVcs.LOG.assertTrue(false, "Uncovered case");
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
