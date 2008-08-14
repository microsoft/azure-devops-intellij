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


public class ServerBrowserDialog extends DialogWrapper {
  private final ServerInfo myServer;
  private final boolean myFoldersOnly;
  private final ServerTree.PathFilter myPathFilter;
  private ServerTree myServerTree;

  public ServerBrowserDialog(String title, final Project project, ServerInfo server, String initialPath, final boolean foldersOnly) {
    this(title, project, server, initialPath, foldersOnly, null);
  }

  public ServerBrowserDialog(String title,
                             final Project project,
                             ServerInfo server,
                             String initialPath,
                             final boolean foldersOnly,
                             ServerTree.PathFilter pathFilter) {
    super(project, false);
    myServer = server;
    myFoldersOnly = foldersOnly;
    myPathFilter = pathFilter;

    setTitle(title);
    setSize(500, 400);
    init();
    // TODO support initial path
  }

  @Nullable
  protected JComponent createCenterPanel() {
    myServerTree = new ServerTree(myFoldersOnly);
    myServerTree.setPathFilter(myPathFilter);
    myServerTree.setServer(myServer);
    return myServerTree.getContentPanel();
  }

  @Nullable
  public String getSelectedPath() {
    return myServerTree.getSelectedPath();
  }
}
