// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.servertree;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.microsoft.alm.plugin.idea.common.actions.InstrumentedAction;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateVirtualFolderAction extends InstrumentedAction {
    public static Logger logger = LoggerFactory.getLogger(CreateVirtualFolderAction.class);

    public CreateVirtualFolderAction() {
        super(false);
    }

    @Override
    public void doUpdate(final AnActionEvent e) {
        boolean isEnabled = isEnabled(e);
        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            e.getPresentation().setVisible(isEnabled);
        } else {
            e.getPresentation().setEnabled(isEnabled);
        }
    }

    private static boolean isEnabled(final AnActionEvent e) {
        final TfsTreeForm form = TfsTreeForm.KEY.getData(e.getDataContext());
        return form != null && form.getSelectedItem() != null && form.canCreateVirtualFolders();
    }

    @Override
    public void doActionPerformed(final AnActionEvent e) {
        final TfsTreeForm form = TfsTreeForm.KEY.getData(e.getDataContext());
        final String folderName = Messages.showInputDialog(form.getContentPane(),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_SERVER_TREE_CREATE_FOLDER_MSG),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_SERVER_TREE_CREATE_FOLDER_TITLE), null);
        if (StringUtil.isEmpty(folderName)) {
            logger.info("No folder name was found");
            return;
        }
        logger.info("Creating a new vrtual folder");
        form.createVirtualFolder(folderName);
    }
}
