package com.microsoft.alm.plugin.external.reactive;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.microsoft.tfs.connector.ReactiveClientConnection;
import com.microsoft.tfs.model.connector.VersionNumber;

import java.util.concurrent.CompletableFuture;

/**
 * A model for the new, reactive client.
 */
public class ReactiveTfClient {
    private final Process process;
    private final ReactiveClientConnection connection;

    public ReactiveTfClient(Process process, ReactiveClientConnection connection) {
        this.process = process;
        this.connection = connection;
    }

    public static ReactiveTfClient create(String clientPath) throws ExecutionException {
        ReactiveClientConnection connection = new ReactiveClientConnection();
        try {
            GeneralCommandLine commandLine = new GeneralCommandLine(clientPath, Integer.toString(connection.getPort()));
            Process process = commandLine.createProcess();
            connection.getLifetime().onTerminationIfAlive(process::destroyForcibly);
            return new ReactiveTfClient(process, connection);
        } catch (Throwable t) {
            connection.terminate();
            throw t;
        }
    }

    public CompletableFuture<Void> startAsync() {
        return connection.startAsync();
    }

    public CompletableFuture<Boolean> checkVersionAsync() {
        return connection.getVersionAsync().thenApply(this::checkVersion);
    }

    private boolean checkVersion(VersionNumber version) {
        // For now, any version is enough.
        return true;
    }

    public CompletableFuture<String> healthCheckAsync() {
        return connection.healthCheckAsync();
    }
}
