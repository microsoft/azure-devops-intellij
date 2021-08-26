// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.ExtendedItemInfo;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.reactive.ReactiveTfvcClientHolder;
import com.microsoft.alm.plugin.external.reactive.ServerIdentification;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.tfs.model.connector.TfsDetailedWorkspaceInfo;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfsPath;
import com.microsoft.tfs.model.connector.TfsWorkspaceInfo;
import com.microsoft.tfs.model.connector.TfvcCheckoutResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The new, "reactive" TFVC client implementation. Most of the methods are asynchronous and delegate to the external
 * client that is always running. Works with Rd protocol.
 */
public class ReactiveTfvcClient implements TfvcClient {

    private static final Logger ourLogger = Logger.getInstance(ReactiveTfvcClient.class);

    public static ReactiveTfvcClient getInstance() {
        return ServiceManager.getService(ReactiveTfvcClient.class);
    }

    private static ServerIdentification getServerIdentification(ServerContext serverContext) {
        return new ServerIdentification(serverContext.getCollectionURI(), serverContext.getAuthenticationInfo());
    }

    @NotNull
    private static <T> CompletionStage<T> traceTime(
            @NotNull String title,
            @NotNull Supplier<CompletionStage<T>> action) {
        long startTime = System.nanoTime();
        return action.get().whenComplete((result, ex) -> {
            long endTime = System.nanoTime();
            double seconds = ((double) endTime - startTime) / 1_000_000_000.0;
            String status = ex == null ? "successfully" : "with error";
            ourLogger.trace(title + " finished " + status + " in " + seconds + " sec");
        });
    }

    @Override
    @NotNull
    public CompletionStage<List<PendingChange>> getStatusForFilesAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess) {
        return traceTime("Status", () -> {
            ServerIdentification serverIdentification = getServerIdentification(serverContext);
            Stream<Path> paths = pathsToProcess.stream().map(Paths::get);

            return ReactiveTfvcClientHolder.getInstance().getClient(project)
                    .thenCompose(client -> client.getPendingChangesAsync(serverIdentification, paths));
        });
    }

    @NotNull
    @Override
    public CompletionStage<Void> getLocalItemsInfoAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ItemInfo> onItemReceived) {
        return traceTime("Info", () -> {
            ServerIdentification serverIdentification = getServerIdentification(serverContext);
            Stream<Path> paths = pathsToProcess.stream().map(Paths::get);
            return ReactiveTfvcClientHolder.getInstance().getClient(project)
                    .thenCompose(client -> client.getLocalItemsInfoAsync(serverIdentification, paths, onItemReceived));
        });
    }

    @NotNull
    @Override
    public CompletionStage<Void> getExtendedItemsInfoAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ExtendedItemInfo> onItemReceived) {
        return traceTime("Extended info", () -> {
            ServerIdentification serverIdentification = getServerIdentification(serverContext);
            Stream<Path> paths = pathsToProcess.stream().map(Paths::get);
            return ReactiveTfvcClientHolder.getInstance().getClient(project)
                    .thenCompose(client -> client.getExtendedItemsInfoAsync(
                            serverIdentification,
                            paths,
                            onItemReceived));
        });
    }

    @NotNull
    @Override
    public CompletionStage<List<Path>> addFilesAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<Path> files) {
        return traceTime("Add", () -> {
            ServerIdentification serverIdentification = getServerIdentification(serverContext);
            List<TfsLocalPath> localPaths = files.stream()
                    .map(TfsFileUtil::createLocalPath)
                    .collect(Collectors.toList());
            return ReactiveTfvcClientHolder.getInstance().getClient(project)
                    .thenCompose(client -> client.addFilesAsync(serverIdentification, localPaths))
                    .thenApply(paths -> paths.stream()
                            .map(path -> Paths.get(path.getPath()))
                            .collect(Collectors.toList()));
        });
    }

    @NotNull
    @Override
    public CompletionStage<TfvcDeleteResult> deleteFilesRecursivelyAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items) {
        return traceTime("Delete", () -> {
            ServerIdentification serverIdentification = getServerIdentification(serverContext);
            return ReactiveTfvcClientHolder.getInstance().getClient(project)
                    .thenCompose(client -> client.deleteFilesRecursivelyAsync(serverIdentification, items))
                    .thenApply(result -> {
                        List<Path> deletedPaths = result.getDeletedPaths().stream()
                                .map(localPath -> Paths.get(localPath.getPath())).collect(Collectors.toList());

                        return new TfvcDeleteResult(deletedPaths, result.getNotFoundPaths(), result.getErrorMessages());
                    });
        });
    }

    @NotNull
    @Override
    public CompletionStage<List<TfsLocalPath>> undoLocalChangesAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items) {
        return traceTime("Undo", () -> {
            ServerIdentification serverIdentification = getServerIdentification(serverContext);
            return ReactiveTfvcClientHolder.getInstance().getClient(project)
                    .thenCompose(client -> client.undoLocalChangesAsync(serverIdentification, items));
        });
    }

    @NotNull
    @Override
    public CompletionStage<TfvcCheckoutResult> checkoutForEditAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<Path> filePaths,
            boolean recursive) {
        return traceTime("Checkout", () -> {
            ServerIdentification serverIdentification = getServerIdentification(serverContext);
            List<TfsLocalPath> paths = filePaths.stream()
                    .map(TfsFileUtil::createLocalPath)
                    .collect(Collectors.toList());
            return ReactiveTfvcClientHolder.getInstance().getClient(project)
                    .thenCompose(client -> client.checkoutFilesForEditAsync(
                            serverIdentification,
                            paths,
                            recursive));
        });
    }

    @NotNull
    @Override
    public CompletionStage<Boolean> renameFileAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull Path oldFile,
            @NotNull Path newFile) {
        return traceTime("Rename", () -> {
            ServerIdentification serverIdentification = getServerIdentification(serverContext);
            TfsLocalPath oldPath = TfsFileUtil.createLocalPath(oldFile);
            TfsLocalPath newPath = TfsFileUtil.createLocalPath(newFile);
            return ReactiveTfvcClientHolder.getInstance().getClient(project)
                    .thenCompose(client -> client.renameFileAsync(serverIdentification, oldPath, newPath));
        });
    }

    @NotNull
    @Override
    public CompletionStage<TfsWorkspaceInfo> getBasicWorkspaceInfoAsync(
            @Nullable Project project,
            @NotNull Path workspacePath) {
        return traceTime("Basic workspace info", () -> ReactiveTfvcClientHolder.getInstance().getClient(project)
                .thenCompose(client -> client.getBasicWorkspaceInfoAsync(workspacePath)));
    }

    @NotNull
    @Override
    public CompletionStage<TfsDetailedWorkspaceInfo> getDetailedWorkspaceInfoAsync(
            @Nullable Project project,
            @NotNull AuthenticationInfo authenticationInfo,
            @NotNull Path workspacePath) {
        return traceTime("Detailed workspace info", () -> ReactiveTfvcClientHolder.getInstance().getClient(project)
                .thenCompose(client -> client.getDetailedWorkspaceInfoAsync(authenticationInfo, workspacePath)));
    }
}
