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


import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.BranchRelative;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ExtendedItem;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.ChangeTypeMask;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.ui.treetable.CellRenderer;
import org.jetbrains.tfsIntegration.ui.treetable.ContentProvider;
import org.jetbrains.tfsIntegration.ui.treetable.CustomTreeTable;
import org.jetbrains.tfsIntegration.ui.treetable.TreeTableColumn;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class ItemInfoForm {

  private JLabel myServerNameLabel;
  private JLabel myLocalNameLabel;
  private JLabel myLatestVersionLabel;
  private JLabel myWorkspaceVersionLabel;
  private JLabel myEncodingLabel;
  private JLabel myPendingChangesLabel;
  private JLabel myBranchesLabel;
  private CustomTreeTable<BranchRelative> myBranchesTree;
  private JPanel myPanel;
  private JLabel myDeletionIdLabel;
  private JLabel myLockLabel;
  private JScrollPane myTreePane;
  private JLabel myWorkspaceLabel;
  private final Collection<BranchRelative> myBranches;

  private static final TreeTableColumn<BranchRelative> SERVER_PATH_COLUMN = new TreeTableColumn<BranchRelative>("Server path", 350) {
    public String getPresentableString(final BranchRelative value) {
      return value.getBranchToItem().getItem();
    }
  };

  private static final TreeTableColumn<BranchRelative> TREE_TABLE_COLUMN =
    new TreeTableColumn<BranchRelative>("Branched from version", 150) {
      public String getPresentableString(final BranchRelative value) {
        if (value.getBranchFromItem() != null) {
          return MessageFormat.format("{0}", value.getBranchFromItem().getCs());
        }
        return "";
      }
    };

  public ItemInfoForm(final WorkspaceInfo workspace, final ExtendedItem item, final Collection<BranchRelative> branches) {
    myBranches = branches;
    myServerNameLabel.setText(item.getTitem() != null ? item.getTitem() : item.getSitem());
    myLocalNameLabel.setText(VersionControlPath.localPathFromTfsRepresentation(item.getLocal()));
    myLatestVersionLabel.setText(String.valueOf(item.getLatest()));
    myWorkspaceVersionLabel.setText(String.valueOf(item.getLver()));
    myEncodingLabel.setText(item.getEnc() != Integer.MIN_VALUE ? String.valueOf(item.getEnc()) : "(not applicable)");
    myPendingChangesLabel.setText(new ChangeTypeMask(item.getChg()).toString());
    myDeletionIdLabel.setText(item.getDid() != Integer.MIN_VALUE ? String.valueOf(item.getDid()) : "(not deleted)");
    myLockLabel.setText(
      item.getLock() != null ? MessageFormat.format("Locked for {0} by {1}", item.getLock().getValue(), item.getLowner()) : "(none)");
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
    List<TreeTableColumn<BranchRelative>> columns = Arrays.asList(SERVER_PATH_COLUMN, TREE_TABLE_COLUMN);
    myBranchesTree = new CustomTreeTable<BranchRelative>(columns, new ContentProviderImpl(), new CellRendererImpl(), false, false);
    myBranchesTree.expandAll();
  }

  private class ContentProviderImpl implements ContentProvider<BranchRelative> {
    public Collection<BranchRelative> getRoots() {
      for (BranchRelative branch : myBranches) {
        if (branch.getRelfromid() == 0) {
          return Collections.singletonList(branch);
        }
      }
      return Collections.emptyList();
    }

    public Collection<BranchRelative> getChildren(final @NotNull BranchRelative parent) {
      final Collection<BranchRelative> children = new ArrayList<BranchRelative>();
      for (BranchRelative branch : myBranches) {
        if (branch.getRelfromid() == parent.getReltoid()) {
          children.add(branch);
        }
      }
      return children;
    }
  }

  private static class CellRendererImpl extends CellRenderer<BranchRelative> {
    public void render(final CustomTreeTable<BranchRelative> treeTable,
                       final TreeTableColumn<BranchRelative> column,
                       final BranchRelative value,
                       final JLabel cell) {
      super.render(treeTable, column, value, cell);
      Font defaultFont = treeTable.getFont();
      Font boldFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
      cell.setFont(value.getReqstd() ? boldFont : defaultFont);
      if (column == SERVER_PATH_COLUMN) {
        cell.setIcon(value.getBranchToItem().getType() == ItemType.Folder ? UiConstants.ICON_FOLDER : UiConstants.ICON_FILE);
      }
    }
  }
}
