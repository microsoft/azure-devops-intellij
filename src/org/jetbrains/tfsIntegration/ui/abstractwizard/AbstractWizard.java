package org.jetbrains.tfsIntegration.ui.abstractwizard;

import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractWizard extends com.intellij.ide.wizard.AbstractWizard<AbstractWizardStep> {

  private final String myTitle;
  private Map<Object, Integer> myStepId2Index = new HashMap<Object, Integer>();
  private Map<Integer, AbstractWizardStep> myIndex2Step = new HashMap<Integer, AbstractWizardStep>();

  private AbstractWizardStep.Listener myStepListener = new AbstractWizardStep.Listener() {
    public void stateChanged() {
      updateButtons();
    }

    public void doNextAction() {
      if (getNextButton().isEnabled()) {
        AbstractWizard.this.doNextAction();
      }
    }
  };

  public AbstractWizard(String title, Project project, List<? extends AbstractWizardStep> steps) {
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

  protected void doNextAction() {
    // Commit data of current step
    final AbstractWizardStep currentStep = mySteps.get(myCurrentStep);
    try {
      if (currentStep.showWaitCursorOnCommit()) {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      }
      currentStep._commit(false);
    }
    catch (final CommitStepException exc) {
      if (currentStep.showWaitCursorOnCommit()) {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
      if (exc.getMessage().length() > 0) { // TODO: use custom exception class
        Messages.showErrorDialog(getContentPane(), exc.getMessage());
      }
      return;
    }
    finally {
      if (currentStep.showWaitCursorOnCommit()) {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
