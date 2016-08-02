// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.controls;

import com.intellij.util.ui.JBUI;

import javax.swing.Icon;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;

/**
 * This class is used to draw an image on a dialog or panel.
 * Use the Icons class to load the icon and pass it to the constructor.
 */
public class IconPanel extends JPanel {
    private Icon icon;

    /**
     * Use this constructor if you need to set the icon dynamically.
     */
    public IconPanel() {
        setPreferredSize(new Dimension(JBUI.scale(32), JBUI.scale(32)));
    }

    /**
     * Create the IconPanel. The preferred size and min size are derived from the width and height of the icon.
     *
     * @param icon
     */
    public IconPanel(Icon icon) {
        setIcon(icon);
    }

    public void setIcon(Icon icon) {
        assert icon != null;

        this.icon = icon;
        final Dimension iconSize = new Dimension(icon.getIconWidth(), icon.getIconHeight());
        setPreferredSize(iconSize);
        setMinimumSize(iconSize);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (icon != null) {
            icon.paintIcon(this, g, 0, 0);
        }
    }
}
