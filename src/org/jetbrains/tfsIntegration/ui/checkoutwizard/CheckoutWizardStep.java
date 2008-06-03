package org.jetbrains.tfsIntegration.ui.checkoutwizard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.ui.abstractwizard.AbstractWizardStep;

public abstract class CheckoutWizardStep extends AbstractWizardStep {

  protected final @NotNull CheckoutWizardModel myModel;

  public CheckoutWizardStep(String title, final CheckoutWizardModel model) {
    super(title);
    myModel = model;
  }


}

