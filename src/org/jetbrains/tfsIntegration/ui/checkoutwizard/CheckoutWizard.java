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

package org.jetbrains.tfsIntegration.ui.checkoutwizard;

import com.intellij.ide.wizard.AbstractWizardEx;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CheckoutWizard extends AbstractWizardEx {
  private final CheckoutWizardModel myModel;

  public CheckoutWizard(@Nullable Project project, List<CheckoutWizardStep> steps, CheckoutWizardModel model) {
    super("Checkout From TFS", project, steps);
    myModel = model;
  }

  protected boolean canFinish() {
    return myModel != null && myModel.isComplete() && getCurrentStepObject().getNextStepId() == null;
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.CheckoutWizard";
  }

}
