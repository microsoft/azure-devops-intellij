// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.tabs;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.util.Observable;

/**
 * Interface for tab controllers
 */
public interface TabController {

    void actionPerformed(final ActionEvent e);

    JComponent getPanel();

    void update(final Observable observable, final Object arg);

    void dispose();
}
