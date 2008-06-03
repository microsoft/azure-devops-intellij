package org.jetbrains.tfsIntegration.ui.abstractwizard;

import com.intellij.ide.wizard.CommitStepException;
import com.intellij.ide.wizard.Step;
import com.intellij.ide.wizard.StepListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWizardStep implements Step {

  private final String myTitle;

  interface Listener extends StepListener {
    void doNextAction();
  }

  private List<Listener> myListeners = new ArrayList<Listener>();

  public AbstractWizardStep(final String title) {
    myTitle = title;
  }

  public void _init() {
  }

  public void _commit(boolean finishChosen) throws CommitStepException {
  }

  public void addStepListener(Listener listener) {
    myListeners.add(listener);
  }

  protected void fireStateChanged() {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (Listener listener : listeners) {
      listener.stateChanged();
    }
  }

  protected void fireGoNext() {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (Listener listener : listeners) {
      listener.doNextAction();
    }
  }

  public Icon getIcon() {
    return null;
  }

  @NotNull
  public abstract Object getStepId();

  @Nullable
  public abstract Object getNextStepId();

  @Nullable
  public abstract Object getPreviousStepId();

  public abstract boolean isComplete();

  public abstract boolean showWaitCursorOnCommit();

  public String getTitle() {
    return myTitle;
  }
}
