// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.actions.RefreshAction;
import com.intellij.util.ObjectUtils;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfIgnoreUtil;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class AddFileToTfIgnoreAction extends AnAction {
    private static final Logger ourLogger = Logger.getInstance(AddFileToTfIgnoreAction.class);

    @NotNull
    private final Project myProject;

    @NotNull
    private final String myServerFilePath;

    public AddFileToTfIgnoreAction(@NotNull Project project, @NotNull String serverFilePath) {
        super(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_ACTION_ADD_TO_TFIGNORE));
        myProject = project;
        myServerFilePath = serverFilePath;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        ourLogger.info("Performing AddFileToTfIgnoreAction for " + myServerFilePath);

        Workspace partialWorkspace = CommandUtils.getPartialWorkspace(myProject, true);
        String filePath = ObjectUtils.assertNotNull(
                TfsFileUtil.translateServerItemToLocalItem(partialWorkspace.getMappings(), myServerFilePath, false));
        File localFile = new File(filePath);
        ourLogger.info("Local file path: " + localFile.getAbsolutePath());

        File tfIgnore = TfIgnoreUtil.findNearestOrRootTfIgnore(partialWorkspace.getMappings(), localFile);
        ourLogger.info(".tfignore location: " + (tfIgnore == null ? "null" : tfIgnore.getAbsolutePath()));

        if (tfIgnore != null) {
            CommandProcessor.getInstance().executeCommand(
                    myProject,
                    () -> ApplicationManager.getApplication().runWriteAction(() -> {
                        try {
                            TfIgnoreUtil.addToTfIgnore(this, tfIgnore, localFile);
                        } catch (IOException ex) {
                            ourLogger.error(ex);
                        }
                    }),
                    null,
                    null);

            // Usually the TFVC is already in a bad state (i.e. all the changed files are lost) before this action gets
            // called, so we need to refresh the VCS changes afterwards.
            RefreshAction.doRefresh(myProject);
        }
    }
}
