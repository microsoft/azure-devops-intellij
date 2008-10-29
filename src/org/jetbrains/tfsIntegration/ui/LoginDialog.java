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

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.net.URI;

public class LoginDialog extends DialogWrapper {

  private LoginForm myLoginForm;

  // TODO use project/component as constructor parameter
  public LoginDialog(URI initialUri, Credentials initialCredentials, boolean allowUrlChange) {
    this(initialUri, initialCredentials != null ? initialCredentials.getUserName() : null,
         initialCredentials != null ? initialCredentials.getDomain() : null,
         initialCredentials != null ? initialCredentials.getPassword() : null, allowUrlChange);
  }

  public LoginDialog(URI initialUri, String initialUsername, String initialDomain, String initialPassword, boolean allowUrlChange) {
    super(false);
    setTitle(TFSBundle.message("logindialog.title"));

    myLoginForm = new LoginForm(initialUri, initialUsername, initialDomain, initialPassword, allowUrlChange);

    myLoginForm.addUriDocumentListener(new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e) {
        updateButtons();
      }
    });

    init();
    setResizable(false);
    updateButtons();
  }

  protected JComponent createCenterPanel() {
    return myLoginForm.getContentPane();
  }

  public JComponent getPreferredFocusedComponent() {
    return myLoginForm.getPreferredFocusedComponent();
  }

  private void updateButtons() {
    setOKActionEnabled(myLoginForm.getUri() != null);
  }

  public URI getUri() {
    return myLoginForm.getUri();
  }

  public String getUsername() {
    return myLoginForm.getUsername();
  }

  public String getDomain() {
    return myLoginForm.getDomain();
  }

  public String getPassword() {
    return myLoginForm.getPassword();
  }

  public Credentials getCredentials() {
    return myLoginForm.getCredentials();
  }

}