// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.progress.ProgressManager;
import com.microsoft.alm.plugin.external.commands.LockCommand;
import com.microsoft.alm.plugin.external.exceptions.LockFailedException;
import com.microsoft.alm.plugin.external.exceptions.ToolBadExitCodeException;
import com.microsoft.alm.plugin.external.models.ExtendedItemInfo;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.TfvcClient;
import com.microsoft.alm.plugin.idea.tfvc.ui.LockItemsDialog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LockAction extends MultipleItemAction<ExtendedItemInfo> {
    public static final Logger logger = LoggerFactory.getLogger(LockAction.class);

    public LockAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LOCK_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LOCK_MSG));
    }

    @Override
    protected void loadItemInfoCollection(MultipleItemActionContext context, List<String> localPaths) {
        TfvcClient client = TfvcClient.getInstance(context.project);
        client.getExtendedItemsInfo(context.serverContext, localPaths, context.itemInfos::add);
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
        runWithProgress(actionContext, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            List<ExtendedItemInfo> selectedItems = d.getSelectedItems();
            final List<String> itemSpecs = new ArrayList<>(selectedItems.size());
            for (ExtendedItemInfo item : selectedItems) {
                itemSpecs.add(item.getServerItem());
            }

            logger.info("Calling the lock command");
            try {
                CommandUtils.lock(actionContext.serverContext, actionContext.workingFolder,
                        d.getLockLevel(), d.getRecursive(), itemSpecs);
            } catch (ToolBadExitCodeException ex) {
                if (ex.getExitCode() == LockCommand.LOCK_FAILED_EXIT_CODE) {
                    throw new LockFailedException();
                }
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
