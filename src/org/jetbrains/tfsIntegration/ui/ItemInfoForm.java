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

import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.BranchRelative;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.text.MessageFormat;
import java.util.Collection;

public class ItemInfoForm {
  private JLabel myServerNameLabel;
  private JLabel myLocalNameLabel;
  private JLabel myLatestVersionLabel;
  private JLabel myWorkspaceVersionLabel;
  private JLabel myEncodingLabel;
  private JLabel myPendingChangesLabel;
  private JLabel myBranchesLabel;
  private JTree myBranchesTree;
  private JPanel myPanel;
  private JLabel myDeletionIdLabel;
  private JLabel myLockLabel;
  private JScrollPane myTreePane;
  private JLabel myWorkspaceLabel;
  private final Collection<BranchRelative> myBranches;

  public ItemInfoForm(final WorkspaceInfo workspace, final ExtendedItem item, final Collection<BranchRelative> branches) {
    myBranches = branches;
    myServerNameLabel.setText(item.getTitem() != null ? item.getTitem() : item.getSitem());
    myLocalNameLabel.setText(item.getLocal());
    myLatestVersionLabel.setText(String.valueOf(item.getLatest()));
    myWorkspaceVersionLabel.setText(String.valueOf(item.getLver()));
    myEncodingLabel.setText(item.getEnc() != Integer.MIN_VALUE ? String.valueOf(item.getEnc()) : "(not applicable)");
    myPendingChangesLabel.setText(item.getChg() != null ? item.getChg() : "(none)");
    myDeletionIdLabel.setText(item.getDid() != Integer.MIN_VALUE ? String.valueOf(item.getDid()) : "(not deleted)");
    myLockLabel.setText(item.getLock() != null ? item.getLock().getValue() : "None");
    myWorkspaceLabel.setText(MessageFormat.format("{0} on server {1}", workspace.getName(), workspace.getServer().getUri()));

    if (myBranches.size() < 2) {
      myTreePane.setVisible(false);
      myBranchesLabel.setText("No branches");
    }
  }

  public JComponent getPanel() {
    return myPanel;
  }

  private void createUIComponents() {
    BranchRelative root = null;
    if (myBranches == null) {
      myBranchesTree = new JTree();
    }
    else {
      for (BranchRelative branch : myBranches) {
        if (branch.getRelfromid() == 0) {
          root = branch;
          break;
        }
      }

      myBranchesTree = new JTree(buildTree(root));
    }

    //final ColumnInfo[] columns = new ColumnInfo[]{new ColumnInfo("Server Path") {
    //  public Object valueOf(final Object o) {
    //    final Object userObject = ((DefaultMutableTreeNode)o).getUserObject();
    //    return ((BranchRelative)userObject).getBranchToItem().getItem();
    //  }
    //}, new ColumnInfo("Branched from Version") {
    //  public Object valueOf(final Object o) {
    //    final Object userObject = ((DefaultMutableTreeNode)o).getUserObject();
    //    return ((BranchRelative)userObject).getBranchToItem().getCs();
    //  }
    //}};
    //
    //ListTreeTableModelOnColumns model = new ListTreeTableModelOnColumns(buildTree(root), columns);
    //myBranchesTree = new TreeTable(model);


    myBranchesTree.setCellRenderer(new DefaultTreeCellRenderer() {
      public Component getTreeCellRendererComponent(final JTree tree,
                                                    final Object value,
                                                    final boolean sel,
                                                    final boolean expanded,
                                                    final boolean leaf,
                                                    final int row,
                                                    final boolean hasFocus) {
        final Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        final Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
        if (userObject instanceof BranchRelative) {
          BranchRelative branch = (BranchRelative)userObject;

          final String text;
          if (branch.getBranchFromItem() != null) {
            text = MessageFormat.format("{0} ({1})", branch.getBranchToItem().getItem(), branch.getBranchFromItem().getCs());
          }
          else {
            text = branch.getBranchToItem().getItem();
          }

          setText(text);

          Font defaultFont = tree.getFont();
          Font boldFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
          setFont(branch.getReqstd() ? boldFont : defaultFont);
        }
        return c;
      }
    });
  }

  private DefaultMutableTreeNode buildTree(BranchRelative root) {
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root);
    rootNode.setUserObject(root);
    for (BranchRelative branch : myBranches) {
      if (branch.getRelfromid() == root.getReltoid()) {
        rootNode.add(buildTree(branch));
      }
    }
    return rootNode;
  }
}
