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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.tfsIntegration.core.TFSProjectConfiguration;
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.TfsPanel;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public class UpdateOptionsPanel implements TfsPanel {
  private JPanel myPanel;
  private JRadioButton latestRadioButton;
  private JRadioButton dateRadioButton;
  private JRadioButton changesetRadioButton;
  private JRadioButton labelRadioButton;
  private JRadioButton workspaceRadioButton;
  private TextFieldWithBrowseButton labelVersionText;
  private TextFieldWithBrowseButton changesetVersionText;
  private JTextField dateText;

  private final WorkspaceInfo myWorkspace;
  private final Project myProject;
  private final Collection<ItemPath> myPaths;

  public UpdateOptionsPanel(final WorkspaceInfo workspace, Project project, final Collection<ItemPath> paths) {
    myWorkspace = workspace;
    myProject = project;
    myPaths = paths;

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
        SelectChangesetDialog d = new SelectChangesetDialog(myProject, myWorkspace, myPaths);
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

  public void reset(TFSProjectConfiguration configuration) {
    setVersionSpec(configuration.getUpdateWorkspaceInfo(myWorkspace).getVersion());
  }

  public void apply(TFSProjectConfiguration configuration) throws ConfigurationException {
    configuration.getUpdateWorkspaceInfo(myWorkspace).setVersion(getVersionSpec());
  }

  public boolean canApply() {
    return true;
  }

  public JPanel getPanel() {
    return myPanel;
  }

  public void setDate(Date date) {
    if (date == null) {
      return;
    }
    dateText.setText(SimpleDateFormat.getInstance().format(date));
  }

  private void setVersionSpec(VersionSpecBase version) {
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

  private VersionSpecBase getVersionSpec() throws ConfigurationException {
    if (latestRadioButton.isSelected()) {
      return LatestVersionSpec.INSTANCE;
    }
    else if (changesetRadioButton.isSelected()) {
      try {
        int changeset = Integer.parseInt(changesetVersionText.getText());
        return new ChangesetVersionSpec(changeset);
      }
      catch (NumberFormatException e) {
        throw new ConfigurationException("Incorrect changeset id '" + changesetVersionText.getText() + "'");
      }
    }
    else if (workspaceRadioButton.isSelected()) {
      return new WorkspaceVersionSpec(myWorkspace.getName(), myWorkspace.getOwnerName());
    }
    else if (dateRadioButton.isSelected()) {
      try {
        return new DateVersionSpec(SimpleDateFormat.getInstance().parse(dateText.getText()));
      }
      catch (ParseException e) {
        throw new ConfigurationException("Incorrect date: '" + dateText.getText() + "'");
      }
    }
    else if (labelRadioButton.isSelected()) {
      return LabelVersionSpec.fromStringRepresentation(labelVersionText.getText());
    }

    throw new ConfigurationException("Unknown version spec");
  }

}

