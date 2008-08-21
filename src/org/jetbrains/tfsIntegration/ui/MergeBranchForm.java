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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Changeset;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.MergeCandidate;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MergeBranchForm {

  public interface Listener {
    void stateChanged(boolean canFinish);
  }

  private enum ChangesType {
    ALL {
      public String toString() {
        return "All changes up to a specific version";
      }},

    SELECTED {
      public String toString() {
        return "Selected changesets";
      }
    }
  }

  private JLabel mySourceBranchLabel;
  private JComboBox myTargetBranchCombo;
  private JComboBox myChangesTypeCombo;
  private SelectRevisionForm mySelectRevisionForm;
  private JPanel myContentPanel;
  private JPanel myChangesetsPanel;
  private final Project myProject;
  private final WorkspaceInfo myWorkspace;
  private final String mySourceServerPath;
  private final JTable myChangesetsTable;
  private final ChangesetsTableModel myChangesetsTableModel;
  private final List<Listener> myListeners = new ArrayList<Listener>();

  public MergeBranchForm(Project project, WorkspaceInfo workspace, String sourceServerPath, final Collection<Item> targetBranches) {
    myProject = project;
    myWorkspace = workspace;
    mySourceServerPath = sourceServerPath;

    myChangesetsTableModel = new ChangesetsTableModel();
    myChangesetsTable = new JTable(myChangesetsTableModel);
    myChangesetsTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

    mySelectRevisionForm = new SelectRevisionForm(myProject, myWorkspace, Collections.singletonList(sourceServerPath));

    myChangesetsPanel.add(mySelectRevisionForm.getPanel(), ChangesType.ALL.toString());
    myChangesetsPanel.add(new JScrollPane(myChangesetsTable), ChangesType.SELECTED.toString());

    mySourceBranchLabel.setText(sourceServerPath);

    myTargetBranchCombo.setModel(new DefaultComboBoxModel(targetBranches.toArray()));

    myTargetBranchCombo.setRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(final JList list,
                                                    final Object value,
                                                    final int index,
                                                    final boolean isSelected,
                                                    final boolean cellHasFocus) {
        final Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null) {
          Item item = (Item)value;
          setText(item.getItem());
        }
        return c;
      }
    });

    myTargetBranchCombo.setSelectedIndex(0);
    myTargetBranchCombo.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        ChangesType changesType = (ChangesType)myChangesTypeCombo.getSelectedItem();
        if (changesType == ChangesType.SELECTED) {
          updateChangesetsTable();
        }
      }
    });

    myChangesTypeCombo.setModel(new DefaultComboBoxModel(ChangesType.values()));

    myChangesTypeCombo.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        ChangesType changesType = (ChangesType)myChangesTypeCombo.getSelectedItem();
        if (changesType == ChangesType.SELECTED) {
          updateChangesetsTable();
        }
        ((CardLayout)myChangesetsPanel.getLayout()).show(myChangesetsPanel, changesType.toString());
        fireStateChanged(changesType == ChangesType.ALL);
      }
    });

    myChangesetsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent e) {
        ChangesType changesType = (ChangesType)myChangesTypeCombo.getSelectedItem();
        if (changesType == ChangesType.SELECTED) {
          fireStateChanged(myChangesetsTable.getSelectedRowCount() > 0);
        }
      }
    });

    myChangesTypeCombo.setSelectedIndex(0);
    mySelectRevisionForm.init(project, workspace, Collections.singletonList(sourceServerPath));
  }

  public JComponent getContentPanel() {
    return myContentPanel;
  }

  private void updateChangesetsTable() {
    getContentPanel().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    List<Changeset> changesets = new ArrayList<Changeset>();
    try {
      final Collection<MergeCandidate> mergeCandidates = myWorkspace.getServer().getVCS()
        .queryMergeCandidates(myWorkspace.getName(), myWorkspace.getOwnerName(), mySourceServerPath, getTargetPath());
      for (MergeCandidate candidate : mergeCandidates) {
        changesets.add(candidate.getChangeset());
      }
    }
    catch (TfsException e) {
      Messages.showErrorDialog(myProject, e.getMessage(), "Query Merge Changesets");
    }
    finally {
      getContentPanel().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    myChangesetsTableModel.setChangesets(changesets);
  }

  public String getTargetPath() {
    Item targetBranch = (Item)myTargetBranchCombo.getSelectedItem();
    return targetBranch.getItem();
  }

  @Nullable
  public VersionSpecBase getFromVersion() {
    ChangesType changesType = (ChangesType)myChangesTypeCombo.getSelectedItem();
    if (changesType == ChangesType.SELECTED) {
      final Changeset fromChangeset =
        myChangesetsTableModel.getChangesets().get(myChangesetsTable.getSelectionModel().getMinSelectionIndex());
      return new ChangesetVersionSpec(fromChangeset.getCset());
    }
    else {
      return null;
    }
  }

  @Nullable
  public VersionSpecBase getToVersion() {
    ChangesType changesType = (ChangesType)myChangesTypeCombo.getSelectedItem();
    if (changesType == ChangesType.SELECTED) {
      final Changeset toChangeset =
        myChangesetsTableModel.getChangesets().get(myChangesetsTable.getSelectionModel().getMaxSelectionIndex());
      return new ChangesetVersionSpec(toChangeset.getCset());
    }
    else {
      return mySelectRevisionForm.getVersionSpec();
    }
  }

  public void addListener(Listener listener) {
    myListeners.add(listener);
  }

  public void removeListener(Listener listener) {
    myListeners.remove(listener);
  }

  private void fireStateChanged(final boolean canFinish) {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (Listener listener : listeners) {
      listener.stateChanged(canFinish);
    }
  }


}
