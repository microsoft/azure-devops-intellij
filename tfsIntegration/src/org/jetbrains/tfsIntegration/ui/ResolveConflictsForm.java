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

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.util.EventDispatcher;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Conflict;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ResolveConflictHelper;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;

public class ResolveConflictsForm {

  public interface Listener extends EventListener {
    void close();
  }

  private JTable myItemsTable;
  private JPanel myContentPanel;
  private JButton myAcceptYoursButton;
  private JButton myAcceptTheirsButton;

  private JButton myMergeButton;
  private final ConflictsTableModel myItemsTableModel;
  private final ResolveConflictHelper myResolveConflictHelper;

  private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);

  private static final Comparator<? super Conflict> CONFLICTS_COMPARATOR = new Comparator<Conflict>() {
    public int compare(final Conflict o1, final Conflict o2) {
      String path1 = ConflictsTableModel.Column.Name.getValue(o1);
      String path2 = ConflictsTableModel.Column.Name.getValue(o2);
      return path1.compareTo(path2);
    }
  };

  public ResolveConflictsForm(ResolveConflictHelper resolveConflictHelper) {
    myResolveConflictHelper = resolveConflictHelper;

    myItemsTableModel = new ConflictsTableModel();
    myItemsTable.setModel(myItemsTableModel);
    myItemsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    addListeners();

    updateConflictsTable();
  }

  private void updateConflictsTable() {
    final List<Conflict> conflicts = new ArrayList<Conflict>(myResolveConflictHelper.getConflicts());
    Collections.sort(conflicts, CONFLICTS_COMPARATOR);
    myItemsTableModel.setConflicts(conflicts);
  }


  private void addListeners() {
    myItemsTableModel.addTableModelListener(new TableModelListener() {
      public void tableChanged(final TableModelEvent e) {
        if (myItemsTableModel.getRowCount() == 0) {
          myEventDispatcher.getMulticaster().close();
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
      protected void execute(final Conflict conflict) throws TfsException, VcsException {
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

  public void addListener(Listener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeListener(Listener listener) {
    myEventDispatcher.removeListener(listener);
  }

  private void enableButtons(final int[] selectedIndices) {
    myAcceptYoursButton.setEnabled(selectedIndices.length > 0);
    myAcceptTheirsButton.setEnabled(selectedIndices.length > 0);
    boolean mergeEnabled = selectedIndices.length > 0;
    for (int index : selectedIndices) {
      Conflict conflict = myItemsTableModel.getConflicts().get(index);
      if (!ResolveConflictHelper.canMerge(conflict)) {
        mergeEnabled = false;
        break;
      }
    }
    myMergeButton.setEnabled(mergeEnabled);
  }

  private abstract class MergeActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent ae) {
      int[] selectedIndices = myItemsTable.getSelectedRows();
      try {
        for (int index : selectedIndices) {
          Conflict conflict = myItemsTableModel.getConflicts().get(index);
          execute(conflict);
        }
        updateConflictsTable();
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


