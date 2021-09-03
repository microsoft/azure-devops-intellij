// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.exceptions.ToolAuthenticationException;
import com.microsoft.alm.plugin.external.exceptions.WorkspaceCouldNotBeDeterminedException;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.tfs.model.connector.TfsDetailedWorkspaceInfo;
import com.microsoft.tfs.model.connector.TfsWorkspaceInfo;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class TfvcWorkspaceLocator {
    private static final Logger logger = LoggerFactory.getLogger(TfvcWorkspaceLocator.class);

    /**
     * This method will return a partially populated Workspace object that includes just the name, server, and mappings.
     * It may require the user to enter the credentials.
     *
     * @param path                  path to a local workspace directory.
     * @param allowCredentialPrompt whether to allow this method to request the user to enter the credentials.
     * @throws ToolAuthenticationException in case when authentication was required by the workspace, but wasn't allowed
     *                                     by the flag passed.
     */
    @Nullable
    public static TfsDetailedWorkspaceInfo getPartialWorkspace(
            @Nullable Project project,
            @NotNull Path path,
            boolean allowCredentialPrompt) {
        // This command will fail to provide detailed information on a server workspace with no authentication provided.
        logger.info("Determining workspace information from path {}", path);
        TfvcClient client = TfvcClient.getInstance();
        TfsWorkspaceInfo resultWithNoAuth = client.getBasicWorkspaceInfo(project, path);
        if (resultWithNoAuth == null) {
            logger.info("No workspace found at path \"{}\"", path);
            return null;
        }

        if (resultWithNoAuth instanceof TfsDetailedWorkspaceInfo) {
            // Local workspace; no authentication was required.
            logger.info("Workspace information determined successfully without authentication for {}", path);
            return (TfsDetailedWorkspaceInfo)resultWithNoAuth;
        }

        logger.info("Workspace information could not be determined without authentication: {}", path);
        URI collectionUri = URI.create(resultWithNoAuth.getServerUri());

        logger.info(
                "Loading authentication info for URI \"{}\", credential prompt allowed: {}",
                collectionUri,
                allowCredentialPrompt);
        AuthenticationInfo authenticationInfo = ServerContextManager.getInstance()
                .getBestAuthenticationInfo(collectionUri, allowCredentialPrompt);
        if (authenticationInfo == null) {
            logger.warn("Wasn't able to load the authentication information for \"{}\"", collectionUri);
            throw new ToolAuthenticationException();
        }

        logger.info("Loading workspace information for path \"{}\" (using authentication info)", path);
        TfsDetailedWorkspaceInfo resultWithAuth = client.getDetailedWorkspaceInfo(project, authenticationInfo, path);
        return Objects.requireNonNull(resultWithAuth);
    }

    /**
     * Determine partial workspace information from the project base directory.
     *
     * @param project               project to determine the root workspace.
     * @param allowCredentialPrompt whether to allow the command to prompt credentials from user if they're required.
     * @return a partially populated {@link Workspace} object that includes just the name, server, and mappings. Will
     * return null in case the project base directory couldn't be determined.
     * @throws WorkspaceCouldNotBeDeterminedException in case project base directory was determined, but the workspace
     *                                                wasn't found.
     */
    @Nullable
    public static Workspace getPartialWorkspace(@NotNull Project project, boolean allowCredentialPrompt) {
        String basePath = project.getBasePath();
        if (basePath == null) return null;
        TfsDetailedWorkspaceInfo workspace = TfvcWorkspaceLocator.getPartialWorkspace(
                project,
                Paths.get(basePath),
                allowCredentialPrompt);
        if (workspace == null) throw new WorkspaceCouldNotBeDeterminedException();
        return Workspace.fromWorkspaceInfo(workspace);
    }

    /**
     * This method will return just the workspace name or empty string (never null)
     *
     * @param project
     * @return
     */
    public static String getWorkspaceName(final Project project) {
        final Workspace workspace = getPartialWorkspace(project, false);
        if (workspace != null) {
            return workspace.getName();
        }
        return StringUtils.EMPTY;
    }
}
