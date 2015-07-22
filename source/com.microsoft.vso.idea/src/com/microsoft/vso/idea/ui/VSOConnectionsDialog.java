package com.microsoft.vso.idea.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.vso.idea.resources.VSOLoginBundle;
import com.microsoft.vso.idea.utils.VSOConnection;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Observable;

/**
 * Created by madhurig on 7/21/2015.
 */
public class VSOConnectionsDialog extends DialogWrapper implements java.util.Observer {

    private ConnectToTeamProjectsPanel connectToTeamProjectsPanel;

    public VSOConnectionsDialog() {
        super(null);
    }

    public VSOConnectionsDialog(final Project project) {
        super(project);
        connectToTeamProjectsPanel = new ConnectToTeamProjectsPanel();
        init();
        setTitle("Visual Studio Online Accounts/Team Foundation Servers");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return connectToTeamProjectsPanel.getPanel();
    }

    @Override
    public void update(Observable o, Object arg) {
        if(this.isVisible()) {
            //arg has connection data from model
            if(arg != null) {
                List<VSOConnection> connectionsList = (List<VSOConnection>)arg;
            }
        }
    }
}
