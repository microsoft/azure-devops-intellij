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

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.EventObject;

class TreeCellEditorWrapper implements TreeCellEditor {

  private final TreeCellEditor myDelegate;

  private final JTree myTree;

  public TreeCellEditorWrapper(JTree tree, CellRenderer cellRenderer) {
    myTree = tree;
    myDelegate = new DefaultTreeCellEditor(tree, cellRenderer);
  }

  public void addCellEditorListener(CellEditorListener l) {
    myDelegate.addCellEditorListener(l);
  }

  public void cancelCellEditing() {
    final Runnable runnable = new Runnable() {
      public void run() {
        myDelegate.cancelCellEditing();
      }
    };
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    }
    else {
      try {
        SwingUtilities.invokeAndWait(runnable);
      }
      catch (InterruptedException e) {
        // ignore
      }
      catch (InvocationTargetException e) {
        // ignore
      }
    }
  }

  public Object getCellEditorValue() {
    final String value = (String)myDelegate.getCellEditorValue();
    return value != null && value.length() > 0 ? value : DeferredTree.NEW_CHILD_DEFAULT_TEXT;
  }

  public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
    return myDelegate.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
  }

  public boolean isCellEditable(EventObject anEvent) {
    final TreePath selection = myTree.getSelectionPath();
    if (selection == null || selection.getLastPathComponent() == null || selection.getLastPathComponent() instanceof VirtualNode == false) {
      return false;
    }
    return myDelegate.isCellEditable(anEvent);
  }

  public void removeCellEditorListener(CellEditorListener l) {
    myDelegate.removeCellEditorListener(l);
  }

  public boolean shouldSelectCell(EventObject anEvent) {
    return myDelegate.shouldSelectCell(anEvent);
  }

  public boolean stopCellEditing() {
    String value = (String)getCellEditorValue();
    return value != null && value.length() > 0 && myDelegate.stopCellEditing();
  }

}
