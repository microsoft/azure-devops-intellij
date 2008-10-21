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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.WorkspaceNotFoundException;
import org.jetbrains.tfsIntegration.ui.treetable.CellRenderer;
import org.jetbrains.tfsIntegration.ui.treetable.ContentProvider;
import org.jetbrains.tfsIntegration.ui.treetable.CustomTreeTable;
import org.jetbrains.tfsIntegration.ui.treetable.TreeTableColumn;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class ManageWorkspacesForm {
  public interface Listener {
    void selectionChanged();
  }

  private static final TreeTableColumn<Object> COLUMN_SERVER_WORKSPACE = new TreeTableColumn<Object>("Server / workspace", 200) {
    public String getPresentableString(final Object value) {
      if (value instanceof ServerInfo) {
        final ServerInfo server = (ServerInfo)value;
        if (server.getQualifiedUsername() != null) {
          return MessageFormat.format("{0} [{1}]", server.getUri().toString(), server.getQualifiedUsername());
        }
        else {
          return server.getUri().toString();
        }
      }
      else if (value instanceof WorkspaceInfo) {
        return ((WorkspaceInfo)value).getName();
      }
      return "";
    }
  };

  private static final TreeTableColumn<Object> COLUMN_SERVER = new TreeTableColumn<Object>("Server", 200) {
    public String getPresentableString(final Object value) {
      if (value instanceof ServerInfo) {
        final ServerInfo server = (ServerInfo)value;
        return MessageFormat.format("{0} [{1}]", server.getUri().toString(), server.getQualifiedUsername());
      }
      return "";
    }
  };

  private static final TreeTableColumn<Object> COLUMN_COMMENT = new TreeTableColumn<Object>("Workspace comment", 100) {
    public String getPresentableString(final Object value) {
      if (value instanceof WorkspaceInfo) {
        return ((WorkspaceInfo)value).getComment();
      }
      return "";
    }
  };

  private JPanel myContentPane;
  private JButton myAddServerButton;
  private JButton myDeleteWorkspaceButton;
  private JButton myEditWorkspaceButton;
  private CustomTreeTable<Object> myTable;
  private JButton myRemoveServerButton;
  private JButton myCreateWorkspaceButton;
  private JLabel myTitleLabel;
  private JPanel myWorkspacesPanel;
  private List<Listener> myListeners = new ArrayList<Listener>();
  private final Project myProject;
  private boolean myShowWorkspaces = true;

  private final ListSelectionListener mySelectionListener = new ListSelectionListener() {
    public void valueChanged(final ListSelectionEvent e) {
      updateButtons();
      fireSelectionChanged();
    }
  };

  private final MouseListener myMouseListener = new MouseAdapter() {
    public void mouseClicked(final MouseEvent e) {
      if (e.getClickCount() == 2) {
        final WorkspaceInfo workspace = getSelectedWorkspace();
        if (workspace != null) {
          editWorkspace(workspace);
        }
      }
    }
  };


  private ContentProvider<Object> myContentProvider = new ContentProvider<Object>() {

    public Collection<?> getRoots() {
      final List<ServerInfo> servers = new ArrayList<ServerInfo>(Workstation.getInstance().getServers());
      Collections.sort(servers, new Comparator<ServerInfo>() {
        public int compare(final ServerInfo s1, final ServerInfo s2) {
          return s1.getUri().toString().compareTo(s2.getUri().toString());
        }
      });
      return servers;
    }

    public Collection<?> getChildren(final @NotNull Object parent) {
      if (parent instanceof ServerInfo && myShowWorkspaces) {
        final List<WorkspaceInfo> workspaces = new ArrayList<WorkspaceInfo>(((ServerInfo)parent).getWorkspacesForCurrentOwnerAndComputer());

        Collections.sort(workspaces, new Comparator<WorkspaceInfo>() {
          public int compare(final WorkspaceInfo o1, final WorkspaceInfo o2) {
            return o1.getName().compareTo(o2.getName());
          }
        });
        return workspaces;
      }
      return Collections.emptyList();
    }
  };

  public ManageWorkspacesForm(final Project project) {
    myProject = project;

    myAddServerButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        addServer();
      }
    });

    myRemoveServerButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        //noinspection ConstantConditions
        removeServer(getSelectedServer());
      }
    });

    myCreateWorkspaceButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        //noinspection ConstantConditions
        ServerInfo server = getSelectedServer();
        if (server == null) {
          //noinspection ConstantConditions
          server = getSelectedWorkspace().getServer();
        }
        createWorkspace(server);
      }
    });

    myEditWorkspaceButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        //noinspection ConstantConditions
        editWorkspace(getSelectedWorkspace());
      }
    });

    myDeleteWorkspaceButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        //noinspection ConstantConditions
        deleteWorkspace(getSelectedWorkspace());
      }
    });
  }

  private void createUIComponents() {
    myTable = new CustomTreeTable<Object>(new CellRendererImpl(), false, true);
    configureTable();
  }

  public void setShowWorkspaces(final boolean showWorkspaces) {
    myShowWorkspaces = showWorkspaces;
    myWorkspacesPanel.setVisible(myShowWorkspaces);
    myTitleLabel.setText(myShowWorkspaces ? "Team servers and workspaces:" : "Team servers:");
    final List<TreeTableColumn<Object>> columns =
      myShowWorkspaces ? Arrays.asList(COLUMN_SERVER_WORKSPACE, COLUMN_COMMENT) : Collections.singletonList(COLUMN_SERVER);
    myTable.initialize(columns, myContentProvider);
    configureTable();
  }

  private void updateControls(WorkspaceInfo workspaceToSelect) {
    myTable.updateContent();
    configureTable();
    myTable.select(workspaceToSelect);
    //updateButtons();
  }

  private void configureTable() {
    myTable.expandAll();
    myTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myTable.getSelectionModel().addListSelectionListener(mySelectionListener);
    myTable.removeMouseListener(myMouseListener);
    myTable.addMouseListener(myMouseListener);
  }

  private void updateButtons() {
    final ServerInfo selectedServer = getSelectedServer();
    final WorkspaceInfo selectedWorkspace = getSelectedWorkspace();

    myRemoveServerButton.setEnabled(selectedServer != null);
    myCreateWorkspaceButton.setEnabled(selectedServer != null || selectedWorkspace != null);
    myEditWorkspaceButton.setEnabled(selectedWorkspace != null);
    myDeleteWorkspaceButton.setEnabled(selectedWorkspace != null);
  }

  private void addServer() {
    AuthenticationHelper.AuthenticationResult result = AuthenticationHelper.authenticate(null, true, true);
    if (result != null) {
      ServerInfo newServer = new ServerInfo(result.uri, result.serverGuid);
      Workstation.getInstance().addServer(newServer);
      try {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        newServer.refreshWorkspacesForCurrentOwner();
        updateControls(null);
      }
      catch (TfsException e) {
        String message = MessageFormat.format("Failed to reload workspaces.\n{0}", e.getMessage());
        Messages.showErrorDialog(myProject, message, "Add server");
      }
      finally {
        getContentPane().setCursor(Cursor.getDefaultCursor());
      }
      updateControls(null);
    }
  }

  private void removeServer(final @NotNull ServerInfo server) {
    String warning = MessageFormat.format("Are you sure you want to remove server ''{0}''?", server.getUri());
    if (Messages.showYesNoDialog(myContentPane, warning, "Remove Team Server", Messages.getWarningIcon()) == 0) {
      Workstation.getInstance().removeServer(server);
      updateControls(null);
    }
  }

  private void createWorkspace(final @NotNull ServerInfo server) {
    WorkspaceInfo newWorkspace = new WorkspaceInfo(server, server.getQualifiedUsername(), Workstation.getComputerName());
    WorkspaceDialog d = new WorkspaceDialog(myProject, newWorkspace);
    d.show();
    if (d.isOK()) {
      try {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        newWorkspace.saveToServer();
        updateControls(newWorkspace);
      }
      catch (TfsException e) {
        String message = MessageFormat.format("Failed to create workspace ''{0}''.\n{1}", newWorkspace.getName(), e.getMessage());
        Messages.showErrorDialog(myProject, message, "Create Workspace");
      }
      finally {
        getContentPane().setCursor(Cursor.getDefaultCursor());
      }
    }
  }

  private void editWorkspace(@NotNull WorkspaceInfo workspace) {
    try {
      getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      workspace.loadFromServer();
    }
    catch (WorkspaceNotFoundException e) {
      String message = MessageFormat.format("Failed to open workspace ''{0}'' for editing.\n{1}", workspace.getName(), e.getMessage());
      Messages.showErrorDialog(myProject, message, "Edit Workspace");
      try {
        workspace.getServer().refreshWorkspacesForCurrentOwner();
        updateControls(null);
      }
      catch (TfsException ex) {
        // skip
      }
      return;
    }
    catch (Exception e) {
      String message = MessageFormat.format("Failed to open workspace ''{0}'' for editing.\n{1}", workspace.getName(), e.getMessage());
      Messages.showErrorDialog(myProject, message, "Edit Workspace");
      return;
    }
    finally {
      getContentPane().setCursor(Cursor.getDefaultCursor());
    }

    WorkspaceInfo workspaceCopyToEdit = workspace.getCopy();
    WorkspaceDialog d = new WorkspaceDialog(myProject, workspaceCopyToEdit);
    d.show();
    if (d.isOK()) {
      try {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        workspace.getServer().replaceWorkspace(workspace, workspaceCopyToEdit);
        workspaceCopyToEdit.saveToServer();
        updateControls(workspaceCopyToEdit);
      }
      catch (Exception e) {
        String message = MessageFormat.format("Failed to save workspace ''{0}'' for editing.\n{1}", workspace.getName(), e.getMessage());
        Messages.showErrorDialog(myProject, message, "Edit Workspace");
      }
      finally {
        getContentPane().setCursor(Cursor.getDefaultCursor());
      }
    }
  }

  private void deleteWorkspace(@NotNull WorkspaceInfo workspace) {
    String warning = MessageFormat.format("Are you sure you want to delete workspace ''{0}''?", workspace.getName());
    if (Messages.showYesNoDialog(myContentPane, warning, "Delete Workspace", Messages.getWarningIcon()) == 0) {
      //noinspection ConstantConditions
      try {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        workspace.getServer().deleteWorkspace(workspace);
        updateControls(null);
      }
      catch (Exception e) {
        String message = MessageFormat.format("Failed to delete workspace ''{0}''.\n{1}", workspace.getName(), e.getMessage());
        Messages.showErrorDialog(myProject, message, "Delete Workspace");
      }
      finally {
        getContentPane().setCursor(Cursor.getDefaultCursor());
      }
    }
  }

  @Nullable
  private Object getSelectedObject() {
    if (myTable.getSelectedRowCount() == 1) {
      final Collection<Object> selection = myTable.getSelectedItems();
      if (selection.size() == 1) {
        return myTable.getSelectedItems().iterator().next();
      }
    }
    return null;
  }

  @Nullable
  public WorkspaceInfo getSelectedWorkspace() {
    Object selectedObject = getSelectedObject();
    if (selectedObject instanceof WorkspaceInfo) {
      return (WorkspaceInfo)selectedObject;
    }
    return null;
  }

  @Nullable
  public ServerInfo getSelectedServer() {
    Object selectedObject = getSelectedObject();
    if (selectedObject instanceof ServerInfo) {
      return (ServerInfo)selectedObject;
    }
    return null;
  }

  public void setSelectedWorkspace(WorkspaceInfo workspace) {
    myTable.select(workspace);
  }

  public void setSelectedServer(ServerInfo server) {
    myTable.select(server);
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
      listener.selectionChanged();
    }
  }

  private static class CellRendererImpl extends CellRenderer<Object> {
    protected void render(final CustomTreeTable<Object> treeTable,
                          final TreeTableColumn<Object> column,
                          final Object value,
                          final JLabel cell) {
      super.render(treeTable, column, value, cell);

      if (column == COLUMN_SERVER_WORKSPACE || column == COLUMN_SERVER) {
        if (value instanceof ServerInfo) {
          cell.setIcon(UiConstants.ICON_TEAM_SERVER);
        }
        else if (value instanceof WorkspaceInfo) {
          cell.setIcon(UiConstants.ICON_FILE);
        }
      }
    }
  }

}
