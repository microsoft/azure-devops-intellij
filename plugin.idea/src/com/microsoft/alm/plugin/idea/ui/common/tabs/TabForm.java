// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.tabs;

import com.microsoft.alm.plugin.idea.ui.common.FilteredModel;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.operations.Operation;

import javax.swing.JComponent;
import java.awt.event.ActionListener;
import java.util.Observer;

/**
 * Interface for IntelliJ tab views
 */
public interface TabForm<T extends FilteredModel> {
    // tab commands
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

    void addObserver(final Observer observer);

    void setModelForView(final T viewModel);

    Operation.Inputs getOperationInputs();

    void refresh();
}
