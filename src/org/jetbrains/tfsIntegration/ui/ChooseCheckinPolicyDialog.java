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
import org.jetbrains.tfsIntegration.checkin.PolicyBase;

import javax.swing.*;

public class ChooseCheckinPolicyDialog extends DialogWrapper {
  private ChooseCheckinPolicyForm myForm;

  public ChooseCheckinPolicyDialog(Project project) {
    super(project, false);
    setTitle("Add Check In Policy");
    init();

    setSize(450, 500);

    getOKAction().setEnabled(false);
  }

  protected JComponent createCenterPanel() {
    myForm = new ChooseCheckinPolicyForm();
    myForm.addListener(new ChooseCheckinPolicyForm.Listener() {
      public void stateChanged() {
        getOKAction().setEnabled(myForm.getSelectedPolicy() != null);
      }

      public void close() {
        doOKAction();
      }
    });

    return myForm.getContentPane();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.ChooseCheckInPolicy";
  }

  public PolicyBase getSelectedPolicy() {
    return myForm.getSelectedPolicy();
  }

}