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

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.ui.ProjectConfigurableForm;

import javax.swing.*;

public class TFSProjectConfigurable implements Configurable {

  private final Project myProject;
  private ProjectConfigurableForm myComponent;

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

  @NonNls
  public String getHelpTopic() {
    return "project.propVCSSupport.VCSs.TFS";
  }

  public JComponent createComponent() {
    myComponent = new ProjectConfigurableForm(myProject);
    return myComponent.getContentPane();
  }

  public boolean isModified() {
    if (TFSConfigurationManager.getInstance().useIdeaHttpProxy() != myComponent.useProxy()) return true;
    if (TFSConfigurationManager.getInstance().supportTfsCheckinPolicies() != myComponent.supportTfsCheckinPolicies()) return true;
    if (TFSConfigurationManager.getInstance().supportStatefulCheckinPolicies() != myComponent.supportStatefulCheckinPolicies()) {
      return true;
    }
    if (TFSConfigurationManager.getInstance().reportNotInstalledCheckinPolicies() != myComponent.reportNotInstalledCheckinPolicies()) {
      return true;
    }
    return false;
  }

  public void apply() throws ConfigurationException {
    TFSConfigurationManager.getInstance().setUseIdeaHttpProxy(myComponent.useProxy());
    TFSConfigurationManager.getInstance().setSupportTfsCheckinPolicies(myComponent.supportTfsCheckinPolicies());
    TFSConfigurationManager.getInstance().setSupportStatefulCheckinPolicies(myComponent.supportStatefulCheckinPolicies());
    TFSConfigurationManager.getInstance().setReportNotInstalledCheckinPolicies(myComponent.reportNotInstalledCheckinPolicies());
  }

  public void reset() {
    myComponent.setUserProxy(TFSConfigurationManager.getInstance().useIdeaHttpProxy());
    myComponent.setSupportTfsCheckinPolicies(TFSConfigurationManager.getInstance().supportTfsCheckinPolicies());
    myComponent.setSupportStatefulCheckinPolicies(TFSConfigurationManager.getInstance().supportStatefulCheckinPolicies());
    myComponent.setReportNotInstalledCheckinPolicies(TFSConfigurationManager.getInstance().reportNotInstalledCheckinPolicies());
  }

  public void disposeUIResources() {
    myComponent = null;
  }

}
