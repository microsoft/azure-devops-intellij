// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabControllerImpl;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabImpl;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;

/**
 * Controller for the WorkItems VC tab
 */
public class VcsWorkItemsController extends TabControllerImpl<VcsWorkItemsModel> {
    private static final String EVENT_NAME = "Work Items";

    public VcsWorkItemsController(final @NotNull Project project) {
        super(new TabImpl(new VcsWorkItemsForm(), EVENT_NAME), new VcsWorkItemsModel(project));
    }

    @Override
    protected void performAction(final ActionEvent e) {
        if (VcsWorkItemsForm.CMD_OPEN_SELECTED_ITEM_IN_BROWSER.equals(e.getActionCommand())) {
            //pop up menu - open WIT link in web
            model.openSelectedItemsLink();
        } else {
            super.performAction(e);
        }
    }
}
