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

import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.CheckinWorkItemAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkItemsCheckinParameters;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemState;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemType;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

class WorkItemsTableModel extends ListTableModel<WorkItem> {

  private WorkItemsCheckinParameters myContent;

  public WorkItemsTableModel() {
    setColumnInfos(new ColumnInfo[]{CHECKBOX, TYPE, ID, TITLE, STATE, CHECKIN_ACTION});
  }

  @NotNull
  public WorkItem getWorkItem(int index) {
    return myContent.getWorkItems().get(index);
  }

  @Nullable
  public CheckinWorkItemAction getAction(@NotNull WorkItem workItem) {
    return myContent.getAction(workItem);
  }

  public void setContent(@NotNull WorkItemsCheckinParameters content) {
    myContent = content;

    setItems(content.getWorkItems());
  }

  public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
    super.setValueAt(aValue, rowIndex, columnIndex);

    fireTableRowsUpdated(rowIndex, rowIndex);
  }

  abstract static class WorkItemFieldColumn<Aspect> extends ColumnInfo<WorkItem, Aspect> {

    private final int myWidth;

    public WorkItemFieldColumn(@NotNull String name, int width) {
      super(name);

      myWidth = width;
    }

    @Nullable
    @Override
    public String getPreferredStringValue() {
      return "";
    }

    @Override
    public int getAdditionalWidth() {
      return myWidth;
    }

    @Nullable
    @Override
    public abstract Aspect valueOf(@NotNull WorkItem workItem);
  }

  WorkItemFieldColumn<Boolean> CHECKBOX = new WorkItemFieldColumn<Boolean>(" ", 50) {

    // TODO: Do we need this renderer?
    private TableCellRenderer myRenderer = new NoBackgroundBooleanTableCellRenderer();

    @Nullable
    @Override
    public Boolean valueOf(@NotNull WorkItem workItem) {
      CheckinWorkItemAction action = myContent.getAction(workItem);

      return action != null && action != CheckinWorkItemAction.None;
    }

    @Override
    public Class<?> getColumnClass() {
      return Boolean.class;
    }

    @Override
    public boolean isCellEditable(@NotNull WorkItem workItem) {
      return true;
    }

    @Nullable
    @Override
    public TableCellRenderer getRenderer(@NotNull WorkItem workItem) {
      return myRenderer;
    }

    @Override
    public void setValue(@NotNull WorkItem workItem, @NotNull Boolean value) {
      if (value == Boolean.TRUE) {
        CheckinWorkItemAction action =
          workItem.isActionPossible(CheckinWorkItemAction.Resolve) ? CheckinWorkItemAction.Resolve : CheckinWorkItemAction.Associate;

        myContent.setAction(workItem, action);
      }
      else {
        myContent.removeAction(workItem);
      }
    }
  };

  static WorkItemFieldColumn<WorkItemType> TYPE = new WorkItemFieldColumn<WorkItemType>("Type", 300) {
    @Nullable
    @Override
    public WorkItemType valueOf(@NotNull WorkItem workItem) {
      return workItem.getType();
    }
  };

  static WorkItemFieldColumn<Integer> ID = new WorkItemFieldColumn<Integer>("Id", 200) {
    @Nullable
    @Override
    public Integer valueOf(@NotNull WorkItem workItem) {
      return workItem.getId();
    }
  };

  static WorkItemFieldColumn<String> TITLE = new WorkItemFieldColumn<String>("Title", 1500) {
    @Nullable
    @Override
    public String valueOf(@NotNull WorkItem workItem) {
      return workItem.getTitle();
    }
  };

  static WorkItemFieldColumn<WorkItemState> STATE = new WorkItemFieldColumn<WorkItemState>("State", 300) {
    @Nullable
    @Override
    public WorkItemState valueOf(@NotNull WorkItem workItem) {
      return workItem.getState();
    }
  };

  WorkItemFieldColumn<CheckinWorkItemAction> CHECKIN_ACTION = new WorkItemFieldColumn<CheckinWorkItemAction>("Checkin Action", 400) {

    private JComboBox myComboBox =
      new JComboBox(new CheckinWorkItemAction[]{CheckinWorkItemAction.Resolve, CheckinWorkItemAction.Associate});
    private TableCellEditor myCellEditor = new DefaultCellEditor(myComboBox) {
      @Nullable
      public Component getTableCellEditorComponent(final JTable table,
                                                   final Object value,
                                                   final boolean isSelected,
                                                   final int row,
                                                   final int column) {
        WorkItemsTableModel model = (WorkItemsTableModel)table.getModel();
        WorkItem workItem = model.getWorkItem(row);
        CheckinWorkItemAction action = model.getAction(workItem);

        if (action != null && workItem.isActionPossible(CheckinWorkItemAction.Resolve)) {
          myComboBox.setSelectedItem(action);

          return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        else {
          return null;
        }
      }
    };

    @Nullable
    @Override
    public CheckinWorkItemAction valueOf(@NotNull WorkItem workItem) {
      CheckinWorkItemAction action = myContent.getAction(workItem);

      return CheckinWorkItemAction.None.equals(action) ? null : action;
    }

    @Override
    public boolean isCellEditable(@NotNull WorkItem workItem) {
      return true;
    }

    @Nullable
    @Override
    public TableCellEditor getEditor(@NotNull WorkItem workItem) {
      return myCellEditor;
    }

    @Override
    public void setValue(@NotNull WorkItem workItem, @NotNull CheckinWorkItemAction value) {
      myContent.setAction(workItem, value);
    }
  };
}
