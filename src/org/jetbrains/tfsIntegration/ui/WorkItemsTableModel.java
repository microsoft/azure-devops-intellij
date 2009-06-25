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

import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.core.tfs.WorkItemsCheckinParameters;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.CheckinWorkItemAction;

import javax.swing.table.AbstractTableModel;

class WorkItemsTableModel extends AbstractTableModel {

  enum Column {
    Checkbox(" ", 50) {
      public Boolean getValue(final WorkItem workItem, final CheckinWorkItemAction action) {
        return action != null && action != CheckinWorkItemAction.None;
      }},
    Type("Type", 300) {
      public Object getValue(final WorkItem workItem, final CheckinWorkItemAction action) {
        return workItem.getType().getSerialized();
      }},
    Id("Id", 200) {
      public Object getValue(final WorkItem workItem, final CheckinWorkItemAction action) {
        return workItem.getId();
      }},
    Title("Title", 1500) {
      public Object getValue(final WorkItem workItem, final CheckinWorkItemAction action) {
        return workItem.getTitle();
      }},
    State("State", 300) {
      public Object getValue(final WorkItem workItem, final CheckinWorkItemAction action) {
        return workItem.getState();
      }},
    CheckinAction("Checkin Action", 400) {
      public Object getValue(final WorkItem workItem, final CheckinWorkItemAction action) {
        if (CheckinWorkItemAction.Resolve == action) {
          return "Resolve";
        }
        else if (CheckinWorkItemAction.Associate == action) {
          return "Associate";
        }
        else {
          return "";
        }
      }};

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

    public abstract Object getValue(WorkItem workItem, CheckinWorkItemAction checkinWorkItemAction);

  }

  private WorkItemsCheckinParameters myContent;

  public WorkItem getWorkItem(int index) {
    return myContent.getWorkItems().get(index);
  }

  @Nullable
  public CheckinWorkItemAction getAction(final WorkItem workItem) {
    return myContent.getAction(workItem);
  }

  public void setContent(WorkItemsCheckinParameters content) {
    myContent = content;
    fireTableDataChanged();
  }

  public int getRowCount() {
    return myContent != null ? myContent.getWorkItems().size() : 0;
  }

  public int getColumnCount() {
    return Column.values().length;
  }

  public String getColumnName(final int column) {
    return Column.values()[column].getName();
  }

  public boolean isCellEditable(final int rowIndex, final int columnIndex) {
    return Column.values()[columnIndex] == Column.Checkbox || Column.values()[columnIndex] == Column.CheckinAction;
  }

  public Object getValueAt(final int rowIndex, final int columnIndex) {
    WorkItem workItem = getWorkItem(rowIndex);
    return Column.values()[columnIndex].getValue(workItem, myContent.getAction(workItem));
  }

  public Class<?> getColumnClass(final int columnIndex) {
    if (columnIndex == Column.Checkbox.ordinal()) {
      return Boolean.class;
    }
    else if (columnIndex == Column.CheckinAction.ordinal()) {
      return CheckinWorkItemAction.class;
    }
    else {
      return super.getColumnClass(columnIndex);
    }
  }

  public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
    final WorkItem workItem = getWorkItem(rowIndex);
    if (columnIndex == Column.Checkbox.ordinal()) {
      if (aValue == Boolean.TRUE) {
        final CheckinWorkItemAction action =
          workItem.isActionPossible(CheckinWorkItemAction.Resolve) ? CheckinWorkItemAction.Resolve : CheckinWorkItemAction.Associate;
        myContent.setAction(workItem, action);
      }
      else {
        myContent.removeAction(workItem);
      }
    }
    else if (columnIndex == Column.CheckinAction.ordinal()) {
      myContent.setAction(workItem, (CheckinWorkItemAction)aValue);
    }
    fireTableRowsUpdated(rowIndex, rowIndex);
  }
}
