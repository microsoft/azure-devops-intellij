// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.controls;

import com.intellij.ui.JBColor;

import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Use this class as the UI for a TextField where you want to add hint text.
 * The hint text is displayed as long as the control doesn't have focus and doesn't have a value.
 */
public class HintTextFieldUI extends BasicTextFieldUI implements FocusListener {
    private final String hintText;

    public HintTextFieldUI(String hint) {
        this.hintText = hint;
    }

    @Override
    protected void paintSafely(Graphics g) {
        super.paintSafely(g);
        JTextComponent component = getComponent();
        if (hintText != null && component.getText().length() == 0 && !component.hasFocus()) {
            g.setColor(JBColor.LIGHT_GRAY);
            final int fontSize = component.getFont().getSize();
            final int padding = (component.getHeight() - fontSize) / 2;
            final int x = component.getInsets().left;
            final int y = component.getHeight() - padding - 1;
            g.drawString(hintText, x, y);
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        repaint();
    }

    @Override
    public void focusLost(FocusEvent e) {
        repaint();
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        getComponent().addFocusListener(this);
    }
    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        getComponent().removeFocusListener(this);
    }

    private void repaint() {
        if(getComponent() != null) {
            getComponent().repaint();
        }
    }
}