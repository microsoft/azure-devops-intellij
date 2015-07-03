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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MultiLineLabelUI;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import icons.TFSIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.checkin.CheckinParameters;
import org.jetbrains.tfsIntegration.checkin.CheckinPoliciesManager;
import org.jetbrains.tfsIntegration.checkin.NotInstalledPolicyFailure;
import org.jetbrains.tfsIntegration.checkin.PolicyFailure;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class CheckinParametersForm {

  private static final Color REQUIRED_BACKGROUND_COLOR = new Color(252, 248, 199);
  private static final Color NOT_INSTALLED_POLICY_COLOR = UIUtil.getInactiveTextColor();
  private static final Icon WARNING_ICON = UIUtil.getBalloonWarningIcon();
  private static final Icon ERROR_ICON = UIUtil.getBalloonErrorIcon();
  private static final Icon EMPTY_ICON = new EmptyIcon(0, WARNING_ICON.getIconHeight());

  private JPanel myContentPane;
  private JComboBox myServersCombo;
  private JPanel myServerChooserPanel;
  private JTabbedPane myTabbedPane;
  private JPanel myNotesPanel;
  private JPanel myCheckinNotesTab;
  private JLabel myErrorLabel;
  private JPanel myWorkItemsTab;
  private JPanel myPoliciesTab;
  private TableView<PolicyFailure> myWarningsTable;
  private JButton myEvaluateButton;
  private WorkItemsPanel myWorkItemsPanel;

  private final CheckinParameters myState;
  private final Project myProject;

  private static final MultiLineTableRenderer WARNING_TABLE_RENDERER = new MultiLineTableRenderer() {

    protected void customize(JTable table, JTextArea textArea, boolean isSelected, Object value) {
      PolicyFailure failure = (PolicyFailure)value;
      textArea.setText(failure.getMessage());
      final String tooltip = failure.getTooltipText();
      textArea.setToolTipText(StringUtil.isNotEmpty(tooltip) ? tooltip : null);

      final Color foreground;
      if (isSelected) {
        foreground = table.getSelectionForeground();
      }
      else {
        foreground = failure instanceof NotInstalledPolicyFailure ? NOT_INSTALLED_POLICY_COLOR : table.getForeground();
      }
      textArea.setForeground(foreground);
    }
  };

  public static final ColumnInfo<PolicyFailure, PolicyFailure> WARNING_COLUMN_INFO =
    new ColumnInfo<PolicyFailure, PolicyFailure>("message") {
      public PolicyFailure valueOf(PolicyFailure policyFailure) {
        return policyFailure;
      }

      @Override
      public TableCellRenderer getRenderer(final PolicyFailure policyFailure) {
        return WARNING_TABLE_RENDERER;
      }
    };


  public CheckinParametersForm(CheckinParameters state, Project project) {
    myProject = project;
    myState = state;

    myServersCombo.setRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(final JList list,
                                                    final Object value,
                                                    final int index,
                                                    final boolean isSelected,
                                                    final boolean cellHasFocus) {
        JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof ServerInfo) {
          label.setText(((ServerInfo)value).getPresentableUri());
        }
        label.setIcon(TFSIcons.Teamserver);
        return label;
      }
    });

    myServersCombo.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        myWorkItemsPanel.update();
        udpateCheckinNotes();
        updatePoliciesWarnings();
        updateErrorMessage(false);
      }
    });

    myWarningsTable.setModelAndUpdateColumns(new ListTableModel<PolicyFailure>(WARNING_COLUMN_INFO));

    myWarningsTable.setTableHeader(null);
    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        final PolicyFailure failure = myWarningsTable.getSelectedObject();
        if (failure != null) {
          failure.activate(myProject);
        }
        return true;
      }
    }.installOn(myWarningsTable);


    myEvaluateButton.setEnabled(myState.evaluationEnabled() && myState.getPoliciesLoadError() == null);

    myEvaluateButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        evaluatePolicies();
      }
    });


    myServerChooserPanel.setVisible(myState.getServers().size() > 1);

    myServersCombo.setModel(new DefaultComboBoxModel(myState.getServers().toArray()));

    final Pair<ServerInfo, ? extends Component> pair = getInitialSelectedTab();

    myServersCombo.setSelectedItem(pair.first);
    myTabbedPane.setSelectedIndex(myTabbedPane.indexOfComponent(pair.second));
  }

  public CheckinParameters getState() {
    return myState;
  }

  public Project getProject() {
    return myProject;
  }

  private void evaluatePolicies() {
    boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      public void run() {
        ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
        pi.setIndeterminate(true);
        myState.evaluatePolicies(pi);
      }
    }, "Evaluating Checkin Policies", true, myProject);
    if (completed) {
      updatePoliciesWarnings();
      updateErrorMessage(false);
    }
  }

  private void updatePoliciesWarnings() {
    List<PolicyFailure> failures = new ArrayList<PolicyFailure>();
    if (!myState.evaluationEnabled()) {
      failures.add(new PolicyFailure(CheckinPoliciesManager.DUMMY_POLICY, "Evaluation of checkin policies was disabled",
                                     "Use Project Settings | TFS configuration settings to enable checkin policies evaluation"));
    }
    else if (myState.getPoliciesLoadError() != null) {
      failures.add(
        new PolicyFailure(CheckinPoliciesManager.DUMMY_POLICY, "Cannot load checkin policies definitions", myState.getPoliciesLoadError()));
    }
    else if (!myState.policiesEvaluated()) {
      failures.add(new PolicyFailure(CheckinPoliciesManager.DUMMY_POLICY, "Checkin policies were not evaluated") {
        @Override
        public void activate(@NotNull Project project) {
          evaluatePolicies();
        }
      });
    }
    else if (myState.getFailures(getSelectedServer()).isEmpty()) {
      failures.add(new PolicyFailure(CheckinPoliciesManager.DUMMY_POLICY, "All checkin policies are satisfied") {
        @Override
        public void activate(@NotNull Project project) {
        }
      });
    }
    else {
      for (PolicyFailure failure : myState.getFailures(getSelectedServer())) {
        failures.add(failure);
      }
    }
    //noinspection unchecked
    ((ListTableModel)myWarningsTable.getModel()).setItems(failures);
  }

  private Pair<ServerInfo, ? extends Component> getInitialSelectedTab() {
    for (ServerInfo server : myState.getServers()) {
      if (myState.hasEmptyNotes(server)) {
        return Pair.create(server, myCheckinNotesTab);
      }
    }

    for (ServerInfo server : myState.getServers()) {
      if (myState.hasPolicyFailures(server)) {
        return Pair.create(server, myPoliciesTab);
      }
    }
    return Pair.create(myState.getServers().get(0), myWorkItemsTab);
  }

  private void createUIComponents() {
    myWorkItemsPanel = new WorkItemsPanel(this);
    // TODO until MultiLineLabel is moved to openapi
    myErrorLabel = new JLabel() {
      public void updateUI() {
        setUI(new MultiLineLabelUI());
      }

      public Dimension getMinimumSize() {
        return getPreferredSize();
      }
    };
    myErrorLabel.setVerticalTextPosition(SwingConstants.TOP);
  }

  public ServerInfo getSelectedServer() {
    return (ServerInfo)myServersCombo.getSelectedItem();
  }

  public JComponent getContentPane() {
    return myContentPane;
  }

  private void udpateCheckinNotes() {
    myNotesPanel.removeAll();

    final List<CheckinParameters.CheckinNote> notes = myState.getCheckinNotes(getSelectedServer());
    final Insets labelInsets = new Insets(0, 0, 5, 0);
    final Insets fieldInsets = new Insets(0, 20, 10, 0);
    int i = 0;
    for (final CheckinParameters.CheckinNote note : notes) {
      String text = note.name + ":";
      if (note.required) {
        text = "<html><b>" + text + "</b></html>";
      }
      JLabel label = new JLabel(text);
      GridBagConstraints c =
        new GridBagConstraints(0, i++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets, 0, 0);
      myNotesPanel.add(label, c);
      final JTextField field = new JTextField(note.value);
      if (note.required) {
        field.setBackground(REQUIRED_BACKGROUND_COLOR);
      }
      c = new GridBagConstraints(0, i++, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, fieldInsets, 0, 0);
      myNotesPanel.add(field, c);

      field.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(DocumentEvent e) {
          note.value = field.getText();
          updateErrorMessage(true);
        }
      });
    }
    myNotesPanel.add(new JPanel(),
                     new GridBagConstraints(0, i, 1, 1, 1, 10, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, labelInsets, 0, 0));
  }

  public void updateErrorMessage(boolean evaluateNotes) {
    if (evaluateNotes) {
      myState.validateNotes();
    }

    Icon icon = myState.hasPolicyFailures(getSelectedServer()) ? WARNING_ICON : EMPTY_ICON;
    myTabbedPane.setIconAt(myTabbedPane.indexOfComponent(myPoliciesTab), icon);
    icon = myState.hasEmptyNotes(getSelectedServer()) ? ERROR_ICON : EMPTY_ICON;
    myTabbedPane.setIconAt(myTabbedPane.indexOfComponent(myCheckinNotesTab), icon);

    @Nullable Pair<String, CheckinParameters.Severity> message = myState.getValidationMessage(CheckinParameters.Severity.BOTH);
    myErrorLabel.setText(message != null ? message.first : null);
    myErrorLabel.setIcon(message == null ? null : message.second == CheckinParameters.Severity.ERROR ? ERROR_ICON : WARNING_ICON);
  }
}
