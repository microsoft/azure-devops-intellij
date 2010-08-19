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

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.EventDispatcher;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.webservice.auth.NativeNTLM2Scheme;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

public class TfsLoginForm {

  private JTextField myAddressField;
  private JTextField myUsernameField;
  private JTextField myDomainField;
  private JPasswordField myPasswordField;
  private JCheckBox myStorePasswordCheckbox;
  private JPanel myContentPane;
  private JLabel myMessageLabel;
  private HyperlinkLabel myProxyPasswordLabel;
  private JPasswordField myProxyPasswordField;
  private JPanel myProxyPanel;
  private JCheckBox myUseSystemCredentialsCheckBox;
  private JLabel myUsernameLabel;
  private JLabel myDomainLabel;
  private JLabel myPasswordLabel;

  private final EventDispatcher<ChangeListener> myEventDispatcher = EventDispatcher.create(ChangeListener.class);

  public TfsLoginForm(URI initialUri, Credentials initialCredentials, boolean allowUrlChange) {
    myAddressField.setText(initialUri != null ? TfsUtil.getPresentableUri(initialUri) : null);
    myAddressField.setEditable(allowUrlChange);
    myUsernameField.setText(initialCredentials != null ? initialCredentials.getUserName() : null);
    myDomainField.setText(initialCredentials != null ? initialCredentials.getDomain() : null);
    myPasswordField.setText(initialCredentials != null ? initialCredentials.getPassword() : null);

    final DocumentListener changeListener = new DocumentAdapter() {
      @Override
      protected void textChanged(final DocumentEvent e) {
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
      }
    };

    myAddressField.getDocument().addDocumentListener(changeListener);
    myUsernameField.getDocument().addDocumentListener(changeListener);
    myDomainField.getDocument().addDocumentListener(changeListener);
    myPasswordField.getDocument().addDocumentListener(changeListener);

    myMessageLabel.setIcon(UIUtil.getBalloonWarningIcon());

    myProxyPasswordLabel
      .setHyperlinkText("", TFSBundle.message("login.dialog.proxy.label.1"), TFSBundle.message("login.dialog.proxy.label.2"));
    myProxyPasswordLabel.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          HttpConfigurable hc = ShowSettingsUtil.getInstance().findApplicationConfigurable(HttpConfigurable.class);
          ShowSettingsUtil.getInstance().editConfigurable((Project)null, hc);
        }
      }
    });

    if (TfsLoginDialog.shouldPromptForProxyPassword(false)) {
      HttpConfigurable hc = HttpConfigurable.getInstance();
      myProxyPasswordField.setText(hc.getPlainProxyPassword());
      myProxyPanel.setVisible(true);
    }
    else {
      myProxyPanel.setVisible(false);
    }

    if (NativeNTLM2Scheme.isAvailable()) {
      myUseSystemCredentialsCheckBox
        .setSelected(initialCredentials == null || initialCredentials.getUseNative() != Credentials.UseNative.No);
      myUseSystemCredentialsCheckBox.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          updateOnUseSystemCredentials();
          if (!myUseSystemCredentialsCheckBox.isSelected()) {
            IdeFocusManager.findInstanceByComponent(myContentPane).requestFocus(myUsernameField, true);
          }
        }
      });
    }
    else {
      myUseSystemCredentialsCheckBox.setVisible(false);
    }

    updateOnUseSystemCredentials();
  }

  private void updateOnUseSystemCredentials() {
    myUsernameLabel.setEnabled(!myUseSystemCredentialsCheckBox.isSelected());
    myUsernameField.setEnabled(!myUseSystemCredentialsCheckBox.isSelected());
    myDomainLabel.setEnabled(!myUseSystemCredentialsCheckBox.isSelected());
    myDomainField.setEnabled(!myUseSystemCredentialsCheckBox.isSelected());
    myPasswordLabel.setEnabled(!myUseSystemCredentialsCheckBox.isSelected());
    myPasswordField.setEnabled(!myUseSystemCredentialsCheckBox.isSelected());
    myStorePasswordCheckbox.setEnabled(!myUseSystemCredentialsCheckBox.isSelected());
    myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
  }

  public JComponent getPreferredFocusedComponent() {
    if (myAddressField.isEditable()) {
      return myAddressField;
    }
    else if (StringUtil.isEmpty(myUsernameField.getText())) {
      return myUsernameField;
    }
    else {
      return myPasswordField;
    }
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public String getUrl() {
    return myAddressField.getText();
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
    if (myUseSystemCredentialsCheckBox.isSelected()) {
      return new Credentials("", "", null, false, Credentials.UseNative.Yes);
    }
    else {
      return new Credentials(getUsername(), getDomain(), getPassword(), myStorePasswordCheckbox.isSelected(), Credentials.UseNative.No);
    }
  }

  public void addListener(ChangeListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeListener(ChangeListener listener) {
    myEventDispatcher.removeListener(listener);
  }

  public void setErrorMessage(@Nullable String errorMessage) {
    if (errorMessage != null && !errorMessage.endsWith(".")) {
      errorMessage += ".";
    }
    myMessageLabel.setText(errorMessage);
    myMessageLabel.setVisible(errorMessage != null);
  }

  public String getProxyPassword() {
    return String.valueOf(myProxyPasswordField.getPassword());
  }

  private void createUIComponents() {
    // TODO mnemonic
    myProxyPasswordLabel = new HyperlinkLabel() {
      @Override
      protected void applyRenderingHints(Graphics g) {
        // do nothing, otherwise label text will be antialiased
      }
    };
  }

  public boolean isUseNative() {
    return myUseSystemCredentialsCheckBox.isSelected();
  }
}
