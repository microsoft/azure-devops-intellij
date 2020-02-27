// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.EULADialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClassicTfvcClient implements TfvcClient {

    @NotNull
    private final Project myProject;

    public ClassicTfvcClient(@NotNull Project project) {
        myProject = project;
    }

    @NotNull
    @Override
    public CompletableFuture<List<PendingChange>> getStatusForFilesAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess) {
        return CompletableFuture.completedFuture(getStatusForFiles(serverContext, pathsToProcess));
    }

    @NotNull
    @Override
    public List<PendingChange> getStatusForFiles(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess) {
        return EULADialog.executeWithGuard(
                myProject,
                () -> CommandUtils.getStatusForFiles(myProject, serverContext, pathsToProcess));
    }

    @NotNull
    @Override
    public CompletableFuture<Void> deleteFilesRecursivelyAsync(
            @NotNull ServerContext serverContext,
            @Nullable String workingFolder,
            @NotNull List<String> filePaths) {
        deleteFilesRecursively(serverContext, workingFolder, filePaths);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void deleteFilesRecursively(
            @NotNull ServerContext serverContext,
            @Nullable String workingFolder,
            @NotNull List<String> filePaths) {
        CommandUtils.deleteFiles(serverContext, filePaths, workingFolder, true);
    }
}
