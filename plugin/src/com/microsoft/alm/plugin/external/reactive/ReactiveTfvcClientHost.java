// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.reactive;

import com.google.common.collect.Lists;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.jetbrains.rd.framework.impl.RdSecureString;
import com.jetbrains.rd.util.threading.SingleThreadScheduler;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.utils.ProcessHelper;
import com.microsoft.tfs.connector.ReactiveClientConnection;
import com.microsoft.tfs.model.connector.TfsCollection;
import com.microsoft.tfs.model.connector.TfsCollectionDefinition;
import com.microsoft.tfs.model.connector.TfsCredentials;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    private static final String REACTIVE_CLIENT_LOG_LEVEL = "INFO";

    private static final Logger ourLogger = Logger.getInstance(ReactiveTfvcClientHost.class);

    private final ReactiveClientConnection myConnection;

    public ReactiveTfvcClientHost(ReactiveClientConnection connection) {
        myConnection = connection;
    }

    public static ReactiveTfvcClientHost create(Project project, Path clientPath) throws ExecutionException {
        SingleThreadScheduler scheduler = new SingleThreadScheduler(defineNestedLifetime(project), "ReactiveTfClient Scheduler");
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

    public CompletableFuture<Void> startAsync() {
        return myConnection.startAsync();
    }

    public CompletableFuture<List<PendingChange>> getPendingChangesAsync(
            ServerIdentification serverIdentification,
            Stream<Path> localPaths) {
        List<TfsLocalPath> paths = localPaths.map(path -> new TfsLocalPath(path.toString()))
                .collect(Collectors.toList());
        return getReadyCollectionAsync(serverIdentification)
                .thenCompose(collection -> myConnection.invalidatePathsAsync(collection, paths).thenApply(v -> collection))
                .thenCompose(collection -> myConnection.getPendingChangesAsync(collection, paths))
                .thenApply(changes -> changes.stream().map(pc -> new PendingChange(
                        pc.getServerItem(),
                        pc.getLocalItem(),
                        Integer.toString(pc.getVersion()),
                        pc.getOwner(),
                        pc.getDate(),
                        pc.getLock(),
                        pc.getChangeTypes().stream().map(ServerStatusType::from).collect(Collectors.toList()),
                        pc.getWorkspace(),
                        pc.getComputer(),
                        pc.isCandidate(),
                        pc.getSourceItem())).collect(Collectors.toList()));
    }

    @NotNull
    public CompletableFuture<Void> deleteFilesRecursivelyAsync(
            @NotNull ServerIdentification serverIdentification,
            @NotNull Stream<Path> localPaths) {
        List<TfsLocalPath> paths = localPaths.map(path -> new TfsLocalPath(path.toString()))
                .collect(Collectors.toList());
        return getReadyCollectionAsync(serverIdentification)
                .thenCompose(collection -> myConnection.deleteFilesRecursivelyAsync(collection, paths));
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

    private CompletableFuture<TfsCollection> getReadyCollectionAsync(
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
