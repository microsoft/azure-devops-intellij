package com.microsoft.tf.idea.ui;

import com.microsoft.tf.idea.resources.TfPluginBundle;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Created by madhurig on 7/21/2015.
 */
public class ConnectToTeamProjectsPanel {

    private JPanel connectionsPanel;

    private JLabel serverLabel;
    private JComboBox serverComboBox;
    private JButton addServerButton;

    private JLabel teamProjectsLabel;
    private JScrollPane teamProjectsScrollPane;
    private JTable teamProjectsTable;

    private final DefaultTableModel teamProjectsTableModel;

    public ConnectToTeamProjectsPanel() {
        teamProjectsTableModel = new DefaultTableModel();
        init();
    }

    private void init() {
        serverComboBox.removeAllItems();

        teamProjectsTableModel.addColumn(TfPluginBundle.TeamProject);
        teamProjectsTableModel.addColumn(TfPluginBundle.ProjectCollection);
        teamProjectsTable.setModel(teamProjectsTableModel);
        teamProjectsTable.setAutoCreateRowSorter(true);
        teamProjectsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public JPanel getPanel() {
        return connectionsPanel;
    }
}
