package com.microsoft.tf.idea.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.tf.core.controllers.ConnectionsManager;
import com.microsoft.tf.idea.resources.TfPluginBundle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LoginDialog extends DialogWrapper {

    private final LoginPanel loginPanel;
    private final ConnectionsManager connectionsManager;

    public LoginDialog() {
        super(null);
        loginPanel = null;
        connectionsManager = null;
    }

    public LoginDialog(final Project project) {
        super(project);
        loginPanel = new LoginPanel();
        init();
        setTitle(TfPluginBundle.AddVSOAccount);
        connectionsManager = ConnectionsManager.getInstance();
    }

    @Nullable
    @Override
    protected final JComponent createCenterPanel() {
        return loginPanel.getPanel();
    }

    @Nullable
    @Override
    public final JComponent getPreferredFocusedComponent() {
        return loginPanel.getPreferredFocusedComponent();
    }

    @Override
    protected final void doOKAction() {
        //call the controller to validate connection and update the model
        connectionsManager.addConnection(loginPanel.getServerUrl(),
                loginPanel.getAuthenticationType(), loginPanel.getUserName(), loginPanel.getPassword());
        dispose();
    }

    private final void onCancel() {
        dispose();
    }
}
