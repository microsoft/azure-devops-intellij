package org.jetbrains.tfsIntegration.checkin;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * This abstract class uses {@link Memento} API to load and save its configuration, similar to Teamprise policies implementations.
 * This way it's easier to port the existing code from Teamprise to the IntelliJ IDEA.
 */
@SuppressWarnings({"AbstractClassNeverImplemented"})
public abstract class MementoStoredPolicyBase extends PolicyBase {

  /**
   * Load policy configuration stored in memento. Runtime exception thrown out of this method will be reported as loading error.
   *
   * @param configurationMemento configuration memento
   */
  protected abstract void loadConfiguration(Memento configurationMemento);

  /**
   * Save policy configuration state to the memento. Runtime exception thrown out of this method will be reported as saving error.
   *
   * @param configurationMemento memento to save configuration to
   */
  protected abstract void saveConfiguration(Memento configurationMemento);

  @Override
  public final void loadState(@NotNull Element element) {
    loadConfiguration(new XMLMemento(element));
  }

  @Override
  public final void saveState(@NotNull Element element) {
    saveConfiguration(new XMLMemento(element));
  }
}
