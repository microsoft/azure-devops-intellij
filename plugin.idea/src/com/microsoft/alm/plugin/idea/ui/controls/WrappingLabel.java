// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.controls;

import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.idea.ui.common.SwingHelper;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;

public class WrappingLabel extends JPanel {
    private final JLabel label;

    public WrappingLabel() {
        this(100);
    }

    public WrappingLabel(final int width) {
        final Dimension size = new Dimension(JBUI.scale(width), -1);
        label = new JLabel();
        label.setPreferredSize(size);
        setLayout(new BorderLayout());
        this.add(label, BorderLayout.CENTER);
    }

    public void setText(final String text) {
        // Add html tags
        label.setText("<HTML>" + text + "</HTML>");
    }

    public String getText() {
        return label.getText();
    }

    public void setMargin(final Insets newMargin) {
        SwingHelper.setMargin(this, newMargin);
    }
}
