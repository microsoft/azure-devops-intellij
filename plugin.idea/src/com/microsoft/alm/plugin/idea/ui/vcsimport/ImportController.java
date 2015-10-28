// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.BaseDialog;
import com.microsoft.alm.plugin.idea.ui.common.BaseDialogImpl;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.PageModel;
import com.microsoft.alm.plugin.idea.ui.common.ValidationListener;
import com.microsoft.alm.plugin.idea.ui.common.forms.TfsLoginForm;
import com.microsoft.alm.plugin.idea.ui.common.forms.VsoLoginForm;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * This class binds the UI (ImportDialog) with the Model (ImportModel) by attaching listeners to both and keeping them
 * in sync.
 */
public class ImportController implements Observer {

    private final BaseDialog dialog;
    private final ImportModel model;
    private final ImportPageController vsoPageController;
    private final ImportPageController tfsPageController;

    public final static int TAB_VSO = 0;
    public final static int TAB_TFS = 1;

    public ImportController(final Project project) {
        this(new BaseDialogImpl(project,
                        TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TITLE),
                        TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_IMPORT_BUTTON),
                        TfPluginBundle.KEY_IMPORT_DIALOG_TITLE),
                new ImportPageImpl(new VsoLoginForm(), new ImportForm(true)),
                new ImportPageImpl(new TfsLoginForm(), new ImportForm(false)),
                new ImportModel(project));
    }

    // This constructor is here for tests to use with mocked pages
    public ImportController(final BaseDialog importDialog,
                            final ImportPage vsoImportPage,
                            final ImportPage tfsImportPage,
                            final ImportModel importModel) {
        this.dialog = importDialog;
        this.model = importModel;
        this.vsoPageController = new ImportPageController(model.getVsoImportPageModel(), vsoImportPage);
        this.tfsPageController = new ImportPageController(model.getTfsImportPageModel(), tfsImportPage);
        setupDialog();
        importModel.addObserver(this);
    }

    public ImportModel getModel() {
        return model;
    }

    public boolean showModalDialog() {
        return dialog.showModalDialog();
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (ImportModel.PROP_IMPORT_ENABLED.equals(arg)) {
            dialog.setOkEnabled(model.isImportEnabled());
        } else if (ImportModel.PROP_VSO_SELECTED.equals(arg)) {
            dialog.setSelectedTabIndex(model.isVsoSelected() ? TAB_VSO : TAB_TFS);
            //evaluate whether to enable/disable the "Import" button
            dialog.setOkEnabled(model.isImportEnabled());
        } else if (PageModel.PROP_ERRORS.equals(arg)) {
            if (model.hasErrors()) {
                dialog.displayError(model.getErrors().get(0).getValidationMessage());
            } else {
                dialog.displayError(null);
            }
        }
    }

    private void setupDialog() {
        dialog.addTabPage(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_VSO_TAB),
                vsoPageController.getPageAsPanel());
        dialog.addTabPage(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TFS_TAB),
                tfsPageController.getPageAsPanel());

        dialog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (BaseDialog.CMD_OK.equals(e.getActionCommand())) {
                    importIntoRepo();
                } else if (BaseDialog.CMD_CANCEL.equals(e.getActionCommand())) {
                    //TODO dispose of the model correctly
                    model.dispose();
                } else if (BaseDialog.CMD_TAB_CHANGED.equals(e.getActionCommand())) {
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

        final ServerContext context = ServerContextManager.getInstance().getActiveContext();
        if (context != ServerContext.NO_CONTEXT && context.getType() == ServerContext.Type.TFS) {
            dialog.setSelectedTabIndex(TAB_TFS);
        }

        dialog.setOkEnabled(model.isImportEnabled());
    }

    private void importIntoRepo() {
        final ImportPageModel pageModel = getActivePageModel();
        pageModel.importIntoRepository();
    }

    private ValidationInfo validate() {
        final ImportPageModel pageModel = getActivePageModel();
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
                    vsoPageController.getComponent(error.getValidationSource()) : tfsPageController.getComponent(error.getValidationSource());
            result = new ValidationInfo(error.getValidationMessage(), errorSource);
        } else {
            result = null;
        }

        return result;
    }

    private ImportPageModel getActivePageModel() {
        final ImportPageModel pageModel;

        if (model.isVsoSelected()) {
            vsoPageController.updateModel();
            pageModel = model.getVsoImportPageModel();
        } else {
            tfsPageController.updateModel();
            pageModel = model.getTfsImportPageModel();
        }

        return pageModel;
    }
}
