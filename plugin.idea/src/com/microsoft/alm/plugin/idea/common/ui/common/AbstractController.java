// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

public abstract class AbstractController implements Observer, ActionListener {
    @Override
    public abstract void update(final Observable o, final Object arg);

    @Override
    public abstract void actionPerformed(final ActionEvent e);

    protected abstract void updateModel();

    protected void requestFocus(final FocusableTabPage page) {
        if (page != null) {
            // Attempt to set focus to the page
            final JComponent c = page.getPreferredFocusedComponent();
            if (c != null) {
                c.requestFocusInWindow();
            }
        }
    }
}
