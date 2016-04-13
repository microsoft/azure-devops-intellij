// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.openapi.project.Project;
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
    private final VcsWorkItemsTab tab;
    private final VcsWorkItemsModel model;

    public VcsWorkItemsController(final @NotNull Project project) {
        model = new VcsWorkItemsModel(project);
        tab = new VcsWorkItemsTab();

        setupTab();

        // Initialize the form with the current values from the model
        update(null, null);

        // add the observer and action listener after we are fully initialized, otherwise we will just get called
        // in the middle
        model.addObserver(this);

        //load the work items
        model.loadWorkItems();
    }

    private void setupTab() {
        tab.addActionListener(this);
        tab.addObserver(this);
    }

    public void actionPerformed(final ActionEvent e) {
        updateModel();

        if (VcsWorkItemsForm.CMD_STATUS_LINK.equals(e.getActionCommand())) {
            if (!model.isConnected()) {
                //import into team services git
                model.importIntoTeamServicesGit();
            } else if (!model.isAuthenticated()) {
                //prompt for credentials and load work items
                model.loadWorkItems();
            } else {
                //open current repository in web
                model.createNewWorkItemLink();
            }
        } else if (VcsWorkItemsForm.CMD_OPEN_SELECTED_WIT_IN_BROWSER.equals(e.getActionCommand())) {
            //pop up menu - open WIT link in web
            model.openSelectedWorkItemsLink();
        }
    }

    public JComponent getPanel() {
        return tab.getPanel();
    }

    @Override
    public void update(final Observable observable, final Object arg) {
        if (arg == null
                || VcsWorkItemsModel.PROP_CONNECTED.equals(arg)
                || VcsWorkItemsModel.PROP_AUTHENTICATED.equals(arg)
                || VcsWorkItemsModel.PROP_LOADING.equals(arg)
                || VcsWorkItemsModel.PROP_AUTHENTICATING.equals(arg)
                || VcsWorkItemsModel.PROP_LOADING_ERRORS.equals(arg)) {
            tab.setConnectionStatus(model.isConnected(), model.isAuthenticating(), model.isAuthenticated(),
                    model.isLoading(), model.hasLoadingErrors());
        }
        if (arg == null) {
            tab.setWorkItemsTable(model.getTableModel());
        }
        if (arg == null || arg.equals(SelectWorkItemsModel.PROP_FILTER)) {
            tab.setFilter(model.getFilter());
        }

        //actions from the form
        if (VcsWorkItemsForm.CMD_CREATE_NEW_WORK_ITEM.equals(arg)) {
            model.createNewWorkItemLink();
        }
        if (VcsWorkItemsForm.CMD_REFRESH.equals(arg)) {
            model.loadWorkItems();
        }
        if (VcsWorkItemsForm.CMD_OPEN_SELECTED_WIT_IN_BROWSER.equals(arg)) {
            model.openSelectedWorkItemsLink();
        }
    }

    protected void updateModel() {
        model.setFilter(tab.getFilter());
    }

    public void dispose() {
        model.dispose();
    }
}
