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
import com.intellij.openapi.vcs.update.FileGroup;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ResolveConflictHelper;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResolveConflictsForm {

  @NonNls public static final String CLOSE_PROPERTY = "ResolveConflictsForm.close";

  private JTable myItemsTable;
  private JPanel myContentPanel;
  private JButton myAcceptYoursButton;
  private JButton myAcceptTheirsButton;

  private JButton myMergeButton;
  private ItemsTableModel myItemsTableModel;
  private final WorkspaceInfo myWorkspace;
  private final UpdatedFiles myUpdatedFiles;
  private final List<ItemPath> myPaths;
  private final ResolveConflictHelper myResolveConflictHelper;

  public ResolveConflictsForm(final WorkspaceInfo workspace,
                              Project project,
                              final List<ItemPath> paths,
                              final List<Conflict> conflicts,
                              final UpdatedFiles updatedFiles) {
    myWorkspace = workspace;
    myUpdatedFiles = updatedFiles;
    myResolveConflictHelper = new ResolveConflictHelper(project, myWorkspace);
    myPaths = paths;

    myItemsTableModel = new ItemsTableModel();
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
        myUpdatedFiles.getGroupById(FileGroup.MODIFIED_ID).add(conflict.getSrclitem());
      }
    });

    myAcceptTheirsButton.addActionListener(new MergeActionListener() {
      protected void execute(final Conflict conflict) throws TfsException, IOException {
        myResolveConflictHelper.acceptTheirs(conflict);
        myUpdatedFiles.getGroupById(FileGroup.RESTORED_ID).add(conflict.getTgtlitem());
      }
    });

    myMergeButton.addActionListener(new MergeActionListener() {
      protected void execute(final Conflict conflict) throws TfsException, VcsException {
        final ConflictData conflictData = myResolveConflictHelper.getConflictData(conflict);
        myResolveConflictHelper.acceptMerge(conflict, conflictData);
        myUpdatedFiles.getGroupById(FileGroup.MERGED_ID).add(conflictData.targetLocalName);
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
      if (conflict.getTsitem() == null) {
        // item deleted on server, so it is
        myMergeButton.setEnabled(false);
      }
      // TODO: disable myMergeButton if names do not conflict and content conflicts and is binary   
    }
  }

  private static class ItemsTableModel extends AbstractTableModel {
    private List<Conflict> myConflicts;

    public List<Conflict> getMergeData() {
      return myConflicts;
    }

    public String getColumnName(final int column) {
      return Column.values()[column].getCaption();
    }

    public int getRowCount() {
      return myConflicts != null ? myConflicts.size() : 0;
    }

    public int getColumnCount() {
      return Column.values().length;
    }

    public Object getValueAt(final int rowIndex, final int columnIndex) {
      Conflict conflict = myConflicts.get(rowIndex);
      return Column.values()[columnIndex].getValue(conflict);
    }

    public List<Conflict> getConflicts() {
      return myConflicts;
    }

    public void setConflicts(final List<Conflict> conflicts) {
      myConflicts = conflicts;
      fireTableDataChanged();
    }
  }

  private enum Column {

    Name("Name") {
      public String getValue(Conflict conflict) {
        return conflict.getSrclitem();
      }
    },
    ConflictType("Conflict type") {
      public String getValue(Conflict conflict) {
        ArrayList<String> types = new ArrayList<String>();
        if (ResolveConflictHelper.isNameConflict(conflict)) {
          types.add("Rename");
        }
        if (ResolveConflictHelper.isContentConflict(conflict)) {
          types.add("Content");
        }
        String res = "";
        for (String type : types) {
          if (res.length() == 0) {
            res = type;
          }
          else {
            res += (", " + type);
          }
        }
        return res;
      }
    };

    private String myCaption;

    Column(String caption) {
      myCaption = caption;
    }

    public String getCaption() {
      return myCaption;
    }

    public abstract String getValue(Conflict conflict);

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
        Messages.showErrorDialog(e.getMessage(), "Merge changes");
      }
      catch (IOException e) {
        Messages.showErrorDialog(e.getMessage(), "Merge changes");
      }
      catch (VcsException e) {
        Messages.showErrorDialog(e.getMessage(), "Merge changes");
      }
    }

    protected abstract void execute(final Conflict conflict) throws TfsException, IOException, VcsException;
  }
}

