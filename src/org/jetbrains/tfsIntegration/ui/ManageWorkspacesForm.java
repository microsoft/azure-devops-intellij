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
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.util.EventDispatcher;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.checkin.*;
import org.jetbrains.tfsIntegration.core.TFSConstants;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.core.configuration.TfsCheckinPoliciesCompatibility;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.OperationFailedException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.UserCancelledException;
import org.jetbrains.tfsIntegration.exceptions.WorkspaceNotFoundException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Annotation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.ui.treetable.CellRenderer;
import org.jetbrains.tfsIntegration.ui.treetable.ContentProvider;
import org.jetbrains.tfsIntegration.ui.treetable.CustomTreeTable;
import org.jetbrains.tfsIntegration.ui.treetable.TreeTableColumn;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class ManageWorkspacesForm {

  public interface Listener extends EventListener {
    void selectionChanged();
  }

  public static class ProjectEntry {
    public final List<StatefulPolicyDescriptor> descriptors;
    public TfsCheckinPoliciesCompatibility policiesCompatibility;

    public ProjectEntry() {
      this(new ArrayList<StatefulPolicyDescriptor>());
    }

    public ProjectEntry(List<StatefulPolicyDescriptor> descriptors) {
      this(descriptors, TfsCheckinPoliciesCompatibility.NO_OVERRIDES);
    }

    public ProjectEntry(List<StatefulPolicyDescriptor> descriptors, TfsCheckinPoliciesCompatibility policiesCompatibility) {
      this.descriptors = descriptors;
      this.policiesCompatibility = policiesCompatibility;
    }
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
        if (server.getQualifiedUsername() != null) {
          return MessageFormat.format("{0} [{1}]", server.getUri().toString(), server.getQualifiedUsername());
        }
        else {
          return server.getUri().toString();
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
    updateButtons();
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

  private void updateControls(Object selectedServerOrWorkspace) {
    myTable.updateContent();
    configureTable();
    myTable.select(selectedServerOrWorkspace);
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
    myProxySettingsButton.setEnabled(selectedServer != null);
    myCreateWorkspaceButton.setEnabled(selectedServer != null || selectedWorkspace != null);
    myEditWorkspaceButton.setEnabled(selectedWorkspace != null);
    myDeleteWorkspaceButton.setEnabled(selectedWorkspace != null);
    myCheckInPoliciesButton.setEnabled(selectedServer != null);
  }

  private void addServer() {
    final Pair<URI, String> uriAndGuid;
    try {
      uriAndGuid = WebServiceHelper.authenticate(null);
    }
    catch (TfsException e) {
      // user cancelled
      return;
    }
    ServerInfo newServer = new ServerInfo(uriAndGuid.first, uriAndGuid.second);
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
    updateControls(newServer);
  }

  private void removeServer(final @NotNull ServerInfo server) {
    String warning = MessageFormat.format("Are you sure you want to remove server ''{0}''?", server.getUri());
    if (Messages.showYesNoDialog(myContentPane, warning, "Remove Team Server", Messages.getWarningIcon()) == 0) {
      Workstation.getInstance().removeServer(server);
      updateControls(null);
    }
  }

  private void changeProxySettings(final @NotNull ServerInfo server) {
    ProxySettingsDialog d = new ProxySettingsDialog(myProject, server.getUri());
    d.show();
    if (d.isOK()) {
      TFSConfigurationManager.getInstance().setProxyUri(server.getUri(), d.getProxyUri());
      //updateControls(server);
    }
  }

  private void createWorkspace(final @NotNull ServerInfo server) {
    if (server.getQualifiedUsername() == null) {
      try {
        WebServiceHelper.authenticate(server.getUri());
      }
      catch (UserCancelledException e) {
        return;
      }
      catch (TfsException e) {
        Messages.showErrorDialog(getContentPane(), e.getMessage(), "Create workspace");
        return;
      }
    }

    WorkspaceDialog d = new WorkspaceDialog(myProject, server);
    d.show();
    if (d.isOK()) {
      try {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        //noinspection ConstantConditions
        WorkspaceInfo newWorkspace = new WorkspaceInfo(server, server.getQualifiedUsername(), Workstation.getComputerName());
        newWorkspace.setName(d.getWorkspaceName());
        newWorkspace.setComment(d.getWorkspaceComment());
        newWorkspace.setWorkingFolders(d.getWorkingFolders());
        newWorkspace.saveToServer();
        updateControls(newWorkspace);
      }
      catch (TfsException e) {
        String message = MessageFormat.format("Failed to create workspace ''{0}''.\n{1}", d.getWorkspaceName(), e.getMessage());
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
    catch (TfsException e) {
      String message = MessageFormat.format("Failed to open workspace ''{0}'' for editing.\n{1}", workspace.getName(), e.getMessage());
      Messages.showErrorDialog(myProject, message, "Edit Workspace");
      return;
    }
    finally {
      getContentPane().setCursor(Cursor.getDefaultCursor());
    }

    WorkspaceInfo modifiedWorkspace = workspace.getCopy();
    WorkspaceDialog d = new WorkspaceDialog(myProject, modifiedWorkspace);
    d.show();
    if (d.isOK()) {
      modifiedWorkspace.setName(d.getWorkspaceName());
      modifiedWorkspace.setComment(d.getWorkspaceComment());
      modifiedWorkspace.setWorkingFolders(d.getWorkingFolders());
      try {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        modifiedWorkspace.saveToServer();
        workspace.getServer().replaceWorkspace(workspace, modifiedWorkspace);
        updateControls(modifiedWorkspace);
      }
      catch (TfsException e) {
        String message = MessageFormat.format("Failed to save workspace ''{0}''.\n{1}", workspace.getName(), e.getMessage());
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
      catch (TfsException e) {
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
        final Collection<Annotation> policiesAnnotations =
          server.getVCS().queryAnnotations(TFSConstants.STATEFUL_CHECKIN_POLICIES_ANNOTATION, Collections.<String>emptyList());
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
          server.getVCS().queryAnnotations(TFSConstants.OVERRRIDES_ANNOTATION, Collections.<String>emptyList());
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
            entry.policiesCompatibility = TfsCheckinPoliciesCompatibility.fromOverridesAnnotationValue(annotation.getValue());
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
        final List<Item> projectItems = server.getVCS().getChildItems(VersionControlPath.ROOT_FOLDER, true);
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
                server.getVCS().deleteAnnotation(i.getKey(), TFSConstants.STATEFUL_CHECKIN_POLICIES_ANNOTATION);
                server.getVCS().deleteAnnotation(i.getKey(), TFSConstants.OVERRRIDES_ANNOTATION);

                // write checkin policies annotation
                ProjectEntry entry = i.getValue();
                if (!entry.descriptors.isEmpty()) {
                  String annotationValue = StatefulPolicyParser.saveDescriptors(entry.descriptors);
                  server.getVCS().createAnnotation(i.getKey(), TFSConstants.STATEFUL_CHECKIN_POLICIES_ANNOTATION, annotationValue);
                }

                // write overrides annotation
                if (entry.policiesCompatibility.hasOverrides()) {
                  server.getVCS().createAnnotation(i.getKey(), TFSConstants.OVERRRIDES_ANNOTATION,
                                                   entry.policiesCompatibility.toOverridesAnnotationValue());
                }
              }
            }
          });
        saveResult.showDialogIfError("Save Checkin Policies");
      }
    }
  }

}
