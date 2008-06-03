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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.UpdateWorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.LatestVersionSpec;

import java.util.Map;
import java.util.HashMap;

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

  @Nullable
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

    public boolean UPDATE_RECURSIVELY = true;

    private final Map<WorkspaceInfo, UpdateWorkspaceInfo> myUpdateWorkspaceInfos = new HashMap<WorkspaceInfo, UpdateWorkspaceInfo>();

    public UpdateWorkspaceInfo getUpdateWorkspaceInfo(WorkspaceInfo workspace) {
      UpdateWorkspaceInfo info = myUpdateWorkspaceInfos.get(workspace);
      if (info == null) {
        info = new UpdateWorkspaceInfo(LatestVersionSpec.INSTANCE);
        myUpdateWorkspaceInfos.put(workspace, info);
      }
      return info;
    }
}
