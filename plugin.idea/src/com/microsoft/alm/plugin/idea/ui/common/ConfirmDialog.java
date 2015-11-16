// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.idea.ui.common.forms.ConfirmForm;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Dimension;

/**
 * A confirmation dialog which allows setting custom ok and cancel button text
 */
public class ConfirmDialog extends DialogWrapper {
    final private String message;
    final private Icon icon;
    private ConfirmForm confirmForm;

    public ConfirmDialog(final Project project, final String title, final String message,
                         @Nullable final Icon icon,
                         @Nullable final String okButtonText, @Nullable final String cancelButtonText) {
        super(project);
        setTitle(title);

        this.message = message;
        this.icon = icon;

        if(StringUtils.isNotEmpty(okButtonText)) {
            super.setOKButtonText(okButtonText);
        }
        if(StringUtils.isNotEmpty(cancelButtonText)) {
            super.setCancelButtonText(cancelButtonText);
        }

        super.init();
    }

    @Override
    protected JComponent createCenterPanel() {
        confirmForm = new ConfirmForm(message, icon);
        final JComponent formContent = confirmForm.getContentPanel();
        formContent.setPreferredSize(new Dimension(JBUI.scale(600), -1));
        return formContent;
    }
}
