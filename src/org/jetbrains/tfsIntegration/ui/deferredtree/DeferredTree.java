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

package org.jetbrains.tfsIntegration.ui.deferredtree;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

public class DeferredTree<T> extends JTree {

  static final String NEW_CHILD_DEFAULT_TEXT = "New Folder";

  public interface Listener<T> {
    void selectionChanged(Collection<SelectedPath<T>> selection);
  }

  private final Collection<Listener<T>> myListeners = new ArrayList<Listener<T>>();

  public DeferredTree() {
    setContentProvider(new NullContentProvider<T>(), null);
    setEditable(true);
    setModel(new DefaultTreeModel(new DefaultMutableTreeNode("")) {
      @SuppressWarnings({"unchecked"})
      public void valueForPathChanged(final TreePath path, final Object newValue) {
        super.valueForPathChanged(path, newValue);
        final Collection<SelectedPath<T>> selectedPaths = getSelectedPaths();
        Listener[] list = myListeners.toArray(new Listener[myListeners.size()]);
        for (Listener<T> listener : list) {
          listener.selectionChanged(selectedPaths);
        }
      }
    });
  }

  public void createChild(final TreePath parentPath) {
    Runnable createRunnable = new Runnable() {
      @SuppressWarnings({"unchecked"})
      public void run() {
        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentPath.getLastPathComponent();

        if (parentNode instanceof DeferredTreeNode) {
          ((DeferredTreeNode<T>)parentNode).ensureChildrenLoadedSync();
        }

        expandPath(parentPath);

        final DefaultMutableTreeNode child = new VirtualNode(NEW_CHILD_DEFAULT_TEXT);
        ((DefaultTreeModel)getModel()).insertNodeInto(child, parentNode, parentNode.getChildCount());

        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            final TreePath childPath = new TreePath(child.getPath());
            scrollPathToVisible(childPath);
            setSelectionPath(childPath);
            startEditingAtPath(childPath);
          }
        });
      }
    };
    new Thread(createRunnable).start();
  }

  public void setLabelProvider(LabelProvider<T> labelProvider) {
    final CellRenderer<T> renderer = new CellRenderer<T>(labelProvider);
    setCellRenderer(renderer);
    setCellEditor(new TreeCellEditorWrapper(this, renderer));
  }

  public void setContentProvider(ContentProvider<T> contentProvider, final T[] pathToSelect) {
    try {
      final Collection<T> roots = contentProvider.getChildren(null);
      if (roots.size() > 1) {
        throw new IllegalArgumentException("There should be only one root item");
      }

      if (!roots.isEmpty()) {
        final DeferredTreeNode<T> rootNode =
          new DeferredTreeNode<T>((DefaultTreeModel)getModel(), contentProvider, roots.iterator().next());

        // find should be called before setRoot(), otherwise async children update will be started and will execute second time after sync one finished
        DeferredTreeNode<T> nodeToSelect = pathToSelect.length > 0 ? find(rootNode, pathToSelect, 0) : null;
        ((DefaultTreeModel)getModel()).setRoot(rootNode);
        if (nodeToSelect != null) {
          TreePath p = new TreePath(nodeToSelect.getPath());
          expandPath(p.getParentPath());
          setSelectionPath(p);
          scrollPathToVisible(p);
        }
      }
      else {
        ((DefaultTreeModel)getModel()).setRoot(null);
      }
    }
    catch (ContentProviderException e) {
      ErrorNode errorNode = new ErrorNode(e.getMessage());
      ((DefaultTreeModel)getModel()).setRoot(errorNode);
    }
  }

  @SuppressWarnings({"unchecked"})
  @Nullable
  private static <T> DeferredTreeNode<T> find(final DeferredTreeNode<T> currentNode, final T[] pathToFind, final int depth) {
    if (currentNode.getContentProvider().equals(currentNode.getUserObjectTyped(), pathToFind[depth])) {
      if (depth == pathToFind.length - 1) {
        return currentNode;
      }
      else {
        currentNode.ensureChildrenLoadedSync();

        for (Enumeration<?> children = currentNode.children(); children.hasMoreElements();) {
          TreeNode child = (TreeNode)children.nextElement();

          if (child instanceof DeferredTreeNode) {
            final DeferredTreeNode<T> found = find((DeferredTreeNode<T>)child, pathToFind, depth + 1);
            if (found != null) {
              return found;
            }
          }
        }
        return currentNode;
      }
    }
    return null;
  }

  @SuppressWarnings({"unchecked"})
  public Collection<SelectedPath<T>> getSelectedPaths() {
    if (getSelectionPaths() == null) {
      return Collections.emptyList();
    }
    Collection<SelectedPath<T>> result = new ArrayList<SelectedPath<T>>(getSelectionPaths().length);
    for (TreePath treePath : getSelectionPaths()) {
      SelectedPath<T> p = toSelectedPath(treePath);
      if (p != null) {
        result.add(p);
      }
    }
    return result;
  }

  @SuppressWarnings({"unchecked"})
  @Nullable
  static <T> SelectedPath<T> toSelectedPath(TreePath path) {
    if (path.getLastPathComponent() instanceof ErrorNode || path.getLastPathComponent() instanceof LoadingNode) {
      return null;
    }
    if (!(path.getPath()[0] instanceof DeferredTreeNode)) {
      return null;
    }

    List<T> existingNodes = new ArrayList<T>();
    List<String> virtualNodes = new ArrayList<String>();
    for (Object o : path.getPath()) {
      if (o instanceof DeferredTreeNode) {
        DeferredTreeNode<T> existingNode = (DeferredTreeNode<T>)o;
        existingNodes.add((T)existingNode.getUserObject());
      }
      else if (o instanceof VirtualNode) {
        VirtualNode virtualNode = (VirtualNode)o;
        virtualNodes.add(virtualNode.getText());
      }
    }
    return new SelectedPath<T>(existingNodes, virtualNodes);
  }

  public void addListener(final Listener<T> listener) {
    getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(final TreeSelectionEvent e) {
        listener.selectionChanged(getSelectedPaths());
      }
    });
    myListeners.add(listener);
  }

}
