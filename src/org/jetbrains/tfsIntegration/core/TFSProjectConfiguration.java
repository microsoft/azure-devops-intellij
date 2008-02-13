package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NonNls;

@State(
  name = "TFS",
  storages = {
    @Storage(
      id ="other",
      file = "$WORKSPACE_FILE$"
    )
    }
)
public class TFSProjectConfiguration implements ProjectComponent, PersistentStateComponent<TFSProjectConfiguration.ConfigurationBean> {

  @NonNls public static final String COMPONENT_NAME = "TFSProjectConfiguration";

  private Project myProject;

  private ConfigurationBean myConfigurationBean;

  public static class ConfigurationBean {
    public String workspace = "";
  }

  private TFSProjectConfiguration() {
    myConfigurationBean = new ConfigurationBean();
  }

  public TFSProjectConfiguration(final Project project) {
    this();
    myProject = project;
  }

  @NotNull
  public String getComponentName() {
    return COMPONENT_NAME;
  }

  public static TFSProjectConfiguration getInstance(Project project) {
    return project.getComponent(TFSProjectConfiguration.class);
  }

  public void initComponent() {
  }

  public void disposeComponent() {
  }

  public void projectOpened() {
  }

  public void projectClosed() {
  }

  public ConfigurationBean getState() {
    return myConfigurationBean;
  }

  public void loadState(ConfigurationBean state) {
    myConfigurationBean = state;
  }

  public String getWorkspace() {
    return myConfigurationBean.workspace;
  }

  public void setWorkspace(String workspace) {
    myConfigurationBean.workspace = workspace;
  }

}
