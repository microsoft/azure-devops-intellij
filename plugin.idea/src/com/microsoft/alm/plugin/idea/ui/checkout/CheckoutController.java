// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.checkout;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.ui.common.BaseDialogImpl;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.ValidationListener;
import com.microsoft.alm.plugin.idea.ui.common.forms.TfsLoginForm;
import com.microsoft.alm.plugin.idea.ui.common.forms.VsoLoginForm;
import com.microsoft.alm.plugin.idea.ui.common.forms.VstsTfsLoginForm;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * This class binds the UI with the Model by attaching listeners to both and keeping them
 * in sync.
 */
public class CheckoutController implements Observer {
    private final BaseDialog dialog;
    private final CheckoutModel model;
    private final CheckoutPageController vsoController;
    private final CheckoutPageController tfsController;
    public final static int TAB_VSO = 0;
    public final static int TAB_TFS = 1;

    public CheckoutController(final Project project, final CheckoutProvider.Listener listener) {
        this(new BaseDialogImpl(project,
                        TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_TITLE),
                        TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_CLONE_BUTTON),
                        TfPluginBundle.KEY_CHECKOUT_DIALOG_TITLE),
                new CheckoutPageImpl(new VstsTfsLoginForm(), new CheckoutForm(true)),
                new CheckoutPageImpl(new TfsLoginForm(), new CheckoutForm(false)),
                new CheckoutModel(project, listener));
    }

    // This constructor is here for tests to use with mocked pages
    public CheckoutController(final BaseDialog dialog,
                              final CheckoutPage vsoCheckoutPage,
                              final CheckoutPage tfsCheckoutPage,
                              final CheckoutModel checkoutModel) {
        this.model = checkoutModel;
        this.dialog = dialog;
        this.vsoController = new CheckoutPageController(model.getVsoModel(), vsoCheckoutPage);
        this.tfsController = new CheckoutPageController(model.getTfsModel(), tfsCheckoutPage);
        setupDialog();
        model.addObserver(this);
    }

    public CheckoutModel getModel() {
        return model;
    }

    public boolean showModalDialog() {
        return dialog.showModalDialog();
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (CheckoutModel.PROP_CLONE_ENABLED.equals(arg)) {
            dialog.setOkEnabled(model.isCloneEnabled());
        } else if (CheckoutModel.PROP_VSO_SELECTED.equals(arg)) {
            dialog.setSelectedTabIndex(model.isVsoSelected() ? TAB_VSO : TAB_TFS);
            //evaluate whether to enable/disable clone button every time tab is changed
            dialog.setOkEnabled(model.isCloneEnabled());
        } else if (CheckoutModel.PROP_ERRORS.equals(arg)) {
            if (model.hasErrors()) {
                dialog.displayError(model.getErrors().get(0).getValidationMessage());
            } else {
                dialog.displayError(null);
            }
        }
    }

    private void setupDialog() {
        dialog.addTabPage(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_VSO_TAB),
                vsoController.getPageAsPanel());
        /*dialog.addTabPage(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_TFS_TAB),
                tfsController.getPageAsPanel());*/

        dialog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (BaseDialog.CMD_OK.equals(e.getActionCommand())) {
                    cloneRepo();
                } else if (BaseDialog.CMD_CANCEL.equals(e.getActionCommand())) {
                    //TODO do we need to dispose of the model in other cases
                    model.dispose();
                } else if (BaseDialog.CMD_TAB_CHANGED.equals(e.getActionCommand())) {
                    model.clearErrors();
                    model.setVsoSelected(dialog.getSelectedTabIndex() == TAB_VSO);
                }
            }
        });

        dialog.addValidationListener(new ValidationListener() {
            @Override
            public ValidationInfo doValidate() {
                return validate();
            }
        });

        if (ServerContextManager.getInstance().lastUsedContextIsTFS()) {
            dialog.setSelectedTabIndex(TAB_TFS);
        }

        dialog.setOkEnabled(model.isCloneEnabled());
    }

    private void cloneRepo() {
        final CheckoutPageModel pageModel = getActivePageModel();
        pageModel.cloneSelectedRepo();
    }

    private ValidationInfo validate() {
        final CheckoutPageModel pageModel = getActivePageModel();
        final ValidationInfo result;

        final ModelValidationInfo error;
        if (model.hasErrors()) {
            //We will display first relevant error only
            error = model.getErrors().get(0);
        } else {
            error = pageModel.validate();
        }

        if (error != null) {
            final JComponent errorSource = model.isVsoSelected() ?
                    vsoController.getComponent(error.getValidationSource()) : tfsController.getComponent(error.getValidationSource());
            result = new ValidationInfo(error.getValidationMessage(), errorSource);
        } else {
            result = null;
        }

        return result;
    }

    private CheckoutPageModel getActivePageModel() {
        final CheckoutPageModel pageModel;

        if (model.isVsoSelected()) {
            vsoController.updateModel();
            pageModel = model.getVsoModel();
        } else {
            tfsController.updateModel();
            pageModel = model.getTfsModel();
        }

        return pageModel;
    }
}
