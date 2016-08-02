// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.workitem;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractController;
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import git4idea.repo.GitRepository;

import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.util.Observable;

public class SelectWorkItemsController extends AbstractController {
    private final SelectWorkItemsModel model;
    private final SelectWorkItemsForm form;

    public SelectWorkItemsController(final Project project) {
        final GitRepository gitRepository = TfGitHelper.getTfGitRepository(project);

        model = new SelectWorkItemsModel(gitRepository);
        form = new SelectWorkItemsForm();

        // add the observer and action listener
        model.addObserver(this);
        form.addActionListener(this);

        update(null, null);
        model.loadWorkItems();
    }

    public JPanel getContentPanel() {
        return form.getContentPanel();
    }

    public String getComment() {
        return model.getComment();
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (arg == null || arg.equals(SelectWorkItemsModel.PROP_LOADING)) {
            form.setLoading(model.isLoading());
            // if we finished loading and we got back the max number of items, show the help panel
            form.setShowHelpPanel(!model.isLoading() && model.isMaxItemsReached());
        }
        if (arg == null || arg.equals(SelectWorkItemsModel.PROP_FILTER)) {
            form.setFilter(model.getFilter());
        }
        if (arg == null || arg.equals(SelectWorkItemsModel.PROP_SERVER_NAME)) {
            form.setServerName(model.getServerName());
        }
        if (arg == null) {
            form.setWorkItemTable(model.getTableModel(), model.getTableSelectionModel());
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        updateModel();

        if (SelectWorkItemsForm.CMD_REFRESH.equals(e.getActionCommand())) {
            // Reload the table (the refresh button shouldn't be visible if the query is currently running)
            model.loadWorkItems();
        } else if (SelectWorkItemsForm.CMD_FILTER_CHANGED.equals(e.getActionCommand())) {
            // No action needed here. We updated the model above which should filter the list automatically.
        } else if (SelectWorkItemsForm.CMD_NEW_WORK_ITEM.equals(e.getActionCommand())) {
            model.createWorkItem();
        } else if (SelectWorkItemsForm.CMD_GOTO_VIEW_MY_WORK_ITEMS.equals(e.getActionCommand())) {
            model.gotoMyWorkItems();
        }
    }

    @Override
    protected void updateModel() {
        model.setFilter(form.getFilter());
    }
}
