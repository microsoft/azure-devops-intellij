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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.locks.LockItemModel;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.LockLevel;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LockItemsTableModel extends AbstractTableModel {

  public interface Listener {
    void selectionChanged();
  }

  enum Column {
    Selection("", 5) {
      public Boolean getValue(final LockItemModel item) {
        return item.getSelectionStatus();
      }},
    Item("Item", 550) {
      public ExtendedItem getValue(final LockItemModel item) {
        return item.getExtendedItem();
      }
    },
    Lock("Current Lock", 110) {
      public String getValue(final LockItemModel item) {
        LockLevel lock = item.getExtendedItem().getLock();
        return lock == null ? "" : lock.getValue();
      }
    },
    LockOwner("Locked By", 130) {
      public String getValue(final LockItemModel item) {
        String lockOwner = item.getLockOwnerWithoutDomain();
        return lockOwner == null ? "" : lockOwner;
      }
    };

    private final String myName;
    private final int myWidth;

    private Column(final String name, final int width) {
      myName = name;
      myWidth = width;
    }

    public String getName() {
      return myName;
    }

    public int getWidth() {
      return myWidth;
    }

    @Nullable
    public abstract Object getValue(final LockItemModel item);
  }

  private final @NotNull List<LockItemModel> myContent;

  private final Collection<Listener> myListeners = new ArrayList<Listener>();

  public LockItemsTableModel(final @NotNull List<LockItemModel> content) {
    myContent = content;
    Collections.sort(myContent, LockItemModel.LOCK_ITEM_PARENT_FIRST);
  }

  public int getRowCount() {
    return myContent.size();
  }

  public int getColumnCount() {
    return Column.values().length;
  }

  public String getColumnName(final int column) {
    return Column.values()[column].getName();
  }

  public boolean isCellEditable(final int rowIndex, final int columnIndex) {
    return Column.values()[columnIndex] == Column.Selection && myContent.get(rowIndex).getSelectionStatus() != null;
  }

  @Nullable
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    return Column.values()[columnIndex].getValue(myContent.get(rowIndex));
  }

  @Override
  public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
    if (Column.values()[columnIndex] == Column.Selection) {
      myContent.get(rowIndex).setSelectionStatus((Boolean)aValue);
      fireSelectionChanged();
    }
  }

  public List<LockItemModel> getSelectedItems() {
    final List<LockItemModel> result = new ArrayList<LockItemModel>();
    for (LockItemModel item : myContent) {
      if (item.getSelectionStatus() == Boolean.TRUE) {
        result.add(item);
      }
    }
    return result;
  }

  public void addListener(final Listener listener) {
    myListeners.add(listener);
  }

  public void removeListener(final Listener listener) {
    myListeners.remove(listener);
  }

  private void fireSelectionChanged() {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (Listener listener : listeners) {
      listener.selectionChanged();
    }
  }

  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    if (columnIndex == Column.Selection.ordinal()) {
      return Boolean.class;
    }
    else if (columnIndex == Column.Item.ordinal()) {
      return ExtendedItem.class;
    }
    else {
      return super.getColumnClass(columnIndex);
    }
  }
}
