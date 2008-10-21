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
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.labels.LabelItemSpecWithItems;
import org.jetbrains.tfsIntegration.core.tfs.labels.LabelModel;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.LabelItemSpec;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ApplyLabelForm {

  public interface Listener {
    void dataChanged(String labelName, int visibleItemsCount);
  }

  private final Project myProject;
  private final WorkspaceInfo myWorkspace;
  private final String mySourcePath;

  private JPanel myContentPane;

  private JTextField myLabelNameTextField;
  private JTextArea myLabelCommentTextArea;
  private JTable myTable;

  private JButton myAddButton;
  private JButton myRemoveButton;

  private LabelItemsTableModel myTableModel;
  private final LabelModel myLabelModel;

  private final Collection<Listener> myListeners = new ArrayList<Listener>();

  public ApplyLabelForm(final Project project, final WorkspaceInfo workspace, final String sourcePath) {
    myProject = project;
    myWorkspace = workspace;
    mySourcePath = sourcePath;
    myLabelModel = new LabelModel();

    myLabelNameTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e) {
        fireDataChanged();
      }
    });

    myAddButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        addItems();
      }
    });

    myRemoveButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        removeItems();
      }
    });

    initTable();
    updateButtons();
  }

  private void initTable() {
    myTableModel = new LabelItemsTableModel();
    myTable.setModel(myTableModel);
    for (int i = 0; i < LabelItemsTableModel.Column.values().length; i++) {
      myTable.getColumnModel().getColumn(i).setPreferredWidth(LabelItemsTableModel.Column.values()[i].getWidth());
    }
    myTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        updateButtons();
      }
    });

    myTable.getColumnModel().getColumn(LabelItemsTableModel.Column.Item.ordinal()).setCellRenderer(new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(final JTable table,
                                                     final Object value,
                                                     final boolean isSelected,
                                                     final boolean hasFocus,
                                                     final int row,
                                                     final int column) {
        final Item item = (Item)value;
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setIcon(item.getType() == ItemType.Folder ? UiConstants.ICON_FOLDER : UiConstants.ICON_FILE);
        setValue(item.getItem());
        return this;
      }
    });

  }

  private void removeItems() {
    final List<LabelItemSpecWithItems> removalSpecs = new ArrayList<LabelItemSpecWithItems>(myTable.getSelectedRows().length);
    for (int selectedRow : myTable.getSelectedRows()) {
      removalSpecs.add(LabelItemSpecWithItems.createForRemove(myTableModel.getItem(selectedRow)));
    }
    myLabelModel.addAll(removalSpecs);
    reloadItems();
  }

  private void reloadItems() {
    myTableModel.setContent(myLabelModel.calculateItemsToDisplay());
    fireDataChanged();
  }

  public void addItems() {
    AddItemDialog d = new AddItemDialog(myProject, myWorkspace, mySourcePath);
    d.show();
    if (d.isOK()) {
      //noinspection ConstantConditions
      myLabelModel.add(d.getLabelSpec());
      reloadItems();
    }
  }

  private void updateButtons() {
    myRemoveButton.setEnabled(myTable.getSelectedRowCount() > 0);
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public String getLabelName() {
    return myLabelNameTextField.getText().trim();
  }

  public String getLabelComment() {
    return myLabelCommentTextArea.getText();
  }

  public List<LabelItemSpec> getLabelItemSpecs() {
    return myLabelModel.getLabelItemSpecs();
  }

  public void addListener(final Listener listener) {
    myListeners.add(listener);
  }

  public void removeListener(final Listener listener) {
    myListeners.remove(listener);
  }

  private void fireDataChanged() {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (Listener listener : listeners) {
      listener.dataChanged(getLabelName(), myTableModel.getRowCount());
    }
  }

}
