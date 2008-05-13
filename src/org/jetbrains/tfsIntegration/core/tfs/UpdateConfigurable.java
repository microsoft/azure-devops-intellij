package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;

import javax.swing.*;

import org.jetbrains.tfsIntegration.core.TFSProjectConfiguration;

public abstract class UpdateConfigurable implements Configurable {
    public UpdateConfigurable(Project project) {
        myProject = project;
    }

    private AbstractUpdatePanel myPanel;

    private final Project myProject;

    public String getHelpTopic() {
      return null; // TODO: help id
    }


    public void apply() throws ConfigurationException {
        myPanel.apply(TFSProjectConfiguration.getInstance(myProject)); 
    }

    public Icon getIcon() {
      return null;
    }

    public JComponent createComponent() {
      myPanel = createPanel();
      return myPanel.getPanel();
    }

    protected abstract AbstractUpdatePanel createPanel();

    public boolean isModified() {
      return false;
    }

    public void reset() {
      myPanel.reset(TFSProjectConfiguration.getInstance(myProject)); 
    }

    public void disposeUIResources() {
      myPanel = null;
    }

    protected Project getProject() {
      return myProject;
    }
}
