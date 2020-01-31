package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.roots.VcsIntegrationEnabler;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.tfvc.actions.ImportWorkspaceAction;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

public class TfvcIntegrationEnabler extends VcsIntegrationEnabler {

    private static final Logger ourLogger = Logger.getInstance(TfvcIntegrationEnabler.class);

    protected TfvcIntegrationEnabler(@NotNull TFSVcs vcs) {
        super(vcs);
    }

    @Override
    protected boolean initOrNotifyError(@NotNull VirtualFile projectDir) {
        VcsNotifier vcsNotifier = VcsNotifier.getInstance(myProject);
        boolean success;
        try {
            ProgressManagerImpl.getInstance().run(new Task.Modal(
                    myProject,
                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_IMPORT_WORKSPACE_TITLE),
                    true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    ImportWorkspaceAction.importWorkspaceAsync(myProject, indicator, Paths.get(projectDir.getPath()))
                            .toCompletableFuture().join();
                }
            });

            vcsNotifier.notifySuccess(
                    TfPluginBundle.message(
                            TfPluginBundle.KEY_TFVC_REPOSITORY_IMPORT_SUCCESS,
                            projectDir.getPresentableUrl()));
            success = true;
        } catch (Throwable error) {
            ourLogger.error(error);
            vcsNotifier.notifyError(
                    TfPluginBundle.message(
                            TfPluginBundle.KEY_TFVC_REPOSITORY_IMPORT_ERROR,
                            projectDir.getPresentableUrl()),
                    LocalizationServiceImpl.getInstance().getExceptionMessage(error));
            success = false;
        }

        return success;
    }
}
