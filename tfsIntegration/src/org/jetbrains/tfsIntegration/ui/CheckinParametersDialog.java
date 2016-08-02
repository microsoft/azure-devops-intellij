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

package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.tfsIntegration.checkin.CheckinParameters;

import javax.swing.*;

public class CheckinParametersDialog extends DialogWrapper {
  private final CheckinParameters myParameters;
  private CheckinParametersForm myForm;
  private final Project myProject;

  public CheckinParametersDialog(final Project project, CheckinParameters parameters) {
    super(project, true);
    myProject = project;
    myParameters = parameters;
    setTitle("Configure Checkin Parameters");
    init();

    setSize(700, 500);
  }

  protected JComponent createCenterPanel() {
    myForm = new CheckinParametersForm(myParameters, myProject);
    Disposer.register(getDisposable(), myForm);
    //myForm.addListener(new CheckinParametersForm.Listener() {
    //  public void stateChanged() {
    //  }
    //});
    return myForm.getContentPane();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.CheckIn.Parameters";
  }


}

