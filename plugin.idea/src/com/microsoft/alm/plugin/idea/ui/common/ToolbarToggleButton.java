// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.project.DumbAware;

import javax.swing.JComponent;
import java.awt.event.ActionListener;

public class ToolbarToggleButton extends CheckboxAction implements DumbAware {
    private final ActionListenerContainer listenerContainer = new ActionListenerContainer();
    private final String actionCommand;
    private boolean checked = false;

    public ToolbarToggleButton(String text, boolean defaultValue, String actionCommand) {
        super(text);
        this.actionCommand = actionCommand;
        checked = defaultValue;
    }

    public void addActionListener(final ActionListener listener) {
        listenerContainer.add(listener);
    }

    @Override
    public boolean isSelected(final AnActionEvent e) {
        return checked;
    }

    @Override
    public void setSelected(final AnActionEvent e, final boolean state) {
        if (checked != state) {
            checked = state;
            listenerContainer.triggerEvent(this, actionCommand);
        }
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
    }

    @Override
    public JComponent createCustomComponent(Presentation presentation) {
        final JComponent customComponent = super.createCustomComponent(presentation);
        customComponent.setFocusable(false);
        customComponent.setOpaque(false);
        return customComponent;
    }
}
