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

package org.jetbrains.tfsIntegration.ui.servertree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class ServerTree {

  public interface SelectionListener {
    void selectionChanged(String selection);
  }

  public interface PathFilter {
    boolean isAcceptablePath(final @NotNull String path);
  }

  private static final Color DISABLED_COLOR = Color.GRAY;

  private final boolean myFoldersOnly;
  private JPanel myContentPanel;
  private JTree myTree;
  private final List<SelectionListener> mySelectionListeners = new ArrayList<SelectionListener>();
  private @Nullable PathFilter myPathFiler;

  public ServerTree(final boolean foldersOnly) {
    myFoldersOnly = foldersOnly;
  }

  private void createUIComponents() {
    myTree = new JTree();
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myTree.setExpandsSelectedPaths(true);
    myTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(final TreeSelectionEvent e) {
        fireSelectionChanged(getSelectedPath());
      }
    });

    myTree.setCellRenderer(new DefaultTreeCellRenderer() {
      public Component getTreeCellRendererComponent(final JTree tree,
                                                    final Object value,
                                                    final boolean sel,
                                                    final boolean expanded,
                                                    final boolean leaf,
                                                    final int row,
                                                    final boolean hasFocus) {
        Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value instanceof ServerTreeNode == false) {
          return component;
        }

        ServerTreeNode node = (ServerTreeNode)value;
        if (myFoldersOnly && node.isLeaf()) {
          setIcon(getClosedIcon());
        }
        if (myPathFiler != null && !myPathFiler.isAcceptablePath(node.getFullPath())) {
          component.setForeground(DISABLED_COLOR);
        }
        return component;
      }
    });
  }

  public void setServer(ServerInfo server) {
    TreeNode rootNode = new ServerTreeNode(server, VersionControlPath.ROOT_FOLDER, myFoldersOnly);
    ((DefaultTreeModel)myTree.getModel()).setRoot(rootNode);
  }

  public JPanel getContentPanel() {
    return myContentPanel;
  }

  @Nullable
  public String getSelectedPath() {
    if (myTree.getSelectionPath() != null) {
      final ServerTreeNode node = (ServerTreeNode)myTree.getSelectionPath().getLastPathComponent();
      return node.getFullPath();
    }
    else {
      return null;
    }
  }

  public void setSelectedPath(@Nullable String serverPath, boolean reload) {
    if (serverPath == null) {
      serverPath = VersionControlPath.ROOT_FOLDER;
    }

    // TODO: refactor
    StringTokenizer tokenizer = new StringTokenizer(serverPath, "/");
    LinkedList<String> paths = new LinkedList<String>();
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      if (paths.isEmpty()) {
        paths.add(token + "/");
      }
      else {
        String prevPath = paths.getLast();
        paths.add(prevPath + (prevPath.endsWith("/") ? "" : "/") + token);
      }
    }

    ServerTreeNode root = (ServerTreeNode)myTree.getModel().getRoot();
    if (paths.isEmpty() || !VersionControlPath.ROOT_FOLDER.equals(paths.getFirst())) {
      // wrong path --- empty or not starts with "$/"
      myTree.setSelectionPath(new TreePath(new TreePath(root)));
      return;
    }

    ServerTreeNode node = (ServerTreeNode)myTree.getModel().getRoot();
    paths.removeFirst(); // remove "$/"

    // find nodes from selected path which already in tree
    while (!paths.isEmpty()) {
      String path = paths.getFirst();
      if (reload) {
        node.markOutOfDate();
      }
      ServerTreeNode childNode = node.findChild(path);
      if (childNode == null) {
        break;
      }
      paths.removeFirst();
      node = childNode;
    }

    TreePath treePath = new TreePath((((DefaultTreeModel)myTree.getModel())).getPathToRoot(node));
    myTree.setSelectionPath(treePath);
  }

  public void addSelectionListener(SelectionListener listener) {
    mySelectionListeners.add(listener);
  }

  public void removeSelectionListener(SelectionListener listener) {
    mySelectionListeners.remove(listener);
  }

  public void setPathFilter(final @Nullable PathFilter pathFilter) {
    myPathFiler = pathFilter;
  }

  private void fireSelectionChanged(String selection) {
    SelectionListener[] listeners = mySelectionListeners.toArray(new SelectionListener[mySelectionListeners.size()]);
    for (SelectionListener listener : listeners) {
      listener.selectionChanged(selection);
    }
  }

}
