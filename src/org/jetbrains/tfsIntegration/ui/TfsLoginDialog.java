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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.net.HttpConfigurable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class TfsLoginDialog extends DialogWrapper {

  private static final Logger LOG = Logger.getInstance(TfsLoginDialog.class.getName());

  private TfsLoginForm myLoginForm;
  @Nullable private Condition<TfsLoginDialog> myOkActionCallback;

  public TfsLoginDialog(Project project,
                        URI initialUri,
                        Credentials initialCredentials,
                        boolean allowAddressChange,
                        @Nullable Condition<TfsLoginDialog> okActionCallback) {
    super(project, true);
    doInit(initialUri, initialCredentials, allowAddressChange, okActionCallback);
  }

  public TfsLoginDialog(JComponent parentComponent,
                        URI initialUri,
                        Credentials initialCredentials,
                        boolean allowAddressChange,
                        @Nullable Condition<TfsLoginDialog> okActionCallback) {
    super(parentComponent, true);
    doInit(initialUri, initialCredentials, allowAddressChange, okActionCallback);
  }

  private void doInit(URI initialUri,
                      Credentials initialCredentials,
                      boolean allowAddressChange,
                      Condition<TfsLoginDialog> okActionCallback) {
    myOkActionCallback = okActionCallback;
    setTitle(TFSBundle.message(allowAddressChange ? "logindialog.title.connect" : "logindialog.title.login"));

    myLoginForm = new TfsLoginForm(initialUri, initialCredentials, allowAddressChange);
    myLoginForm.addListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        revalidate();
      }
    });

    init();
    revalidate();
  }

  protected JComponent createCenterPanel() {
    return myLoginForm.getContentPane();
  }

  public JComponent getPreferredFocusedComponent() {
    return myLoginForm.getPreferredFocusedComponent();
  }

  private void revalidate() {
    setMessage(getErrorMessage(), true);
  }

  @Nullable
  private String getErrorMessage() {
    if (StringUtil.isEmptyOrSpaces(myLoginForm.getUrl())) {
      return TFSBundle.message("login.dialog.address.empty");
    }
    if (TfsUtil.getUrl(myLoginForm.getUrl(), false, false) == null) {
      return TFSBundle.message("login.dialog.address.invalid");
    }

    if (!myLoginForm.isUseNative() && StringUtil.isEmptyOrSpaces(myLoginForm.getUsername())) {
      return TFSBundle.message("login.dialog.username.empty");
    }
    return null;
  }

  public URI getUri() {
    URI uri = TfsUtil.getUrl(myLoginForm.getUrl(), false, false);
    LOG.assertTrue(uri != null);
    return uri;
  }

  public Credentials getCredentials() {
    return myLoginForm.getCredentials();
  }

  public void setMessage(@Nullable String message, boolean disableOkAction) {
    if (message != null && !message.endsWith(".")) {
      message += ".";
    }
    setErrorText(message);
    setOKActionEnabled(!disableOkAction || message == null);
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.Login";
  }

  @Override
  protected void doOKAction() {
    if (shouldPromptForProxyPassword(false)) {
      HttpConfigurable hc = HttpConfigurable.getInstance();
      hc.setPlainProxyPassword(myLoginForm.getProxyPassword());
    }

    if (myLoginForm.getCredentials().getType() == Credentials.Type.Alternate && "http".equals(getUri().getScheme())) {
      if (Messages.showYesNoDialog(myLoginForm.getContentPane(),
                                   "You're about to send your credentials over unsecured HTTP connection. Continue?", getTitle(),
                                   null) != Messages.YES) {
        return;
      }
    }

    if (myOkActionCallback == null || myOkActionCallback.value(this)) {
      super.doOKAction();
    }
  }

  public static boolean shouldPromptForProxyPassword(boolean strictOnly) {
    HttpConfigurable hc = HttpConfigurable.getInstance();
    return TFSConfigurationManager.getInstance().useIdeaHttpProxy() &&
           hc.USE_HTTP_PROXY &&
           hc.PROXY_AUTHENTICATION &&
           !hc.KEEP_PROXY_PASSWORD && (!strictOnly || StringUtil.isEmpty(hc.getPlainProxyPassword()));
  }
}
