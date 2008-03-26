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

import java.util.List;
import java.util.Map;

public class StatusProvider {

  public static void processPaths(final @NotNull WorkspaceInfo workspace,
                                  final @NotNull List<ItemPath> paths,
                                  final @NotNull ChangeProcessor processor,
                                  final @Nullable ProgressIndicator progressIndicator) throws TfsException {

    // TODO: query pending changes for all items here to handle rename/move

    Map<ItemPath, ExtendedItem> extendedItems = workspace.getExtendedItems(paths);

    for (Map.Entry<ItemPath, ExtendedItem> entry : extendedItems.entrySet()) {
      processFile(workspace, entry.getKey(), entry.getValue(), processor, progressIndicator);
      TFSProgressUtil.checkCanceled(progressIndicator);
    }
  }

  private static void processFile(final @NotNull WorkspaceInfo workspace,
                                  final @NotNull ItemPath itemPath,
                                  final @Nullable ExtendedItem item,
                                  final @NotNull ChangeProcessor processor,
                                  final @Nullable ProgressIndicator progressIndicator) throws TfsException {

    TFSProgressUtil.setProgressText(progressIndicator, "Check status of " + itemPath.getLocalPath());

    VirtualFile file = itemPath.getLocalPath().getVirtualFile();
    boolean localFileExists = file != null && file.isValid() && file.exists();

    if (localFileExists && file.getParent() == null) {
      // TODO IDEA crashes if reported virtual file has no parent
      return;
    }

    if (workspace.isWorkingFolder(itemPath.getLocalPath())) {
      // TODO mapping root folder should be always reported as up to date?
      return;
    }

    if (item == null) {
      if (localFileExists) {
        processor.processUnversioned(itemPath);
      }
      else {
        processor.processGhost(itemPath);
      }
      return;
    }

    if (item.getDid() != Integer.MIN_VALUE) {
      if (localFileExists) {
        processor.processDeletedOnServer(itemPath, item);
      }
      else {
        processor.processUnexistingDeleted(itemPath, item);
      }
      return;
    }

    if (isExistingButNotDownloaded(item)) {
      processor.processExistsButNotDownloaded(itemPath, item);
      return;
    }

    if (!localFileExists) {
      if (isScheduledForDeletion(item)) {
        processor.processScheduledForDeletion(itemPath, item);
      }
      else {
        processor.processLocallyDeleted(itemPath, item);
      }
      return;
    }

    if (isCheckedOutForEdit(item)) {
      // TODO: compare content with base revision?
      processor.processCheckedOutForEdit(itemPath, item);
      return;
    }

    if (isHijacked(file, item)) {
      // TODO: compare content with base revision?
      processor.processHijacked(itemPath, item);
      return;
    }

    if (isScheduledForAddition(item)) {
      processor.processScheduledForAddition(itemPath, item);
      return;
    }

    if (isOutOfDate(item)) {
      processor.processOutOfDate(itemPath, item);
      return;
    }

    if (isUpToDate(item)) {
      processor.processUpToDate(itemPath, item);
      return;
    }

    TFSVcs.LOG.assertTrue(false, "Uncovered case");
  }

  private static boolean isExistingButNotDownloaded(final @NotNull ExtendedItem item) {
    if (item.getLocal() == null) {
      ChangeType changeType = ChangeType.fromString(item.getChg());
      if (changeType.isEmpty()) {
        //TFSVcs.assertTrue(item.getLatest() == Integer.MIN_VALUE);
        TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
        TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
        return true;
      }
    }
    return false;
  }

  private static boolean isScheduledForAddition(final @NotNull ExtendedItem item) {
    ChangeType changeType = ChangeType.fromString(item.getChg());
    if (changeType.contains(ChangeType.Value.Add)) {
      TFSVcs.assertTrue(changeType.contains(ChangeType.Value.Edit) || item.getType() == ItemType.Folder);
      TFSVcs.assertTrue(changeType.contains(ChangeType.Value.Encoding));
      TFSVcs.assertTrue(item.getLatest() == Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
      return true;
    }
    return false;
  }

  private static boolean isScheduledForDeletion(final @NotNull ExtendedItem item) {
    ChangeType changeType = ChangeType.fromString(item.getChg());
    if (changeType.contains(ChangeType.Value.Delete)) {
      TFSVcs.assertTrue(changeType.containsOnly(ChangeType.Value.Delete));
      TFSVcs.assertTrue(item.getLver() == Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
      return true;
    }
    return false;
  }


  private static boolean isCheckedOutForEdit(final @NotNull ExtendedItem item) {
    ChangeType changeType = ChangeType.fromString(item.getChg());
    if (changeType.containsOnly(ChangeType.Value.Edit) || changeType.containsOnly(ChangeType.Value.Edit, ChangeType.Value.Encoding)) {
      TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
      return true;
    }
    return false;
  }

  private static boolean isHijacked(final @NotNull VirtualFile file, final @NotNull ExtendedItem item) {
    ChangeType changeType = ChangeType.fromString(item.getChg());
    if (changeType.isEmpty()) {
      TFSVcs.assertTrue(item.getLatest() != Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getLver() != Integer.MIN_VALUE);
      TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
      return file.isWritable() && !file.isDirectory(); // TODO: it seems that folders must never be treated as hijacked
    }
    return false;
  }

  private static boolean isOutOfDate(final @NotNull ExtendedItem item) {
    ChangeType changeType = ChangeType.fromString(item.getChg());
    if (changeType.isEmpty() && item.getLatest() > item.getLver()) {
      TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
      return true;
    }
    return false;
  }

  private static boolean isUpToDate(final ExtendedItem item) {
    // TODO: compare content with base revision?
    ChangeType changeType = ChangeType.fromString(item.getChg());
    if (changeType.isEmpty() && item.getLatest() == item.getLver()) {
      TFSVcs.assertTrue(item.getDid() == Integer.MIN_VALUE);
      return true;
    }
    return false;
  }


}
