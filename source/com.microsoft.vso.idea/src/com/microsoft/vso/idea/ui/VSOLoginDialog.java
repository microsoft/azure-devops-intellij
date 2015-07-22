package com.microsoft.vso.idea.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.vso.idea.controllers.VSOConnectionsManager;
import com.microsoft.vso.idea.resources.VSOLoginBundle;
import com.microsoft.vso.idea.utils.VSOConnection;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class VSOLoginDialog extends DialogWrapper implements Observer {

    VSOLoginPanel vsoLoginPanel = null;

    public VSOLoginDialog() {
        super(null);
    }

    public VSOLoginDialog(final Project project) {
        super(project);
        vsoLoginPanel = new VSOLoginPanel();
        init();
        setTitle(VSOLoginBundle.message(VSOLoginBundle.AddVSOAccount));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return vsoLoginPanel.getPanel();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return vsoLoginPanel.getPreferredFocusedComponent();
    }

    @Override
    protected void doOKAction() {
        VSOConnectionsManager connectionsManager = new VSOConnectionsManager();
        connectionsManager.addConnection(vsoLoginPanel.getServerUrl(),
                vsoLoginPanel.getAuthentication(), vsoLoginPanel.getUserName(), vsoLoginPanel.getPassword());
        dispose();
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

    private void onCancel() {
        dispose();
    }
}
