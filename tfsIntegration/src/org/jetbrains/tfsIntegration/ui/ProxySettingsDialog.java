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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;

import javax.swing.*;
import java.net.URI;

public class ProxySettingsDialog extends DialogWrapper {

  private ProxySettingsForm myForm;
  private final @NotNull URI myServerUri;

  public ProxySettingsDialog(final Project project, final @NotNull URI serverUri) {
    super(project, true);
    myServerUri = serverUri;
    String title = TFSBundle.message("proxy.dialog.title", TfsUtil.getPresentableUri(serverUri));
    setTitle(title);

    init();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    Credentials credentials = TFSConfigurationManager.getInstance().getCredentials(myServerUri);
    myForm = new ProxySettingsForm(TFSConfigurationManager.getInstance().getProxyUri(myServerUri),
                                   credentials != null ? credentials.getQualifiedUsername() : null);

    return myForm.getContentPane();
  }

  private void updateButtons() {
    String errorMessage = myForm.isValid() ? null : "Please enter valid proxy address.";
    myForm.setMessage(errorMessage);
    setOKActionEnabled(myForm.isValid());
  }

  @Override
  protected void doOKAction() {
    if (myForm.isValid()) {
      super.doOKAction();
    }
    else {
      updateButtons();
      myForm.addListener(new ProxySettingsForm.Listener() {
        public void stateChanged() {
          updateButtons();
        }
      });
    }
  }

  @Nullable
  public URI getProxyUri() {
    return myForm.getProxyUri();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.ProxySettings";
  }

}
