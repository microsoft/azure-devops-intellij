// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.settings;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractController;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.util.Observable;

/**
 * Controller for the general settings page
 */
public class TeamServicesSettingsController extends AbstractController {
    private final TeamServicesSettingsForm form;
    private final TeamServicesSettingsModel model;

    public TeamServicesSettingsController() {
        this(new TeamServicesSettingsForm(), new TeamServicesSettingsModel(IdeaHelper.getCurrentProject()));
    }

    @VisibleForTesting
    protected TeamServicesSettingsController(final TeamServicesSettingsForm form, final TeamServicesSettingsModel model) {
        this.form = form;
        this.model = model;

        // add the observer and action listener
        model.addObserver(this);
        form.addActionListener(this);

        update(null, null);
        model.loadSettings();
    }

    public JComponent getContentPane() {
        return form.getContentPane();
    }

    public boolean isModified() {
        return model.isModified();
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (arg == null) {
            form.setContextTable(model.getTableModel(), model.getTableSelectionModel());
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        updateModel();

        if (TeamServicesConfigurable.CMD_RESET_CHANGES.equals(e.getActionCommand())) {
            // Reload the table
            model.reset();
        } else if (TeamServicesConfigurable.CMD_APPLY_CHANGES.equals(e.getActionCommand())) {
            model.apply();
        } else if (TeamServicesSettingsForm.CMD_DELETE_PASSWORD.equals(e.getActionCommand())) {
            model.deletePasswords();
        } else if (TeamServicesSettingsForm.CMD_UPDATE_PASSWORD.equals(e.getActionCommand())) {
            model.updatePasswords();
        }
    }

    @Override
    protected void updateModel() {
        // nothing to update for the model
    }
}
