// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.ExtendedItemInfo;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.services.PropertyService;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfsPath;
import com.microsoft.tfs.model.connector.TfvcCheckoutResult;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

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
            @NotNull List<String> pathsToProcess) {
        try {
            return getStatusForFilesAsync(serverContext, pathsToProcess).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Collects local repository information on selected items asynchronously and passes it into a user-provided
     * callback.
     *
     * @param serverContext  server context to extract a authorization information from
     * @param pathsToProcess list of items to process information
     * @param onItemReceived callback that will be called for each item received. Should be free-threaded (may be
     *                       called from any thread, including the one that performed this call), but will be called in
     *                       a thread-safe way (multiple simultaneous calls are prohibited).
     * @return a completion stage that will be finished after the call is completely finished and all of the callbacks
     * are done.
     */
    @NotNull
    CompletionStage<Void> getLocalItemsInfoAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ItemInfo> onItemReceived);

    /**
     * Collects local repository information on selected items and passes it into a user-provided callback.
     *
     * @param serverContext  server context to extract a authorization information from
     * @param pathsToProcess list of items to process information
     * @param onItemReceived callback that will be called for each item received. Should be free-threaded (may be
     *                       called from any thread, including the one that performed this call), but will be called in
     *                       a thread-safe way (multiple simultaneous calls are prohibited).
     */
    default void getLocalItemsInfo(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ItemInfo> onItemReceived) {
        try {
            getLocalItemsInfoAsync(serverContext, pathsToProcess, onItemReceived).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Collects extended item information on selected items asynchronously and passes it into a user-provided callback.
     * Differs from {@link #getLocalItemsInfoAsync(ServerContext, List, Consumer)} in that it also returns lock information.
     *
     * @param serverContext  server context to extract a authorization information from
     * @param pathsToProcess list of items to process information
     * @param onItemReceived callback that will be called for each item received. Should be free-threaded (may be
     *                       called from any thread, including the one that performed this call), but will be called in
     *                       a thread-safe way (multiple simultaneous calls are prohibited).
     * @return a completion stage that will be finished after the call is completely finished and all of the callbacks
     * are done.
     */
    @NotNull
    CompletionStage<Void> getExtendedItemsInfoAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ExtendedItemInfo> onItemReceived);

    /**
     * Collects extended item information on selected items and passes it into a user-provided callback. Differs from
     * {@link #getLocalItemsInfo(ServerContext, List, Consumer)} in that it also returns lock information.
     *
     * @param serverContext  server context to extract a authorization information from
     * @param pathsToProcess list of items to process information
     * @param onItemReceived callback that will be called for each item received. Should be free-threaded (may be
     *                       called from any thread, including the one that performed this call), but will be called in
     *                       a thread-safe way (multiple simultaneous calls are prohibited).
     */
    default void getExtendedItemsInfo(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ExtendedItemInfo> onItemReceived) {
        try {
            getExtendedItemsInfoAsync(serverContext, pathsToProcess, onItemReceived).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes the passed paths recursively using the TFS client.
     *
     * @param serverContext server context to authenticate.
     * @param items         items to delete.
     * @return a completion stage that will be finished after the call is completely finished and all of the callbacks
     * are done.
     */
    @NotNull
    CompletionStage<TfvcDeleteResult> deleteFilesRecursivelyAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items);

    /**
     * Deletes the passed paths recursively using the TFS client.
     *
     * @param serverContext server context to authenticate.
     * @param items         items to delete.
     */
    default TfvcDeleteResult deleteFilesRecursively(@NotNull ServerContext serverContext, @NotNull List<TfsPath> items) {
        try {
            return deleteFilesRecursivelyAsync(serverContext, items).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs asynchronous local change undo for passed file paths. This operation is non-recursive.
     *
     * @param serverContext server context to extract a authorization information from
     * @param items         list of items to undo changes
     * @return a completion stage with the list of the files undone that will be resolved when the operation ends.
     */
    @NotNull
    CompletionStage<List<TfsLocalPath>> undoLocalChangesAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items);

    /**
     * Performs synchronous local change undo for passed file paths. This operation is non-recursive.
     *
     * @param serverContext server context to extract a authorization information from
     * @param items         list of items to undo changes
     * @return list of the paths undone.
     */
    default List<TfsLocalPath> undoLocalChanges(@NotNull ServerContext serverContext, @NotNull List<TfsPath> items) {
        try {
            return undoLocalChangesAsync(serverContext, items).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs asynchronous checkout of passed files for edit.
     *
     * @param serverContext a server context to extract the authorization information from.
     * @param filePaths     list of file paths to checkout.
     * @param recursive     whether the operation should be recursive.
     * @return a completion stage with the checkout result that will be resolved when the operation ends.
     */
    @NotNull
    CompletionStage<TfvcCheckoutResult> checkoutForEditAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<Path> filePaths,
            boolean recursive);

    /**
     * Performs asynchronous checkout of passed files for edit.
     *
     * @param serverContext a server context to extract the authorization information from.
     * @param filePaths     list of file paths to checkout.
     * @param recursive     whether the operation should be recursive.
     * @return the checkout result.
     */
    @NotNull
    default TfvcCheckoutResult checkoutForEdit(
            @NotNull ServerContext serverContext,
            @NotNull List<Path> filePaths,
            boolean recursive) {
        try {
            return checkoutForEditAsync(serverContext, filePaths, recursive).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs file rename using TFVC asynchronously.
     *
     * @param serverContext a server context to extract the authorization information from.
     * @param oldFile       the old file path and name.
     * @param newFile       the new file path and name.
     * @return a completion stage with the checkout result that will be resolved when the operation ends. The stage
     * contains the operation success status.
     */
    @NotNull
    CompletionStage<Boolean> renameFileAsync(
            @NotNull ServerContext serverContext,
            @NotNull Path oldFile,
            @NotNull Path newFile);

    /**
     * Performs file rename using TFVC.
     *
     * @param serverContext a server context to extract the authorization information from.
     * @param oldFile       the old file path and name.
     * @param newFile       the new file path and name.
     * @return whether the operation was successful.
     */
    default boolean renameFile(@NotNull ServerContext serverContext, @NotNull Path oldFile, @NotNull Path newFile) {
        try {
            return renameFileAsync(serverContext, oldFile, newFile).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
