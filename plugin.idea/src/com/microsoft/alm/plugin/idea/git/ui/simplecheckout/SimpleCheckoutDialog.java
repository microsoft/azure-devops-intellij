// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.simplecheckout;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialogImpl;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryConstants;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import org.apache.commons.lang.StringUtils;

import javax.swing.JComponent;
import java.awt.event.ActionListener;

/**
 * UI class for the SimpleCheckout dialog
 */
public class SimpleCheckoutDialog extends BaseDialogImpl {
    private SimpleCheckoutForm simpleCheckoutForm;

    public SimpleCheckoutDialog(final Project project, final String title, final String okButtonText, final String feedbackContext) {
        super(project, title, okButtonText, feedbackContext);
        super.setTitle(title);
        super.setOKButtonText(okButtonText);
        super.init();

        // Make a telemetry entry for this UI dialog
        TfsTelemetryHelper.getInstance().sendDialogOpened(this.getClass().getName(),
                new TfsTelemetryHelper.PropertyMapBuilder()
                        .activeServerContext()
                        .pair(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_DIALOG, title)
                        .build());
    }

    @Override
    protected JComponent createCenterPanel() {
        simpleCheckoutForm = new SimpleCheckoutForm();
        return simpleCheckoutForm.getContentPanel();
    }

    public void setParentDirectory(final String path) {
        simpleCheckoutForm.setParentDirectory(path);
    }

    public String getParentDirectory() {
        return simpleCheckoutForm.getParentDirectory();
    }

    public void setDirectoryName(final String name) {
        simpleCheckoutForm.setDirectoryName(name);
    }

    public String getDirectoryName() {
        return simpleCheckoutForm.getDirectoryName();
    }

    public void setRepoUrl(final String repoName) { simpleCheckoutForm.setRepoUrl(repoName); }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return simpleCheckoutForm.getPreferredFocusedComponent();
    }

    public JComponent getComponent(final String componentPropName) {
        if (StringUtils.isEmpty(componentPropName)) {
            return null;
        }
        return simpleCheckoutForm.getComponent(componentPropName);
    }

    @Override
    public void setOkEnabled(final boolean enabled) {
        super.setOkEnabled(enabled);
    }

    @Override
    public void addActionListener(final ActionListener listener) {
        super.addActionListener(listener);
        simpleCheckoutForm.addActionListener(listener);
    }

    @Override
    public void displayError(final String message) {
        setErrorText(message);
    }
}
