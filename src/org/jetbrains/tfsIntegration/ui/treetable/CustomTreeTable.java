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

package org.jetbrains.tfsIntegration.ui.treetable;

import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.treetable.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomTreeTable<T> extends TreeTable {
  private final List<TreeTableColumn<T>> myColumns;
  private final CellRenderer<T> myRenderer;
  private final boolean myShowCellFocus;
  private final boolean myShowSelection;

  /**
   * @param columns  first one will be used as tree-style
   * @param renderer
   */
  public CustomTreeTable(List<TreeTableColumn<T>> columns,
                         ContentProvider<T> contentProvider,
                         final CellRenderer<T> renderer,
                         boolean showCellFocus, boolean showSelection) {
    super(createModel(columns, contentProvider));
    myColumns = columns;
    myRenderer = renderer;
    myShowCellFocus = showCellFocus;
    myShowSelection = showSelection;
    setTreeCellRenderer(new TreeColumnRenderer());
    setDefaultRenderer(TableColumnMarker.class, new TableColumnRenderer());

    for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
      getColumnModel().getColumn(i).setPreferredWidth(myColumns.get(i).getWidth());
    }
  }

  public TreeTableCellRenderer createTableRenderer(final TreeTableModel treeTableModel) {
    return new TreeTableCellRenderer(this, getTree()) {
      public Component getTableCellRendererComponent(final JTable table,
                                                     final Object value,
                                                     final boolean isSelected,
                                                     final boolean hasFocus,
                                                     final int row,
                                                     final int column) {
        return super.getTableCellRendererComponent(table, value, myShowSelection && isSelected, myShowCellFocus && hasFocus, row, column);
      }
    };
  }

  private static <T> TreeTableModel createModel(final Collection<TreeTableColumn<T>> columns, ContentProvider<T> contentProvider) {
    Collection<ColumnInfo> columnsInfos = new ArrayList<ColumnInfo>(columns.size());
    boolean first = true;
    for (final TreeTableColumn<T> column : columns) {
      if (first) {
        columnsInfos.add(new TreeColumnInfo(column.getCaption()));
      }
      else {
        columnsInfos.add(new ColumnInfo(column.getCaption()) {
          public Object valueOf(final Object o) {
            return o;
          }

          public Class getColumnClass() {
            return TableColumnMarker.class;
          }
        });
      }
      first = false;
    }

    final DefaultMutableTreeNode root;
    if (contentProvider.getRoot() != null) {
      root = new DefaultMutableTreeNode(contentProvider.getRoot());
      addChildren(root, contentProvider);
    }
    else {
      root = null;
    }
    return new ListTreeTableModelOnColumns(root, columnsInfos.toArray(new ColumnInfo[columnsInfos.size()]));
  }

  private static <T> void addChildren(DefaultMutableTreeNode parentNode, ContentProvider<T> contentProvider) {
    //noinspection unchecked
    final Collection<T> children = contentProvider.getChildren((T)parentNode.getUserObject());
    for (T child : children) {
      final DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
      parentNode.add(childNode);
      addChildren(childNode, contentProvider);
    }
  }

  private class TreeColumnRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(final JTree tree,
                                                  final Object value,
                                                  final boolean sel,
                                                  final boolean expanded,
                                                  final boolean leaf,
                                                  final int row,
                                                  final boolean hasFocus) {
      final Component c = super.getTreeCellRendererComponent(tree, value, myShowSelection && sel, expanded, leaf, row, myShowCellFocus && hasFocus);
      JLabel label = (JLabel)c;
      //noinspection unchecked
      T typedValue = (T)((DefaultMutableTreeNode)value).getUserObject();
      myRenderer.render(CustomTreeTable.this, myColumns.get(0), typedValue, label);
      return c;
    }
  }

  private class TableColumnRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(final JTable table,
                                                   final Object value,
                                                   final boolean isSelected,
                                                   final boolean hasFocus,
                                                   final int row,
                                                   final int column) {
      Component c = super.getTableCellRendererComponent(table, value, myShowSelection && isSelected, myShowCellFocus && hasFocus, row, column);

      JLabel label = (JLabel)c;
      //noinspection unchecked
      T typedValue = (T)((DefaultMutableTreeNode)value).getUserObject();
      myRenderer.render(CustomTreeTable.this, myColumns.get(column), typedValue, label);
      return c;
    }
  }

  private static final class TableColumnMarker {
  }

}
