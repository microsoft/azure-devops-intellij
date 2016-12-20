// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.progress.ProgressManager;
import com.microsoft.alm.plugin.external.commands.LockCommand;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.ui.LockItemsDialog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LockAction extends MultipleItemAction {
    public static final Logger logger = LoggerFactory.getLogger(LockAction.class);

    public LockAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LOCK_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LOCK_MSG));
    }

    @Override
    protected void execute(@NotNull final MultipleItemActionContext actionContext) {
        logger.info("Starting Lock/unlock action");
        final LockItemsDialog d = new LockItemsDialog(actionContext.project, actionContext.itemInfos);
        d.show();
        int exitCode = d.getExitCode();
        if (exitCode != LockItemsDialog.LOCK_EXIT_CODE && exitCode != LockItemsDialog.UNLOCK_EXIT_CODE) {
            logger.info("User canceled Lock/unlock action");
            actionContext.cancelled = true;
            return;
        }

        final String title = d.getLockLevel() == LockCommand.LockLevel.NONE ?
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LOCK_PROGRESS_UNLOCKING) :
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LOCK_PROGRESS_LOCKING);
        runWithProgress(actionContext, new Runnable() {
            public void run() {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                final List<ItemInfo> selectedItems = d.getSelectedItems();
                final List<String> itemSpecs = new ArrayList<String>(selectedItems.size());
                for (final ItemInfo item : selectedItems) {
                    itemSpecs.add(item.getServerItem());
                }

                logger.info("Calling the lock command");
                CommandUtils.lock(actionContext.serverContext, actionContext.workingFolder,
                        d.getLockLevel(), d.getRecursive(), itemSpecs);
            }
        }, title);

        if (!actionContext.hasErrors()) {
            logger.info("Files locked/unlocked successfully.");
            final String message = exitCode == LockItemsDialog.LOCK_EXIT_CODE ?
                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LOCK_SUCCESS_LOCKED) :
                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LOCK_SUCCESS_UNLOCKED);
            showSuccess(actionContext, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LOCK_TITLE), message);
        }

        // Note: errors that exist at this point will be shown by our super class.
    }
}
