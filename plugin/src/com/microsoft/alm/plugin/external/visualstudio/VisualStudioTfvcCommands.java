// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.visualstudio;

import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.external.models.ToolVersion;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualStudioTfvcCommands {

    private static final Logger ourLogger = Logger.getInstance(VisualStudioTfvcCommands.class);

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
    private static final Pattern WORKFOLD_REPORT_PATTERN = Pattern.compile("^(.*?): (.*)$");
    private static final Pattern WORKFOLD_WORKSPACE_NAME_AND_OWNER_PATTERN = Pattern.compile("^(.*?) \\((.*)\\)$");

    @NotNull
    public static CompletionStage<ToolVersion> getVersionAsync(@NotNull Project project, @NotNull Path clientPath) {
        String basePath = project.getBasePath();
        return executeClientAndProcessOutputAsync(
                clientPath,
                basePath == null ? null : Paths.get(basePath),
                Collections.emptyList(),
                output -> {
                    for (String line : output) {
                        Matcher matcher = VERSION_PATTERN.matcher(line);
                        if (matcher.find()) {
                            ourLogger.info("Client version: " + matcher.group());
                            return new ToolVersion(matcher.group());
                        }
                    }

                    return null;
                });
    }

    /**
     * Returns partial workspace information (only workspace path, owner, collection name, and mappings) from the Visual
     * Studio TF client.
     *
     * @param clientPath    path to VS TF client.
     * @param workspacePath path to workspace.
     * @return workspace or null if it could not be determined.
     */
    @NotNull
    public static CompletionStage<Workspace> getPartialWorkspaceAsync(
            @NotNull Path clientPath,
            @NotNull Path workspacePath) {
        return executeClientAndProcessOutputAsync(clientPath, workspacePath, Collections.singletonList("workfold"), output -> {
            String workspaceName = null, workspaceUser = null, collectionUrl = null;
            boolean workspaceDataAvailable = false;
            List<Workspace.Mapping> mappings = Lists.newArrayList();
            for (String line : output) {
                Matcher matcher = WORKFOLD_REPORT_PATTERN.matcher(line);
                if (matcher.matches()) {
                    if (workspaceName == null) {
                        String workspaceNameAndOwner = matcher.group(2);
                        Matcher workspaceNameAndOwnerMatcher = WORKFOLD_WORKSPACE_NAME_AND_OWNER_PATTERN.matcher(
                                workspaceNameAndOwner);
                        if (workspaceNameAndOwnerMatcher.matches()) {
                            workspaceName = workspaceNameAndOwnerMatcher.group(1);
                            workspaceUser = workspaceNameAndOwnerMatcher.group(2);
                        } else {
                            workspaceName = workspaceNameAndOwner;
                        }

                        workspaceDataAvailable = true;
                    } else if (collectionUrl == null) {
                        collectionUrl = matcher.group(2);
                    } else {
                        String serverPath = matcher.group(1).trim();
                        String localPath = matcher.group(2);
                        mappings.add(new Workspace.Mapping(serverPath, localPath, false));
                    }
                }
            }

            return workspaceDataAvailable
                    ? new Workspace(collectionUrl, workspaceName, null, workspaceUser, null, mappings)
                    : null;
        });
    }

    private static <T> CompletionStage<T> executeClientAndProcessOutputAsync(
            @NotNull Path clientPath,
            @Nullable Path workingDirectory,
            @NotNull List<String> arguments,
            @NotNull Function<List<String>, T> action) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    List<String> output = VisualStudioTfvcClient.executeClientAndGetOutput(
                            clientPath,
                            workingDirectory,
                            arguments);
                    T processResult = action.apply(output);

                    result.complete(processResult);
                } catch (Throwable t) {
                    result.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            result.completeExceptionally(t);
        }

        return result;
    }
}
