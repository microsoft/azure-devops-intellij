// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.ProjectConfigurableForm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public class TFSProjectConfigurable implements Configurable {

    private final Project project;
    private ProjectConfigurableForm form;

    public TFSProjectConfigurable(final Project project) {
        this.project = project;
    }

    @Nullable
    @Nls
    public String getDisplayName() {
        return TFSVcs.TFVC_NAME;
    }

    @NonNls
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        form = new ProjectConfigurableForm(project);
        return form.getContentPane();
    }

    @Override
    public boolean isModified() {
        return form.isModified();
        // TODO: move this all to the component isModified() method once needed
//    if (TFSConfigurationManager.getInstance().useIdeaHttpProxy() != myComponent.useProxy()) return true;
//    TfsCheckinPoliciesCompatibility c = TFSConfigurationManager.getInstance().getCheckinPoliciesCompatibility();
//    if (c.teamExplorer != myComponent.supportTfsCheckinPolicies()) return true;
//    if (c.teamprise != myComponent.supportStatefulCheckinPolicies()) return true;
//    if (c.nonInstalled != myComponent.reportNotInstalledCheckinPolicies()) return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        form.apply();
        // TODO: move this all to the component apply() method once needed
//    TFSConfigurationManager.getInstance().setUseIdeaHttpProxy(myComponent.useProxy());
//    TFSConfigurationManager.getInstance().setSupportTfsCheckinPolicies(myComponent.supportTfsCheckinPolicies());
//    TFSConfigurationManager.getInstance().setSupportStatefulCheckinPolicies(myComponent.supportStatefulCheckinPolicies());
//    TFSConfigurationManager.getInstance().setReportNotInstalledCheckinPolicies(myComponent.reportNotInstalledCheckinPolicies());
    }

    @Override
    public void reset() {
        form.reset();
        // TODO: move this all to the component reset() method once needed
//    myComponent.setUserProxy(TFSConfigurationManager.getInstance().useIdeaHttpProxy());
//    TfsCheckinPoliciesCompatibility c = TFSConfigurationManager.getInstance().getCheckinPoliciesCompatibility();
//    myComponent.setSupportTfsCheckinPolicies(c.teamExplorer);
//    myComponent.setSupportStatefulCheckinPolicies(c.teamprise);
//    myComponent.setReportNotInstalledCheckinPolicies(c.nonInstalled);
    }

    @Override
    public void disposeUIResources() {
        form = null;
    }
}
