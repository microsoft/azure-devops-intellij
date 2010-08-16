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
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.util.EventDispatcher;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Annotation;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Item;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.checkin.*;
import org.jetbrains.tfsIntegration.config.TfsServerConnectionHelper;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.TFSConstants;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.core.configuration.TfsCheckinPoliciesCompatibility;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.OperationFailedException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.UserCancelledException;
import org.jetbrains.tfsIntegration.exceptions.WorkspaceNotFoundException;
import org.jetbrains.tfsIntegration.ui.treetable.CellRenderer;
import org.jetbrains.tfsIntegration.ui.treetable.ContentProvider;
import org.jetbrains.tfsIntegration.ui.treetable.CustomTreeTable;
import org.jetbrains.tfsIntegration.ui.treetable.TreeTableColumn;
import org.jetbrains.tfsIntegration.webservice.TfsRequestManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

public class ManageWorkspacesForm {

  public interface Listener extends EventListener {
    void selectionChanged();
  }

  public static class ProjectEntry {
    public final List<StatefulPolicyDescriptor> descriptors;
    public @Nullable TfsCheckinPoliciesCompatibility policiesCompatibilityOverride;

    public ProjectEntry() {
      this(new ArrayList<StatefulPolicyDescriptor>());
    }

    public ProjectEntry(List<StatefulPolicyDescriptor> descriptors) {
      this(descriptors, null);
    }

    public ProjectEntry(List<StatefulPolicyDescriptor> descriptors,
                        @Nullable TfsCheckinPoliciesCompatibility policiesCompatibilityOverride) {
      this.descriptors = descriptors;
      this.policiesCompatibilityOverride = policiesCompatibilityOverride;
    }
  }

  private static final TreeTableColumn<Object> COLUMN_SERVER_WORKSPACE = new TreeTableColumn<Object>("Server / workspace", 200) {
    public String getPresentableString(final Object value) {
      if (value instanceof ServerInfo) {
        final ServerInfo server = (ServerInfo)value;
        if (server.getQualifiedUsername() != null) {
          return MessageFormat.format("{0} [{1}]", server.getPresentableUri(), server.getQualifiedUsername());
        }
        else {
          return server.getPresentableUri();
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
        if (server.getQualifiedUsername() != null) {
          return MessageFormat.format("{0} [{1}]", server.getPresentableUri(), server.getQualifiedUsername());
        }
        else {
          return server.getPresentableUri();
        }
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
  private JButton myRemoveServerButton;
  private JButton myProxySettingsButton;
  private JButton myCreateWorkspaceButton;
  private JButton myEditWorkspaceButton;
  private JButton myDeleteWorkspaceButton;
  private CustomTreeTable<Object> myTable;
  private JLabel myTitleLabel;
  private JPanel myWorkspacesPanel;
  private JButton myCheckInPoliciesButton;
  private JButton myReloadWorkspacesButton;
  private final Project myProject;
  private boolean myShowWorkspaces = true;
  private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);

  private final ListSelectionListener mySelectionListener = new ListSelectionListener() {
    public void valueChanged(final ListSelectionEvent e) {
      updateButtons();
      myEventDispatcher.getMulticaster().selectionChanged();
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


  private final ContentProvider<Object> myContentProvider = new ContentProvider<Object>() {

    public Collection<?> getRoots() {
      final List<ServerInfo> servers = new ArrayList<ServerInfo>(Workstation.getInstance().getServers());
      Collections.sort(servers, new Comparator<ServerInfo>() {
        public int compare(final ServerInfo s1, final ServerInfo s2) {
          return s1.getPresentableUri().compareTo(s2.getPresentableUri());
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

  public ManageWorkspacesForm(final Project project, boolean editPoliciesButtonVisible) {
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

    myProxySettingsButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        //noinspection ConstantConditions
        changeProxySettings(getSelectedServer());
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

    myCheckInPoliciesButton.setVisible(editPoliciesButtonVisible);

    myCheckInPoliciesButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        configureCheckinPolicies();
      }
    });

    myReloadWorkspacesButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        reloadWorkspaces(getSelectedServer());
      }
    });
    updateButtons();
  }

  private void reloadWorkspaces(ServerInfo server) {
    try {
      Object selection = getSelectedObject();
      server.refreshWorkspacesForCurrentOwner(myContentPane, true);
      updateControls(selection);
    }
    catch (UserCancelledException e) {
      // ignore
    }
    catch (TfsException e) {
      Messages.showErrorDialog(myContentPane, e.getMessage(), TFSBundle.message("reload.workspaces.title"));
    }
  }

  private void createUIComponents() {
    myTable = new CustomTreeTable<Object>(new CellRendererImpl(), false, true);
    configureTable();
  }

  public void setShowWorkspaces(final boolean showWorkspaces) {
    myShowWorkspaces = showWorkspaces;
    myWorkspacesPanel.setVisible(myShowWorkspaces);
    myReloadWorkspacesButton.setVisible(myShowWorkspaces);
    myTitleLabel.setText(myShowWorkspaces ? "Team servers and workspaces:" : "Team servers:");
    final List<TreeTableColumn<Object>> columns =
      myShowWorkspaces ? Arrays.asList(COLUMN_SERVER_WORKSPACE, COLUMN_COMMENT) : Collections.singletonList(COLUMN_SERVER);
    myTable.initialize(columns, myContentProvider);
    configureTable();
  }

  private void updateControls(Object selectedServerOrWorkspace) {
    myTable.updateContent();
    configureTable();
    Object newSelection = null;
    for (int i = 0; i < myTable.getModel().getRowCount(); i++) {
      Object o = ((DefaultMutableTreeNode)myTable.getModel().getValueAt(i, 0)).getUserObject();
      if (Comparing.equal(o, selectedServerOrWorkspace)) {
        newSelection = o;
        break;
      }
      if (selectedServerOrWorkspace instanceof WorkspaceInfo) {
        WorkspaceInfo selectedWorkspace = (WorkspaceInfo)selectedServerOrWorkspace;
        if (selectedWorkspace.getServer().equals(o)) {
          newSelection = o;
        }
        if (o instanceof WorkspaceInfo) {
          WorkspaceInfo workspace = (WorkspaceInfo)o;
          if (selectedWorkspace.getName().equals(workspace.getName()) &&
              selectedWorkspace.getOwnerName().equals(workspace.getOwnerName()) &&
              selectedWorkspace.getServer().equals(workspace.getServer())) {
            newSelection = o;
            break;
          }
        }
      }
    }
    myTable.select(newSelection);
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
    myProxySettingsButton.setEnabled(selectedServer != null);
    myCreateWorkspaceButton.setEnabled(selectedServer != null || selectedWorkspace != null);
    myEditWorkspaceButton.setEnabled(selectedWorkspace != null);
    myDeleteWorkspaceButton.setEnabled(selectedWorkspace != null);
    myCheckInPoliciesButton.setEnabled(selectedServer != null);
    myReloadWorkspacesButton.setEnabled(selectedServer != null);
  }

  private void addServer() {
    final TfsServerConnectionHelper.AddServerResult result = TfsServerConnectionHelper.addServer(getContentPane());
    if (result == null) {
      // user canceled
      return;
    }

    if (result.workspacesLoadError != null) {
      Messages.showErrorDialog(myContentPane, TFSBundle.message("failed.to.load.workspaces", result.workspacesLoadError),
                               TFSBundle.message("add.server.title"));
    }

    TFSConfigurationManager.getInstance().storeCredentials(result.uri, result.authorizedCredentials);
    final ServerInfo newServer =
      new ServerInfo(result.uri, result.instanceId, result.workspaces, result.authorizedCredentials.getQualifiedUsername(),
                     result.beans);
    Workstation.getInstance().addServer(newServer);
    List<WorkspaceInfo> workspaces = newServer.getWorkspacesForCurrentOwnerAndComputer();
    updateControls(workspaces.isEmpty() ? newServer : workspaces.iterator().next());
  }

  private void removeServer(final @NotNull ServerInfo server) {
    String warning = TFSBundle.message("remove.server.prompt", server.getPresentableUri());
    if (Messages.showYesNoDialog(myContentPane, warning, TFSBundle.message("remove.server.title"), Messages.getWarningIcon()) == 0) {
      Workstation.getInstance().removeServer(server);
      updateControls(null);
    }
  }

  private void changeProxySettings(final @NotNull ServerInfo server) {
    ProxySettingsDialog d = new ProxySettingsDialog(myProject, server.getUri());
    d.show();
    if (d.isOK()) {
      TFSConfigurationManager.getInstance().setProxyUri(server.getUri(), d.getProxyUri());
    }
  }

  private void createWorkspace(final @NotNull ServerInfo server) {
    boolean update = false;
    if (TfsRequestManager.shouldShowLoginDialog(server.getUri())) {
      update = true;
      try {
        TfsServerConnectionHelper.ensureAuthenticated(myContentPane, server.getUri(), true);
      }
      catch (UserCancelledException e) {
        return;
      }
      catch (TfsException e) {
        Messages.showErrorDialog(getContentPane(), e.getMessage(), TFSBundle.message("create.workspace"));
        return;
      }
    }

    WorkspaceDialog d = new WorkspaceDialog(myProject, server);
    d.show();
    if (d.isOK()) {
      try {
        //noinspection ConstantConditions
        WorkspaceInfo newWorkspace = new WorkspaceInfo(server, server.getQualifiedUsername(), Workstation.getComputerName());
        newWorkspace.setName(d.getWorkspaceName());
        newWorkspace.setComment(d.getWorkspaceComment());
        newWorkspace.setWorkingFolders(d.getWorkingFolders());
        newWorkspace.saveToServer(myContentPane);
        updateControls(newWorkspace);
        return;
      }
      catch (UserCancelledException e) {
        // ignore to execute updateControls()
      }
      catch (TfsException e) {
        Messages.showErrorDialog(myProject, e.getMessage(), TFSBundle.message("create.workspace.title"));
      }
    }
    if (update) {
      updateControls(null);
    }
  }

  private void editWorkspace(@NotNull WorkspaceInfo workspace) {
    try {
      workspace.loadFromServer(myContentPane, true);
    }
    catch (WorkspaceNotFoundException e) {
      Messages.showErrorDialog(myProject, e.getMessage(), TFSBundle.message("edit.workspace.title"));
      try {
        workspace.getServer().refreshWorkspacesForCurrentOwner(myContentPane, true);
        updateControls(null);
      }
      catch (UserCancelledException e2) {
        // ignore
      }
      catch (TfsException e2) {
        Messages.showErrorDialog(myContentPane, e2.getMessage(), TFSBundle.message("reload.workspaces.title"));
      }
      return;
    }
    catch (UserCancelledException e) {
      return;
    }
    catch (TfsException e) {
      Messages.showErrorDialog(myProject, e.getMessage(), TFSBundle.message("edit.workspace.title"));
      return;
    }

    WorkspaceInfo modifiedWorkspace = workspace.getCopy();
    WorkspaceDialog d = new WorkspaceDialog(myProject, modifiedWorkspace);
    d.show();
    if (d.isOK()) {
      modifiedWorkspace.setName(d.getWorkspaceName());
      modifiedWorkspace.setComment(d.getWorkspaceComment());
      modifiedWorkspace.setWorkingFolders(d.getWorkingFolders());
      try {
        // replace old workspace with a new one first, otherwise server will hold and old one while writing to cache
        workspace.getServer().replaceWorkspace(workspace, modifiedWorkspace);
        modifiedWorkspace.saveToServer(myContentPane);
        updateControls(modifiedWorkspace);
      }
      catch (UserCancelledException e) {
        // ignore
      }
      catch (TfsException e) {
        Messages.showErrorDialog(myProject, e.getMessage(), TFSBundle.message("save.workspace.title"));
      }
    }
  }

  private void deleteWorkspace(@NotNull WorkspaceInfo workspace) {
    if (Messages.showYesNoDialog(myContentPane, TFSBundle.message("delete.workspace.prompt", workspace.getName()),
                                 TFSBundle.message("delete.workspace.title"), Messages.getWarningIcon()) != 0) {
      return;
    }

    try {
      workspace.getServer().deleteWorkspace(workspace, myContentPane, true);
      updateControls(workspace);
    }
    catch (UserCancelledException e) {
      // ignore
    }
    catch (TfsException e) {
      Messages.showErrorDialog(myProject, e.getMessage(), TFSBundle.message("delete.workspace.title"));
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
    if (selectedObject instanceof WorkspaceInfo) {
      return ((WorkspaceInfo)selectedObject).getServer();
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
    myEventDispatcher.addListener(listener);
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

  private void configureCheckinPolicies() {
    try {
      CheckinPoliciesManager.getInstalledPolicies();
    }
    catch (DuplicatePolicyIdException e) {
      final String message = MessageFormat
        .format("Several checkin policies with the same id found: ''{0}''.\nPlease review your extensions.", e.getDuplicateId());
      Messages.showErrorDialog(myProject, message, "Edit Checkin Policies");
      return;
    }

    @SuppressWarnings({"ConstantConditions"}) @NotNull final ServerInfo server = getSelectedServer();

    final TfsExecutionUtil.Process<Map<String, ProjectEntry>> process = new TfsExecutionUtil.Process<Map<String, ProjectEntry>>() {
      public Map<String, ProjectEntry> run() throws TfsException, VcsException {
        Map<String, ProjectEntry> entries = new HashMap<String, ProjectEntry>();

        // load policies
        final Collection<Annotation> policiesAnnotations = server.getVCS()
          .queryAnnotations(TFSConstants.STATEFUL_CHECKIN_POLICIES_ANNOTATION, Collections.<String>emptyList(), myContentPane, null,
                            true);
        for (Annotation annotation : policiesAnnotations) {
          if (annotation.getValue() == null) {
            continue;
          }
          try {
            List<StatefulPolicyDescriptor> descriptors = StatefulPolicyParser.parseDescriptors(annotation.getValue());
            entries.put(annotation.getItem(), new ProjectEntry(descriptors));
          }
          catch (PolicyParseException ex) {
            String message = MessageFormat.format("Cannot load checkin policies definitions:\n{0}", ex.getMessage());
            throw new OperationFailedException(message);
          }
        }

        // load overrides
        final Collection<Annotation> overridesAnnotations =
          server.getVCS().queryAnnotations(TFSConstants.OVERRRIDES_ANNOTATION, Collections.<String>emptyList(), myContentPane, null, true);
        for (Annotation annotation : overridesAnnotations) {
          if (annotation.getValue() == null) {
            continue;
          }

          try {
            ProjectEntry entry = entries.get(annotation.getItem());
            if (entry == null) {
              entry = new ProjectEntry();
              entries.put(annotation.getItem(), entry);
            }
            entry.policiesCompatibilityOverride = TfsCheckinPoliciesCompatibility.fromOverridesAnnotationValue(annotation.getValue());
          }
          catch (IOException ex) {
            String message = MessageFormat.format("Cannot load checkin policies overrides:\n{0}", ex.getMessage());
            throw new OperationFailedException(message);
          }
          catch (JDOMException ex) {
            String message = MessageFormat.format("Cannot load checkin policies overrides:\n{0}", ex.getMessage());
            throw new OperationFailedException(message);
          }
        }

        // load projects
        final List<Item> projectItems = server.getVCS().getChildItems(VersionControlPath.ROOT_FOLDER, true, myContentPane, null);
        if (projectItems.isEmpty()) {
          throw new OperationFailedException("No team project found");
        }

        for (Item projectItem : projectItems) {
          if (!entries.containsKey(projectItem.getItem())) {
            entries.put(projectItem.getItem(), new ProjectEntry());
          }
        }
        return entries;
      }
    };

    final TfsExecutionUtil.ResultWithError<Map<String, ProjectEntry>> loadResult =
      TfsExecutionUtil.executeInBackground("Loading Checkin Policies", myProject, process);
    if (loadResult.cancelled || loadResult.showDialogIfError("Configure Checkin Policies")) {
      return;
    }

    final Map<String, ProjectEntry> projectToDescriptors = loadResult.result;
    final CheckInPoliciesDialog d = new CheckInPoliciesDialog(myProject, getSelectedServer(), projectToDescriptors);
    d.show();
    if (d.isOK()) {
      final Map<String, ProjectEntry> modifications = d.getModifications();
      if (!modifications.isEmpty()) {
        final TfsExecutionUtil.ResultWithError<Void> saveResult =
          TfsExecutionUtil.executeInBackground("Saving Checkin Policies", myProject, new TfsExecutionUtil.VoidProcess() {
            public void run() throws TfsException, VcsException {
              for (Map.Entry<String, ProjectEntry> i : modifications.entrySet()) {
                // remove annotations
                server.getVCS().deleteAnnotation(i.getKey(), TFSConstants.STATEFUL_CHECKIN_POLICIES_ANNOTATION, myContentPane, null);
                server.getVCS().deleteAnnotation(i.getKey(), TFSConstants.OVERRRIDES_ANNOTATION, myContentPane, null);

                // write checkin policies annotation
                ProjectEntry entry = i.getValue();
                if (!entry.descriptors.isEmpty()) {
                  String annotationValue = StatefulPolicyParser.saveDescriptors(entry.descriptors);
                  server.getVCS()
                    .createAnnotation(i.getKey(), TFSConstants.STATEFUL_CHECKIN_POLICIES_ANNOTATION, annotationValue, myContentPane, null);
                }

                // write overrides annotation
                if (entry.policiesCompatibilityOverride != null) {
                  server.getVCS().createAnnotation(i.getKey(), TFSConstants.OVERRRIDES_ANNOTATION,
                                                   entry.policiesCompatibilityOverride.toOverridesAnnotationValue(), myContentPane, null);
                }
              }
            }
          });
        saveResult.showDialogIfError("Save Checkin Policies");
      }
    }
  }

  public JComponent getPreferredFocusedComponent() {
    return myTable;
  }

}
