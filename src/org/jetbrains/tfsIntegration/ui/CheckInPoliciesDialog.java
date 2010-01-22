/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.Map;

public class CheckInPoliciesDialog extends DialogWrapper {
  private final Project myProject;
  private final Map<String, ManageWorkspacesForm.ProjectEntry> myProjectToDescriptors;
  private CheckInPoliciesForm myForm;

  public CheckInPoliciesDialog(final Project project,
                               ServerInfo server,
                               Map<String, ManageWorkspacesForm.ProjectEntry> projectToDescriptors) {
    super(project, false);
    myProject = project;
    myProjectToDescriptors = projectToDescriptors;
    String title = MessageFormat.format("{0}: Edit Checkin Policies", server.getUri());
    setTitle(title);
    init();
    setSize(800, 500);
  }

  protected JComponent createCenterPanel() {
    myForm = new CheckInPoliciesForm(myProject, myProjectToDescriptors);
    return myForm.getContentPane();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.ConfigureCheckInPolicies";
  }

  public Map<String, ManageWorkspacesForm.ProjectEntry> getModifications() {
    return myForm.getModifications();
  }

  @Override
  protected String getHelpId() {
    return "project.propVCSSupport.VCSs.TFS.edit.checkin.policies";
  }

}