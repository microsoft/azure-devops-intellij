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

import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ExtendedItem;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ItemType;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.LockLevel;
import org.jetbrains.tfsIntegration.core.tfs.locks.LockItemModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class LockItemsForm {

  private JPanel myContentPane;

  private JTable myLockItemsTable;

  private JRadioButton myLockCheckOutRadioButton;
  private JRadioButton myLockCheckInRadioButton;

  private final LockItemsTableModel myLockItemsTableModel;

  public LockItemsForm(List<LockItemModel> items) {
    myLockCheckOutRadioButton.setSelected(true);

    myLockItemsTableModel = new LockItemsTableModel(items);
    myLockItemsTable.setModel(myLockItemsTableModel);
    for (int i = 0; i < LockItemsTableModel.Column.values().length; i++) {
      myLockItemsTable.getColumnModel().getColumn(i).setPreferredWidth(LockItemsTableModel.Column.values()[i].getWidth());
    }
    myLockItemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myLockItemsTable.setDefaultRenderer(Boolean.class,  new NoBackgroundBooleanTableCellRenderer());
    myLockItemsTable.setDefaultRenderer(ExtendedItem.class, new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row,
                                                       final int column) {
          final ExtendedItem item = (ExtendedItem) value;
          super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                                                     column);
          setIcon(item.getType() == ItemType.Folder ? UiConstants.ICON_FOLDER : UiConstants.ICON_FILE);
          setValue(item.getSitem());
          return this;
        }
      });
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public void setRadioButtonsEnabled(final boolean isEnabled) {
    myLockCheckInRadioButton.setEnabled(isEnabled);
    myLockCheckOutRadioButton.setEnabled(isEnabled);
  }

  public List<LockItemModel> getSelectedItems() {
    return myLockItemsTableModel.getSelectedItems();
  }

  public LockLevel getLockLevel() {
    if (myLockCheckInRadioButton.isEnabled() && myLockCheckInRadioButton.isSelected()) {
      return LockLevel.Checkin;
    } else if (myLockCheckOutRadioButton.isEnabled() && myLockCheckOutRadioButton.isSelected()) {
      return LockLevel.CheckOut;
    }
    return LockLevel.None;
  }

  public void addListener(final LockItemsTableModel.Listener listener) {
    myLockItemsTableModel.addListener(listener);
  }

  public void removeListener(final LockItemsTableModel.Listener listener) {
    myLockItemsTableModel.removeListener(listener);
  }
}
