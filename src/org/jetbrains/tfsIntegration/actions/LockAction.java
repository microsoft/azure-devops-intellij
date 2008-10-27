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

package org.jetbrains.tfsIntegration.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.locks.LockItemModel;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.LockLevel;
import org.jetbrains.tfsIntegration.ui.LockItemsDialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LockAction extends AnAction {

  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(DataKeys.PROJECT);
    final VirtualFile[] files = VcsUtil.getVirtualFiles(e);

    final List<LockItemModel> items = new ArrayList<LockItemModel>();
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    final Ref<Boolean> mappingFound = new Ref<Boolean>(false);

    ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      public void run() {
        try {
          ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
          WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), false, new WorkstationHelper.VoidProcessDelegate() {
            public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
              mappingFound.set(true);
              final Map<FilePath, ExtendedItem> itemsMap = workspace.getExtendedItems2(paths);
              for (ExtendedItem item : itemsMap.values()) {
                if (item != null) {
                  items.add(new LockItemModel(item, workspace));
                }
              }
            }
          });
        }
        catch (TfsException e) {
          exceptions.add(new VcsException(e));
        }
      }
    }, "Reading existing locks...", false, project);

    if (!exceptions.isEmpty()) {
      AbstractVcsHelper.getInstance(project).showErrors(exceptions, TFSVcs.TFS_NAME);
      return;
    }
    if (!mappingFound.get()) {
      Messages.showInfoMessage(project, "No Team Foundation Server mapping found.", e.getPresentation().getText());
      return;
    }

    if (items.isEmpty()) {
      Messages.showInfoMessage(project, "No server items found.", e.getPresentation().getText());
      return;
    }

    performInitialSelection(items);

    final LockItemsDialog d = new LockItemsDialog(project, items);
    d.show();
    int exitCode = d.getExitCode();
    if (exitCode != LockItemsDialog.LOCK_EXIT_CODE && exitCode != LockItemsDialog.UNLOCK_EXIT_CODE) {
      return;
    }

    final List<LockItemModel> selectedItems = d.getSelectedItems();
    String title = d.getLockLevel() == LockLevel.None ? "Unlocking..." : "Locking...";
    ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      public void run() {
        ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
        exceptions.addAll(lockOrUnlockItems(selectedItems, d.getLockLevel()));
      }
    }, title, false, project);

    if (exceptions.isEmpty()) {
      String message = MessageFormat.format("{0} {1} {2}", selectedItems.size(), selectedItems.size() == 1 ? "item" : "items",
                                            exitCode == LockItemsDialog.LOCK_EXIT_CODE ? "locked" : "unlocked");
      WindowManager.getInstance().getStatusBar(project).setInfo(message);
    }
    else {
      AbstractVcsHelper.getInstance(project).showErrors(exceptions, TFSVcs.TFS_NAME);
    }
  }

  /**
   * Performs smart initial items selection for dialog.
   * If <code>items</code> parameter contains items locked by current user then all such items are marked as selected
   * ('Unlock' action will be enabled in dialog).
   * Otherwise all unlocked items are selected ('Lock' action will be enabled in dialog).
   */
  private static void performInitialSelection(final List<LockItemModel> items) {
    boolean unlockableExists = false;
    for (LockItemModel item : items) {
      if (item.canBeUnlocked()) {
        unlockableExists = true;
        item.setSelectionStatus(Boolean.TRUE);
      }
    }
    if (!unlockableExists) {
      for (LockItemModel item : items) {
        if (item.canBeLocked()) {
          item.setSelectionStatus(Boolean.TRUE);
        }
      }
    }
  }

  private static List<VcsException> lockOrUnlockItems(final List<LockItemModel> items, LockLevel lockLevel) {
    Map<WorkspaceInfo, List<ExtendedItem>> itemsByWorkspace = new HashMap<WorkspaceInfo, List<ExtendedItem>>();
    for (LockItemModel item : items) {
      List<ExtendedItem> itemsForWorkspace = itemsByWorkspace.get(item.getWorkspace());
      if (itemsForWorkspace == null) {
        itemsForWorkspace = new ArrayList<ExtendedItem>();
        itemsByWorkspace.put(item.getWorkspace(), itemsForWorkspace);
      }
      itemsForWorkspace.add(item.getExtendedItem());
    }

    List<VcsException> exceptions = new ArrayList<VcsException>();
    for (Map.Entry<WorkspaceInfo, List<ExtendedItem>> entry : itemsByWorkspace.entrySet()) {
      try {
        WorkspaceInfo workspace = entry.getKey();
        ResultWithFailures<GetOperation> resultWithFailures =
          workspace.getServer().getVCS().lockOrUnlockItems(workspace.getName(), workspace.getOwnerName(), lockLevel, entry.getValue());
        exceptions.addAll(TfsUtil.getVcsExceptions(resultWithFailures.getFailures()));
      }
      catch (TfsException e) {
        exceptions.add(new VcsException(e));
      }
    }
    return exceptions;
  }

  public void update(final AnActionEvent e) {
    final Project project = e.getData(DataKeys.PROJECT);
    final VirtualFile[] files = VcsUtil.getVirtualFiles(e);
    e.getPresentation().setEnabled(isEnabled(project, files));
  }

  private static boolean isEnabled(Project project, VirtualFile[] files) {
    if (files.length == 0) {
      return false;
    }

    FileStatusManager fileStatusManager = FileStatusManager.getInstance(project);
    for (VirtualFile file : files) {
      final FileStatus fileStatus = fileStatusManager.getStatus(file);
      if (fileStatus != FileStatus.NOT_CHANGED && fileStatus != FileStatus.MODIFIED && fileStatus != FileStatus.HIJACKED) {
        return false;
      }
    }

    return true;
  }

}
