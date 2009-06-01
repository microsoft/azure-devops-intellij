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
import com.intellij.openapi.ui.MultiLineLabelUI;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.util.Condition;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.CheckinParameters;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.TfsExecutionUtil;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemsQuery;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.CheckinWorkItemAction;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.List;
import java.util.Map;

public class CheckinParametersForm {

  public interface Listener extends EventListener {
    void stateChanged();
  }

  private static final Color REQUIRED_BACKGROUND_COLOR = new Color(252, 248, 199);
  private static final Icon WARNING_ICON = UIUtil.getBalloonWarningIcon();
  private static final Icon EMPTY_ICON = new EmptyIcon(0, WARNING_ICON.getIconHeight());

  private JPanel myContentPane;
  private JComboBox myServersCombo;
  private JComboBox myQueriesCombo;
  private JButton mySearchButton;
  private JTable myWorkItemsTable;
  private JPanel myServerChooserPanel;
  private JTabbedPane myTabbedPane;
  private JPanel myNotesPanel;
  private JPanel myCheckinNotesTab;
  private JLabel myErrorLabel;
  private JPanel myWorkItemsTab;

  private final WorkItemsTableModel myWorkItemsTableModel;

  private final Map<ServerInfo, CheckinParameters> myState;
  private final Project myProject;

  //private final Collection<Listener> myListeners = new ArrayList<Listener>();

  public CheckinParametersForm(final Map<ServerInfo, CheckinParameters> state, Project project) {
    myState = state;
    myProject = project;

    myServerChooserPanel.setVisible(myState.size() > 1);

    myServersCombo.setModel(new DefaultComboBoxModel(myState.keySet().toArray()));
    myServersCombo.setRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(final JList list,
                                                    final Object value,
                                                    final int index,
                                                    final boolean isSelected,
                                                    final boolean cellHasFocus) {
        setText(((ServerInfo)value).getUri().toString());
        setIcon(UiConstants.ICON_TEAM_SERVER);
        return this;
      }
    });

    myServersCombo.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateQueryCombo();
        updateWorkItemsTable();
        udpateCheckinNotes();
        updateErrorMessage();
      }
    });

    myQueriesCombo.setModel(new DefaultComboBoxModel(WorkItemsQuery.values()));

    mySearchButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        queryWorkItems();
      }
    });

    myWorkItemsTableModel = new WorkItemsTableModel();

    myWorkItemsTable.setModel(myWorkItemsTableModel);
    myWorkItemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    for (int i = 0; i < WorkItemsTableModel.Column.values().length; i++) {
      myWorkItemsTable.getColumnModel().getColumn(i).setPreferredWidth(WorkItemsTableModel.Column.values()[i].getWidth());
    }

    final JComboBox actionCombo =
      new JComboBox(new CheckinWorkItemAction[]{CheckinWorkItemAction.Resolve, CheckinWorkItemAction.Associate});

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

    ServerInfo serverToSelect = myState.keySet().iterator().next();
    Component tabToSelect = myWorkItemsTab;

    for (Map.Entry<ServerInfo, CheckinParameters> entry : myState.entrySet()) {
      if (entry.getValue().validateCheckinNotes() != null) {
        serverToSelect = entry.getKey();
        tabToSelect = myCheckinNotesTab;
        break;
      }
    }

    myServersCombo.setSelectedItem(serverToSelect);
    myTabbedPane.setSelectedIndex(myTabbedPane.indexOfComponent(tabToSelect));

    updateQueryCombo();
    updateWorkItemsTable();
    udpateCheckinNotes();
    updateErrorMessage();
  }

  private void updateQueryCombo() {
    final WorkItemsQuery previousQuery = myState.get(getSelectedServer()).getWorkItems().getQuery();
    myQueriesCombo.setSelectedItem(previousQuery != null ? previousQuery : WorkItemsQuery.AllMyActive);
  }

  private void createUIComponents() {
    // TODO until MultiLineLabel is moved to openapi
    myErrorLabel = new JLabel() {
      public void updateUI() {
        setUI(new MultiLineLabelUI());
      }

      public Dimension getMinimumSize() {
        return getPreferredSize();
      }
    };
    myErrorLabel.setVerticalTextPosition(SwingConstants.TOP);
  }

  private void queryWorkItems() {
    final TfsExecutionUtil.ResultWithError<List<WorkItem>> result =
      TfsExecutionUtil.executeInBackground("Performing Query", myProject, new TfsExecutionUtil.Process<List<WorkItem>>() {
        public List<WorkItem> run() throws TfsException, VcsException {
          final WorkItemsQuery selectedQuery = (WorkItemsQuery)myQueriesCombo.getSelectedItem();
          return selectedQuery.queryWorkItems(getSelectedServer());
        }
      });

    final String title = "Query Work Items";
    if (result.cancelled || result.showDialogIfError(title)) {
      return;
    }

    if (result.result.isEmpty()) {
      final String message = "No work items found for the selected query";
      Messages.showInfoMessage(myProject, message, title);
    }
    myState.get(getSelectedServer()).getWorkItems().update((WorkItemsQuery)myQueriesCombo.getSelectedItem(), result.result);
    updateWorkItemsTable();
  }

  private void updateWorkItemsTable() {
    myWorkItemsTableModel.setContent(myState.get(getSelectedServer()).getWorkItems());
  }

  private ServerInfo getSelectedServer() {
    return (ServerInfo)myServersCombo.getSelectedItem();
  }

  public JComponent getContentPane() {
    return myContentPane;
  }

  private void udpateCheckinNotes() {
    myNotesPanel.removeAll();

    final List<CheckinParameters.CheckinNote> notes = myState.get(getSelectedServer()).getCheckinNotes();
    final Insets labelInsets = new Insets(0, 0, 5, 0);
    final Insets fieldInsets = new Insets(0, 20, 10, 0);
    int i = 0;
    for (final CheckinParameters.CheckinNote note : notes) {
      String text = note.name + ":";
      if (note.required) {
        text = "<html><b>" + text + "</b></html>";
      }
      JLabel label = new JLabel(text);
      GridBagConstraints c =
        new GridBagConstraints(0, i++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets, 0, 0);
      myNotesPanel.add(label, c);
      final JTextField field = new JTextField(note.value);
      if (note.required) {
        field.setBackground(REQUIRED_BACKGROUND_COLOR);
      }
      c = new GridBagConstraints(0, i++, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0);
      myNotesPanel.add(field, c);

      field.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(DocumentEvent e) {
          note.value = field.getText();
          updateErrorMessage();
          //fireStateChanged();
        }
      });
    }
    myNotesPanel.add(new JPanel(),
                     new GridBagConstraints(0, i, 1, 1, 1, 10, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, labelInsets, 0, 0));
  }

  //private void fireStateChanged() {
  //  Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
  //  for (Listener listener : listeners) {
  //    listener.stateChanged();
  //  }
  //}

  //public void addListener(Listener listener) {
  //  myListeners.add(listener);
  //}

  public void updateErrorMessage() {
    Icon icon = myState.get(getSelectedServer()).validateCheckinNotes() != null ? WARNING_ICON : EMPTY_ICON;
    myTabbedPane.setIconAt(myTabbedPane.indexOfComponent(myCheckinNotesTab), icon);

    String errorMessage = CheckinParameters.validate(myState, Condition.TRUE);
    myErrorLabel.setText(errorMessage);
    myErrorLabel.setIcon(errorMessage != null ? WARNING_ICON : null);
  }
}
