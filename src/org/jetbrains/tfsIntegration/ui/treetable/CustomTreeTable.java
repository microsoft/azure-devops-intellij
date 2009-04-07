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
import com.intellij.ui.treeStructure.treetable.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.List;

public class CustomTreeTable<T> extends TreeTable {
  private List<? extends TreeTableColumn<T>> myColumns;
  private ContentProvider<T> myContentProvider;
  private final CellRenderer<T> myRenderer;
  private final boolean myShowCellFocus;
  private final boolean myShowSelection;

  private static final Object HIDDEN_ROOT = new Object();

  private static final class FakeColumn<T> extends TreeTableColumn<T> {

    public FakeColumn() {
      super(null, 0);
    }

    public String getPresentableString(final T value) {
      return "";
    }
  }

  private static final class FakeContentProvider<T> implements ContentProvider<T> {

    public Collection<? extends T> getRoots() {
      return Collections.emptyList();
    }

    public Collection<? extends T> getChildren(final @NotNull T parent) {
      return Collections.emptyList();
    }
  }

  public CustomTreeTable(final CellRenderer<T> renderer, boolean showCellFocus, boolean showSelection) {
    this(Arrays.asList(new FakeColumn<T>()), new FakeContentProvider<T>(), renderer, showCellFocus, showSelection);
  }

  /**
   * @param columns  first one will be used as tree-style
   * @param renderer
   */
  public CustomTreeTable(List<? extends TreeTableColumn<T>> columns,
                         ContentProvider<T> contentProvider,
                         final CellRenderer<T> renderer,
                         boolean showCellFocus,
                         boolean showSelection) {
    super(createModel(columns, contentProvider));
    myColumns = columns;
    myContentProvider = contentProvider;
    myRenderer = renderer;
    myShowCellFocus = showCellFocus;
    myShowSelection = showSelection;
    initialize();
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

  private static <T> TreeTableModel createModel(final Collection<? extends TreeTableColumn<T>> columns,
                                                ContentProvider<T> contentProvider) {
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
    final Collection<? extends T> rootObjects = contentProvider.getRoots();
    if (!rootObjects.isEmpty()) {
      if (rootObjects.size() == 1) {
        root = new DefaultMutableTreeNode(rootObjects.iterator().next());
        addChildren(root, contentProvider);
      }
      else {
        root = new DefaultMutableTreeNode(HIDDEN_ROOT);
        for (T rootObject : rootObjects) {
          DefaultMutableTreeNode subRoot = new DefaultMutableTreeNode(rootObject);
          addChildren(subRoot, contentProvider);
          root.add(subRoot);
        }
      }
    }
    else {
      root = null;
    }
    return new ListTreeTableModelOnColumns(root, columnsInfos.toArray(new ColumnInfo[columnsInfos.size()]));
  }

  private static <T> void addChildren(DefaultMutableTreeNode parentNode, ContentProvider<T> contentProvider) {
    //noinspection unchecked
    final Collection<? extends T> children = contentProvider.getChildren((T)parentNode.getUserObject());
    for (T child : children) {
      final DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
      parentNode.add(childNode);
      addChildren(childNode, contentProvider);
    }
  }

  public void expandAll() {
    for (int i = 0; i < getTree().getRowCount(); i++) {
      getTree().expandRow(i);
    }
  }

  public void initialize(List<TreeTableColumn<T>> columns, ContentProvider<T> contentProvider) {
    myColumns = columns;
    myContentProvider = contentProvider;
    setModel(createModel(columns, contentProvider));
    initialize();
  }

  public void updateContent() {
    setModel(createModel(myColumns, myContentProvider));
    initialize();
  }

  private void initialize() {
    setTreeCellRenderer(new TreeColumnRenderer());
    setDefaultRenderer(TableColumnMarker.class, new TableColumnRenderer());

    for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
      getColumnModel().getColumn(i).setPreferredWidth(myColumns.get(i).getWidth());
    }
    final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)getTableModel().getRoot();
    setRootVisible(rootNode == null || rootNode.getUserObject() != HIDDEN_ROOT);
  }


  public Collection<T> getSelectedItems() {
    final int[] selectedRows = getSelectedRows();
    Collection<T> result = new ArrayList<T>(selectedRows.length);
    for (int row : selectedRows) {
      DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)getTree().getPathForRow(row).getLastPathComponent();
      //noinspection unchecked
      T userObject = (T)treeNode.getUserObject();
      if (userObject != HIDDEN_ROOT) {
        result.add(userObject);
      }
    }
    return result;
  }

  public void select(final T userObject) {
    if (userObject != null) {
      final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)getTableModel().getRoot();
      if (rootNode == null) {
        return;
      }

      DefaultMutableTreeNode node = find(rootNode, userObject);
      if (node != null) {
        final int row = getTree().getRowForPath(new TreePath(node.getPath()));
        getSelectionModel().setSelectionInterval(row, row);
      }
    }
    else {
      getSelectionModel().clearSelection();
    }
  }

  @Nullable
  private DefaultMutableTreeNode find(DefaultMutableTreeNode root, @NotNull T userObject) {
    if (userObject.equals(root.getUserObject())) {
      return root;
    }
    for (int i = 0; i < root.getChildCount(); i++) {
      DefaultMutableTreeNode result = find((DefaultMutableTreeNode)root.getChildAt(i), userObject);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private class TreeColumnRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(final JTree tree,
                                                  final Object value,
                                                  final boolean sel,
                                                  final boolean expanded,
                                                  final boolean leaf,
                                                  final int row,
                                                  final boolean hasFocus) {
      final Component c =
        super.getTreeCellRendererComponent(tree, value, myShowSelection && sel, expanded, leaf, row, myShowCellFocus && hasFocus);
      JLabel label = (JLabel)c;
      final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
      if (HIDDEN_ROOT == treeNode.getUserObject()) {
        return c;
      }
      //noinspection unchecked
      T typedValue = (T)treeNode.getUserObject();
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
      Component c =
        super.getTableCellRendererComponent(table, value, myShowSelection && isSelected, myShowCellFocus && hasFocus, row, column);

      JLabel label = (JLabel)c;
      final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
      if (HIDDEN_ROOT == treeNode.getUserObject()) {
        return c;
      }
      //noinspection unchecked
      T typedValue = (T)(treeNode).getUserObject();
      myRenderer.render(CustomTreeTable.this, myColumns.get(column), typedValue, label);
      return c;
    }
  }

  private static final class TableColumnMarker {
  }

}
