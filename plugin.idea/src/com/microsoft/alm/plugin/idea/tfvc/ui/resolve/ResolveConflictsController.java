// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.resolve;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.ServerContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Controller for resolving conflicts interactively with users
 */
public class ResolveConflictsController implements Observer, ActionListener {
    private final ResolveConflictsDialog dialog;
    private final ResolveConflictsModel model;

    public ResolveConflictsController(final Project project, final ServerContext serverContext, final List<String> filePaths) {
        this.dialog = new ResolveConflictsDialog(project);
        this.model = new ResolveConflictsModel(project, serverContext, filePaths);

        this.dialog.addActionListener(this);
        this.model.addObserver(this);

        update(null, null);
    }

    public boolean showModalDialog() {
        return dialog.showModalDialog();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateModel();

        if (ResolveConflictsForm.CMD_ACCEPT_THEIRS.equals(e.getActionCommand())) {
            //TODO: take their changes
        } else if (ResolveConflictsForm.CMD_ACCEPT_YOURS.equals(e.getActionCommand())) {
            //TODO: take your changes
        } else if (ResolveConflictsForm.CMD_MERGE.equals(e.getActionCommand())) {
            //TODO: merge
        }
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (arg == null || arg.equals(ResolveConflictsModel.PROP_LOADING)) {
            dialog.setLoading(model.isLoading());
        }
        if (arg == null) {
            dialog.setConflictsTableModel(model.getConflictsTableModel());
        }
    }

    protected void updateModel() {
        // TODO: implement updateModel once buttons are configured
    }
}
