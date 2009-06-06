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
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.tfsIntegration.checkin.PolicyFailure;

import javax.swing.*;
import java.util.List;

public class OverridePolicyWarningsDialog extends DialogWrapper {
  private OverridePolicyWarningsForm myForm;
  private final Project myProject;
  private final List<PolicyFailure> myFailures;

  public OverridePolicyWarningsDialog(final Project project, List<PolicyFailure> failures) {
    super(project, false);
    myProject = project;
    myFailures = failures;
    setTitle("Check In: Policy Warnings");
    init();
    getOKAction().setEnabled(false);
    setSize(500, 500);
  }

  protected JComponent createCenterPanel() {
    myForm = new OverridePolicyWarningsForm(myProject, myFailures);
    myForm.addListener(new OverridePolicyWarningsForm.Listener() {
      public void stateChanged() {
        getOKAction().setEnabled(StringUtil.isNotEmpty(myForm.getReason()));
      }
    });
    return myForm.getContentPane();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.CheckIn.OverridePolicyWarnings";
  }

  public String getReason() {
    return myForm.getReason();
  }

}