// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.extensions;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsConfigurableProvider;
import com.microsoft.alm.plugin.idea.ui.common.forms.TeamServiceSettings1Form;
import com.microsoft.alm.plugin.idea.ui.common.forms.TeamServiceSettingsForm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * Created by madhurig on 12/9/2015.
 */
public class TfGitSettingsConfigurable implements SearchableConfigurable, VcsConfigurableProvider {

    private TeamServiceSettings1Form form = null;

    @NotNull
    public String getDisplayName() {
        return "Visual Studio Team Services";
    }

    @NotNull
    public String getHelpTopic() {
        return "settings.vsts";
    }

    @NotNull
    public JComponent createComponent() {
        if(form == null) {
            form = new TeamServiceSettings1Form();
        }
        return form.getContentPanel();
    }

    public void disposeUIResources() {

    }

    public void apply() throws ConfigurationException {

    }

    public void reset() {

    }

    public Runnable enableSearch(String option) {
        return null;
    }

    @Nullable
    @Override
    public Configurable getConfigurable(Project project) {
        return this;
    }

    @NotNull
    public String getId() {
        return getHelpTopic();
    }

    public boolean isModified() {
        return false;
    }
}
