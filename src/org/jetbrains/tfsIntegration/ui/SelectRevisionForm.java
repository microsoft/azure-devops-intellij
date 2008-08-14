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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public class SelectRevisionForm {

  private static final DateFormat DATEFORMAT = SimpleDateFormat.getInstance();

  private JPanel myPanel;
  private JRadioButton latestRadioButton;
  private JRadioButton dateRadioButton;
  private JRadioButton changesetRadioButton;
  private JRadioButton labelRadioButton;
  private JRadioButton workspaceRadioButton;
  private TextFieldWithBrowseButton labelVersionText;
  private TextFieldWithBrowseButton changesetVersionText;
  private JTextField dateText;

  private WorkspaceInfo myWorkspace;
  private Project myProject;
  private Collection<String> myServerPaths;

  public SelectRevisionForm(final WorkspaceInfo workspace, Project project, final Collection<String> serverPaths) {
    this();
    init(workspace, project, serverPaths);
  }

  public SelectRevisionForm() {
    final ActionListener radioButtonListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateContols();
      }
    };

    latestRadioButton.addActionListener(radioButtonListener);
    dateRadioButton.addActionListener(radioButtonListener);
    changesetRadioButton.addActionListener(radioButtonListener);
    labelRadioButton.addActionListener(radioButtonListener);
    workspaceRadioButton.addActionListener(radioButtonListener);

    changesetVersionText.getButton().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SelectChangesetDialog d = new SelectChangesetDialog(myProject, myWorkspace, myServerPaths);
        d.show();
        if (d.isOK()) {
          changesetVersionText.setText(String.valueOf(d.getChangeset()));
        }
      }
    });

    labelVersionText.getButton().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SelectLabelDialog d = new SelectLabelDialog(myProject, myWorkspace);
        d.show();
        if (d.isOK()) {
          labelVersionText.setText(d.getLabelString());
        }
      }
    });

    latestRadioButton.setSelected(true);
  }

  public void init(final WorkspaceInfo workspace, Project project, final Collection<String> serverPaths) {
    myWorkspace = workspace;
    myProject = project;
    myServerPaths = serverPaths;
  }

  private void updateContols() {
    dateText.setEnabled(dateRadioButton.isSelected());
    if (!dateRadioButton.isSelected()) {
      dateText.setText(null);
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

  private void setDate(Date date) {
    if (date == null) {
      return;
    }
    dateText.setText(DATEFORMAT.format(date));
  }

  public void setVersionSpec(VersionSpecBase version) {
    if (version instanceof LatestVersionSpec) {
      latestRadioButton.setSelected(true);
    }
    else if (version instanceof ChangesetVersionSpec) {
      int changeset = ((ChangesetVersionSpec)version).getChangeSetId();
      changesetRadioButton.setSelected(true);
      changesetVersionText.setEnabled(true);
      changesetVersionText.setText(String.valueOf(changeset));
    }
    else if (version instanceof WorkspaceVersionSpec) {
      workspaceRadioButton.setSelected(true);
    }
    else if (version instanceof DateVersionSpec) {
      Date date = ((DateVersionSpec)version).getDate();
      dateRadioButton.setSelected(true);
      dateText.setEnabled(true);
      setDate(date);
    }
    else if (version instanceof LabelVersionSpec) {
      labelRadioButton.setSelected(true);
      labelVersionText.setEnabled(true);
      labelVersionText.setText(((LabelVersionSpec)version).getStringRepresentation());
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
      return new WorkspaceVersionSpec(myWorkspace.getName(), myWorkspace.getOwnerName());
    }
    else if (dateRadioButton.isSelected()) {
      try {
        return new DateVersionSpec(DATEFORMAT.parse(dateText.getText()));
      }
      catch (ParseException e) {
        return null;
      }
    }
    else if (labelRadioButton.isSelected()) {
      return LabelVersionSpec.fromStringRepresentation(labelVersionText.getText());
    }

    return null;
  }

}

