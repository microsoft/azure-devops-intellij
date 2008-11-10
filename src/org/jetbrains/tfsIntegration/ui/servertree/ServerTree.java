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
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.ui.deferredtree.DeferredTree;
import org.jetbrains.tfsIntegration.ui.deferredtree.SelectedPath;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ServerTree {

  public interface SelectionListener {
    void selectionChanged(SelectedItem selection);
  }

  public interface PathFilter {
    boolean isAcceptablePath(final @NotNull String path);
  }

  public static class SelectedItem {
    public final String path;
    public final boolean isDirectory;

    public SelectedItem(final String path, final boolean directory) {
      this.path = path;
      isDirectory = directory;
    }
  }

  private static final Color DISABLED_COLOR = Color.GRAY;

  private final boolean myFoldersOnly;
  private JPanel myContentPanel;
  private DeferredTree<Item> myTree;
  private final List<SelectionListener> mySelectionListeners = new ArrayList<SelectionListener>();

  public ServerTree(final boolean foldersOnly) {
    myFoldersOnly = foldersOnly;
  }

  private void createUIComponents() {
    myTree = new DeferredTree<Item>();
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myTree.setExpandsSelectedPaths(true);
    myTree.addListener(new DeferredTree.Listener<Item>() {
      public void selectionChanged(final Collection<SelectedPath<Item>> selection) {
        fireSelectionChanged(getSelectedItem());
      }
    });
  }

  public void configure(final @NotNull ServerInfo server, final @Nullable String pathToSelect, final @Nullable PathFilter pathFilter) {
    final Item[] itemsToSelect;
    if (pathToSelect != null) {
      final String[] pathComponents = VersionControlPath.getPathComponents(pathToSelect);
      List<Item> items = new ArrayList<Item>(pathComponents.length);
      for (String component : pathComponents) {
        final Item item;
        if (items.isEmpty()) {
          item = ServerTreeContentProvider.ROOT;
        }
        else {
          item = new Item();
          String parentPath = items.get(items.size() - 1).getItem();
          if (!parentPath.endsWith(VersionControlPath.PATH_SEPARATOR)) {
            parentPath += VersionControlPath.PATH_SEPARATOR;
          }
          item.setItem(parentPath + component);
        }
        items.add(item);
      }
      itemsToSelect = items.toArray(new Item[items.size()]);
    }
    else {
      itemsToSelect = new Item[0];
    }

    myTree.setContentProvider(new ServerTreeContentProvider(server, myFoldersOnly), itemsToSelect);
    myTree.setLabelProvider(new ServerTreeLabelProvider(server.getUri().toString(), pathFilter));
  }

  public JPanel getContentPanel() {
    return myContentPanel;
  }

  @Nullable
  public SelectedItem getSelectedItem() {
    if (!myTree.getSelectedPaths().isEmpty()) {
      final SelectedPath<Item> selectedPath = myTree.getSelectedPaths().iterator().next();
      final Item deepestRealItem = selectedPath.getRealNodes().get(selectedPath.getRealNodes().size() - 1);
      boolean isDirectory = deepestRealItem.getType() == ItemType.Folder;
      StringBuilder path = new StringBuilder(deepestRealItem.getItem());
      for (String virtualItem : selectedPath.getVirtualNodes()) {
        if (!path.toString().endsWith(VersionControlPath.PATH_SEPARATOR)) {
          path.append(VersionControlPath.PATH_SEPARATOR);
        }
        path.append(virtualItem);
        isDirectory = false;
      }
      return new SelectedItem(path.toString(), isDirectory);
    }
    return null;
  }

  public boolean isItemSelected() {
    return !myTree.getSelectedPaths().isEmpty();
  }

  public void createVirtualFolder() {
    myTree.createChild(myTree.getSelectionPath());
  }

  public void addSelectionListener(SelectionListener listener) {
    mySelectionListeners.add(listener);
  }

  public void removeSelectionListener(SelectionListener listener) {
    mySelectionListeners.remove(listener);
  }

  private void fireSelectionChanged(SelectedItem selectedItem ) {
    SelectionListener[] listeners = mySelectionListeners.toArray(new SelectionListener[mySelectionListeners.size()]);
    for (SelectionListener listener : listeners) {
      listener.selectionChanged(selectedItem);
    }
  }

}
