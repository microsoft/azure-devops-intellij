// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.reactive;

import com.intellij.execution.ExecutionException;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
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
        Path clientPath = Paths.get(
                Objects.requireNonNull(PluginManager.getPlugin(IdeaHelper.PLUGIN_ID)).getPath().getAbsolutePath(),
                "backend",
                "bin",
                SystemInfo.isWindows ? "backend.bat" : "backend");
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
