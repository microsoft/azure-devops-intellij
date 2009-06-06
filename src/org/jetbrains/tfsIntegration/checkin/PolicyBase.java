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

package org.jetbrains.tfsIntegration.checkin;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jdom.Element;

import java.text.MessageFormat;

public abstract class PolicyBase {

  public static final ExtensionPointName<PolicyBase> EP_NAME = new ExtensionPointName<PolicyBase>("TFS.checkinPolicy");

  protected static final PolicyFailure[] NO_FAILURES = new PolicyFailure[0];

  @NotNull
  public abstract PolicyType getPolicyType();

  public abstract PolicyFailure[] evaluate(@NotNull PolicyContext policycontext, @NotNull ProgressIndicator progressIndicator);

  public abstract boolean canEdit();

  public abstract boolean edit(Project project);

  public abstract void loadState(@NotNull Element element);

  public abstract void saveState(@NotNull Element element);


  public void activate(@NotNull Project project, @NotNull PolicyFailure policyFailure) {
    final String message;
    if (policyFailure.getTooltipText() != null) {
      message = MessageFormat.format("{0}\n\n{1}", policyFailure.getMessage(), policyFailure.getTooltipText());
    }
    else {
      message = policyFailure.getMessage();
    }
    Messages.showWarningDialog(project, message, "Check In Policy Warning");
  }

  //public void displayHelp(PolicyFailure policyFailure) {
  //}

}
