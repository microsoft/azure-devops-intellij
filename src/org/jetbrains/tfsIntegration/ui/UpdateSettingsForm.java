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
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.tfsIntegration.core.TFSProjectConfiguration;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.WorkstationHelper;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class UpdateSettingsForm {
  @NonNls private static final String EMPTY = "empty";

  private final Map<WorkspaceInfo, SelectRevisionForm> myWorkspaceRevisions = new HashMap<WorkspaceInfo, SelectRevisionForm>();

  private JPanel myPanel;
  private JCheckBox myRecursiveBox;
  private JList myWorkspacesList;
  private JPanel myWorkspaceSettingsPanel;

  public UpdateSettingsForm(final Project project, Collection<FilePath> roots) {
    final DefaultListModel listModel = new DefaultListModel();
    try {
      WorkstationHelper.processByWorkspaces(roots, new WorkstationHelper.VoidProcessDelegate() {
        public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
          listModel.addElement(workspace);

          Collection<String> serverPaths = new ArrayList<String>(paths.size());
          for (ItemPath path : paths) {
            serverPaths.add(path.getServerPath());
          }
          SelectRevisionForm selectRevisionForm = new SelectRevisionForm(workspace, project, serverPaths);
          myWorkspaceRevisions.put(workspace, selectRevisionForm);
          myWorkspaceSettingsPanel.add(selectRevisionForm.getPanel(), workspace.getName());
        }
      });
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      AbstractVcsHelper.getInstance(project).showError(new VcsException(e), TFSVcs.TFS_NAME);
    }

    final CardLayout layout = (CardLayout)myWorkspaceSettingsPanel.getLayout();
    myWorkspaceSettingsPanel.add(new JPanel(), EMPTY);

    myWorkspacesList.setModel(listModel);
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
        WorkspaceInfo workspace = ((WorkspaceInfo)myWorkspacesList.getSelectedValue());
        layout.show(myWorkspaceSettingsPanel, workspace != null ? workspace.getName() : EMPTY);
      }
    });

    myWorkspacesList.setSelectedIndex(0);
    myWorkspacesList.requestFocus();
  }


  public void reset(final TFSProjectConfiguration configuration) {
    myRecursiveBox.setSelected(configuration.UPDATE_RECURSIVELY);

    for (Map.Entry<WorkspaceInfo, SelectRevisionForm> e : myWorkspaceRevisions.entrySet()) {
      e.getValue().setVersionSpec(configuration.getUpdateWorkspaceInfo(e.getKey()).getVersion());
    }
  }

  public void apply(final TFSProjectConfiguration configuration) throws ConfigurationException {
    configuration.UPDATE_RECURSIVELY = myRecursiveBox.isSelected();

    for (Map.Entry<WorkspaceInfo, SelectRevisionForm> e : myWorkspaceRevisions.entrySet()) {
      final VersionSpecBase version = e.getValue().getVersionSpec();
      if (version != null) {
        configuration.getUpdateWorkspaceInfo(e.getKey()).setVersion(version);
      }
      else {
        throw new ConfigurationException("Invalid version");
      }
    }
  }

  public JComponent getPanel() {
    return myPanel;
  }

}
