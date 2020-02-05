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
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.roots.VcsIntegrationEnabler;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ObjectUtils;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationListener;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.stream.Collectors.toList;

public class TfvcIntegrationEnabler extends VcsIntegrationEnabler {

    private static final Logger ourLogger = Logger.getInstance(TfvcIntegrationEnabler.class);

    private final TFSVcs myVcs;

    protected TfvcIntegrationEnabler(@NotNull TFSVcs vcs) {
        super(vcs);
        myVcs = vcs;
    }

    @Override
    public void enable(@NotNull Collection<VcsRoot> vcsRoots) {
        // This override does the same as base method, but tries to determine a workspace directory instead of using
        // project.getBaseDir().
        Collection<VirtualFile> existingRoots = vcsRoots.stream().filter(root -> {
            AbstractVcs<?> vcs = root.getVcs();
            return vcs != null && vcs.getName().equals(myVcs.getName());
        }).map(VcsRoot::getPath).collect(toList());

        if (!existingRoots.isEmpty()) {
            super.enable(vcsRoots);
            return;
        }

        String basePath = myProject.getBasePath();
        if (basePath == null) {
            ourLogger.warn("Project base path is null");
            return;
        }

        Path workspacePath = determineWorkspaceDirectory(Paths.get(basePath));
        VirtualFile workspaceFile = ObjectUtils.notNull(
                LocalFileSystem.getInstance().findFileByIoFile(workspacePath.toFile()));

        if (initOrNotifyError(workspaceFile))
            addVcsRoot(workspaceFile);
    }

    @NotNull
    private static Path determineWorkspaceDirectory(@NotNull Path projectBasePath) {
        String vsClient = PropertyService.getInstance().getProperty(PropertyService.PROP_VISUAL_STUDIO_TF_CLIENT_PATH);
        Path vsClientPath = StringUtil.isEmpty(vsClient) ? null : Paths.get(vsClient);

        Path path = projectBasePath;
        do {
            Workspace workspace = null;
            try {
                ourLogger.info("Analyzing path \"" + path + "\" using TF Everywhere client");
                workspace = CommandUtils.getPartialWorkspace(path);
            } catch (WorkspaceCouldNotBeDeterminedException ex) {
                ourLogger.info("Path \"" + path + "\" has no TF Everywhere workspace");
            }

            if (workspace == null && vsClientPath != null)
                workspace = VisualStudioTfvcCommands.getPartialWorkspaceAsync(vsClientPath, path)
                        .toCompletableFuture().join();

            String currentPath = path.toAbsolutePath().toString();
            boolean correspondsToAnyMapping = workspace != null && workspace.getMappings().stream()
                    .anyMatch(mapping -> FileUtil.pathsEqual(currentPath, mapping.getLocalPath()));
            if (correspondsToAnyMapping)
                return path;

            path = path.getParent();
        } while (path != null);

        ourLogger.warn("No workspace found, falling back to project base path \"" + projectBasePath + "\"");
        return projectBasePath;
    }

    private void addVcsRoot(@NotNull VirtualFile root) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
        List<VirtualFile> currentVcsRoots = Arrays.asList(vcsManager.getRootsUnderVcs(myVcs));

        List<VcsDirectoryMapping> mappings = new ArrayList<>(vcsManager.getDirectoryMappings(myVcs));
        if (!currentVcsRoots.contains(root)) {
            mappings.add(new VcsDirectoryMapping(root.getPath(), myVcs.getName()));
        }

        vcsManager.setDirectoryMappings(mappings);
    }

    private static void showNoVsClientDialog(@Nullable Project project) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        Messages.showErrorDialog(
                project,
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_SETTINGS_VS_CLIENT_PATH_EMPTY),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_IMPORT_WORKSPACE_TITLE));
        ShowSettingsUtil.getInstance().showSettingsDialog(project, TFSVcs.TFVC_NAME);
    }

    private static void showNoWorkspaceDetectedDialog(@Nullable Project project) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        Messages.showErrorDialog(
                project,
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_WORKSPACE_NOT_DETECTED),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_IMPORT_WORKSPACE_TITLE));
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
            AuthenticationProvider authenticationProvider = serverContextManager.getAuthenticationProvider(serverUrl);
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

    public static CompletionStage<Boolean> importWorkspaceAsync(@Nullable Project project, @NotNull ProgressIndicator indicator, @NotNull Path workspacePath) {
        Application application = ApplicationManager.getApplication();

        final double totalSteps = 5.0;
        indicator.setIndeterminate(false);
        indicator.setFraction(0.0 / totalSteps);

        ourLogger.info("Checking if workspace under path \"" + workspacePath + "\" is already imported");
        try {
            Workspace existingWorkspace = CommandUtils.getPartialWorkspace(workspacePath);
            if (existingWorkspace != null) {
                ourLogger.info("Workspace under path \"" + workspacePath + "\" is already imported, exiting");
                return CompletableFuture.completedFuture(true);
            }
        } catch (WorkspaceCouldNotBeDeterminedException ex) {
            ourLogger.info("No known workspace detected under path \"" + workspacePath + "\"");
        }
        indicator.setFraction(1.0 / totalSteps);

        PropertyService propertyService = PropertyService.getInstance();
        String visualStudioClientPath = propertyService.getProperty(PropertyService.PROP_VISUAL_STUDIO_TF_CLIENT_PATH);
        if (StringUtils.isEmpty(visualStudioClientPath)) {
            if (SystemInfo.isWindows)
                application.invokeLater(() -> showNoVsClientDialog(project));

            return CompletableFuture.completedFuture(false);
        }

        ourLogger.info("Determining workspace information from client \"" + visualStudioClientPath + "\" for path \"" + workspacePath + "\"");
        return VisualStudioTfvcCommands.getPartialWorkspaceAsync(Paths.get(visualStudioClientPath), workspacePath)
                .thenCompose(vsWorkspace -> {
                    if (vsWorkspace == null) {
                        ourLogger.info("No workspace information, exiting");
                        application.invokeLater(() -> showNoWorkspaceDetectedDialog(project));
                        return CompletableFuture.completedFuture(false);
                    }

                    if (indicator.isCanceled()) {
                        ourLogger.info("Operation canceled, exiting");
                        return CompletableFuture.completedFuture(false);
                    }

                    indicator.setFraction(2.0 / totalSteps);

                    String collectionUrl = vsWorkspace.getServer();
                    ourLogger.info("Gathering authentication info for URL: " + collectionUrl);
                    return getAuthenticationInfoAsync(collectionUrl).thenApply(authenticationInfo -> {
                        if (authenticationInfo == null) {
                            ourLogger.info("authenticationInfo == null, exiting");
                            return false;
                        }

                        if (indicator.isCanceled()) {
                            ourLogger.info("Operation canceled, exiting");
                            return false;
                        }

                        indicator.setFraction(3.0 / totalSteps);

                        ourLogger.info("Refreshing workspaces for server: " + collectionUrl);
                        CommandUtils.refreshWorkspacesForServer(authenticationInfo, collectionUrl);
                        indicator.setFraction(4.0 / totalSteps);

                        ourLogger.info("Checking if workspace was successfully imported from path: \"" + workspacePath + "\"");
                        Workspace workspace;
                        try {
                            workspace = CommandUtils.getPartialWorkspace(workspacePath);
                            indicator.setFraction(5.0 / totalSteps);
                        } catch (WorkspaceCouldNotBeDeterminedException ex) {
                            return false;
                        }

                        return workspace != null;
                    });
                });
    }

    @Override
    protected boolean initOrNotifyError(@NotNull VirtualFile projectDir) {
        VcsNotifier vcsNotifier = VcsNotifier.getInstance(myProject);
        boolean success;
        try {
            success = ProgressManagerImpl.getInstance().run(new Task.WithResult<Boolean, Exception>(
                    myProject,
                    TfPluginBundle.message(TfPluginBundle.KEY_TFVC_IMPORT_WORKSPACE_TITLE),
                    true) {
                @Override
                protected Boolean compute(@NotNull ProgressIndicator indicator) {
                    return importWorkspaceAsync(myProject, indicator, Paths.get(projectDir.getPath()))
                            .toCompletableFuture().join();
                }
            });

            if (success) {
                vcsNotifier.notifySuccess(
                        TfPluginBundle.message(
                                TfPluginBundle.KEY_TFVC_REPOSITORY_IMPORT_SUCCESS,
                                projectDir.getPresentableUrl()));
            }
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
