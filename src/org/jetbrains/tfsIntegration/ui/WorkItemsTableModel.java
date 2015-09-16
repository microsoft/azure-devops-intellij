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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ColumnInfo;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemLinkInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkItemsCheckinParameters;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemState;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemType;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.List;
import java.util.Map;

class WorkItemsTableModel extends ListTreeTableModelOnColumns {

  private static final Logger LOG = Logger.getInstance(WorkItemsTableModel.class);

  @NotNull private final DefaultMutableTreeNode myRoot;
  @NotNull private final WorkItemsCheckinParameters myContent;

  public WorkItemsTableModel(@NotNull WorkItemsCheckinParameters content) {
    super(null, new ColumnInfo[]{new CheckBoxColumn(content), TYPE, ID, TITLE, STATE, new CheckInActionColumn(content)});

    myContent = content;

    myRoot = new DefaultMutableTreeNode();
    setRoot(myRoot);
  }

  @Nullable
  public CheckinWorkItemAction getAction(@NotNull WorkItem workItem) {
    return myContent.getAction(workItem);
  }

  public void setContent(@NotNull WorkItemsCheckinParameters content) {
    myContent.update(content);

    myRoot.removeAllChildren();
    buildModel();
    reload(myRoot);
  }

  private void buildModel() {
    List<WorkItemLinkInfo> links = myContent.getLinks();

    if (!ContainerUtil.isEmpty(links)) {
      buildTreeModel(links);
    }
    else {
      buildFlatModel();
    }
  }

  private void buildTreeModel(@NotNull List<WorkItemLinkInfo> links) {
    validateLinksStructure(links);

    Map<Integer, DefaultMutableTreeNode> workItemsMap =
      ContainerUtil.map2Map(myContent.getWorkItems(), new Function<WorkItem, Pair<Integer, DefaultMutableTreeNode>>() {
        @Override
        public Pair<Integer, DefaultMutableTreeNode> fun(@NotNull WorkItem workItem) {
          return Pair.create(workItem.getId(), new DefaultMutableTreeNode(workItem));
        }
      });
    workItemsMap.put(0, myRoot);

    for (WorkItemLinkInfo link : links) {
      DefaultMutableTreeNode parentNode = workItemsMap.get(link.getSourceID());
      DefaultMutableTreeNode childNode = workItemsMap.get(link.getTargetID());

      if (parentNode != null && childNode != null) {
        parentNode.add(childNode);
      }
      else {
        LOG.info("Could not resolve work item link " + link.getSourceID() + "-" + link.getTargetID());
      }
    }
  }

  private void validateLinksStructure(@NotNull List<WorkItemLinkInfo> links) {
    if (links.size() != myContent.getWorkItems().size()) {
      String linksValue = StringUtil.join(links, new Function<WorkItemLinkInfo, String>() {
        @Override
        public String fun(@NotNull WorkItemLinkInfo info) {
          return info.getSourceID() + " - " + info.getTargetID();
        }
      }, ", ");
      String workItemIdsValue = StringUtil.join(myContent.getWorkItems(), new Function<WorkItem, String>() {
        @Override
        public String fun(@NotNull WorkItem workItem) {
          return String.valueOf(workItem.getId());
        }
      }, ", ");

      LOG.error("Unknown work item links structure\nLinks: " + linksValue + "\nWork Items: " + workItemIdsValue);
    }
  }

  private void buildFlatModel() {
    for (WorkItem workItem : myContent.getWorkItems()) {
      myRoot.add(new DefaultMutableTreeNode(workItem));
    }
  }

  @Override
  public void setValueAt(Object aValue, Object node, int column) {
    super.setValueAt(aValue, node, column);

    nodeChanged((TreeNode)node);
  }

  abstract static class WorkItemFieldColumn<Aspect> extends ColumnInfo<DefaultMutableTreeNode, Aspect> {

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
    public Aspect valueOf(@NotNull DefaultMutableTreeNode node) {
      Object userObject = node.getUserObject();

      return userObject instanceof WorkItem ? valueOf((WorkItem)userObject) : null;
    }

    @Override
    public void setValue(@NotNull DefaultMutableTreeNode node, @NotNull Aspect value) {
      if (node.getUserObject() instanceof WorkItem) {
        setValue((WorkItem)node.getUserObject(), value);
      }
    }

    public void setValue(@NotNull WorkItem workItem, @NotNull Aspect value) {
    }

    @Nullable
    public abstract Aspect valueOf(@NotNull WorkItem workItem);
  }

  static class CheckBoxColumn extends WorkItemFieldColumn<Boolean> {

    @NotNull private final WorkItemsCheckinParameters myContent;
    // TODO: Do we need this renderer?
    private TableCellRenderer myRenderer = new NoBackgroundBooleanTableCellRenderer();

    public CheckBoxColumn(@NotNull WorkItemsCheckinParameters content) {
      super(" ", 50);

      myContent = content;
    }

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
    public boolean isCellEditable(@NotNull DefaultMutableTreeNode node) {
      return true;
    }

    @Nullable
    @Override
    public TableCellRenderer getRenderer(@NotNull DefaultMutableTreeNode node) {
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
  }

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

    @Override
    public Class<?> getColumnClass() {
      // Such column class indicates that this column will be used as tree - indentations, icons, etc. will be displayed in this column
      return TreeTableModel.class;
    }
  };

  static WorkItemFieldColumn<WorkItemState> STATE = new WorkItemFieldColumn<WorkItemState>("State", 300) {
    @Nullable
    @Override
    public WorkItemState valueOf(@NotNull WorkItem workItem) {
      return workItem.getState();
    }
  };

  static class CheckInActionColumn extends WorkItemFieldColumn<CheckinWorkItemAction> {

    private ComboBox myComboBox = new ComboBox(new CheckinWorkItemAction[]{CheckinWorkItemAction.Resolve, CheckinWorkItemAction.Associate});
    private TableCellEditor myCellEditor = new DefaultCellEditor(myComboBox) {
      @Nullable
      public Component getTableCellEditorComponent(final JTable table,
                                                   final Object value,
                                                   final boolean isSelected,
                                                   final int row,
                                                   final int column) {
        TreeTable treeTable = (TreeTable)table;
        WorkItemsTableModel model = (WorkItemsTableModel)treeTable.getTableModel();
        WorkItem workItem =
          (WorkItem)((DefaultMutableTreeNode)treeTable.getTree().getPathForRow(row).getLastPathComponent()).getUserObject();
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

    @NotNull private final WorkItemsCheckinParameters myContent;

    public CheckInActionColumn(@NotNull WorkItemsCheckinParameters content) {
      super("Checkin Action", 400);

      myContent = content;
    }

    @Nullable
    @Override
    public CheckinWorkItemAction valueOf(@NotNull WorkItem workItem) {
      CheckinWorkItemAction action = myContent.getAction(workItem);

      return CheckinWorkItemAction.None.equals(action) ? null : action;
    }

    @Override
    public boolean isCellEditable(@NotNull DefaultMutableTreeNode node) {
      return true;
    }

    @Nullable
    @Override
    public TableCellEditor getEditor(@NotNull DefaultMutableTreeNode node) {
      return myCellEditor;
    }

    @Override
    public void setValue(@NotNull WorkItem workItem, @NotNull CheckinWorkItemAction value) {
      myContent.setAction(workItem, value);
    }
  }
}
