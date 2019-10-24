// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.reactive;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
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
import com.microsoft.tfs.model.connector.VersionNumber;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.alm.plugin.external.reactive.Lifetimes.defineNestedLifetime;

/**
 * A model for the reactive TF client.
 */
public class ReactiveTfClient {
    private static final String REACTIVE_CLIENT_LOG_LEVEL = "INFO";

    private static final Logger ourLogger = Logger.getInstance(ReactiveTfClient.class);

    private final ReactiveClientConnection myConnection;

    public ReactiveTfClient(ReactiveClientConnection connection) {
        myConnection = connection;
    }

    public static ReactiveTfClient create(Project project, Path clientPath) throws ExecutionException {
        SingleThreadScheduler scheduler = new SingleThreadScheduler(defineNestedLifetime(project), "ReactiveTfClient Scheduler");
        ReactiveClientConnection connection = new ReactiveClientConnection(scheduler);
        try {
            Path logDirectory = Paths.get(PathManager.getLogPath(), "ReactiveTfsClient");
            Path clientHomeDir = clientPath.getParent().getParent();
            GeneralCommandLine commandLine = ProcessHelper.patchPathEnvironmentVariable(
                    new GeneralCommandLine(
                            clientPath.toString(),
                            Integer.toString(connection.getPort()),
                            logDirectory.toString(),
                            REACTIVE_CLIENT_LOG_LEVEL)
                            .withWorkDirectory(clientHomeDir.toString()));
            ProcessHandler processHandler = new OSProcessHandler(commandLine);
            connection.getLifetime().onTerminationIfAlive(processHandler::destroyProcess);

            processHandler.addProcessListener(createProcessListener(connection));
            processHandler.startNotify();

            return new ReactiveTfClient(connection);
        } catch (Throwable t) {
            connection.terminate();
            throw t;
        }
    }

    public CompletableFuture<Void> startAsync() {
        return myConnection.startAsync();
    }

    public CompletableFuture<Boolean> checkVersionAsync() {
        return myConnection.getVersionAsync().thenApply(this::checkVersion);
    }

    public CompletableFuture<String> healthCheckAsync() {
        return myConnection.healthCheckAsync();
    }

    public CompletableFuture<List<PendingChange>> getPendingChangesAsync(
            URI serverUri,
            AuthenticationInfo authenticationInfo,
            Stream<Path> localPaths) {
        List<TfsLocalPath> paths = localPaths.map(path -> new TfsLocalPath(path.toString()))
                .collect(Collectors.toList());
        return getReadyCollectionAsync(serverUri, authenticationInfo)
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

    private static ProcessListener createProcessListener(ReactiveClientConnection connection) {
        return new ProcessAdapter() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                ourLogger.info("Process is terminated, terminating the connection");
                connection.terminate();
            }
        };
    }

    private boolean checkVersion(VersionNumber version) {
        // For now, any version is enough.
        return true;
    }

    private CompletableFuture<TfsCollection> getReadyCollectionAsync(
            @NotNull URI serverUri,
            @NotNull AuthenticationInfo authenticationInfo) {
        TfsCredentials tfsCredentials = new TfsCredentials(
                authenticationInfo.getUserName(),
                new RdSecureString(authenticationInfo.getPassword()));
        TfsCollectionDefinition workspaceDefinition = new TfsCollectionDefinition(serverUri, tfsCredentials);

        return myConnection.getOrCreateCollectionAsync(workspaceDefinition)
                .thenCompose(workspace -> myConnection.waitForReadyAsync(workspace)
                        .thenApply(unused -> workspace));
    }
}
