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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class ManageWorkspacesForm {

  public enum Mode {
    Manage, Choose
  }

  public interface Listener {
    void selectionChanged(WorkspaceInfo selection);

    void chosen(final WorkspaceInfo selection);
  }

  private JPanel myContentPane;
  private JButton myAddButton;
  private JButton myReloadButton;
  private JButton myDeleteButton;
  private JButton myEditButton;
  private WorkspacesTableModel myWorkspacesTableModel;
  private ServerInfo myServer;
  private JTable myWorkspacesTable;
  private JLabel myWorkspacesLabel;
  private List<Listener> myListeners = new ArrayList<Listener>();
  private final Project myProject;
  private final Mode myMode;

  private enum WorkspacesTableColumn {
    /*Server(TFSBundle.message("workspacesdialog.column.server")) {
      public String getValue(WorkspaceInfo workspaceInfo) {
        return workspaceInfo.getServer().getUri().toString();
      }
    },*/
    Name(TFSBundle.message("workspacesdialog.column.name"), 100) {
      public String getValue(WorkspaceInfo workspaceInfo) {
        return workspaceInfo.getName();
      }
    },
    /*Owner(TFSBundle.message("workspacesdialog.column.owner")) {
      public String getValue(WorkspaceInfo workspaceInfo) {
        return workspaceInfo.getOwnerName();
      }
    },
    Computer(TFSBundle.message("workspacesdialog.column.computer")) {
      public String getValue(WorkspaceInfo workspaceInfo) {
        return workspaceInfo.getComputer();
      }
    },*/
    Comment(TFSBundle.message("workspacesdialog.column.comment"), 200) {
      public String getValue(WorkspaceInfo workspaceInfo) {
        return workspaceInfo.getComment();
      }
    };

    private final String myCaption;
    private final int myWidth;

    WorkspacesTableColumn(String caption, int width) {
      myCaption = caption;
      myWidth = width;
    }

    public String getCaption() {
      return myCaption;
    }

    public abstract String getValue(WorkspaceInfo workspaceInfo);

    public int getWidth() {
      return myWidth;
    }
  }

  private static class WorkspacesTableModel extends AbstractTableModel {
    private List<WorkspaceInfo> myWorkspaces;

    public void setWorkspaces(List<WorkspaceInfo> workspaceInfos) {
      myWorkspaces = workspaceInfos;
      fireTableDataChanged();
    }

    public List<WorkspaceInfo> getWorkspaces() {
      return myWorkspaces;
    }

    public String getColumnName(final int column) {
      return WorkspacesTableColumn.values()[column].getCaption();
    }

    public int getRowCount() {
      return myWorkspaces != null ? myWorkspaces.size() : 0;
    }

    public int getColumnCount() {
      return WorkspacesTableColumn.values().length;
    }

    public Object getValueAt(final int rowIndex, final int columnIndex) {
      return WorkspacesTableColumn.values()[columnIndex].getValue(myWorkspaces.get(rowIndex));
    }
  }

  public void setServer(ServerInfo server) {
    myServer = server;
    String labelText = MessageFormat.format("Workspaces for user {0} at server {1}", server.getQualifiedUsername(), server.getUri());
    myWorkspacesLabel.setText(labelText);
    updateControls();
  }

  public ManageWorkspacesForm(final Project project, final Mode mode) {
    myProject = project;
    myMode = mode;
    myAddButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        WorkspaceInfo newWorkspaceInfo = new WorkspaceInfo(myServer, myServer.getQualifiedUsername(), Workstation.getComputerName());
        WorkspaceDialog workspaceDialog = new WorkspaceDialog(myProject, newWorkspaceInfo);
        workspaceDialog.show();
        if (workspaceDialog.isOK()) {
          try {
            newWorkspaceInfo.saveToServer();
            updateControls();
            int rowToSelect = myServer.getWorkspacesForCurrentOwner().indexOf(newWorkspaceInfo);
            TFSVcs.assertTrue(rowToSelect >= 0);
            myWorkspacesTable.getSelectionModel().setSelectionInterval(rowToSelect, rowToSelect);
          }
          catch (TfsException ex) {
            String message =
              MessageFormat.format("Failed to create workspace ''{0}''.\n{1}", newWorkspaceInfo.getName(), ex.getLocalizedMessage());
            Messages.showErrorDialog(myProject, message, "Create Workspace");
          }
        }
      }
    });

    myEditButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        editWorkspace();
      }
    });

    myDeleteButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        String message = "Are you sure you want to delete workspace?"; // TODO workspace name
        if (JOptionPane.showConfirmDialog(myContentPane, message, "Delete Workspace", JOptionPane.YES_NO_OPTION) == JOptionPane
          .YES_OPTION) {
          deleteWorkspace();
        }
      }
    });

    myReloadButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        try {
          myServer.refreshWorkspacesForCurrentOwner();
          updateControls();
        }
        catch (Exception ex) {
          String message = MessageFormat.format("Failed to refresh workspaces.\n{0}", ex.getLocalizedMessage());
          Messages.showErrorDialog(myProject, message, "Refresh Workspaces");
        }
      }
    });
    myWorkspacesTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(final MouseEvent e) {
        if (e.getClickCount() == 2) {
          if (myMode == Mode.Manage) {
            editWorkspace();
          }
          else {
            fireChosen();
          }
        }
      }
    });
  }

  private void createUIComponents() {
    myWorkspacesTableModel = new WorkspacesTableModel();
    myWorkspacesTable = new JTable(myWorkspacesTableModel);
    for (WorkspacesTableColumn column : WorkspacesTableColumn.values()) {
      myWorkspacesTable.getColumn(column.getCaption()).setPreferredWidth(column.getWidth());
    }

    myWorkspacesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    myWorkspacesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent e) {
        updateButtons();
        fireSelectionChanged();
      }
    });
  }

  private void updateControls() {
    myWorkspacesTableModel.setWorkspaces(myServer.getWorkspacesForCurrentOwner());
    updateButtons();
  }

  private void updateButtons() {
    myEditButton.setEnabled(myWorkspacesTable.getSelectedRowCount() == 1);
    myDeleteButton.setEnabled(myWorkspacesTable.getSelectedRowCount() == 1);
  }

  private void editWorkspace() {
    int selectedRow = myWorkspacesTable.getSelectedRow();
    WorkspaceInfo workspace = myWorkspacesTableModel.getWorkspaces().get(selectedRow);
    try {
      workspace.loadFromServer();
      WorkspaceInfo workspaceCopyToEdit = workspace.getCopy();
      WorkspaceDialog workspaceDialog = new WorkspaceDialog(myProject, workspaceCopyToEdit);
      workspaceDialog.show();
      if (workspaceDialog.isOK()) {
        workspaceCopyToEdit.saveToServer();
        workspace.getServer().replaceWorkspace(workspace, workspaceCopyToEdit);
        updateControls();
        myWorkspacesTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
      }
    }
    catch (Exception ex) {
      String message =
        MessageFormat.format("Failed to open workspace ''{0}'' for editing.\n{1}", workspace.getName(), ex.getLocalizedMessage());
      Messages.showErrorDialog(myProject, message, "Edit Workspace");
    }
  }

  private void deleteWorkspace() {
    WorkspaceInfo workspaceInfo = myWorkspacesTableModel.getWorkspaces().get(myWorkspacesTable.getSelectedRow());
    try {
      workspaceInfo.getServer().deleteWorkspace(workspaceInfo);
      updateControls();
    }
    catch (Exception ex) {
      String message = MessageFormat.format("Failed to delete workspace ''{0}''.\n{1}", workspaceInfo.getName(), ex.getLocalizedMessage());
      Messages.showErrorDialog(myProject, message, "Delete Workspace");
    }
  }

  @Nullable
  public WorkspaceInfo getSelectedWorkspace() {
    if (myWorkspacesTable.getSelectedRowCount() == 1) {
      return myWorkspacesTableModel.getWorkspaces().get(myWorkspacesTable.getSelectedRow());
    }
    else {
      return null;
    }
  }

  public void setSelectedWorkspace(WorkspaceInfo workspace) {
    if (workspace != null) {
      int indexToSelect = myWorkspacesTableModel.getWorkspaces().indexOf(workspace);
      TFSVcs.assertTrue(indexToSelect >= 0);
      myWorkspacesTable.getSelectionModel().setSelectionInterval(indexToSelect, indexToSelect);
    }
    else {
      myWorkspacesTable.getSelectionModel().clearSelection();
    }
  }

  public JComponent getContentPane() {
    return myContentPane;
  }

  public void addSelectionListener(Listener listener) {
    myListeners.add(listener);
  }

  public void removeSelectionListener(Listener listener) {
    myListeners.remove(listener);
  }


  private void fireSelectionChanged() {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (Listener listener : listeners) {
      listener.selectionChanged(getSelectedWorkspace());
    }
  }

  private void fireChosen() {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (Listener listener : listeners) {
      listener.chosen(getSelectedWorkspace());
    }
  }

}
