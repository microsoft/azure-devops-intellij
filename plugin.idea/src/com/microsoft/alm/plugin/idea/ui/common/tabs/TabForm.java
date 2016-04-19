// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.tabs;

import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;

import javax.swing.JComponent;
import java.awt.event.ActionListener;

/**
 * Interface for IntelliJ tab views
 */
public interface TabForm {
    // tab commands
    String CMD_REFRESH = "refresh";
    String CMD_FILTER_CHANGED = "filter";
    String CMD_STATUS_LINK = "statusLink";
    String CMD_SEND_FEEDBACK = "sendFeedback";

    JComponent getPanel();

    void setStatus(final VcsTabStatus status);

    void addActionListener(final ActionListener listener);

    void setFilter(final String filterString);

    String getFilter();
}
