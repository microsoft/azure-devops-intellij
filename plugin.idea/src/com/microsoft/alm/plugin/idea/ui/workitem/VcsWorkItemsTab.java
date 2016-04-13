// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.microsoft.alm.plugin.telemetry.TfsTelemetryConstants;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;

import javax.swing.JComponent;
import java.awt.event.ActionListener;
import java.util.Observer;

/**
 * UI class for Version Control work items tab
 */
public class VcsWorkItemsTab {
    private static final String EVENT_NAME = "Work Items";
    private VcsWorkItemsForm form;

    public VcsWorkItemsTab() {
        form = new VcsWorkItemsForm();

        // Make a telemetry entry for this UI tab opening
        TfsTelemetryHelper.getInstance().sendDialogOpened(this.getClass().getName(),
                new TfsTelemetryHelper.PropertyMapBuilder()
                        .activeServerContext()
                        .pair(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_DIALOG, EVENT_NAME)
                        .build());

    }

    public JComponent getPanel() {
        return form.getPanel();
    }

    public void addActionListener(final ActionListener listener) {
        form.addActionListener(listener);
    }

    public void setFilter(final String filterString) {
        form.setFilter(filterString);
    }

    public String getFilter() {
        return form.getFilter();
    }

    public void addObserver(final Observer observer) {
        form.addObserver(observer);
    }

    public void setConnectionStatus(final boolean connected, final boolean authenticating, final boolean authenticated,
                                    final boolean loading, final boolean loadingErrors) {
        form.setConnectionStatus(connected, authenticating, authenticated, loading, loadingErrors);
    }

    public void setWorkItemsTable(final WorkItemsTableModel tableModel) {
        form.setWorkItemsTable(tableModel);
    }
}
