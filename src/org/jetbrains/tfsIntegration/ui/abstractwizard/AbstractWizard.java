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

package org.jetbrains.tfsIntegration.ui.abstractwizard;

import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractWizard extends com.intellij.ide.wizard.AbstractWizard<AbstractWizardStep> {

  private final String myTitle;
  private final Map<Object, Integer> myStepId2Index = new HashMap<Object, Integer>();
  private final Map<Integer, AbstractWizardStep> myIndex2Step = new HashMap<Integer, AbstractWizardStep>();

  private final AbstractWizardStep.Listener myStepListener = new AbstractWizardStep.Listener() {
    public void stateChanged() {
      updateButtons();
    }

    public void doNextAction() {
      if (getNextButton().isEnabled()) {
        AbstractWizard.this.doNextAction();
      }
    }
  };

  public AbstractWizard(String title, @Nullable Project project, List<? extends AbstractWizardStep> steps) {
    super(title, project);
    myTitle = title;

    int index = 0;
    for (AbstractWizardStep step : steps) {
      myStepId2Index.put(step.getStepId(), index);
      myIndex2Step.put(index, step);
      addStep(step);
      step.addStepListener(myStepListener);
      index++;
    }

    init();
  }

  protected void doPreviousAction() {
    // Commit data of current step
    final AbstractWizardStep currentStep = mySteps.get(myCurrentStep);
    try {
      if (currentStep.showWaitCursorOnCommit()) {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      }
      currentStep._commitPrev();
    }
    catch (final CommitStepCancelledException e) {
      return;
    }
    catch (CommitStepException e) {
      Messages.showErrorDialog(getContentPane(), e.getMessage());
      return;
    }
    finally {
      if (currentStep.showWaitCursorOnCommit()) {
        getContentPane().setCursor(Cursor.getDefaultCursor());
      }
    }

    myCurrentStep = getPreviousStep(myCurrentStep);
    updateStep();
  }

  protected void doNextAction() {
    // Commit data of current step
    final AbstractWizardStep currentStep = mySteps.get(myCurrentStep);
    try {
      if (currentStep.showWaitCursorOnCommit()) {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      }
      currentStep._commit(false);
    }
    catch (final CommitStepCancelledException e) {
      return;
    }
    catch (CommitStepException e) {
      Messages.showErrorDialog(getContentPane(), e.getMessage());
      return;
    }
    finally {
      if (currentStep.showWaitCursorOnCommit()) {
        getContentPane().setCursor(Cursor.getDefaultCursor());
      }
    }

    myCurrentStep = getNextStep(myCurrentStep);
    updateStep();
  }

  protected int getNextStep(final int step) {
    AbstractWizardStep stepObject = myIndex2Step.get(step);
    Object nextStepId = stepObject.getNextStepId();
    return myStepId2Index.get(nextStepId);
  }

  protected int getPreviousStep(final int step) {
    AbstractWizardStep stepObject = myIndex2Step.get(step);
    Object previousStepId = stepObject.getPreviousStepId();
    return myStepId2Index.get(previousStepId);
  }

  protected String getHelpID() {
    return null;
  }

  protected void updateStep() {
    super.updateStep();
    updateButtons();
    setTitle(myTitle + ": " + getCurrentStepObject().getTitle());
  }

  private void updateButtons() {
    getNextButton().setEnabled(getCurrentStepObject().isComplete() && getCurrentStepObject().getNextStepId() != null);
    getPreviousButton().setEnabled(getCurrentStepObject().getPreviousStepId() != null);
    getFinishButton().setEnabled(canFinish());
  }

  protected boolean canFinish() {
    for (AbstractWizardStep step : mySteps) {
      if (!step.isComplete()) {
        return false;
      }
    }
    return true;
  }
}
