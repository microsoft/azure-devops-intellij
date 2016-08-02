// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common.forms;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.event.ActionListener;

/**
 * This base form represents the basic methods that all forms should implement.
 */
public interface BasicForm {
    /**
     * The getContentPanel method returns the main UI element for the form.
     */
    JPanel getContentPanel();

    /**
     * The getPreferredFocusedComponent method returns the control that should get the initial focus.
     */
    JComponent getPreferredFocusedComponent();

    /**
     * The addActionListener method should attach the provided listener to all applicable controls.
     */
    void addActionListener(ActionListener listener);
}
