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
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlServer;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.labels.LabelItemSpecWithItems;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemSpec;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;
import org.jetbrains.tfsIntegration.ui.servertree.ServerTree;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.List;

public class AddItemDialog extends DialogWrapper {

  private final Project myProject;
  private final WorkspaceInfo myWorkspace;
  private final String mySourcePath;

  private AddItemForm myAddItemForm;
  private LabelItemSpecWithItems myLabelSpec;

  public AddItemDialog(final Project project, final WorkspaceInfo workspace, final String sourcePath) {
    super(project, true);
    myProject = project;
    myWorkspace = workspace;
    mySourcePath = sourcePath;

    setTitle("Add Item");

    init();

    myAddItemForm.addServerTreeSelectionListener(new ServerTree.SelectionListener() {
      public void selectionChanged(final ServerTree.SelectedItem selection) {
        updateButtons();
      }
    });

    myAddItemForm.addSelectRevisionListener(new SelectRevisionForm.Listener() {
      public void revisionChanged() {
        updateButtons();
      }
    });

    updateButtons();
  }

  private void updateButtons() {
    setOKActionEnabled(myAddItemForm.getServerItem() != null && myAddItemForm.getVersion() != null);
  }

  @Nullable
  protected JComponent createCenterPanel() {
    myAddItemForm = new AddItemForm(myProject, myWorkspace, mySourcePath);
    return myAddItemForm.getContentPane();
  }

  @Nullable
  public LabelItemSpecWithItems getLabelSpec() {
    return myLabelSpec;
  }

  protected void doOKAction() {
    try {
      getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      final ServerTree.SelectedItem serverItem = myAddItemForm.getServerItem();
      //noinspection ConstantConditions
      ItemSpec itemSpec = VersionControlServer.createItemSpec(serverItem.path, serverItem.isDirectory ? RecursionType.Full : null);
      List<Item> items = myWorkspace.getServer().getVCS().queryItems(itemSpec, myAddItemForm.getVersion());
      if (!items.isEmpty()) {
        myLabelSpec = LabelItemSpecWithItems.createForAdd(itemSpec, myAddItemForm.getVersion(), items);
      }
      else {
        String message = MessageFormat.format("Item ''{0}'' was not found in source control at version ''{1}''.", serverItem.path,
                                              myAddItemForm.getVersion().getPresentableString());

        Messages.showErrorDialog(myProject, message, "Apply label");
        return;
      }
    }
    catch (TfsException e) {
      Messages.showErrorDialog(myProject, e.getMessage(), "Apply label");
      return;
    }
    finally {
      getContentPane().setCursor(Cursor.getDefaultCursor());
    }
    super.doOKAction();
  }
}
