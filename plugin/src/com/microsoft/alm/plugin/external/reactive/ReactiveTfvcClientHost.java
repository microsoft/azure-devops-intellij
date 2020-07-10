// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.reactive;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.jetbrains.rd.framework.impl.RdSecureString;
import com.jetbrains.rd.util.threading.SingleThreadScheduler;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.external.models.ExtendedItemInfo;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.utils.ProcessHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.tfs.connector.ReactiveClientConnection;
import com.microsoft.tfs.model.connector.TfsCollection;
import com.microsoft.tfs.model.connector.TfsCollectionDefinition;
import com.microsoft.tfs.model.connector.TfsCredentials;
import com.microsoft.tfs.model.connector.TfsDeleteResult;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfsPath;
import com.microsoft.tfs.model.connector.TfvcCheckoutResult;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.alm.plugin.external.reactive.Lifetimes.defineNestedLifetime;

/**
 * A model for the reactive TFVC client.
 */
public class ReactiveTfvcClientHost {
    static {
        RdIdeaLoggerFactory.initialize();
    }

    private static final int INFO_PARTITION_COUNT = 1000;

    private static final String REACTIVE_CLIENT_LOG_LEVEL = "INFO";

    private static final Logger ourLogger = Logger.getInstance(ReactiveTfvcClientHost.class);

    private final ReactiveClientConnection myConnection;

    public ReactiveTfvcClientHost(ReactiveClientConnection connection) {
        myConnection = connection;
    }

    public static ReactiveTfvcClientHost create(Disposable parent, Path clientPath) throws ExecutionException {
        SingleThreadScheduler scheduler = new SingleThreadScheduler(defineNestedLifetime(parent), "ReactiveTfClient Scheduler");
        ReactiveClientConnection connection = new ReactiveClientConnection(scheduler);
        try {
            Path logDirectory = Paths.get(PathManager.getLogPath(), "ReactiveTfsClient");
            Path clientHomeDir = clientPath.getParent().getParent();
            GeneralCommandLine commandLine = ProcessHelper.patchPathEnvironmentVariable(
                    getClientCommandLine(clientPath, connection.getPort(), logDirectory, clientHomeDir));
            ProcessHandler processHandler = new OSProcessHandler(commandLine);
            connection.getLifetime().onTerminationIfAlive(processHandler::destroyProcess);

            processHandler.addProcessListener(createProcessListener(connection));
            processHandler.startNotify();

            return new ReactiveTfvcClientHost(connection);
        } catch (Throwable t) {
            connection.terminate();
            throw t;
        }
    }

    public void terminate() {
        myConnection.terminate();
    }

    @NotNull
    private static GeneralCommandLine getClientCommandLine(
            Path clientExecutable,
            int protocolPort,
            Path logDirectory,
            Path clientHome) {
        ArrayList<String> command = Lists.newArrayList(
                clientExecutable.toString(),
                Integer.toString(protocolPort),
                logDirectory.toString(),
                REACTIVE_CLIENT_LOG_LEVEL);
        if (SystemInfo.isUnix) {
            // Client executable is a shell script on Unix-like operating systems
            command.addAll(0, Arrays.asList("/usr/bin/env", "sh"));
        }

        return new GeneralCommandLine(command).withWorkDirectory(clientHome.toString());
    }

    public CompletionStage<Void> startAsync() {
        return myConnection.startAsync();
    }

    public CompletionStage<List<PendingChange>> getPendingChangesAsync(
            ServerIdentification serverIdentification,
            Stream<Path> localPaths) {
        List<TfsLocalPath> paths = localPaths.map(TfsFileUtil::createLocalPath).collect(Collectors.toList());
        return getReadyCollectionAsync(serverIdentification)
                .thenCompose(collection -> myConnection.invalidatePathsAsync(collection, paths).thenApply(v -> collection))
                .thenCompose(collection -> myConnection.getPendingChangesAsync(collection, paths))
                .thenApply(changes -> changes.stream().map(PendingChange::from).collect(Collectors.toList()));
    }

    private CompletionStage<Void> getLocalItemsInfoAsyncChunk(
            TfsCollection collection,
            Iterator<List<Path>> chunkIterator,
            Consumer<ItemInfo> onItemReceived) {
        if (!chunkIterator.hasNext())
            return CompletableFuture.completedFuture(null);

        List<Path> chunk = chunkIterator.next();
        List<TfsLocalPath> paths = chunk.stream().map(TfsFileUtil::createLocalPath).collect(Collectors.toList());
        return myConnection.getLocalItemsInfoAsync(collection, paths)
                .thenCompose(infos -> {
                    infos.forEach(ii -> onItemReceived.accept(ItemInfo.from(ii)));
                    return getLocalItemsInfoAsyncChunk(collection, chunkIterator, onItemReceived);
                });
    }

    private CompletionStage<Void> getExtendedItemsInfoAsyncChunk(
            TfsCollection collection,
            Iterator<List<Path>> chunkIterator,
            Consumer<ExtendedItemInfo> onItemReceived) {
        if (!chunkIterator.hasNext())
            return CompletableFuture.completedFuture(null);

        List<Path> chunk = chunkIterator.next();
        List<TfsLocalPath> paths = chunk.stream().map(TfsFileUtil::createLocalPath).collect(Collectors.toList());
        return myConnection.getExtendedItemsInfoAsync(collection, paths)
                .thenCompose(infos -> {
                    infos.forEach(ii -> onItemReceived.accept(ExtendedItemInfo.from(ii)));
                    return getExtendedItemsInfoAsyncChunk(collection, chunkIterator, onItemReceived);
                });
    }

    public CompletionStage<Void> getLocalItemsInfoAsync(
            ServerIdentification serverIdentification,
            Stream<Path> localPaths,
            Consumer<ItemInfo> onItemReceived) {
        // Pack the paths into partitions of predefined size to avoid overloading the protocol.
        Iterable<List<Path>> partitions = Iterables.partition(localPaths::iterator, INFO_PARTITION_COUNT);
        return getReadyCollectionAsync(serverIdentification)
                .thenCompose(collection -> getLocalItemsInfoAsyncChunk(collection, partitions.iterator(), onItemReceived));
    }

    public CompletionStage<Void> getExtendedItemsInfoAsync(
            ServerIdentification serverIdentification,
            Stream<Path> localPaths,
            Consumer<ExtendedItemInfo> onItemReceived) {
        // Pack the paths into partitions of predefined size to avoid overloading the protocol.
        Iterable<List<Path>> partitions = Iterables.partition(localPaths::iterator, INFO_PARTITION_COUNT);
        return getReadyCollectionAsync(serverIdentification)
                .thenCompose(collection -> getExtendedItemsInfoAsyncChunk(
                        collection,
                        partitions.iterator(),
                        onItemReceived));
    }

    @NotNull
    public CompletionStage<List<TfsLocalPath>> addFilesAsync(
            @NotNull ServerIdentification serverIdentification,
            @NotNull List<TfsLocalPath> files) {
        return getReadyCollectionAsync(serverIdentification)
                .thenCompose(collection -> myConnection.addFilesAsync(collection, files));
    }

    @NotNull
    public CompletionStage<TfsDeleteResult> deleteFilesRecursivelyAsync(
            @NotNull ServerIdentification serverIdentification,
            @NotNull List<TfsPath> paths) {
        return getReadyCollectionAsync(serverIdentification)
                .thenCompose(collection -> myConnection.deleteFilesRecursivelyAsync(collection, paths));
    }

    @NotNull
    public CompletionStage<List<TfsLocalPath>> undoLocalChangesAsync(
            @NotNull ServerIdentification serverIdentification,
            @NotNull List<TfsPath> paths) {
        return getReadyCollectionAsync(serverIdentification)
                .thenCompose(collection -> myConnection.undoLocalChangesAsync(collection, paths));
    }

    @NotNull
    public CompletionStage<TfvcCheckoutResult> checkoutFilesForEditAsync(
            @NotNull ServerIdentification serverIdentification,
            @NotNull List<TfsLocalPath> paths,
            boolean recursive) {
        return getReadyCollectionAsync(serverIdentification)
                .thenCompose(collection -> myConnection.checkoutFilesForEditAsync(collection, paths, recursive));
    }

    public CompletionStage<Boolean> renameFileAsync(
            @NotNull ServerIdentification serverIdentification,
            @NotNull TfsLocalPath oldPath,
            @NotNull TfsLocalPath newPath) {
        return getReadyCollectionAsync(serverIdentification)
                .thenCompose(collection -> myConnection.renameFileAsync(collection, oldPath, newPath));
    }

    private static ProcessListener createProcessListener(ReactiveClientConnection connection) {
        return new ProcessAdapter() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                if (outputType == ProcessOutputTypes.STDERR || ourLogger.isTraceEnabled()) {
                    String message = "Process output (" + outputType + "): " + event.getText();
                    if (outputType == ProcessOutputTypes.STDERR)
                        ourLogger.warn(message);
                    else
                        ourLogger.trace(message);
                }
            }

            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                ourLogger.info("Process is terminated, terminating the connection");
                connection.terminate();
            }
        };
    }

    private CompletionStage<TfsCollection> getReadyCollectionAsync(
            @NotNull ServerIdentification serverIdentification) {
        AuthenticationInfo authenticationInfo = serverIdentification.getAuthenticationInfo();
        TfsCredentials tfsCredentials = new TfsCredentials(
                authenticationInfo.getUserName(),
                new RdSecureString(authenticationInfo.getPassword()));
        TfsCollectionDefinition workspaceDefinition = new TfsCollectionDefinition(
                serverIdentification.getServerUri(),
                tfsCredentials);

        return myConnection.getOrCreateCollectionAsync(workspaceDefinition)
                .thenCompose(workspace -> myConnection.waitForReadyAsync(workspace)
                        .thenApply(unused -> workspace));
    }
}
