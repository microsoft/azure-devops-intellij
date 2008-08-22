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

package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.Nls;
import org.jetbrains.tfsIntegration.core.TFSProjectConfiguration;
import org.jetbrains.tfsIntegration.ui.UpdateSettingsForm;

import javax.swing.*;
import java.util.Collection;

public class UpdateConfigurable implements Configurable {

  private final Project myProject;

  private final Collection<FilePath> myFiles;

  private UpdateSettingsForm myUpdateSettingsForm;

  public UpdateConfigurable(Project project, final Collection<FilePath> files) {
    myProject = project;
    myFiles = files;
  }

  public String getHelpTopic() {
    return null; // TODO: help id
  }

  public void apply() throws ConfigurationException {
    myUpdateSettingsForm.apply(TFSProjectConfiguration.getInstance(myProject));
  }

  public void reset() {
    myUpdateSettingsForm.reset(TFSProjectConfiguration.getInstance(myProject));
  }

  @Nls
  public String getDisplayName() {
    return "Update Project";
  }

  public Icon getIcon() {
    return null;
  }

  public JComponent createComponent() {
    myUpdateSettingsForm = new UpdateSettingsForm(myProject, myFiles, getDisplayName());
    return myUpdateSettingsForm.getPanel();
  }

  public boolean isModified() {
    return false;
  }

  public void disposeUIResources() {
    myUpdateSettingsForm = null;
  }

}
