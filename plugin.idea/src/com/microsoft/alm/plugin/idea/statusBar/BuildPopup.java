// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.statusBar;

import com.intellij.openapi.ui.JBPopupMenu;
import com.microsoft.alm.plugin.idea.resources.Icons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.FeedbackAction;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

public class BuildPopup extends JBPopupMenu {
    private final BuildStatusModel model;

    public BuildPopup(final BuildStatusModel model) {
        if (model == null) throw new IllegalArgumentException("model");
        this.model = model;
        addBuildMenus();
    }

    private void addBuildMenus() {
        if (model.hasStatusInformation()) {
            // Add the builds to the menu if they exist
            for (int i = 0; i < model.getBuildCount(); i++) {
                final URI url = model.getBuildURI(i);
                final JMenuItem item = new JMenuItem(
                        TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_VIEW_DETAILS, model.getBuildBranch(i)),
                        model.getBuildSuccess(i) ? Icons.BUILD_STATUS_SUCCEEDED : Icons.BUILD_STATUS_FAILED);
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        if (url != null) {
                            model.gotoLink(url.toString());
                        }
                    }
                });
                this.add(item);
            }

            // Add a separator if there were builds added
            if (getComponentCount() > 0) {
                this.addSeparator();
            }

            // Add the refresh menu
            final JMenuItem refreshItem = new JMenuItem(
                    TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_REFRESH));
            refreshItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    // Updating the entire status bar for this project
                    StatusBarManager.updateStatusBar(model.getProject(), false);
                }
            });
            this.add(refreshItem);
        } else {
            final JMenuItem signInItem = new JMenuItem(
                    TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_SIGN_IN), Icons.VSLogoSmall);
            signInItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    StatusBarManager.updateStatusBar(model.getProject(), true);
                }
            });
            this.add(signInItem);
        }

        // Add feedback submenu
        this.addSeparator();
        final FeedbackAction feedbackAction = new FeedbackAction(model.getProject(), this.getClass().getName());
        final JMenu subMenu = feedbackAction.getSubMenu();
        this.add(subMenu);
    }
}
