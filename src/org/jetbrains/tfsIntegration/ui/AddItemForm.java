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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.TitledSeparatorWithMnemonic;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;
import org.jetbrains.tfsIntegration.ui.servertree.TfsTreeForm;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class AddItemForm implements Disposable {

  private JPanel myContentPane;
  private SelectRevisionForm mySelectRevisionForm;
  private TfsTreeForm myTreeForm;
  private TitledSeparatorWithMnemonic myServerItemSeparator;

  private final EventDispatcher<ChangeListener> myEventDispatcher = EventDispatcher.create(ChangeListener.class);

  public AddItemForm(final Project project, final WorkspaceInfo workspace, final String serverPath) {
    myServerItemSeparator.setLabelFor(myTreeForm.getPreferredFocusedComponent());
    Disposer.register(this, myTreeForm);
    myTreeForm.addListener(new TfsTreeForm.SelectionListener() {
      @Override
      public void selectionChanged() {
        TfsTreeForm.SelectedItem selectedItem = myTreeForm.getSelectedItem();
        if (selectedItem != null) {
          mySelectRevisionForm.init(project, workspace, selectedItem.path, selectedItem.isDirectory);
        }
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
      }
    });

    mySelectRevisionForm.addListener(new SelectRevisionForm.Listener() {
      @Override
      public void revisionChanged() {
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
      }
    });

    myTreeForm.initialize(workspace.getServer(), serverPath, false, false, null);
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public void addListener(ChangeListener listener) {
    myEventDispatcher.addListener(listener);
  }

  @Nullable
  public TfsTreeForm.SelectedItem getServerItem() {
    return myTreeForm.getSelectedItem();
  }

  @Nullable
  public VersionSpecBase getVersion() {
    return mySelectRevisionForm.getVersionSpec();
  }

  public JComponent getPreferredFocusedComponent() {
    return myTreeForm.getPreferredFocusedComponent();
  }

  @Override
  public void dispose() {
  }
}
