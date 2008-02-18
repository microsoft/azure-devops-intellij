package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
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
    if (myProject.isDefault()) {
      return false;
    }
    return false; 
  }

  public void apply() throws ConfigurationException {
  }

  public void reset() {
  }

  public void disposeUIResources() {
    myComponent = null;
  }

}
