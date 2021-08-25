// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.commands.FindWorkspaceCommand;
import com.microsoft.alm.plugin.external.exceptions.ToolAuthenticationException;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.models.WorkspaceInformation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Objects;

public class TfvcWorkspaceLocator {
    private static final Logger logger = LoggerFactory.getLogger(TfvcWorkspaceLocator.class);

    /**
     * This method will return a partially populated Workspace object that includes just the name, server, and mappings.
     * It may require the user to enter the credentials.
     *
     * @param path                  path to a local workspace directory.
     * @param allowCredentialPrompt whether to allow this method to request the user to enter the credentials.
     */
    @NotNull
    public static Workspace getPartialWorkspace(@NotNull java.nio.file.Path path, boolean allowCredentialPrompt) {
        // This command will fail to provide detailed information on a server workspace with no authentication provided.
        logger.info("Determining workspace information from path {}", path);
        FindWorkspaceCommand command = new FindWorkspaceCommand(path.toString(), null, true);
        WorkspaceInformation resultWithNoAuth = command.runSynchronously();
        if (resultWithNoAuth.getDetailed() != null) {
            // Local workspace; no authentication was required.
            logger.info("Workspace information determined successfully without authentication for {}", path);
            return resultWithNoAuth.getDetailed();
        }

        logger.info("Workspace information could not be determined without authentication: {}", path);
        WorkspaceInformation.BasicInformation basicInfo = Objects.requireNonNull(resultWithNoAuth.getBasic());
        URI collectionUri = basicInfo.getCollectionUri();

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
        WorkspaceInformation resultWithAuth = new FindWorkspaceCommand(path.toString(), authenticationInfo, false)
                .runSynchronously();
        return Objects.requireNonNull(resultWithAuth.getDetailed());
    }
}
