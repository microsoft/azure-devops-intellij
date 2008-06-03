package org.jetbrains.tfsIntegration.ui.checkoutwizard;

import org.jetbrains.tfsIntegration.ui.abstractwizard.AbstractWizard;

import java.util.List;

public class CheckoutWizard extends AbstractWizard {
  private final CheckoutWizardModel myModel;

  public CheckoutWizard(List<CheckoutWizardStep> steps, CheckoutWizardModel model) {
    super("Checkout From TFS", null, steps);
    myModel = model;
  }

  protected boolean canFinish() {
    return myModel != null && myModel.isComplete() && getCurrentStepObject().getNextStepId() == null;
  }
}
