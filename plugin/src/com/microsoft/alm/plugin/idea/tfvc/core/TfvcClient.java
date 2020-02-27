// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.services.PropertyService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * This is an interface for TFVC client which have two implementations: one based on TF Everywhere (the "classic"
 * client), and another one based on the reactive TFVC client.
 * <p>
 * This client may have both synchronous and asynchronous operations. By default, you should implement asynchronous
 * ones and use the default implementations for synchronous. Any implementation should be ready that both synchronous
 * and asynchronous versions of its methods will be called on IDEA threads (both UI and background ones).
 * <p>
 * This means that an implementation shouldn't perform any calls to a UI-blocking API from an asynchronous method.
 */
public interface TfvcClient {

    @NotNull
    static TfvcClient getInstance(@NotNull Project project) {
        boolean useReactiveClient = "true".equalsIgnoreCase(
                PropertyService.getInstance().getProperty(PropertyService.PROP_TFVC_USE_REACTIVE_CLIENT));
        return useReactiveClient
                ? ServiceManager.getService(project, ReactiveTfvcClient.class)
                : ServiceManager.getService(project, ClassicTfvcClient.class);
    }

    @NotNull
    CompletableFuture<List<PendingChange>> getStatusForFilesAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess);

    @NotNull
    default List<PendingChange> getStatusForFiles(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess) throws ExecutionException, InterruptedException {
        return getStatusForFilesAsync(serverContext, pathsToProcess).get();
    }

    @NotNull
    CompletableFuture<Void> deleteFilesRecursivelyAsync(
            @NotNull ServerContext serverContext,
            @Nullable String workingFolder,
            @NotNull List<String> filePaths);

    default void deleteFilesRecursively(
            @NotNull ServerContext serverContext,
            @Nullable String workingFolder,
            @NotNull List<String> filePaths) throws ExecutionException, InterruptedException {
        deleteFilesRecursivelyAsync(serverContext, workingFolder, filePaths).get();
    }
}
