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
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventListener;

public class SelectRevisionForm {

  public interface Listener extends EventListener {
    void revisionChanged();
  }

  private static final DateFormat DATE_FORMAT = SimpleDateFormat.getInstance();

  private JPanel myPanel;
  private JRadioButton latestRadioButton;
  private JRadioButton dateRadioButton;
  private JRadioButton changesetRadioButton;
  private JRadioButton labelRadioButton;
  private JRadioButton workspaceRadioButton;
  private TextFieldWithBrowseButton.NoPathCompletion labelVersionText;
  private TextFieldWithBrowseButton.NoPathCompletion changesetVersionText;
  private JTextField dateText;
  private JTextField workspaceText;

  private WorkspaceInfo myWorkspace;
  private Project myProject;
  private String myServerPath;
  private boolean myIsDirectory;

  private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);

  public SelectRevisionForm(Project project, final WorkspaceInfo workspace, final String serverPath, final boolean isDirectory) {
    this();
    init(project, workspace, serverPath, isDirectory);
  }

  public SelectRevisionForm() {
    final DocumentListener documentListener = new DocumentAdapter() {
      protected void textChanged(final DocumentEvent e1) {
        myEventDispatcher.getMulticaster().revisionChanged();
      }
    };

    labelVersionText.getTextField().getDocument().addDocumentListener(documentListener);
    changesetVersionText.getTextField().getDocument().addDocumentListener(documentListener);
    dateText.getDocument().addDocumentListener(documentListener);
    workspaceText.getDocument().addDocumentListener(documentListener);

    final ActionListener radioButtonListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateContols();
        myEventDispatcher.getMulticaster().revisionChanged();
      }
    };

    latestRadioButton.addActionListener(radioButtonListener);
    dateRadioButton.addActionListener(radioButtonListener);
    changesetRadioButton.addActionListener(radioButtonListener);
    labelRadioButton.addActionListener(radioButtonListener);
    workspaceRadioButton.addActionListener(radioButtonListener);

    changesetVersionText.getButton().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SelectChangesetDialog d = new SelectChangesetDialog(myProject, myWorkspace, myServerPath, myIsDirectory);
        if (d.showAndGet()) {
          changesetVersionText.setText(String.valueOf(d.getChangeset()));
        }
      }
    });

    labelVersionText.getButton().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SelectLabelDialog d = new SelectLabelDialog(myProject, myWorkspace);
        if (d.showAndGet()) {
          labelVersionText.setText(d.getLabelString());
        }
      }
    });

    latestRadioButton.setSelected(true);
  }

  public void init(Project project, final WorkspaceInfo workspace, final String serverPath, final boolean isDirectory) {
    myWorkspace = workspace;
    myProject = project;
    myServerPath = serverPath;
    myIsDirectory = isDirectory;
  }

  private void updateContols() {
    workspaceText.setEnabled(workspaceRadioButton.isSelected());
    if (!workspaceRadioButton.isSelected()) {
      workspaceText.setText(null);
    }
    else {
      workspaceText.setText(myWorkspace.getName() + ';' + TfsUtil.getNameWithoutDomain(myWorkspace.getOwnerName()));
    }

    dateText.setEnabled(dateRadioButton.isSelected());
    if (!dateRadioButton.isSelected()) {
      dateText.setText(null);
    }
    else {
      dateText.setText(DATE_FORMAT.format(new Date()));
    }
    labelVersionText.setEnabled(labelRadioButton.isSelected());
    if (!labelRadioButton.isSelected()) {
      labelVersionText.setText(null);
    }
    changesetVersionText.setEnabled(changesetRadioButton.isSelected());
    if (!changesetRadioButton.isSelected()) {
      changesetVersionText.setText(null);
    }
  }

  public JPanel getPanel() {
    return myPanel;
  }

  public void setVersionSpec(VersionSpecBase version) {
    latestRadioButton.setEnabled(true);
    dateRadioButton.setEnabled(true);
    changesetRadioButton.setEnabled(true);
    labelRadioButton.setEnabled(true);
    workspaceRadioButton.setEnabled(true);


    if (version instanceof LatestVersionSpec) {
      latestRadioButton.setSelected(true);
    }
    else if (version instanceof ChangesetVersionSpec) {
      changesetRadioButton.setSelected(true);
      changesetVersionText.setEnabled(true);
      changesetVersionText.setText(version.getPresentableString());
    }
    else if (version instanceof WorkspaceVersionSpec) {
      workspaceRadioButton.setSelected(true);
      workspaceText.setEnabled(true);
      workspaceText.setText(version.getPresentableString());
    }
    else if (version instanceof DateVersionSpec) {
      dateRadioButton.setSelected(true);
      dateText.setEnabled(true);
      dateText.setText(version.getPresentableString());
    }
    else if (version instanceof LabelVersionSpec) {
      labelRadioButton.setSelected(true);
      labelVersionText.setEnabled(true);
      labelVersionText.setText(version.getPresentableString());
    }
    updateContols();
  }

  @Nullable
  public VersionSpecBase getVersionSpec() {
    if (latestRadioButton.isSelected()) {
      return LatestVersionSpec.INSTANCE;
    }
    else if (changesetRadioButton.isSelected()) {
      try {
        int changeset = Integer.parseInt(changesetVersionText.getText());
        return new ChangesetVersionSpec(changeset);
      }
      catch (NumberFormatException e) {
        return null;
      }
    }
    else if (workspaceRadioButton.isSelected()) {
      return parseWorkspaceVersionSpec(workspaceText.getText());
    }
    else if (dateRadioButton.isSelected()) {
      try {
        return new DateVersionSpec(DATE_FORMAT.parse(dateText.getText()));
      }
      catch (ParseException e) {
        return null;
      }
    }
    else if (labelRadioButton.isSelected()) {
      if (labelVersionText.getText().trim().length() > 0) {
        return LabelVersionSpec.fromStringRepresentation(labelVersionText.getText());
      }
      else {
        return null;
      }
    }

    return null;
  }

  @Nullable
  private WorkspaceVersionSpec parseWorkspaceVersionSpec(final String versionSpec) {
    if (versionSpec == null || versionSpec.length() == 0 || versionSpec.charAt(0) == ';') {
      return null;
    }

    int semicolonIndex = versionSpec.indexOf(';');
    String workspaceName = semicolonIndex < 0 ? versionSpec : versionSpec.substring(0, semicolonIndex);
    if (!WorkspaceInfo.isValidName(workspaceName)) {
      return null;
    }

    String ownerName = semicolonIndex < 0 || semicolonIndex == versionSpec.length() - 1
                       ? TfsUtil.getNameWithoutDomain(myWorkspace.getOwnerName())
                       : versionSpec.substring(semicolonIndex + 1);
    // remove spaces from the end
    int newLength = ownerName.length();
    while (newLength > 0 && ownerName.charAt(newLength - 1) == ' ') {
      newLength--;
    }

    if (newLength == 0) {
      ownerName = TfsUtil.getNameWithoutDomain(myWorkspace.getOwnerName());
    }
    else if (newLength < ownerName.length()) {
      ownerName = ownerName.substring(0, newLength);
    }

    return new WorkspaceVersionSpec(workspaceName, ownerName);
  }

  public void disable() {
    latestRadioButton.setSelected(true);

    latestRadioButton.setEnabled(false);
    dateRadioButton.setEnabled(false);
    changesetRadioButton.setEnabled(false);
    labelRadioButton.setEnabled(false);
    workspaceRadioButton.setEnabled(false);

    updateContols();
  }

  public void addListener(Listener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeListener(Listener listener) {
    myEventDispatcher.removeListener(listener);
  }

}

