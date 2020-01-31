package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.roots.VcsIntegrationEnabler;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationListener;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.exceptions.WorkspaceCouldNotBeDeterminedException;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.external.visualstudio.VisualStudioTfvcCommands;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.services.PropertyService;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class TfvcIntegrationEnabler extends VcsIntegrationEnabler {

    private static final Logger ourLogger = Logger.getInstance(TfvcIntegrationEnabler.class);

    protected TfvcIntegrationEnabler(@NotNull TFSVcs vcs) {
        super(vcs);
    }

    private static void showNoVsClientDialog(@Nullable Project project) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        Messages.showErrorDialog(
                project,
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_VS_CLIENT_PATH_EMPTY),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_IMPORT_WORKSPACE_TITLE));
        ShowSettingsUtil.getInstance().showSettingsDialog(project, TFSVcs.TFVC_NAME);
    }

    private static void showNoWorkspaceDetectedDialog(@Nullable Project project) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        Messages.showErrorDialog(
                project,
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_WORKSPACE_NOT_DETECTED),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_IMPORT_WORKSPACE_TITLE));
    }

    private static void showErrorDialog(@Nullable Project project, Throwable error) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        LocalizationServiceImpl localizationService = LocalizationServiceImpl.getInstance();

        Messages.showErrorDialog(
                project,
                localizationService.getExceptionMessage(error),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_IMPORT_WORKSPACE_TITLE));
    }

    private static CompletionStage<AuthenticationInfo> getAuthenticationInfoAsync(@NotNull String serverUrl) {
        CompletableFuture<AuthenticationInfo> result = new CompletableFuture<>();

        try {
            // First, try to get the authentication info from existing context:
            ourLogger.info("Server context lookup for URL: " + serverUrl);
            ServerContextManager serverContextManager = ServerContextManager.getInstance();
            ServerContext serverContext = serverContextManager.get(serverUrl);
            if (serverContext != null) {
                ourLogger.info("Server context found for URL: " + serverUrl);
                AuthenticationInfo authenticationInfo = serverContext.getAuthenticationInfo();
                if (authenticationInfo != null) {
                    ourLogger.info("Authentication info found for URL: " + serverUrl);
                    result.complete(authenticationInfo);
                    return result;
                }
            }

            ourLogger.info("Authentication provider pass for URL: " + serverUrl);
            AuthenticationProvider authenticationProvider = TfsAuthenticationProvider.getInstance();
            authenticationProvider.authenticateAsync(serverUrl, new AuthenticationListener() {
                @Override
                public void authenticating() {
                    ourLogger.info("Authenticating URL: " + serverUrl);
                }

                @Override
                public void authenticated(AuthenticationInfo authenticationInfo, Throwable throwable) {
                    ourLogger.info("Authentication result for URL " + serverUrl + ": " + (authenticationInfo != null));
                    if (throwable != null) {
                        result.completeExceptionally(throwable);
                        return;
                    }

                    result.complete(authenticationInfo);
                }
            });
        } catch (Throwable t) {
            result.completeExceptionally(t);
        }

        return result;
    }

    public static CompletionStage<Void> importWorkspaceAsync(@Nullable Project project, @NotNull ProgressIndicator indicator, @NotNull Path workspacePath) {
        Application application = ApplicationManager.getApplication();

        final double totalSteps = 4.0;
        indicator.setIndeterminate(false);
        indicator.setFraction(0.0 / totalSteps);

        ourLogger.info("Checking if workspace under path \"" + workspacePath + "\" is already imported");
        try {
            Workspace existingWorkspace = CommandUtils.getPartialWorkspace(workspacePath);
            if (existingWorkspace != null) {
                ourLogger.info("Workspace under path \"" + workspacePath + "\" is already imported, exiting");
                // TODO: Call the VcsIntegrationEnabler here
                return CompletableFuture.completedFuture(null);
            }
        } catch (WorkspaceCouldNotBeDeterminedException ex) {
            ourLogger.info("No known workspace detected under path \"" + workspacePath + "\"");
        }
        indicator.setFraction(1.0 / totalSteps);

        PropertyService propertyService = PropertyService.getInstance();
        String visualStudioClientPath = propertyService.getProperty(PropertyService.PROP_VISUAL_STUDIO_TF_CLIENT_PATH);
        if (StringUtils.isEmpty(visualStudioClientPath)) {
            application.invokeLater(() -> showNoVsClientDialog(project));
            return CompletableFuture.completedFuture(null);
        }

        ourLogger.info("Determining workspace information from client \"" + visualStudioClientPath + "\" for path \"" + workspacePath + "\"");
        return VisualStudioTfvcCommands.getPartialWorkspaceAsync(Paths.get(visualStudioClientPath), workspacePath)
                .thenCompose(vsWorkspace -> {
                    if (vsWorkspace == null) {
                        ourLogger.info("No workspace information, exiting");
                        application.invokeLater(() -> showNoWorkspaceDetectedDialog(project));
                        return CompletableFuture.completedFuture(null);
                    }

                    if (indicator.isCanceled()) {
                        ourLogger.info("Operation canceled, exiting");
                        return CompletableFuture.completedFuture(null);
                    }

                    indicator.setFraction(2.0 / totalSteps);

                    String collectionUrl = vsWorkspace.getServer();
                    ourLogger.info("Gathering authentication info for URL: " + collectionUrl);
                    return getAuthenticationInfoAsync(collectionUrl).thenAccept(authenticationInfo -> {
                        if (authenticationInfo == null) {
                            ourLogger.info("authenticationInfo == null, exiting");
                            return;
                        }

                        if (indicator.isCanceled()) {
                            ourLogger.info("Operation canceled, exiting");
                            return;
                        }

                        indicator.setFraction(3.0 / totalSteps);

                        ourLogger.info("Refreshing workspaces for server: " + collectionUrl);
                        CommandUtils.refreshWorkspacesForServer(authenticationInfo, collectionUrl);
                        indicator.setFraction(4.0 / totalSteps);

                        // TODO: Call the VcsIntegrationEnabler here
                    });
                }).exceptionally(ex -> {
                    ourLogger.error(ex);
                    application.invokeLater(() -> showErrorDialog(project, ex));
                    return null;
                });
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
                    importWorkspaceAsync(myProject, indicator, Paths.get(projectDir.getPath()))
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
