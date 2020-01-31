package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.roots.VcsIntegrationEnabler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.tfvc.actions.ImportWorkspaceAction;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

public class TfvcIntegrationEnabler extends VcsIntegrationEnabler {

    private static final Logger ourLogger = Logger.getInstance(TfvcIntegrationEnabler.class);

    protected TfvcIntegrationEnabler(@NotNull TFSVcs vcs) {
        super(vcs);
    }

    @Override
    protected boolean initOrNotifyError(@NotNull VirtualFile projectDir) {
        VcsNotifier vcsNotifier = VcsNotifier.getInstance(myProject);
        AtomicBoolean success = new AtomicBoolean();
        UIUtil.invokeAndWaitIfNeeded(
                () -> ImportWorkspaceAction.importWorkspaceUnderProgressAsync(
                        myProject,
                        Paths.get(projectDir.getPath()))).handle((result, error) -> {
            if (error == null) {
                success.set(true);
                vcsNotifier.notifySuccess(
                        TfPluginBundle.message(
                                TfPluginBundle.KEY_TFVC_REPOSITORY_IMPORT_SUCCESS,
                                projectDir.getPresentableUrl()));
            } else {
                success.set(false);
                String exceptionMessage = LocalizationServiceImpl.getInstance().getExceptionMessage(error);
                vcsNotifier.notifyError(
                        TfPluginBundle.message(
                                TfPluginBundle.KEY_TFVC_REPOSITORY_IMPORT_ERROR,
                                projectDir.getPresentableUrl()),
                        exceptionMessage);
                ourLogger.error(error);
            }

            return null;
        }).toCompletableFuture().join();

        return success.get();
    }
}
