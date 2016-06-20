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
 * Tab interface for IntelliJ
 */
public interface Tab<T extends FilteredModel> {

    JComponent getPanel();

    void addActionListener(final ActionListener listener);

    void addObserver(final Observer observer);

    void setStatus(final VcsTabStatus status);

    void setFilter(final String filterString);

    String getFilter();

    void setViewModel(final T modelView);

    Operation.Inputs getOperationInputs();

    void refresh();
}
