// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsConfigurableProvider;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;

/**
 * This class creates the general settings menu
 */
public class TeamServicesConfigurable implements Configurable, VcsConfigurableProvider {
    public static String CMD_APPLY_CHANGES = "applyChanges";
    public static String CMD_RESET_CHANGES = "resetChanges";

    private TeamServicesSettingsController controller;
    private final ActionEvent applyEvent;
    private final ActionEvent resetEvent;

    public TeamServicesConfigurable() {
        applyEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, CMD_APPLY_CHANGES);
        resetEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, CMD_RESET_CHANGES);
    }

    @Nls
    @Override
    public String getDisplayName() {
        return TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_MENU_TITLE);
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        controller = new TeamServicesSettingsController();
        return controller.getContentPane();
    }

    @Override
    public boolean isModified() {
        return controller != null && controller.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (controller != null) {
            controller.actionPerformed(applyEvent);
        }
    }

    @Override
    public void reset() {
        if (controller != null) {
            controller.actionPerformed(resetEvent);
        }
    }

    @Nullable
    @Override
    public Configurable getConfigurable(final Project project) {
        return this;
    }

    @Override
    public void disposeUIResources() {
        controller = null;
    }
}
