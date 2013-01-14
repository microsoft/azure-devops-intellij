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

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.EventDispatcher;
import com.intellij.util.net.HttpConfigurable;
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
  private HyperlinkLabel myProxyPasswordLabel;
  private JPasswordField myProxyPasswordField;
  private JPanel myProxyPanel;
  private JLabel myUsernameLabel;
  private JLabel myDomainLabel;
  private JLabel myPasswordLabel;
  private ComboBox myTypeCombo;

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

    myProxyPasswordLabel
      .setHyperlinkText("", TFSBundle.message("login.dialog.proxy.label.1"), TFSBundle.message("login.dialog.proxy.label.2"));
    myProxyPasswordLabel.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          HttpConfigurable.editConfigurable(TfsLoginForm.this.myContentPane);
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

    myTypeCombo.setRenderer(new ListCellRendererWrapper<Credentials.Type>() {
      @Override
      public void customize(final JList list,
                            final Credentials.Type value,
                            final int index,
                            final boolean selected,
                            final boolean hasFocus) {
        setText(value.getPresentableText());
      }
    });
    if (NativeNTLM2Scheme.isAvailable()) {
      myTypeCombo.setModel(new DefaultComboBoxModel(
        new Credentials.Type[]{Credentials.Type.NtlmNative, Credentials.Type.NtlmExplicit, Credentials.Type.Alternate}));
      myTypeCombo.setSelectedItem(initialCredentials == null ? Credentials.Type.NtlmNative : initialCredentials.getType());
      myTypeCombo.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          updateOnTypeChange();
          if (getCredentialsType() != Credentials.Type.NtlmNative) {
            IdeFocusManager.findInstanceByComponent(myContentPane).requestFocus(myUsernameField, true);
          }
        }
      });
    }
    else {
      myTypeCombo.setModel(new DefaultComboBoxModel(new Credentials.Type[]{Credentials.Type.NtlmExplicit, Credentials.Type.Alternate}));
    }

    updateOnTypeChange();
  }

  private Credentials.Type getCredentialsType() {
    return (Credentials.Type)myTypeCombo.getSelectedItem();
  }

  private void updateOnTypeChange() {
    boolean isNative = getCredentialsType() == Credentials.Type.NtlmNative;
    myUsernameLabel.setEnabled(!isNative);
    myUsernameField.setEnabled(!isNative);
    myDomainLabel.setEnabled(getCredentialsType() == Credentials.Type.NtlmExplicit);
    myDomainField.setEnabled(getCredentialsType() == Credentials.Type.NtlmExplicit);
    myPasswordLabel.setEnabled(!isNative);
    myPasswordField.setEnabled(!isNative);
    myStorePasswordCheckbox.setEnabled(!isNative);
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
    if (getCredentialsType() == Credentials.Type.NtlmNative) {
      return Credentials.createNative();
    }
    else {
      return new Credentials(getUsername(), getDomain(), getPassword(), myStorePasswordCheckbox.isSelected(), getCredentialsType());
    }
  }

  public void addListener(ChangeListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeListener(ChangeListener listener) {
    myEventDispatcher.removeListener(listener);
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
    return getCredentialsType() == Credentials.Type.NtlmNative;
  }
}
