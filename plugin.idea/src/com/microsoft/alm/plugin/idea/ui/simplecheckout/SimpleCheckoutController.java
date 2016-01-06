// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.simplecheckout;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.ValidationListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * The controller for the SimpleCheckoutDialog
 */
public class SimpleCheckoutController implements Observer, ActionListener {
    private final SimpleCheckoutDialog dialog;
    private final SimpleCheckoutModel model;

    public SimpleCheckoutController(final Project project, final CheckoutProvider.Listener listener, final String gitUrl) {
        this(new SimpleCheckoutDialog(
                        project,
                        TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_TITLE),
                        TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_CLONE_BUTTON),
                        TfPluginBundle.KEY_CHECKOUT_DIALOG_TITLE),
                new SimpleCheckoutModel(project, listener, gitUrl));
    }

    public SimpleCheckoutController(final SimpleCheckoutDialog dialog, final SimpleCheckoutModel model) {
        this.dialog = dialog;
        this.dialog.addActionListener(this);

        this.model = model;
        this.model.addObserver(this);

        setupDialog();
        update(null, null);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        // Update model before action is initiated on it
        updateModel();

        if (BaseDialog.CMD_OK.equals(e.getActionCommand())) {
            model.cloneRepo();
        }
    }

    public boolean showModalDialog() {
        return dialog.showModalDialog();
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (arg == null || arg.equals(SimpleCheckoutModel.PROP_DIRECTORY_NAME)) {
            dialog.setDirectoryName(model.getDirectoryName());
        }
        if (arg == null || arg.equals(SimpleCheckoutModel.PROP_PARENT_DIR)) {
            dialog.setParentDirectory(model.getParentDirectory());
        }
    }

    private void setupDialog() {
        dialog.setRepoUrl(model.getRepoUrl());
        dialog.addValidationListener(new ValidationListener() {
            @Override
            public ValidationInfo doValidate() {
                return validate();
            }
        });
    }

    protected ValidationInfo validate() {
        updateModel();

        ModelValidationInfo validationInfo = model.validate();
        if (validationInfo != ModelValidationInfo.NO_ERRORS) {
            return new ValidationInfo(validationInfo.getValidationMessage(),
                    dialog.getComponent(validationInfo.getValidationSource()));
        }
        return null;
    }

    protected void updateModel() {
        model.setParentDirectory(dialog.getParentDirectory());
        model.setDirectoryName(dialog.getDirectoryName());
    }
}
