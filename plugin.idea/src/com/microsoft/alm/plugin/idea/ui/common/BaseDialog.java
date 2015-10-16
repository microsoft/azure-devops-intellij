// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import java.awt.Component;
import java.awt.event.ActionListener;

public interface BaseDialog {
    String CMD_OK = "ok"; // the clone button is clicked
    String CMD_CANCEL = "cancel"; // the user cancels
    String CMD_TAB_CHANGED = "tabChanged"; // the user changed the selected tab

    void displayError(String message);

    void addTabPage(final String text, final Component component);

    int getSelectedTabIndex();

    void setSelectedTabIndex(final int index);

    void setOkEnabled(final boolean enabled);

    void addActionListener(final ActionListener listener);

    void addValidationListener(final ValidationListener listener);

    boolean showModalDialog();
}
