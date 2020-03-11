// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.services.PropertyService;
import com.microsoft.tfs.model.connector.TfsPath;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletionStage;
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
    CompletionStage<List<PendingChange>> getStatusForFilesAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess);

    @NotNull
    default List<PendingChange> getStatusForFiles(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess) throws ExecutionException, InterruptedException {
        return getStatusForFilesAsync(serverContext, pathsToProcess).toCompletableFuture().get();
    }

    @NotNull
    CompletionStage<Void> deleteFilesRecursivelyAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items);

    default void deleteFilesRecursively(
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items) throws ExecutionException, InterruptedException {
        deleteFilesRecursivelyAsync(serverContext, items).toCompletableFuture().get();
    }

    /**
     * Performs asynchronous local change undo for passed file paths. This operation is non-recursive.
     *
     * @param serverContext server context to extract a authorization information from
     * @param items         list of items to undo changes
     * @return a completion stage that will be resolved when the operation ends.
     */
    @NotNull
    CompletionStage<Void> undoLocalChangesAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items);

    /**
     * Performs synchronous local change undo for passed file paths. This operation is non-recursive.
     *
     * @param serverContext server context to extract a authorization information from
     * @param items         list of items to undo changes
     */
    default void undoLocalChanges(
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items) throws ExecutionException, InterruptedException {
        undoLocalChangesAsync(serverContext, items).toCompletableFuture().get();
    }
}
