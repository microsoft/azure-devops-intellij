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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.ui.CollectionListModel;
import org.jetbrains.tfsIntegration.core.TFSProjectConfiguration;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkstationHelper;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class UpdateSettingsForm {

  private static class WorkspaceSettings {
    public final String serverPath;
    public final boolean isDirectory;
    public VersionSpecBase version = LatestVersionSpec.INSTANCE;

    public WorkspaceSettings(final String serverPath, final boolean isDirectory) {
      this.serverPath = serverPath;
      this.isDirectory = isDirectory;
    }
  }

  private final Map<WorkspaceInfo, WorkspaceSettings> myWorkspaceSettings = new HashMap<WorkspaceInfo, WorkspaceSettings>();

  private JPanel myPanel;
  private JCheckBox myRecursiveBox;
  private JList myWorkspacesList;
  private JPanel myWorkspaceSettingsPanel;
  private SelectRevisionForm mySelectRevisionForm;
  private WorkspaceInfo mySelectedWorkspace;
  private TfsException myErrorOnInitialization;

  public UpdateSettingsForm(final Project project, Collection<FilePath> roots, final String title) {
    final List<WorkspaceInfo> workspaces = new ArrayList<WorkspaceInfo>();
    try {
      WorkstationHelper.processByWorkspaces(roots, true, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          final Map<FilePath, ExtendedItem> result = workspace.getExtendedItems2(paths);
          Collection<ExtendedItem> items = new ArrayList<ExtendedItem>(result.values());
          for (Iterator<ExtendedItem> i = items.iterator(); i.hasNext();) {
            if (i.next() == null) {
              i.remove();
            }
          }

          if (items.isEmpty()) {
            return;
          }

          workspaces.add(workspace);

          // determine common ancestor of all the paths
          ExtendedItem someExtendedItem = items.iterator().next();
          WorkspaceSettings workspaceSettings =
            new WorkspaceSettings(someExtendedItem.getSitem(), someExtendedItem.getType() == ItemType.Folder);
          for (ExtendedItem extendedItem : items) {
            final String path1 = workspaceSettings.serverPath;
            final String path2 = extendedItem.getSitem();
            if (VersionControlPath.isUnder(path2, path1)) {
              workspaceSettings = new WorkspaceSettings(path2, extendedItem.getType() == ItemType.Folder);
            }
            else if (!VersionControlPath.isUnder(path1, path2)) {
              workspaceSettings = new WorkspaceSettings(VersionControlPath.getCommonAncestor(path1, path2), true);
            }
          }
          myWorkspaceSettings.put(workspace, workspaceSettings);
        }
      });
      myErrorOnInitialization = null;
    }
    catch (TfsException e) {
      myErrorOnInitialization = e;
    }

    if (myErrorOnInitialization != null) {
      mySelectRevisionForm.disable(); // in case of list model will stay empty because of error is thrown while enumerating workspaces
    }

    Collections.sort(workspaces, new Comparator<WorkspaceInfo>() {
      public int compare(final WorkspaceInfo o1, final WorkspaceInfo o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });

    myWorkspacesList.setModel(new CollectionListModel(workspaces));
    myWorkspacesList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    myWorkspacesList.setCellRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(final JList list,
                                                    final Object value,
                                                    final int index,
                                                    final boolean isSelected,
                                                    final boolean cellHasFocus) {
        final Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        final WorkspaceInfo workspace = ((WorkspaceInfo)value);
        String label = MessageFormat.format("{0} [{1}]", workspace.getName(), workspace.getServer().getUri());
        setText(label);
        return c;
      }
    });

    myWorkspacesList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        try {
          applyCurrentValue();
        }
        catch (ConfigurationException ex) {
          Messages.showErrorDialog(project, ex.getMessage(), title);
        }

        mySelectedWorkspace = ((WorkspaceInfo)myWorkspacesList.getSelectedValue());
        if (mySelectedWorkspace != null) {
          final WorkspaceSettings workspaceSettings = myWorkspaceSettings.get(mySelectedWorkspace);
          mySelectRevisionForm.init(project, mySelectedWorkspace, workspaceSettings.serverPath, workspaceSettings.isDirectory);
          mySelectRevisionForm.setVersionSpec(workspaceSettings.version);
        }
        else {
          mySelectRevisionForm.disable();
        }
      }
    });

    if (workspaces.isEmpty()) {
      mySelectRevisionForm.disable();
    }
    else {
      myWorkspacesList.setSelectedIndex(0);
      myWorkspacesList.requestFocus();
    }
  }

  private void applyCurrentValue() throws ConfigurationException {
    if (myErrorOnInitialization != null) {
      throw new ConfigurationException(myErrorOnInitialization.getMessage());
    }

    if (mySelectedWorkspace != null) {
      VersionSpecBase version = mySelectRevisionForm.getVersionSpec();
      if (version != null) {
        myWorkspaceSettings.get(mySelectedWorkspace).version = version;
      }
      else {
        throw new ConfigurationException("Invalid version specified");
      }
    }
  }


  public void reset(final TFSProjectConfiguration configuration) {
    myRecursiveBox.setSelected(configuration.UPDATE_RECURSIVELY);

    for (Map.Entry<WorkspaceInfo, WorkspaceSettings> e : myWorkspaceSettings.entrySet()) {
      e.getValue().version = configuration.getUpdateWorkspaceInfo(e.getKey()).getVersion();
    }
  }

  public void apply(final TFSProjectConfiguration configuration) throws ConfigurationException {
    applyCurrentValue();
    configuration.UPDATE_RECURSIVELY = myRecursiveBox.isSelected();

    for (Map.Entry<WorkspaceInfo, WorkspaceSettings> e : myWorkspaceSettings.entrySet()) {
      configuration.getUpdateWorkspaceInfo(e.getKey()).setVersion(e.getValue().version);
    }
  }

  public JComponent getPanel() {
    return myPanel;
  }


}
