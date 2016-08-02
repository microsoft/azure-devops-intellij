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
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Item;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ItemSpec;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.RecursionType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlServer;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.labels.LabelItemSpecWithItems;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.ui.servertree.TfsTreeForm;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.text.MessageFormat;
import java.util.List;

public class AddItemDialog extends DialogWrapper {

  private final Project myProject;
  private final WorkspaceInfo myWorkspace;

  private final AddItemForm myForm;
  private LabelItemSpecWithItems myLabelSpec;

  public AddItemDialog(final Project project, final WorkspaceInfo workspace, final String sourcePath) {
    super(project, true);
    myForm = new AddItemForm(project, workspace, sourcePath);
    myProject = project;
    myWorkspace = workspace;

    setTitle("Add Item");

    init();

    myForm.addListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        updateButtons();
      }
    });
    updateButtons();
  }

  private void updateButtons() {
    setOKActionEnabled(myForm.getServerItem() != null && myForm.getVersion() != null);
  }

  @Nullable
  protected JComponent createCenterPanel() {
    return myForm.getContentPane();
  }

  @Nullable
  public LabelItemSpecWithItems getLabelSpec() {
    return myLabelSpec;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myForm.getPreferredFocusedComponent();
  }

  protected void doOKAction() {
    try {
      final TfsTreeForm.SelectedItem serverItem = myForm.getServerItem();
      //noinspection ConstantConditions
      ItemSpec itemSpec = VersionControlServer.createItemSpec(serverItem.path, serverItem.isDirectory ? RecursionType.Full : null);
      List<Item> items = myWorkspace.getServer().getVCS()
        .queryItems(itemSpec, myForm.getVersion(), getContentPane(), TFSBundle.message("loading.item"));
      if (!items.isEmpty()) {
        myLabelSpec = LabelItemSpecWithItems.createForAdd(itemSpec, myForm.getVersion(), items);
      }
      else {
        String message = MessageFormat.format("Item ''{0}'' was not found in source control at version ''{1}''.", serverItem.path,
                                              myForm.getVersion().getPresentableString());

        Messages.showErrorDialog(myProject, message, "Apply label");
        return;
      }
    }
    catch (TfsException e) {
      Messages.showErrorDialog(myProject, e.getMessage(), "Apply label");
      return;
    }
    super.doOKAction();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.AddItem";
  }
}
