// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryConstants;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.Date;
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

    public void addMouseListener(final MouseListener listener) {
        form.addMouseListener(listener);
    }

    public void setConnectionStatus(final boolean connected, final boolean authenticating, final boolean authenticated,
                                    final boolean loading, final boolean loadingErrors) {
        form.setConnectionStatus(connected, authenticating, authenticated, loading, loadingErrors);
    }

    public void setLastRefreshed(final Date lastRefreshed) {
        form.setLastRefreshed(lastRefreshed);
    }

    public void setPullRequestsTree(final PullRequestsTreeModel treeModel) {
        form.setPullRequestsTree(treeModel);
    }

    public void showPopupMenu(final Component component, final int x, final int y, final ActionListener listener) {
        final JBPopupMenu menu = new JBPopupMenu();
        final JBMenuItem openMenuItem = createMenuItem(TfPluginBundle.KEY_VCS_PR_OPEN_IN_BROWSER, null, VcsPullRequestsForm.CMD_OPEN_SELECTED_PR_IN_BROWSER, listener);
        menu.add(openMenuItem);
        final JBMenuItem abandonMenuItem = createMenuItem(TfPluginBundle.KEY_VCS_PR_ABANDON, null, VcsPullRequestsForm.CMD_ABANDON_SELECTED_PR, listener);
        menu.add(abandonMenuItem);
        final JBMenuItem completeMenuItem = createMenuItem(TfPluginBundle.KEY_VCS_PR_COMPLETE, null, VcsPullRequestsForm.CMD_COMPLETE_SELECTED_PR, listener);
        menu.add(completeMenuItem);

        menu.show(component, x, y);
    }

    private JBMenuItem createMenuItem(final String resourceKey, final Icon icon, final String actionCommand, final ActionListener listener) {
        final String text = TfPluginBundle.message(resourceKey);
        final JBMenuItem menuItem = new JBMenuItem(text, icon);
        menuItem.setActionCommand(actionCommand);
        menuItem.addActionListener(listener);
        return menuItem;
    }
}
