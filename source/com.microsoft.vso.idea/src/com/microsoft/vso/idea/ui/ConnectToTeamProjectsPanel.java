package com.microsoft.vso.idea.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    private DefaultTableModel teamProjectsTableModel;

    public ConnectToTeamProjectsPanel() {
        init();
    }

    protected void init() {
        serverComboBox.removeAllItems();

        teamProjectsTableModel = new DefaultTableModel();
        teamProjectsTableModel.addColumn("Team Project");
        teamProjectsTableModel.addColumn("Project Collection");
        teamProjectsTable.setModel(teamProjectsTableModel);
        teamProjectsTable.setAutoCreateRowSorter(true);
        teamProjectsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public JPanel getPanel() {
        return connectionsPanel;
    }
}
