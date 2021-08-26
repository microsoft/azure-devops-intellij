// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.exceptions.ToolAuthenticationException;
import com.microsoft.tfs.model.connector.TfsDetailedWorkspaceInfo;
import com.microsoft.tfs.model.connector.TfsWorkspaceInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
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
}
