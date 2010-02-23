/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.tfsIntegration.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.table.TableView;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.checkin.*;
import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.configuration.TfsCheckinPoliciesCompatibility;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class CheckInPoliciesForm {
  private final ColumnInfo[] COLUMNS = new ColumnInfo[]{

    new ColumnInfo<Pair<StatefulPolicyDescriptor, Boolean>, Boolean>("Enabled") {
      public Boolean valueOf(Pair<StatefulPolicyDescriptor, Boolean> item) {
        return item.first.isEnabled();
      }

      @Override
      public Class getColumnClass() {
        return Boolean.class;
      }

      @Override
      public int getWidth(JTable table) {
        return 60;
      }

      @Override
      public boolean isCellEditable(Pair<StatefulPolicyDescriptor, Boolean> item) {
        return true;
      }

      @Override
      public void setValue(Pair<StatefulPolicyDescriptor, Boolean> item, Boolean value) {
        item.first.setEnabled(value);
        myProjectToDescriptors.get(getSelectedProject()).isModified = true;
      }
    },

    new ColumnInfo<Pair<StatefulPolicyDescriptor, Boolean>, Pair<StatefulPolicyDescriptor, Boolean>>("Policy Type") {
      @Override
      public Pair<StatefulPolicyDescriptor, Boolean> valueOf(Pair<StatefulPolicyDescriptor, Boolean> item) {
        return item;
      }

      @Override
      public TableCellRenderer getRenderer(Pair<StatefulPolicyDescriptor, Boolean> item) {
        return NAME_RENDERER;
      }
    },

    new ColumnInfo<Pair<StatefulPolicyDescriptor, Boolean>, Pair<StatefulPolicyDescriptor, Boolean>>("Description") {
      @Override
      public Pair<StatefulPolicyDescriptor, Boolean> valueOf(Pair<StatefulPolicyDescriptor, Boolean> item) {
        return item;
      }

      @Override
      public TableCellRenderer getRenderer(Pair<StatefulPolicyDescriptor, Boolean> item) {
        return DESCRIPTION_RENDERER;
      }
    }};

  private static class ModifyableProjectEntry extends ManageWorkspacesForm.ProjectEntry {
    public boolean isModified = false;

    public ModifyableProjectEntry(ManageWorkspacesForm.ProjectEntry entry) {
      super(new ArrayList<StatefulPolicyDescriptor>(entry.descriptors), entry.policiesCompatibilityOverride);
    }
  }

  private JComboBox myProjectCombo;
  private JButton myAddButton;
  private JButton myEditButton;
  private TableView<Pair<StatefulPolicyDescriptor, Boolean>> myPoliciesTable;
  private JButton myRemoveButton;
  private JPanel myContentPane;
  private JCheckBox myTeampriseCheckBox;
  private JCheckBox myNonInstalledPoliciesCheckBox;
  private JCheckBox myTeamExplorerCheckBox;
  private JCheckBox myOverrideCheckBox;

  private final Project myProject;
  private final Map<String, ModifyableProjectEntry> myProjectToDescriptors;

  public CheckInPoliciesForm(Project project, Map<String, ManageWorkspacesForm.ProjectEntry> projectToDescriptors) {
    myProject = project;
    myProjectToDescriptors = new HashMap<String, ModifyableProjectEntry>(projectToDescriptors.size());
    for (Map.Entry<String, ManageWorkspacesForm.ProjectEntry> e : projectToDescriptors.entrySet()) {
      myProjectToDescriptors.put(e.getKey(), new ModifyableProjectEntry(new ModifyableProjectEntry(e.getValue())));
    }

    myProjectCombo.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateTable();
        updateCheckboxes();
      }
    });

    myPoliciesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        updateButtons();
      }
    });

    List<String> projects = new ArrayList<String>(myProjectToDescriptors.keySet());
    Collections.sort(projects, new Comparator<String>() {
      public int compare(String s1, String s2) {
        return s1.compareTo(s2);
      }
    });

    myProjectCombo.setModel(new DefaultComboBoxModel(ArrayUtil.toStringArray(projects)));
    myProjectCombo.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel component = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        String path = (String)value;
        component.setText(VersionControlPath.getTeamProject(path));
        return component;
      }
    });

    myPoliciesTable.setModel(new ListTableModel<Pair<StatefulPolicyDescriptor, Boolean>>(COLUMNS));

    myEditButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final StatefulPolicyDescriptor descriptor = getSelectedDescriptor();
        editPolicy(descriptor);
      }
    });

    myRemoveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final StatefulPolicyDescriptor descriptor = getSelectedDescriptor();
        final String message = MessageFormat.format("Are you sure to remove checkin policy ''{0}''?", descriptor.getType().getName());
        if (Messages.showOkCancelDialog(myProject, message, "Remove Checkin Policy", Messages.getQuestionIcon()) == 0) {
          final ModifyableProjectEntry projectEntry = myProjectToDescriptors.get(getSelectedProject());
          projectEntry.descriptors.remove(descriptor);
          projectEntry.isModified = true;
          updateTable();
          updateButtons();
        }
      }
    });

    myAddButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final ModifyableProjectEntry projectEntry = myProjectToDescriptors.get(getSelectedProject());

        List<PolicyBase> policies = new ArrayList<PolicyBase>();
        try {
          // do not allow to add the same unconfigurable policy several times
          main_loop:
          for (PolicyBase installed : CheckinPoliciesManager.getInstalledPolicies()) {
            if (!installed.canEdit()) {
              for (StatefulPolicyDescriptor descriptor : projectEntry.descriptors) {
                if (descriptor.getType().equals(installed.getPolicyType())) {
                  continue main_loop;
                }
              }
            }
            policies.add(installed);
          }
        }
        catch (DuplicatePolicyIdException ex) {
          final String message = MessageFormat
            .format("Several checkin policies with the same id found: ''{0}''.\nPlease review your extensions.", ex.getDuplicateId());
          Messages.showErrorDialog(myProject, message, "Add Checkin Policy");
          return;
        }

        ChooseCheckinPolicyDialog d = new ChooseCheckinPolicyDialog(myProject, policies);
        d.show();
        if (!d.isOK()) {
          return;
        }

        PolicyBase policy = d.getSelectedPolicy();
        StatefulPolicyDescriptor newDescriptor =
          new StatefulPolicyDescriptor(policy.getPolicyType(), true, StatefulPolicyParser.createEmptyConfiguration(),
                                       Collections.<String>emptyList(), StatefulPolicyDescriptor.DEFAULT_PRIORITY, null);

        if (!editPolicy(newDescriptor)) {
          return;
        }

        projectEntry.descriptors.add(newDescriptor);
        projectEntry.isModified = true;
        updateTable();
        int index = projectEntry.descriptors.size() - 1;
        myPoliciesTable.getSelectionModel().setSelectionInterval(index, index);
        updateButtons();
      }
    });

    myPoliciesTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          final StatefulPolicyDescriptor descriptor = getSelectedDescriptor();
          if (descriptor != null) {
            editPolicy(descriptor);
          }
        }
      }
    });

    myTeampriseCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ModifyableProjectEntry entry = myProjectToDescriptors.get(getSelectedProject());
        entry.policiesCompatibilityOverride.teamprise = myTeampriseCheckBox.isSelected();
        entry.isModified = true;
      }
    });

    myTeamExplorerCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ModifyableProjectEntry entry = myProjectToDescriptors.get(getSelectedProject());
        entry.policiesCompatibilityOverride.teamExplorer = myTeamExplorerCheckBox.isSelected();
        entry.isModified = true;
      }
    });

    myNonInstalledPoliciesCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ModifyableProjectEntry entry = myProjectToDescriptors.get(getSelectedProject());
        entry.policiesCompatibilityOverride.nonInstalled = myNonInstalledPoliciesCheckBox.isSelected();
        entry.isModified = true;
      }
    });

    myOverrideCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ModifyableProjectEntry entry = myProjectToDescriptors.get(getSelectedProject());
        if (myOverrideCheckBox.isSelected()) {
          entry.policiesCompatibilityOverride = new TfsCheckinPoliciesCompatibility(false, false, false);
        }
        else {
          entry.policiesCompatibilityOverride = null;
        }
        updateCheckboxes();
        entry.isModified = true;
      }
    });

    ActionListener l = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean b = myTeamExplorerCheckBox.isSelected() || myTeampriseCheckBox.isSelected();
        myNonInstalledPoliciesCheckBox.setEnabled(b);
        if (!b) {
          myNonInstalledPoliciesCheckBox.setSelected(false);
          myProjectToDescriptors.get(getSelectedProject()).policiesCompatibilityOverride.nonInstalled = false;
        }
      }
    };
    myTeamExplorerCheckBox.addActionListener(l);
    myTeampriseCheckBox.addActionListener(l);

    updateTable();
    updateCheckboxes();
    updateButtons();
  }

  private void updateCheckboxes() {
    ModifyableProjectEntry entry = myProjectToDescriptors.get(getSelectedProject());
    myOverrideCheckBox
      .setText(TFSBundle.message("override.policies.compatibility.checkbox", VersionControlPath.getTeamProject(getSelectedProject())));
    myOverrideCheckBox.setSelected(entry.policiesCompatibilityOverride != null);
    updateSlaveCheckboxes(entry.policiesCompatibilityOverride);
  }

  private void updateSlaveCheckboxes(@Nullable TfsCheckinPoliciesCompatibility override) {
    if (override != null) {
      myTeampriseCheckBox.setEnabled(true);
      myTeampriseCheckBox.setSelected(override.teamprise);
      myTeamExplorerCheckBox.setEnabled(true);
      myTeamExplorerCheckBox.setSelected(override.teamExplorer);
      myNonInstalledPoliciesCheckBox.setSelected(override.nonInstalled);
      myNonInstalledPoliciesCheckBox.setEnabled(myTeamExplorerCheckBox.isSelected() || myTeampriseCheckBox.isSelected());
    }
    else {
      myOverrideCheckBox.setSelected(false);
      myTeampriseCheckBox.setSelected(false);
      myTeampriseCheckBox.setEnabled(false);
      myTeamExplorerCheckBox.setSelected(false);
      myTeamExplorerCheckBox.setEnabled(false);
      myNonInstalledPoliciesCheckBox.setSelected(false);
      myNonInstalledPoliciesCheckBox.setEnabled(false);
    }
  }

  private void updateButtons() {
    StatefulPolicyDescriptor descriptor = getSelectedDescriptor();
    if (descriptor == null) {
      myEditButton.setEnabled(false);
      myRemoveButton.setEnabled(false);
    }
    else {
      try {
        PolicyBase policy = CheckinPoliciesManager.find(descriptor.getType());
        myEditButton.setEnabled(policy != null && canEditSafe(policy));
        myRemoveButton.setEnabled(true);
      }
      catch (DuplicatePolicyIdException e) {
        // can't get here
        throw new RuntimeException(e);
      }
    }
  }

  private void updateTable() {
    //noinspection unchecked
    List<Pair<StatefulPolicyDescriptor, Boolean>> list =
      new ArrayList<Pair<StatefulPolicyDescriptor, Boolean>>(myProjectToDescriptors.get(getSelectedProject()).descriptors.size());
    try {
      for (StatefulPolicyDescriptor descriptor : myProjectToDescriptors.get(getSelectedProject()).descriptors) {
        list.add(Pair.create(descriptor, CheckinPoliciesManager.find(descriptor.getType()) != null));
      }
    }
    catch (DuplicatePolicyIdException e) {
      // can't get here
      throw new RuntimeException(e);
    }
    //noinspection unchecked
    ((ListTableModel)myPoliciesTable.getModel()).setItems(list);
  }

  private String getSelectedProject() {
    return (String)myProjectCombo.getSelectedItem();
  }

  @Nullable
  private StatefulPolicyDescriptor getSelectedDescriptor() {
    final Pair<StatefulPolicyDescriptor, Boolean> selected = myPoliciesTable.getSelectedObject();
    return selected != null ? selected.first : null;
  }

  public JComponent getContentPane() {
    return myContentPane;
  }

  private boolean editPolicy(StatefulPolicyDescriptor descriptor) {
    PolicyBase policy;
    try {
      policy = CheckinPoliciesManager.find(descriptor.getType());
    }
    catch (DuplicatePolicyIdException e) {
      // can't get here
      throw new RuntimeException(e);
    }

    if (policy == null) {
      return false;
    }
    if (!canEditSafe(policy)) {
      return true;
    }

    try {
      policy.loadState((Element)descriptor.getConfiguration().clone());
    }
    catch (Throwable t) {
      String message =
        MessageFormat.format("Cannot load state of checkin policy ''{0}'':\n{1}", descriptor.getType().getName(), t.getMessage());
      Messages.showErrorDialog(myProject, message, "Edit Checkin Policy");
      return false;
    }

    boolean result;
    try {
      result = policy.edit(myProject);
    }
    catch (Throwable t) {
      String message = MessageFormat.format("Cannot edit checkin policy ''{0}'':\n{1}", descriptor.getType().getName(), t.getMessage());
      Messages.showErrorDialog(myProject, message, "Edit Checkin Policy");
      return false;
    }

    if (result) {
      Element configurationElement = StatefulPolicyParser.createEmptyConfiguration();
      try {
        policy.saveState(configurationElement);
        descriptor.setConfiguration(configurationElement);
        myProjectToDescriptors.get(getSelectedProject()).isModified = true;
      }
      catch (Throwable t) {
        String message =
          MessageFormat.format("Cannot save state of checkin policy ''{0}'':\n{1}", descriptor.getType().getName(), t.getMessage());
        Messages.showErrorDialog(myProject, message, "Edit Checkin Policy");
      }
    }
    return result;
  }

  private static boolean canEditSafe(PolicyBase policy) {
    try {
      return policy.canEdit();
    }
    catch (Throwable t) {
      TFSVcs.LOG.warn(t);
      return false;
    }
  }

  private static abstract class MyRenderer extends DefaultTableCellRenderer {
    private static final Color NOT_INSTALLED_POLICY_COLOR = UIUtil.getInactiveTextColor();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JLabel component = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      @SuppressWarnings({"unchecked"}) Pair<StatefulPolicyDescriptor, Boolean> item = (Pair<StatefulPolicyDescriptor, Boolean>)value;
      component.setText(getValue(item.first));
      final Color foreground;
      if (isSelected) {
        foreground = table.getSelectionForeground();
      }
      else {
        foreground = item.second ? table.getForeground() : NOT_INSTALLED_POLICY_COLOR;
      }
      component.setForeground(foreground);
      return component;
    }

    protected abstract String getValue(StatefulPolicyDescriptor descriptor);
  }

  private static final MyRenderer NAME_RENDERER = new MyRenderer() {
    @Override
    protected String getValue(StatefulPolicyDescriptor descriptor) {
      return descriptor.getType().getName();
    }
  };

  private static final MyRenderer DESCRIPTION_RENDERER = new MyRenderer() {
    @Override
    protected String getValue(StatefulPolicyDescriptor descriptor) {
      return descriptor.getType().getDescription();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JLabel component = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      @SuppressWarnings({"unchecked"}) Pair<StatefulPolicyDescriptor, Boolean> item = (Pair<StatefulPolicyDescriptor, Boolean>)value;
      component.setToolTipText(getValue(item.first));
      return component;
    }
  };

  public Map<String, ManageWorkspacesForm.ProjectEntry> getModifications() {
    Map<String, ManageWorkspacesForm.ProjectEntry> result = new HashMap<String, ManageWorkspacesForm.ProjectEntry>();
    for (Map.Entry<String, ModifyableProjectEntry> entry : myProjectToDescriptors.entrySet()) {
      if (entry.getValue().isModified) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }


}
