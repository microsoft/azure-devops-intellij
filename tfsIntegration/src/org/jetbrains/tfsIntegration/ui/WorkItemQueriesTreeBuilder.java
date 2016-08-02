package org.jetbrains.tfsIntegration.ui;

import com.intellij.ui.treeStructure.SimpleTreeBuilder;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class WorkItemQueriesTreeBuilder extends SimpleTreeBuilder {

  public WorkItemQueriesTreeBuilder(@NotNull JTree tree, @NotNull SimpleTreeStructure treeStructure) {
    super(tree, new DefaultTreeModel(new DefaultMutableTreeNode(treeStructure.getRootElement())), treeStructure, null);
  }

  @Override
  protected boolean isSmartExpand() {
    return false;
  }
}
