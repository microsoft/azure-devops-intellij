// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryConstants;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.event.ActionListener;
import java.util.Observer;

/**
 * UI class for Version Control pull requests tab
 */
public class VcsPullRequestsTab {
    private final Project project;
    private VcsPullRequestsForm form;

    public VcsPullRequestsTab(@NotNull final Project project) {
        this.project = project;
        form = new VcsPullRequestsForm();

        // Make a telemetry entry for this UI tab opening
        TfsTelemetryHelper.getInstance().sendDialogOpened(this.getClass().getName(),
                new TfsTelemetryHelper.PropertyMapBuilder()
                        .activeServerContext()
                        .pair(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_DIALOG, "Pull Requests")
                        .build());

    }

    public JComponent getPanel() {
        return form.getPanel();
    }

    public void addActionListener(final ActionListener listener) {
        form.addActionListener(listener);
    }

    public void addObserver(final Observer observer) {
        form.addObserver(observer);
    }

    public void setConnectionStatus(final boolean connected, final boolean authenticating, final boolean authenticated,
                                    final boolean loading, final boolean loadingErrors) {
        form.setConnectionStatus(connected, authenticating, authenticated, loading, loadingErrors);
    }

    public void setPullRequestsTree(final PullRequestsTreeModel treeModel) {
        form.setPullRequestsTree(treeModel);
    }
}
