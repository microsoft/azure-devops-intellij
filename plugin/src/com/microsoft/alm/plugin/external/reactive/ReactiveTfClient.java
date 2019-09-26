package com.microsoft.alm.plugin.external.reactive;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.utils.ProcessHelper;
import com.microsoft.tfs.connector.ReactiveClientConnection;
import com.microsoft.tfs.model.connector.TfsCredentials;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfsWorkspace;
import com.microsoft.tfs.model.connector.TfsWorkspaceDefinition;
import com.microsoft.tfs.model.connector.VersionNumber;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A model for the new, reactive client.
 */
public class ReactiveTfClient {
    private static final Logger ourLogger = Logger.getInstance(ReactiveTfClient.class);

    private final ReactiveClientConnection myConnection;

    public ReactiveTfClient(ReactiveClientConnection connection) {
        myConnection = connection;
    }

    public static ReactiveTfClient create(String clientPath) throws ExecutionException {
        ReactiveClientConnection connection = new ReactiveClientConnection(SwingScheduler.INSTANCE);
        try {
            GeneralCommandLine commandLine = ProcessHelper.patchPathEnvironmentVariable(
                    new GeneralCommandLine(clientPath, Integer.toString(connection.getPort())));
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
            Path workspacePath,
            AuthenticationInfo authenticationInfo,
            Stream<Path> localPaths) {
        List<TfsLocalPath> paths = localPaths.map(path -> new TfsLocalPath(path.toString()))
                .collect(Collectors.toList());
        return getReadyWorkspaceAsync(workspacePath, authenticationInfo)
                .thenCompose(workspace -> myConnection.getPendingChangesAsync(workspace, paths))
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

            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                ourLogger.info(String.format("Output from process (%s): %s", outputType, event.getText()));
            }
        };
    }

    private boolean checkVersion(VersionNumber version) {
        // For now, any version is enough.
        return true;
    }

    private CompletableFuture<TfsWorkspace> getReadyWorkspaceAsync(
            @NotNull Path workspacePath,
            @NotNull AuthenticationInfo authenticationInfo) {
        TfsCredentials tfsCredentials = new TfsCredentials(
                authenticationInfo.getUserName(),
                authenticationInfo.getPassword());
        TfsWorkspace workspace = myConnection.getOrCreateWorkspace(
                new TfsWorkspaceDefinition(new TfsLocalPath(workspacePath.toString()), tfsCredentials));
        return myConnection.waitForReadyAsync(workspace).thenApply(unused -> workspace);
    }
}
