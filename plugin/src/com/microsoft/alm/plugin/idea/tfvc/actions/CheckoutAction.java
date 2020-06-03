// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.util.ui.UIUtil;
import com.microsoft.alm.plugin.external.utils.TfvcCheckoutResultUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.tfvc.core.TfvcClient;
import com.microsoft.tfs.model.connector.TfvcCheckoutResult;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class CheckoutAction extends SimpleMultipleItemAction {

    private static final Logger ourLogger = Logger.getInstance(CheckoutAction.class);

    public CheckoutAction() {
        super(
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_CHECKOUT_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_CHECKOUT_DESCRIPTION));
    }

    @Override
    protected void execute(@NotNull MultipleItemActionContext actionContext) {
        Project project = actionContext.project;
        ProgressManager.getInstance().run(
                new Task.Modal(
                        project,
                        TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_CHECKOUT_PROGRESS),
                        false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        TfvcClient client = TfvcClient.getInstance(project);
                        List<Path> filePaths = actionContext.itemInfos.stream()
                                .map(item -> Paths.get(item.getLocalItem()))
                                .collect(Collectors.toList());
                        TfvcCheckoutResult checkoutResult = client.checkoutForEdit(
                                actionContext.serverContext,
                                filePaths,
                                true);
                        try {
                            TfvcCheckoutResultUtils.verify(checkoutResult);
                        } catch (VcsException e) {
                            ourLogger.warn(e);
                            UIUtil.invokeLaterIfNeeded(() -> Messages.showErrorDialog(
                                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_CHECKOUT_TITLE),
                                    LocalizationServiceImpl.getInstance().getExceptionMessage(e)));
                        }
                    }
                });
    }
}
