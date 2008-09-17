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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class ServerBrowserDialog extends DialogWrapper {
  private final ServerInfo myServer;
  private final String myInitialPath;
  private final boolean myFoldersOnly;
  private ServerBrowserForm myForm;
  private final Collection<? extends ServerBrowserAction> myActions;


  public ServerBrowserDialog(String title,
                             final Project project,
                             ServerInfo server,
                             @Nullable String initialPath,
                             final boolean foldersOnly, final Collection<? extends ServerBrowserAction> actions) {
    super(project, false);
    myServer = server;
    myInitialPath = initialPath;
    myFoldersOnly = foldersOnly;
    myActions = actions;

    setTitle(title);
    init();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    myForm = new ServerBrowserForm(myFoldersOnly, myServer, myInitialPath, null, myActions);
    myForm.getContentPanel().setPreferredSize(new Dimension(400, 500));
    return myForm.getContentPanel();
  }

  @Nullable
  public String getSelectedPath() {
    return myForm.getSelectedPath();
  }
}
