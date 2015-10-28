// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.microsoft.alm.plugin.idea.resources.Icons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.vcsimport.ImportController;
import git4idea.actions.BasicAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point to Importing a project
 */
public class ImportAction extends InstrumentedAction {

    private static final Logger logger = LoggerFactory.getLogger(ImportAction.class);

    public ImportAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_IMPORT),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_IMPORT_MSG),
                Icons.VSLogoSmall);
    }

    @Override
    public void doUpdate(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        if (project == null || project.isDefault()) {
            anActionEvent.getPresentation().setVisible(false);
            anActionEvent.getPresentation().setEnabled(false);
            return;
        }
        anActionEvent.getPresentation().setVisible(true);
        anActionEvent.getPresentation().setEnabled(true);

    }

    @Override
    public void doActionPerformed(final AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        if (project == null || project.isDisposed()) {
            return;
        }

        BasicAction.saveAll();

        try {
            final ImportController controller = new ImportController(project);
            controller.showModalDialog();
        } catch (Throwable t) {
            //unexpected error
            logger.warn("ImportAction doActionPerformed failed unexpected error", t);
            VcsNotifier.getInstance(project).notifyError(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_IMPORT),
                    TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ERRORS_UNEXPECTED, t.getMessage()));
        }
    }
}
