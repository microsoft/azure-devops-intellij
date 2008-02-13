package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.ui.WorkspacesDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TFSProjectConfigurable implements Configurable {

  private static class WorkspaceInfoComboItem {

    private final WorkspaceInfo myWorkspaceInfo;

    private WorkspaceInfoComboItem(final WorkspaceInfo workspaceInfo) {
      myWorkspaceInfo = workspaceInfo;
    }

    public WorkspaceInfo getWorkspaceInfo() {
      return myWorkspaceInfo;
    }

    public String toString() {
      return getWorkspaceInfo().getName();
    }
  }

  private Project myProject;
  private JComponent myComponent;
  private JComboBox myWorkspaceCombo;

  public TFSProjectConfigurable(Project project) {
    myProject = project;
  }

  @Nullable
  @Nls
  public String getDisplayName() {
    return null;
  }

  @Nullable
  public Icon getIcon() {
    return null;
  }

  @Nullable
  @NonNls
  public String getHelpTopic() {
    return null;
  }

  public JComponent createComponent() {
    myComponent = new JPanel(new GridBagLayout());

    GridBagConstraints gc = new GridBagConstraints();

    gc.gridx = 0;
    gc.gridy = 0;
    gc.anchor = GridBagConstraints.WEST;

    JLabel comboLabel = new JLabel(TFSBundle.message("projectconfigurable.label.workspace"));
    comboLabel.setDisplayedMnemonic('W');
    myComponent.add(comboLabel, gc);

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 0;
    gc.weightx = 1;
    gc.fill = GridBagConstraints.HORIZONTAL;

    myWorkspaceCombo = new JComboBox();
    comboLabel.setLabelFor(myWorkspaceCombo);

    myComponent.add(myWorkspaceCombo, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 1;
    gc.insets = new Insets(20, 0, 0, 0);
    gc.anchor = GridBagConstraints.NORTHWEST;
    JButton globalSettingsButton = new JButton("Global settings...");
    myComponent.add(globalSettingsButton, gc);

    globalSettingsButton.addActionListener(new ActionListener() {

      public void actionPerformed(final ActionEvent e) {
        WorkspacesDialog d = new WorkspacesDialog(WorkspacesDialog.Mode.Manage);
        d.show();
        resetCombo(getSelectedWorkspaceName());
      }
    });

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 1;
    gc.gridwidth = 3;
    gc.weightx = 2;
    gc.weighty = 1;
    gc.fill = GridBagConstraints.BOTH;

    myComponent.add(new JPanel(), gc);

    return myComponent;
  }

  public boolean isModified() {
    String projectWorkspaceName = getProjectWorkspaceName();
    if (projectWorkspaceName != null) {
      return projectWorkspaceName.equals(getSelectedWorkspaceName());
    }
    else {
      return getSelectedWorkspaceName() == null;
    }
  }

  @Nullable
  private String getProjectWorkspaceName() {
    if (myProject.isDefault()) {
      return null;
    }
    else {
      return TFSProjectConfiguration.getInstance(myProject).getWorkspace();
    }
  }

  @Nullable
  private String getSelectedWorkspaceName() {
    WorkspaceInfoComboItem selectedItem = (WorkspaceInfoComboItem)myWorkspaceCombo.getSelectedItem();
    return selectedItem != null ? selectedItem.getWorkspaceInfo().getName() : null;
  }

  public void apply() throws ConfigurationException {
    if (!myProject.isDefault()) {
      TFSProjectConfiguration.getInstance(myProject).setWorkspace(getSelectedWorkspaceName());
    }
  }

  public void reset() {
    resetCombo(getProjectWorkspaceName());
  }

  private void resetCombo(String workspaceNameToSelect) {
    myWorkspaceCombo.removeAllItems();

    WorkspaceInfoComboItem itemToSelect = null;
    for (WorkspaceInfo workspaceInfo : Workstation.getInstance().getAllWorkspaces()) {
      WorkspaceInfoComboItem item = new WorkspaceInfoComboItem(workspaceInfo);
      if (workspaceInfo.getName().equals(workspaceNameToSelect)) {
        itemToSelect = item;
      }
      myWorkspaceCombo.addItem(item);
    }
    myWorkspaceCombo.setSelectedItem(itemToSelect);
    myWorkspaceCombo.setEnabled(!myProject.isDefault());
  }

  public void disposeUIResources() {
    myComponent = null;
  }

}
