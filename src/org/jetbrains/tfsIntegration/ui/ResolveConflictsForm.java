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

package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.ChangeType;
import org.jetbrains.tfsIntegration.core.tfs.EnumMask;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ResolveConflictHelper;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ConflictType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

public class ResolveConflictsForm {

  @NonNls public static final String CLOSE_PROPERTY = "ResolveConflictsForm.close";

  private JTable myItemsTable;
  private JPanel myContentPanel;
  private JButton myAcceptYoursButton;
  private JButton myAcceptTheirsButton;

  private JButton myMergeButton;
  private CoflictsTableModel myItemsTableModel;
  private final WorkspaceInfo myWorkspace;
  private final List<ItemPath> myPaths;
  private final ResolveConflictHelper myResolveConflictHelper;

  public ResolveConflictsForm(final WorkspaceInfo workspace,
                              Project project,
                              final List<ItemPath> paths,
                              final List<Conflict> conflicts,
                              final UpdatedFiles updatedFiles) {
    myWorkspace = workspace;
    myResolveConflictHelper = new ResolveConflictHelper(project, myWorkspace, updatedFiles);
    myPaths = paths;

    myItemsTableModel = new CoflictsTableModel();
    myItemsTable.setModel(myItemsTableModel);
    myItemsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    addListeners();

    myItemsTableModel.setConflicts(conflicts);
  }

  private void addListeners() {
    myItemsTableModel.addTableModelListener(new TableModelListener() {
      public void tableChanged(final TableModelEvent e) {
        if (myItemsTableModel.getRowCount() == 0) {
          fireClose();
        }
      }
    });

    myItemsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent se) {
        int[] selectedIndices = myItemsTable.getSelectedRows();
        enableButtons(selectedIndices);
      }
    });

    myAcceptYoursButton.addActionListener(new MergeActionListener() {
      protected void execute(final Conflict conflict) throws TfsException {
        myResolveConflictHelper.acceptYours(conflict);
      }
    });

    myAcceptTheirsButton.addActionListener(new MergeActionListener() {
      protected void execute(final Conflict conflict) throws TfsException, IOException, VcsException {
        myResolveConflictHelper.acceptTheirs(conflict);
      }
    });

    myMergeButton.addActionListener(new MergeActionListener() {
      protected void execute(final Conflict conflict) throws TfsException, VcsException {
        myResolveConflictHelper.acceptMerge(conflict);
      }
    });
  }

  public JComponent getPanel() {
    return myContentPanel;
  }

  private void fireClose() {
    getPanel().firePropertyChange(CLOSE_PROPERTY, false, true);
  }

  private void enableButtons(final int[] selectedIndices) {
    myAcceptYoursButton.setEnabled(selectedIndices.length > 0);
    myAcceptTheirsButton.setEnabled(selectedIndices.length > 0);
    myMergeButton.setEnabled(selectedIndices.length > 0);
    for (int index : selectedIndices) {
      Conflict conflict = myItemsTableModel.getConflicts().get(index);
      if (!canMerge(conflict)) {
        myMergeButton.setEnabled(false);
      }
    }
  }

  private static boolean canMerge(final @NotNull Conflict conflict) {
    boolean isNamespaceConflict =
      ((conflict.getCtype().equals(ConflictType.Get)) || (conflict.getCtype().equals(ConflictType.Checkin))) && conflict.getIsnamecflict();
    if ((conflict.getYtype() != ItemType.Folder) && !isNamespaceConflict) {
      if (EnumMask.fromString(ChangeType.class, conflict.getYchg()).contains(ChangeType.Edit) &&
          EnumMask.fromString(ChangeType.class, conflict.getBchg()).contains(ChangeType.Edit)) {
        return true;
      }
      if (conflict.getCtype().equals(ConflictType.Merge) && EnumMask.fromString(ChangeType.class, conflict.getBchg()).contains(ChangeType.Edit)) {
        if (EnumMask.fromString(ChangeType.class, conflict.getYchg()).contains(ChangeType.Edit)) {
          return true;
        }
        if (conflict.getIsforced()) {
          return true;
        }
        if ((conflict.getTlmver() != conflict.getBver()) || (conflict.getYlmver() != conflict.getYver())) {
          return true;
        }
      }
    }
    return false;
  }

  private abstract class MergeActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent ae) {
      int[] selectedIndices = myItemsTable.getSelectedRows();
      try {
        for (int index : selectedIndices) {
          Conflict conflict = myItemsTableModel.getConflicts().get(index);
          execute(conflict);
        }
        myItemsTableModel.setConflicts(
          myWorkspace.getServer().getVCS().queryConflicts(myWorkspace.getName(), myWorkspace.getOwnerName(), myPaths, RecursionType.Full));
      }
      catch (TfsException e) {
        String message = "Failed to resolve conlict.\n" + e.getMessage();
        Messages.showErrorDialog(myContentPanel, message, "Resolve Conflicts");
      }
      catch (IOException e) {
        String message = "Failed to resolve conlict.\n" + e.getMessage();
        Messages.showErrorDialog(myContentPanel, message, "Resolve Conflicts");
      }
      catch (VcsException e) {
        String message = "Failed to resolve conlict.\n" + e.getMessage();
        Messages.showErrorDialog(myContentPanel, message, "Resolve Conflicts");
      }
    }

    protected abstract void execute(final Conflict conflict) throws TfsException, IOException, VcsException;
  }
}


