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


import com.intellij.openapi.ui.Messages;
import com.microsoft.alm.plugin.external.models.TfvcLabel;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.ui.ApplyLabelDialog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LabelAction extends MultipleItemAction {
    public static final Logger logger = LoggerFactory.getLogger(LabelAction.class);

    public LabelAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_MSG));
    }

    @Override
    protected void execute(@NotNull final MultipleItemActionContext actionContext) {
        // Open the Apply Label dialog and allow the user to enter label name and description
        final ApplyLabelDialog d = new ApplyLabelDialog(actionContext.project, actionContext.itemInfos);
        if (!d.showAndGet()) {
            logger.info("User canceled Apply Label action");
            actionContext.cancelled = true;
            return;
        }

        // Check to see if the label name already exists
        final List<TfvcLabel> labels = new ArrayList<TfvcLabel>();
        runWithProgress(actionContext, new Runnable() {
            public void run() {
                logger.info("Getting labels that match: " + d.getLabelName());
                labels.addAll(CommandUtils.getLabels(actionContext.serverContext, actionContext.workingFolder, d.getLabelName()));
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_PROGRESS_GATHERING_INFORMATION));

        if (actionContext.hasErrors()) {
            logger.info("Errors occurred. Returning control to the super class.");
            return;
        }

        if (!labels.isEmpty()) {
            logger.info("There is a label on the server already with the name " + d.getLabelName());
            if (Messages.showOkCancelDialog(actionContext.project,
                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_OVERWRITE, d.getLabelName()),
                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_TITLE),
                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_OVERWRITE_OK_TEXT),
                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_OVERWRITE_CANCEL_TEXT),
                    Messages.getQuestionIcon()) !=
                    Messages.OK) {
                // The user chose not to overwrite the label
                logger.info("User canceled Update Label action");
                actionContext.cancelled = true;
                return;
            }
        }

        final StringBuilder successMessage = new StringBuilder();
        runWithProgress(actionContext, new Runnable() {
            public void run() {
                logger.info("Creating/Updating the label on the server");
                final boolean labelCreated = CommandUtils.createLabel(actionContext.serverContext, actionContext.workingFolder,
                        d.getLabelName(), d.getLabelComment(), d.isRecursiveChecked(), d.getLabelItemSpecs());
                if (labelCreated) {
                    successMessage.append(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_SUCCESS_CREATED, d.getLabelName()));
                } else {
                    successMessage.append(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_SUCCESS_UPDATED, d.getLabelName()));
                }
            }
        }, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_PROGRESS_CREATING_LABEL));

        if (!actionContext.hasErrors()) {
            logger.info("Label successfully created/updated");
            showSuccess(actionContext, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_LABEL_TITLE), successMessage.toString());
        }

        // Note: errors that exist at this point will be shown by our super class.
    }
}
