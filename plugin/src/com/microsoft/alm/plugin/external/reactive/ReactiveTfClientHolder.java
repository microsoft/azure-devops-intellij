package com.microsoft.alm.plugin.external.reactive;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;

import java.util.concurrent.CompletableFuture;

public class ReactiveTfClientHolder {

    public static ReactiveTfClientHolder getInstance(Project project) {
        return ServiceManager.getService(project, ReactiveTfClientHolder.class);
    }

    private final Object myClientLock = new Object();
    private Project myProject;
    private CompletableFuture<ReactiveTfClient> myClient; // TODO: Clear cached instance on settings change

    public ReactiveTfClientHolder(Project myProject) {
        this.myProject = myProject;
    }

    public CompletableFuture<ReactiveTfClient> getClient() {
        synchronized (myClientLock) {
            if (myClient == null || myClient.isCompletedExceptionally() || myClient.isCancelled()) {
                try {
                    return myClient = createNewClientAsync();
                } catch (Throwable t) {
                    CompletableFuture<ReactiveTfClient> result = new CompletableFuture<>();
                    result.completeExceptionally(t);
                    return result;
                }
            }

            return myClient;
        }
    }

    private CompletableFuture<ReactiveTfClient> createNewClientAsync() throws ExecutionException {
        PropertyService propertyService = PluginServiceProvider.getInstance().getPropertyService();
        String clientPath = propertyService.getProperty(PropertyService.PROP_REACTIVE_CLIENT_PATH);
        ReactiveTfClient client = ReactiveTfClient.create(myProject, clientPath);
        return client.startAsync()
                .thenCompose(unused -> client.checkVersionAsync().thenAccept(isOk -> {
                    if (!isOk) throw new RuntimeException("Client version check failed");
                }))
                .thenCompose(unused -> client.healthCheckAsync().thenAccept(errorMessage -> {
                    if (errorMessage != null) throw new RuntimeException("Client health check failed: " + errorMessage);
                }))
                .thenApply(unused -> client);
    }
}
