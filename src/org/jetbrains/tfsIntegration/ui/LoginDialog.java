/*
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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.credentials.Credentials;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

public class LoginDialog extends DialogWrapper {

  private static final Logger LOG = Logger.getInstance(LoginDialog.class.getName());

  private final URI myInitialUri;
  private final String myInitialUsername;
  private final String myInitialDomain;
  private final String myInitialPassword;
  private final boolean myAllowUrlChange;

  private JTextField myUriField;
  private JTextField myUsernameField;
  private JTextField myDomainField;
  private JPasswordField myPasswordField;
  private JCheckBox myStorePasswordCheckbox;

  // TODO use project/component as constructor parameter
  // TODO UI designer

  public LoginDialog(URI initialUri, Credentials initialCredentials, boolean allowUrlChange) {
    this(initialUri, initialCredentials != null ? initialCredentials.getUserName() : null,
         initialCredentials != null ? initialCredentials.getDomain() : null,
         initialCredentials != null ? initialCredentials.getPassword() : null, allowUrlChange);
  }

  public LoginDialog(URI initialUri, String initialUsername, String initialDomain, String initialPassword, boolean allowUrlChange) {
    super((Project)null, true);
    myInitialUri = initialUri;
    myInitialUsername = initialUsername;
    myInitialDomain = initialDomain;
    myInitialPassword = initialPassword;
    myAllowUrlChange = allowUrlChange;

    setTitle(TFSBundle.message("logindialog.title"));
    setResizable(false);

    init();
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
      LOG.info(e);
      return null;
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

  protected String getDimensionServiceKey() {
    return "tfs.loginDialog";
  }

  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridBagLayout());

    GridBagConstraints gc = new GridBagConstraints();

    gc.gridx = 0;
    gc.gridy = 0;
    gc.insets = new Insets(3, 5, 0, 0);
    gc.anchor = GridBagConstraints.WEST;

    JLabel urlLabel = new JLabel(TFSBundle.message("logindialog.url"));
    urlLabel.setDisplayedMnemonic('S');
    panel.add(urlLabel, gc);

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 0;
    gc.weightx = 1.0;
    gc.insets = new Insets(3, 0, 0, 5);
    gc.fill = GridBagConstraints.HORIZONTAL;

    // TODO
    myUriField = new JTextField(myInitialUri != null ? myInitialUri.toString() : null);
    urlLabel.setLabelFor(myUriField);
    myUriField.setEditable(myAllowUrlChange);
    panel.add(myUriField, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 1;
    gc.insets = new Insets(3, 5, 0, 0);
    gc.anchor = GridBagConstraints.WEST;

    JLabel usernameLabel = new JLabel(TFSBundle.message("logindialog.username"));
    usernameLabel.setDisplayedMnemonic('n');
    panel.add(usernameLabel, gc);

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 1;
    gc.weightx = 1.0;
    gc.insets = new Insets(3, 0, 0, 5);
    gc.fill = GridBagConstraints.HORIZONTAL;

    myUsernameField = new JTextField(myInitialUsername);
    usernameLabel.setLabelFor(myUsernameField);
    panel.add(myUsernameField, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 2;
    gc.insets = new Insets(3, 5, 0, 0);
    gc.anchor = GridBagConstraints.WEST;

    JLabel domainLabel = new JLabel(TFSBundle.message("logindialog.domain"));
    domainLabel.setDisplayedMnemonic('d');
    panel.add(domainLabel, gc);

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 2;
    gc.weightx = 1.0;
    gc.insets = new Insets(3, 0, 0, 5);
    gc.fill = GridBagConstraints.HORIZONTAL;

    myDomainField = new JTextField(myInitialDomain);
    myDomainField.setColumns(20);
    domainLabel.setLabelFor(myDomainField);
    panel.add(myDomainField, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 3;
    gc.insets = new Insets(3, 5, 0, 0);
    gc.anchor = GridBagConstraints.WEST;

    JLabel passwordLabel = new JLabel(TFSBundle.message("logindialog.password"));
    passwordLabel.setDisplayedMnemonic('p');
    panel.add(passwordLabel, gc);

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 3;
    gc.weightx = 1.0;
    gc.insets = new Insets(3, 0, 0, 5);
    gc.fill = GridBagConstraints.HORIZONTAL;

    myPasswordField = new JPasswordField(myInitialPassword);
    passwordLabel.setLabelFor(myPasswordField);
    panel.add(myPasswordField, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridwidth = 2;
    gc.gridy = 4;
    gc.weightx = 0;
    gc.anchor = GridBagConstraints.WEST;
    myStorePasswordCheckbox = new JCheckBox(TFSBundle.message("logindialog.storepassword"));
    myStorePasswordCheckbox.setBorderPaintedFlat(true);
    panel.add(myStorePasswordCheckbox, gc);

    gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridwidth = 2;
    gc.gridy = 5;
    gc.weightx = 1.0;
    gc.weighty = 1.0;
    JPanel dummyPanel = new JPanel();
    panel.add(dummyPanel, gc);
    return panel;
  }

  public JComponent getPreferredFocusedComponent() {
    if (myUriField.getText() == null || myUriField.getText().length() == 0) {
      return myUriField;
    }
    else if (myUsernameField.getText() == null || myUsernameField.getText().length() == 0) {
      return myUsernameField;
    }
    else {
      return myPasswordField;
    }
  }

  public Credentials getCredentials() {
    if (!isOK()) {
      throw new IllegalStateException("Dialog did not succeed");
    }
    return new Credentials(getUsername(), getDomain(), getPassword(), myStorePasswordCheckbox.isSelected());
  }

}