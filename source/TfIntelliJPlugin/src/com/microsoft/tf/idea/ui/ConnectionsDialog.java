package com.microsoft.tf.idea.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.tf.idea.resources.TfPluginBundle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by madhurig on 7/21/2015.
 */
public class ConnectionsDialog extends DialogWrapper implements Observer {

    private final ConnectToTeamProjectsPanel connectToTeamProjectsPanel;

    public ConnectionsDialog() {
        super(null);
        connectToTeamProjectsPanel = null;
    }

    public ConnectionsDialog(final Project project) {
        super(project);
        connectToTeamProjectsPanel = new ConnectToTeamProjectsPanel();
        init();
        setTitle(TfPluginBundle.ConnectToTeamProject);
    }

    @Nullable
    @Override
    protected final JComponent createCenterPanel() {
        return connectToTeamProjectsPanel.getPanel();
    }

    @Override
    public final void update(Observable o, Object arg) {
        if(this.isVisible()) {
            //model has changed, so query controller for data needed to re-load view
        }
    }
}
