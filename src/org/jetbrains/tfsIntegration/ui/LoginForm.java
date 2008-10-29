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
import org.jetbrains.tfsIntegration.core.configuration.Credentials;

import javax.swing.*;
import java.net.URI;
import java.net.URISyntaxException;

public class LoginForm {
  private JTextField myUriField;
  private JTextField myUsernameField;
  private JTextField myDomainField;
  private JPasswordField myPasswordField;
  private JCheckBox myStorePasswordCheckbox;
  private JPanel myContentPane;

  public LoginForm(URI initialUri, String initialUsername, String initialDomain, String initialPassword, boolean allowUrlChange) {
    myUriField.setText(initialUri != null ? initialUri.toString() : "http://");
    myUriField.setEditable(allowUrlChange);
    myUsernameField.setText(initialUsername);
    myDomainField.setText(initialDomain);
    myPasswordField.setText(initialPassword);
  }

  public void addUriDocumentListener(final DocumentAdapter documentAdapter) {
    myUriField.getDocument().addDocumentListener(documentAdapter);
  }

  public void removeUrlDocumentListener(final DocumentAdapter documentAdapter) {
    myUriField.getDocument().removeDocumentListener(documentAdapter);
  }

  public JComponent getPreferredFocusedComponent() {
    if (myUriField.isEditable()) {
      return myUriField;
    }
    else if (myUsernameField.getText() == null || myUsernameField.getText().length() == 0) {
      return myUsernameField;
    }
    else {
      return myPasswordField;
    }
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public URI getUri() {
    try {
      String uriText = myUriField.getText();
      if (!uriText.endsWith("/")) {
        uriText = uriText.concat("/");
      }
      return new URI(uriText).normalize();
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public String getUsername() {
    return myUsernameField.getText();
  }

  public String getDomain() {
    return myDomainField.getText();
  }

  public String getPassword() {
    return String.valueOf(myPasswordField.getPassword());
  }

  public Credentials getCredentials() {
    return new Credentials(getUsername(), getDomain(), getPassword(), myStorePasswordCheckbox.isSelected());
  }

}
