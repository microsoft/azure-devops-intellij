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

package org.jetbrains.tfsIntegration.ui.servertree;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;

import javax.swing.*;

public class ServerBrowserDialog extends DialogWrapper {
  private final TfsTreeForm myForm;

  public ServerBrowserDialog(String title,
                             final Project project,
                             ServerInfo server,
                             @Nullable String initialPath,
                             final boolean foldersOnly,
                             boolean canCreateVirtualFolders) {
    super(project, false);

    myForm = new TfsTreeForm();
    myForm.initialize(server, initialPath, foldersOnly, canCreateVirtualFolders, null);
    myForm.addListener(new TfsTreeForm.SelectionListener() {
      @Override
      public void selectionChanged() {
        setOKActionEnabled(myForm.getSelectedItem() != null);
      }
    });
    Disposer.register(getDisposable(), myForm);

    setSize(500, 600);

    setTitle(title);
    init();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    return myForm.getContentPane();
  }

  @Nullable
  public String getSelectedPath() {
    return myForm.getSelectedPath();
  }

  @Nullable
  public TfsTreeForm.SelectedItem getSelectedItem() {
    return myForm.getSelectedItem();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.ServerBrowser";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myForm.getPreferredFocusedComponent();
  }
}
