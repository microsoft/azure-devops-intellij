// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.reactive;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.jetbrains.rd.framework.impl.RdSecureString;
import com.jetbrains.rd.util.lifetime.LifetimeDefinition;
import com.jetbrains.rd.util.threading.SingleThreadScheduler;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.external.models.ExtendedItemInfo;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.utils.ProcessHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.services.PropertyService;
import com.microsoft.tfs.connector.ReactiveClientConnection;
import com.microsoft.tfs.model.connector.TfsCollection;
import com.microsoft.tfs.model.connector.TfsCollectionDefinition;
import com.microsoft.tfs.model.connector.TfsCredentials;
import com.microsoft.tfs.model.connector.TfsDeleteResult;
import com.microsoft.tfs.model.connector.TfsDetailedWorkspaceInfo;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfsPath;
import com.microsoft.tfs.model.connector.TfsWorkspaceInfo;
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

    public static final String REACTIVE_CLIENT_OPTIONS_ENV = "BACKEND_OPTS";
    public static final int REACTIVE_CLIENT_DEFAULT_MEMORY_LIMIT = 2048;

    private static final int INFO_PARTITION_COUNT = 1000;

    private static final String REACTIVE_CLIENT_LOG_LEVEL = "INFO";

    private static final Logger ourLogger = Logger.getInstance(ReactiveTfvcClientHost.class);

    private final LifetimeDefinition myLifetime;
    private final ReactiveClientConnection myConnection;

    public ReactiveTfvcClientHost(LifetimeDefinition myLifetime, ReactiveClientConnection connection) {
        this.myLifetime = myLifetime;
        myConnection = connection;
    }

    public static ReactiveTfvcClientHost create(Disposable parent, Path clientPath) throws ExecutionException {
        LifetimeDefinition hostLifetime = defineNestedLifetime(parent);
        SingleThreadScheduler scheduler = new SingleThreadScheduler(hostLifetime, "ReactiveTfClient Scheduler");
        ReactiveClientConnection connection = new ReactiveClientConnection(hostLifetime, scheduler);
        try {
            Path logDirectory = Paths.get(PathManager.getLogPath(), "ReactiveTfsClient");
            Path clientHomeDir = clientPath.getParent().getParent();
            GeneralCommandLine commandLine = ProcessHelper.patchPathEnvironmentVariable(
                    getClientCommandLine(clientPath, connection.getPort(), logDirectory, clientHomeDir));

            ProcessHandler processHandler = new OSProcessHandler(commandLine) {
                @Override
                protected void notifyProcessTerminated(int exitCode) {
                    super.notifyProcessTerminated(exitCode);
                    if (exitCode == 0)
                        ourLogger.info("Reactive client process terminated with exit code " + exitCode);
                    else
                        ourLogger.warn("Reactive client process terminated with exit code " + exitCode);

                    hostLifetime.terminate(false);
                }

                @Override
                public void notifyTextAvailable(@NotNull String text, @NotNull Key outputType) {
                    super.notifyTextAvailable(text, outputType);
                    if (outputType.equals(ProcessOutputTypes.STDOUT))
                        ourLogger.trace(outputType + ": " + text);
                    else
                        ourLogger.info(outputType + ": " + text);
                }
            };
            connection.getLifetime().onTerminationIfAlive(() -> {
                ourLogger.info("TFVC client connection terminated, terminating process");
                processHandler.destroyProcess();
            });

            processHandler.startNotify();

            return new ReactiveTfvcClientHost(hostLifetime, connection);
        } catch (Throwable t) {
            hostLifetime.terminate(false);
            throw t;
        }
    }

    public void terminate() {
        myLifetime.terminate(false);
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

        String backendOptions = System.getenv(REACTIVE_CLIENT_OPTIONS_ENV);
        if (Strings.isNullOrEmpty(backendOptions)) {
            String memoryMb = PropertyService.getInstance().getProperty(PropertyService.PROP_REACTIVE_CLIENT_MEMORY);
            if (memoryMb == null)
                memoryMb = Integer.toString(REACTIVE_CLIENT_DEFAULT_MEMORY_LIMIT);

            backendOptions = String.format("-Xmx%sm", memoryMb);
        }

        ourLogger.info("Reactive client will be started with env " + REACTIVE_CLIENT_OPTIONS_ENV + "=" + backendOptions);
        return new GeneralCommandLine(command)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.SYSTEM)
                .withEnvironment(REACTIVE_CLIENT_OPTIONS_ENV, backendOptions)
                .withWorkDirectory(clientHome.toString());
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

    @NotNull
    public CompletionStage<TfsWorkspaceInfo> getBasicWorkspaceInfoAsync(@NotNull Path workspacePath) {
        return myConnection.getBasicWorkspaceInfoAsync(new TfsLocalPath(workspacePath.toString()));
    }

    @NotNull
    public CompletionStage<TfsDetailedWorkspaceInfo> getDetailedWorkspaceInfoAsync(
            @NotNull AuthenticationInfo authenticationInfo,
            @NotNull Path workspacePath) {
        TfsCredentials workspaceDefinition = getTfsCredentials(authenticationInfo);
        return myConnection.getDetailedWorkspaceInfoAsync(
                workspaceDefinition,
                new TfsLocalPath(workspacePath.toString()));
    }

    private TfsCredentials getTfsCredentials(@NotNull AuthenticationInfo authenticationInfo) {
        return new TfsCredentials(
                authenticationInfo.getUserName(),
                new RdSecureString(authenticationInfo.getPassword()));
    }

    private CompletionStage<TfsCollection> getReadyCollectionAsync(
            @NotNull ServerIdentification serverIdentification) {
        TfsCredentials credentials = getTfsCredentials(serverIdentification.getAuthenticationInfo());
        TfsCollectionDefinition workspaceDefinition = new TfsCollectionDefinition(
                serverIdentification.getServerUri(),
                credentials);
        return myConnection.getOrCreateCollectionAsync(workspaceDefinition)
                .thenCompose(workspace -> myConnection.waitForReadyAsync(workspace)
                        .thenApply(unused -> workspace));
    }
}
