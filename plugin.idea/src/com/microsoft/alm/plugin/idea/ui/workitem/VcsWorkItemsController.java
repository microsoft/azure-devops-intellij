// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.ui.common.tabs.Tab;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * Controller for the WorkItems VC tab
 */
public class VcsWorkItemsController implements Observer, ActionListener {
    private static final String EVENT_NAME = "Work Items";

    private final Tab tab;
    private final VcsWorkItemsModel model;

    public VcsWorkItemsController(final @NotNull Project project) {
        model = new VcsWorkItemsModel(project);
        tab = new TabImpl(new VcsWorkItemsForm(), EVENT_NAME);

        setupTab();

        // Initialize the form with the current values from the model
        update(null, null);

        // add the observer and action listener after we are fully initialized, otherwise we will just get called
        // in the middle
        model.addObserver(this);

        //load the work items
        model.loadData();
    }

    private void setupTab() {
        tab.addActionListener(this);
        tab.addObserver(this);
    }

    public void actionPerformed(final ActionEvent e) {
        updateModel();

        if (VcsWorkItemsForm.CMD_STATUS_LINK.equals(e.getActionCommand())) {
            if (model.getTabStatus() == VcsTabStatus.NOT_TF_GIT_REPO) {
                //import into team services git
                model.importIntoTeamServicesGit();
            } else if (model.getTabStatus() == VcsTabStatus.NO_AUTH_INFO) {
                //prompt for credentials and load work items
                model.loadData();
            } else {
                //open current repository in web
                model.createNewItem();
            }
        } else if (VcsWorkItemsForm.CMD_OPEN_SELECTED_WIT_IN_BROWSER.equals(e.getActionCommand())) {
            //pop up menu - open WIT link in web
            model.openSelectedItemsLink();
        }
    }

    public JComponent getPanel() {
        return tab.getPanel();
    }

    @Override
    public void update(final Observable observable, final Object arg) {
        if (arg == null
                || VcsWorkItemsModel.PROP_PR_WI_STATUS.equals(arg)) {
            tab.setStatus(model.getTabStatus());
        }
        if (arg == null) {
            tab.setViewModel(model.getModelForView());
        }
        if (arg == null || arg.equals(SelectWorkItemsModel.PROP_FILTER)) {
            tab.setFilter(model.getFilter());
        }

        //actions from the form
        if (VcsWorkItemsForm.CMD_CREATE_NEW_WORK_ITEM.equals(arg)) {
            model.createNewItem();
        }
        if (VcsWorkItemsForm.CMD_REFRESH.equals(arg)) {
            model.loadData();
        }
        if (VcsWorkItemsForm.CMD_OPEN_SELECTED_WIT_IN_BROWSER.equals(arg)) {
            model.openSelectedItemsLink();
        }
    }

    protected void updateModel() {
        model.setFilter(tab.getFilter());
    }

    public void dispose() {
        model.dispose();
    }
}
