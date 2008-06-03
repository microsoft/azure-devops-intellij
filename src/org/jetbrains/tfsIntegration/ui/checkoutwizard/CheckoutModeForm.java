package org.jetbrains.tfsIntegration.ui.checkoutwizard;

import com.intellij.ui.DocumentAdapter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class CheckoutModeForm {

  public interface Listener {
    void modeChanged(boolean autoModeSelected);

    void newWorkspaceNameChanged(String workspaceName);
  }

  private JPanel myContentPanel;

  private JRadioButton myAutoModeButton;

  private JRadioButton myManualModeButton;

  private JTextField myWorkspaceNameField;
  private JLabel myErrorLabel;

  private List<Listener> myListeners = new ArrayList<Listener>();

  public CheckoutModeForm() {
    myAutoModeButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        fireModeChanged();
      }
    });
    myManualModeButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        fireModeChanged();
      }
    });
    myWorkspaceNameField.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e) {
        myAutoModeButton.setSelected(true);
        fireWorkspaceNameChanged();
      }
    });
  }

  public JPanel getContentPanel() {
    return myContentPanel;
  }

  public boolean isAutoModeSelected() {
    return myAutoModeButton.isSelected();
  }

  public void setAutoModeSelected(boolean selected) {
    if (selected) {
      myAutoModeButton.setSelected(true);
    }
    else {
      myManualModeButton.setSelected(true);
    }
  }

  public String getNewWorkspaceName() {
    return myWorkspaceNameField.getText();
  }

  public void setNewWorkspaceName(String name) {
    myWorkspaceNameField.setText(name);
  }

  public void addListener(Listener listener) {
    myListeners.add(listener);
  }

  public void removeListener(Listener listener) {
    myListeners.remove(listener);
  }

  public void setErrorMessage(String message) {
    myErrorLabel.setText(message);
  }

  private void fireModeChanged() {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (Listener listener : listeners) {
      listener.modeChanged(isAutoModeSelected());
    }
  }

  private void fireWorkspaceNameChanged() {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (Listener listener : listeners) {
      listener.newWorkspaceNameChanged(getNewWorkspaceName());
    }
  }

}
