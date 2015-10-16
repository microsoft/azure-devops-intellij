// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.forms.FeedbackForm;

import javax.swing.JComponent;
import java.awt.event.ActionListener;
import java.util.Collections;

public class FeedbackDialog extends BaseDialogImpl {
    public static final String PROP_SMILE = "smile";
    private FeedbackForm feedbackForm;

    public FeedbackDialog(final Project project, final boolean smile) {
        super(project,
                TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE),
                smile ? TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_OK_SMILE) :
                        TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_OK_FROWN),
                null,
                /* showFeedback */ false,
                Collections.<String,Object>singletonMap(PROP_SMILE, smile));
    }

    public String getComment() {
        return feedbackForm.getComment();
    }

    public String getEmail() {
        return feedbackForm.getEmail();
    }

    public void addActionListener(final ActionListener listener) {
        // Hook up listener to all actions
        feedbackForm.addActionListener(listener);
    }

    @Override
    protected JComponent createCenterPanel() {
        feedbackForm = new FeedbackForm();
        return feedbackForm.getContentPanel((Boolean)getProperty(PROP_SMILE));
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return feedbackForm.getPreferredFocusedComponent();
    }
}
