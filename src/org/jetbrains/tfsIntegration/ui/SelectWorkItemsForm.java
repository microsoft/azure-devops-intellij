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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemsQuery;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.CheckinWorkItemAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

public class SelectWorkItemsForm {
  private JPanel myContentPane;
  private JComboBox myServersCombo;
  private JComboBox myQueriesCombo;
  private JButton mySearchButton;
  private JTable myWorkItemsTable;

  private final WorkItemsTableModel myWorkItemsTableModel;

  private final Map<ServerInfo, WorkItemsDialogState> myState;
  private final String myTitle;
  private final Component myDialogPane;

  public SelectWorkItemsForm(final Map<ServerInfo, WorkItemsDialogState> state, final String title, final Component dialogPane) {
    myTitle = title;
    myDialogPane = dialogPane;
    myState = state;

    myServersCombo.setModel(new DefaultComboBoxModel(myState.keySet().toArray()));
    myServersCombo.setRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(final JList list,
                                                    final Object value,
                                                    final int index,
                                                    final boolean isSelected,
                                                    final boolean cellHasFocus) {
        setText(((ServerInfo)value).getUri().toString());
        return this;
      }
    });

    myServersCombo.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        myQueriesCombo.setSelectedItem(myState.get(getSelectedServer()).getQuery());
        updateTable();
      }
    });

    myQueriesCombo.setModel(new DefaultComboBoxModel(WorkItemsQuery.values()));

    mySearchButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        try {
          myDialogPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          final WorkItemsQuery selectedQuery = (WorkItemsQuery)myQueriesCombo.getSelectedItem();
          List<WorkItem> queryResult = selectedQuery.queryWorkItems(getSelectedServer());
          myState.get(getSelectedServer()).update(selectedQuery, queryResult);
          updateTable();
          if (queryResult.isEmpty()) {
            final String message = "No work items found for the selected query";
            Messages.showInfoMessage(dialogPane, message, myTitle);
          }
        }
        catch (TfsException e) {
          Messages.showErrorDialog(e.getMessage(), myTitle);
        }
        finally {
          myDialogPane.setCursor(Cursor.getDefaultCursor());
        }
      }
    });

    myWorkItemsTableModel = new WorkItemsTableModel();

    myWorkItemsTable.setModel(myWorkItemsTableModel);
    myWorkItemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    for (int i = 0; i < WorkItemsTableModel.Column.values().length; i++) {
      myWorkItemsTable.getColumnModel().getColumn(i).setPreferredWidth(WorkItemsTableModel.Column.values()[i].getWidth());
    }

    final JComboBox actionCombo = new JComboBox(new CheckinWorkItemAction[]{CheckinWorkItemAction.Resolve, CheckinWorkItemAction.Associate})
      ;

    myWorkItemsTable.getColumnModel().getColumn(WorkItemsTableModel.Column.Checkbox.ordinal())
      .setCellRenderer(new NoBackgroundBooleanTableCellRenderer());
    myWorkItemsTable.getColumnModel().getColumn(WorkItemsTableModel.Column.CheckinAction.ordinal())
      .setCellEditor(new DefaultCellEditor(actionCombo) {
        @Nullable
        public Component getTableCellEditorComponent(final JTable table,
                                                     final Object value,
                                                     final boolean isSelected,
                                                     final int row,
                                                     final int column) {
          WorkItem workItem = ((WorkItemsTableModel)table.getModel()).getWorkItem(row);
          CheckinWorkItemAction action = ((WorkItemsTableModel)table.getModel()).getAction(workItem);
          if (action != null && workItem.isActionPossible(CheckinWorkItemAction.Resolve)) {
            actionCombo.setSelectedItem(action);
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
          }
          else {
            return null;
          }
        }
      });


    final WorkItemsQuery previousQuery = myState.get(getSelectedServer()).getQuery();
    myQueriesCombo.setSelectedItem(previousQuery != null ? previousQuery : WorkItemsQuery.AllMyActive);
    updateTable();
  }

  private void updateTable() {
    myWorkItemsTableModel.setContent(myState.get(getSelectedServer()));
  }

  private ServerInfo getSelectedServer() {
    return (ServerInfo)myServersCombo.getSelectedItem();
  }

  public JComponent getContentPane() {
    return myContentPane;
  }

}
