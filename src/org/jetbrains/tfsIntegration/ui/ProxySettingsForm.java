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

import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ProxySettingsForm {

  private JPanel myContentPane;
  private JRadioButton myNoProxyRadioButton;
  private JRadioButton myProxyServerRadioButton;
  private JTextField myProxyServerTextField;

  private List<Listener> myListeners = new ArrayList<Listener>();

  public interface Listener {
    void stateChanged();
  }

  public ProxySettingsForm(URI currentProxyUri) {
    if (currentProxyUri == null) {
      myNoProxyRadioButton.setSelected(true);
      myProxyServerTextField.setText("http://");
    }
    else {
      myProxyServerRadioButton.setSelected(true);
      myProxyServerTextField.setText(currentProxyUri.toString());
    }

    final ActionListener radioButtonListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateContols();
        fireStateChanged();
      }
    };

    myNoProxyRadioButton.addActionListener(radioButtonListener);
    myProxyServerRadioButton.addActionListener(radioButtonListener);
    myProxyServerTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e) {
        fireStateChanged();
      }
    });
    
    updateContols();
  }

  private void updateContols() {
    if (myProxyServerRadioButton.isSelected()) {
      myProxyServerTextField.setEnabled(true);
      myProxyServerTextField.requestFocus();
    } else {
      myProxyServerTextField.setEnabled(false);
    }
  }


  public JComponent getContentPane() {
    return myContentPane;
  }

  private void fireStateChanged() {
    Listener[] listeners = myListeners.toArray(new Listener[myListeners.size()]);
    for (Listener listener : listeners) {
      listener.stateChanged();
    }
  }

  public void addListener(final Listener listener) {
    myListeners.add(listener);
  }

  public void removeListener(final Listener listener) {
    myListeners.remove(listener);
  }

  public boolean isConsistentState() {
    return myNoProxyRadioButton.isSelected() || parseProxyUri(myProxyServerTextField.getText()) != null;
  }

  @Nullable
  public URI getProxyUri() {
    if (myNoProxyRadioButton.isSelected()) {
      return null;
    }
    else {
      return parseProxyUri(myProxyServerTextField.getText());
    }
  }

  @Nullable
  private static URI parseProxyUri(String uriText) {
    try {
      if (!uriText.endsWith("/")) {
        uriText = uriText.concat("/");
      }
      return new URI(uriText).normalize();
    }
    catch (URISyntaxException e) {
      return null;
    }
  }

}
