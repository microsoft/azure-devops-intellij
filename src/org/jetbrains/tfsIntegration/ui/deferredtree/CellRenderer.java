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
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

class CellRenderer<T> extends DefaultTreeCellRenderer {

  private final LabelProvider<T> myLabelProvider;

  public CellRenderer(final LabelProvider<T> labelProvider) {
    myLabelProvider = labelProvider;
    setLeafIcon(myLabelProvider.getVirtualItemIcon()); // this is used only by cell editor and only virtual nodes are editable
  }

  @SuppressWarnings({"unchecked"})
  public Component getTreeCellRendererComponent(final JTree tree,
                                                final Object value,
                                                final boolean sel,
                                                final boolean expanded,
                                                final boolean leaf,
                                                final int row,
                                                final boolean hasFocus) {
    Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

    if (value instanceof ErrorNode) {
      setIcon(myLabelProvider.getErrorIcon());
      setForeground(myLabelProvider.getErrorColor());
    }
    else if (value instanceof DeferredTreeNode) {
      DeferredTreeNode<T> node = (DeferredTreeNode<T>)value;
      setIcon(myLabelProvider.getIcon(node.getUserObjectTyped()));
      final Color color = myLabelProvider.getColor(node.getUserObjectTyped());
      if (color != null) {
        setForeground(color);
      }
      setText(myLabelProvider.getLabel(node.getUserObjectTyped()));
    }
    else if (value instanceof VirtualNode) {
      VirtualNode virtualNode = (VirtualNode)value;
      setIcon(myLabelProvider.getVirtualItemIcon());
      setText(virtualNode.getText());
      final Color color = myLabelProvider.getVirtualItemColor();
      if (color != null) {
        setForeground(color);
      }
    }
    else if (value instanceof LoadingNode) {
      setText("Loading...");
      setIcon(null);
    }
    return component;
  }

}
