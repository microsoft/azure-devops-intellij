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
import com.microsoft.alm.plugin.external.utils.ProcessHelper;
import com.microsoft.tfs.connector.ReactiveClientConnection;
import com.microsoft.tfs.model.connector.VersionNumber;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<String> healthCheckAsync() {
        return myConnection.healthCheckAsync();
    }
}
