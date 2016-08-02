// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common.tabs;

import com.microsoft.alm.plugin.idea.common.ui.common.FilteredModel;
import com.microsoft.alm.plugin.idea.common.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.operations.Operation;

import javax.swing.JComponent;
import java.awt.event.ActionListener;

/**
 * Interface for IntelliJ tab views
 */
public interface TabForm<T extends FilteredModel> {
    // tab commands
    String CMD_AUTO_REFRESH_CHANGED = "autoRefreshChanged";
    String CMD_REFRESH = "refresh";
    String CMD_FILTER_CHANGED = "filter";
    String CMD_STATUS_LINK = "statusLink";
    String CMD_SEND_FEEDBACK = "sendFeedback";
    String CMD_CREATE_NEW_ITEM = "createNewItemLink";
    String CMD_OPEN_SELECTED_ITEM_IN_BROWSER = "openSelectedItem";

    JComponent getPanel();

    void setStatus(final VcsTabStatus status);

    void addActionListener(final ActionListener listener);

    void setFilter(final String filterString);

    String getFilter();

    void setAutoRefresh(final boolean autoRefresh);

    boolean getAutoRefresh();

    void setModelForView(final T viewModel);

    Operation.CredInputsImpl getOperationInputs();

    void refresh();
}
