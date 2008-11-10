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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForAddition;
import org.jetbrains.tfsIntegration.core.tfs.operations.ScheduleForDeletion;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;
import org.jetbrains.tfsIntegration.ui.SelectWorkItemsDialog;
import org.jetbrains.tfsIntegration.ui.WorkItemsDialogState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class TFSCheckinEnvironment implements CheckinEnvironment {
  private final @NotNull TFSVcs myVcs;
  private Map<ServerInfo, WorkItemsDialogState> myWorkItems = new HashMap<ServerInfo, WorkItemsDialogState>();

  public TFSCheckinEnvironment(final @NotNull TFSVcs vcs) {
    myVcs = vcs;
  }

  @Nullable
  public RefreshableOnComponent createAdditionalOptionsPanel(final CheckinProjectPanel checkinProjectPanel) {
    final JComponent panel = new JPanel();
    panel.setLayout(new BorderLayout());

    JLabel workItemsLabel = new JLabel("Work Items: ");
    panel.add(workItemsLabel, BorderLayout.WEST);

    final JTextField summaryField = new JTextField(5);
    summaryField.setEditable(false);
    panel.add(summaryField, BorderLayout.CENTER);

    JButton selectButton = new JButton("Select...");
    selectButton.addActionListener(new ActionListener() {

      public void actionPerformed(final ActionEvent event) {
        try {
          if (myWorkItems.isEmpty()) {
            Collection<FilePath> filePaths = new ArrayList<FilePath>(checkinProjectPanel.getFiles().size());
            for (File file : checkinProjectPanel.getFiles()) {
              filePaths.add(VcsUtil.getFilePath(file));
            }
            WorkstationHelper.processByWorkspaces(filePaths, false, new WorkstationHelper.VoidProcessDelegate() {
              public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
                myWorkItems.put(workspace.getServer(), new WorkItemsDialogState());
              }
            });
          }
          Map<ServerInfo, WorkItemsDialogState> dialogState = new HashMap<ServerInfo, WorkItemsDialogState>(myWorkItems.size());
          for (Map.Entry<ServerInfo, WorkItemsDialogState> e : myWorkItems.entrySet()) {
            dialogState.put(e.getKey(), e.getValue().createCopy());
          }

          SelectWorkItemsDialog d = new SelectWorkItemsDialog(myVcs.getProject(), dialogState);
          d.show();
          if (d.isOK()) {
            myWorkItems = dialogState;
            updateSummary(summaryField);
          }
        }
        catch (TfsException e) {
          Messages.showErrorDialog(myVcs.getProject(), e.getMessage(), "Checkin");
        }
      }
    });
    panel.add(selectButton, BorderLayout.EAST);

    return new RefreshableOnComponent() {
      public JComponent getComponent() {
        return panel;
      }

      public void refresh() {
      }

      public void saveState() {
      }

      public void restoreState() {
        myWorkItems.clear();
        updateSummary(summaryField);
      }
    };
  }

  private void updateSummary(final JTextField summaryField) {
    summaryField.setText(getSummary());
    summaryField.setCaretPosition(0);
  }

  private String getSummary() {
    StringBuffer summary = new StringBuffer();
    for (Map.Entry<ServerInfo, WorkItemsDialogState> e : myWorkItems.entrySet()) {
      List<Map.Entry<WorkItem, CheckinWorkItemAction>> sortedActions =
        new ArrayList<Map.Entry<WorkItem, CheckinWorkItemAction>>(e.getValue().getWorkItemsActions().entrySet());
      Collections.sort(sortedActions, new Comparator<Map.Entry<WorkItem, CheckinWorkItemAction>>() {
        public int compare(final Map.Entry<WorkItem, CheckinWorkItemAction> e1, final Map.Entry<WorkItem, CheckinWorkItemAction> e2) {
          //noinspection AutoUnboxing
          return e1.getKey().getId() - e2.getKey().getId();
        }
      });
      for (Map.Entry<WorkItem, CheckinWorkItemAction> itemAction : sortedActions) {
        final String actionLabel;
        if (CheckinWorkItemAction.Resolve == itemAction.getValue()) {
          actionLabel = "R";
        }
        else if (CheckinWorkItemAction.Associate == itemAction.getValue()) {
          actionLabel = "A";
        }
        else {
          throw new IllegalStateException("Invalid action: " + itemAction.getValue());
        }
        if (summary.length() > 0) {
          summary.append(",");
        }
        summary.append(MessageFormat.format("{0}({1})", itemAction.getKey().getId(), actionLabel));
      }
    }
    return summary.length() > 0 ? summary.toString() : "(None)";
  }

  @Nullable
  public String getDefaultMessageFor(final FilePath[] filesToCheckin) {
    return null;
  }
 
  @Nullable
  @NonNls
  public String getHelpId() {
    return null;  // TODO: help id for check in
  }

  public String getCheckinOperationName() {
    return "Check In";
  }

  public boolean showCheckinDialogInAnyCase() {
    return false;
  }

  @Nullable
  public List<VcsException> commit(final List<Change> changes, final String preparedComment) {
    final List<FilePath> files = new ArrayList<FilePath>();
    for (Change change : changes) {
      FilePath path = null;
      ContentRevision beforeRevision = change.getBeforeRevision();
      ContentRevision afterRevision = change.getAfterRevision();
      if (afterRevision != null) {
        path = afterRevision.getFile();
      }
      else if (beforeRevision != null) {
        path = beforeRevision.getFile();
      }
      if (path != null) {
        files.add(path);
      }
    }
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, false, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          try {
            // get pending changes for given items
            Collection<PendingChange> pendingChanges = workspace.getServer().getVCS()
              .queryPendingSetsByLocalPaths(workspace.getName(), workspace.getOwnerName(), paths, RecursionType.None);

            if (pendingChanges.isEmpty()) {
              return;
            }

            Collection<String> checkIn = new ArrayList<String>();
            // upload files
            for (PendingChange pendingChange : pendingChanges) {
              if (pendingChange.getType() == ItemType.File) {
                EnumMask<ChangeType> changeType = EnumMask.fromString(ChangeType.class, pendingChange.getChg());
                if (changeType.contains(ChangeType.Edit) || changeType.contains(ChangeType.Add)) {
                  workspace.getServer().getVCS().uploadItem(workspace, pendingChange);
                }
              }
              checkIn.add(pendingChange.getItem());
            }

            final WorkItemsDialogState state = myWorkItems.get(workspace.getServer());
            final Map<WorkItem, CheckinWorkItemAction> workItemActions =
              state != null ? state.getWorkItemsActions() : Collections.<WorkItem, CheckinWorkItemAction>emptyMap();
            ResultWithFailures<CheckinResult> result = workspace.getServer().getVCS()
              .checkIn(workspace.getName(), workspace.getOwnerName(), checkIn, preparedComment, workItemActions);
            errors.addAll(TfsUtil.getVcsExceptions(result.getFailures()));

            Collection<String> commitFailed = new ArrayList<String>(result.getFailures().size());
            for (Failure failure : result.getFailures()) {
              TFSVcs.assertTrue(failure.getItem() != null);
              commitFailed.add(failure.getItem());
            }

            Collection<FilePath> invalidateRoots = new ArrayList<FilePath>(pendingChanges.size());
            Collection<FilePath> invalidateFiles = new ArrayList<FilePath>();
            // set readonly status for files
            for (PendingChange pendingChange : pendingChanges) {
              TFSVcs.assertTrue(pendingChange.getItem() != null);
              if (commitFailed.contains(pendingChange.getItem())) {
                continue;
              }

              EnumMask<ChangeType> changeType = EnumMask.fromString(ChangeType.class, pendingChange.getChg());
              if (pendingChange.getType() == ItemType.File) {
                if (changeType.contains(ChangeType.Edit) || changeType.contains(ChangeType.Add) || changeType.contains(ChangeType.Rename)) {
                  VirtualFile file = VcsUtil.getVirtualFile(pendingChange.getLocal());
                  if (file != null && file.isValid()) {
                    TfsFileUtil.setReadOnly(file, true);
                  }
                }
              }

              // TODO don't add recursive invalidate
              // TODO if Rename, invalidate old and new items?
              final FilePath path = VcsUtil.getFilePath(pendingChange.getLocal(), pendingChange.getType() == ItemType.Folder);
              invalidateRoots.add(path);
              if (changeType.contains(ChangeType.Add)) {
                // [IDEADEV-27087] invalidate parent folders since they can be implicitly checked in with child checkin
                final VirtualFile vcsRoot = ProjectLevelVcsManager.getInstance(myVcs.getProject()).getVcsRootFor(path);
                if (vcsRoot != null) {
                  final FilePath vcsRootPath = TfsFileUtil.getFilePath(vcsRoot);
                  for (FilePath parent = path.getParentPath();
                       parent != null && parent.isUnder(vcsRootPath, false);
                       parent = parent.getParentPath()) {
                    invalidateFiles.add(parent);
                  }
                }
              }
            }

            if (commitFailed.isEmpty()) {
              CheckinResult checkinResult = result.getResult().iterator().next();
              workspace.getServer().getVCS()
                .updateWorkItemsAfterCheckin(workspace.getOwnerName(), workItemActions, checkinResult.getCset());
            }

            TfsFileUtil.markDirty(myVcs.getProject(), invalidateRoots, invalidateFiles);
          }
          catch (IOException e) {
            //noinspection ThrowableInstanceNeverThrown
            errors.add(new VcsException(e));
          }
        }
      });
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      errors.add(new VcsException(e));
    }
    myVcs.fireRevisionChanged();
    return errors;
  }

  @Nullable
  public List<VcsException> scheduleMissingFileForDeletion(final List<FilePath> files) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    try {
      WorkstationHelper.processByWorkspaces(files, false, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) {
          Collection<VcsException> schedulingErrors = ScheduleForDeletion.execute(myVcs.getProject(), workspace, paths);
          errors.addAll(schedulingErrors);
        }
      });
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      errors.add(new VcsException(e));
    }
    return errors;
  }

  @Nullable
  public List<VcsException> scheduleUnversionedFilesForAddition(final List<VirtualFile> files) {
    // TODO: schedule parent folders?
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      final List<FilePath> orphans =
        WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), false, new WorkstationHelper.VoidProcessDelegate() {
          public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) {
            Collection<VcsException> schedulingErrors = ScheduleForAddition.execute(myVcs.getProject(), workspace, paths);
            exceptions.addAll(schedulingErrors);
          }
        });
      if (!orphans.isEmpty()) {
        StringBuilder s = new StringBuilder();
        for (FilePath orpan : orphans) {
          if (s.length() > 0) {
            s.append("\n");
          }
          s.append(orpan.getPresentableUrl());
        }
        exceptions.add(new VcsException("No Team Foundation Server mapping found for the following items: " + s.toString()));
      }
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      exceptions.add(new VcsException(e));
    }
    return exceptions;
  }

  public boolean keepChangeListAfterCommit(final ChangeList changeList) {
    return false;
  }

}
