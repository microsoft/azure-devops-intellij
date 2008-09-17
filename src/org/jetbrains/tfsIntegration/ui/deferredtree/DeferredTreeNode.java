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

import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

class DeferredTreeNode<T> extends DefaultMutableTreeNode {
  private enum State {
    OutOfDate, UpToDate, Loading
  }

  private final DefaultTreeModel myTreeModel;

  private final ContentProvider<T> myContentProvider;

  private volatile State myState = State.OutOfDate;

  public DeferredTreeNode(DefaultTreeModel treeModel, ContentProvider<T> contentProvider, final @NotNull T userObject) {
    super(userObject, contentProvider.canHaveChildren(userObject));
    myTreeModel = treeModel;
    myContentProvider = contentProvider;
  }

  void ensureChildrenLoadedSync() {
    ensureChildrenLoaded(false);
  }

  ContentProvider<T> getContentProvider() {
    return myContentProvider;
  }

  public boolean isLeaf() {
    return !getContentProvider().canHaveChildren(getUserObjectTyped());
  }

  public int getChildCount() {
    ensureChildrenLoaded(true);
    return super.getChildCount();
  }

  private void ensureChildrenLoaded(boolean async) {
    if (myState != State.OutOfDate || !getAllowsChildren()) {
      return;
    }

    myState = State.Loading;
    removeAllChildren();
    add(new LoadingNode(this));

    Runnable updateChildrenRunnable = new Runnable() {
      public void run() {
        try {
          final Collection<T> children = myContentProvider.getChildren(getUserObjectTyped());
          Collection<DeferredTreeNode<T>> childNodes = new ArrayList<DeferredTreeNode<T>>(children.size());
          for (T child : children) {
            childNodes.add(new DeferredTreeNode<T>(myTreeModel, myContentProvider, child));
          }
          setChildNodes(childNodes);
        }
        catch (Exception e) {
          ErrorNode errorNode = new ErrorNode(e.getMessage());
          setChildNodes(Collections.singletonList(errorNode));
        }
      }
    };

    if (async) {
      new Thread(updateChildrenRunnable).start();
    }
    else {
      updateChildrenRunnable.run();
    }
  }

  public Enumeration<?> children() {
    ensureChildrenLoaded(true);
    return super.children();
  }

  public TreeNode getChildAt(final int index) {
    ensureChildrenLoaded(true);
    return super.getChildAt(index);
  }

  public void remove(final int childIndex) {
    ensureChildrenLoaded(true);
    super.remove(childIndex);
  }

  synchronized void updateChildren(final Collection<T> newElements) {
    removeAllChildren(); // remove loading node
    for (T child : newElements) {
      add(new DeferredTreeNode<T>(myTreeModel, myContentProvider, child));
    }
    myTreeModel.nodeStructureChanged(this);
    myState = State.UpToDate;
  }

  synchronized private void setChildNodes(final Collection<? extends DefaultMutableTreeNode> children) {
    removeAllChildren(); // remove loading node
    for (DefaultMutableTreeNode child : children) {
      add(child);
    }
    myTreeModel.nodeStructureChanged(this);
    myState = State.UpToDate;
  }

  @SuppressWarnings("unchecked")
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof DeferredTreeNode) {
      DeferredTreeNode<T> deferredTreeNode = (DeferredTreeNode<T>)obj;
      return myContentProvider.equals(getUserObjectTyped(), deferredTreeNode.getUserObjectTyped());
    }
    return false;
  }

  public int hashCode() {
    return myContentProvider.getHashCode(getUserObjectTyped());
  }

  @SuppressWarnings("unchecked")
  T getUserObjectTyped() {
    return (T)getUserObject();
  }

}
