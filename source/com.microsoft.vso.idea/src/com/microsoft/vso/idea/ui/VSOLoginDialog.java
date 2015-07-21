package com.microsoft.vso.idea.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.vso.idea.resources.VSOLoginBundle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;

public class VSOLoginDialog extends DialogWrapper {
    final VSOLoginPanel vsoLoginPanel;

    public VSOLoginDialog(final Project project) {
        super(project);
        vsoLoginPanel = new VSOLoginPanel();
        init();
        setTitle(VSOLoginBundle.message("AddVSOAccount"));
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

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
