// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.reactive;

import com.intellij.execution.ExecutionException;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.microsoft.alm.plugin.idea.common.settings.SettingsChangedNotifier;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.services.PropertyService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ReactiveTfvcClientHolder implements Disposable {

    public static ReactiveTfvcClientHolder getInstance(Project project) {
        return ServiceManager.getService(project, ReactiveTfvcClientHolder.class);
    }

    private final Object myClientLock = new Object();
    private final Project myProject;
    private CompletableFuture<ReactiveTfvcClientHost> myClient;

    public ReactiveTfvcClientHolder(Project myProject) {
        this.myProject = myProject;
        ApplicationManager.getApplication().getMessageBus()
                .connect(myProject)
                .subscribe(SettingsChangedNotifier.SETTINGS_CHANGED_TOPIC, propertyKey -> {
                    if (propertyKey.equals(PropertyService.PROP_TFVC_CLIENT_TYPE)) {
                        destroyClientIfExists();
                    }
                });
    }

    public CompletableFuture<ReactiveTfvcClientHost> getClient() {
        synchronized (myClientLock) {
            if (myClient == null || myClient.isCompletedExceptionally() || myClient.isCancelled()) {
                try {
                    return myClient = createNewClientAsync();
                } catch (Throwable t) {
                    CompletableFuture<ReactiveTfvcClientHost> result = new CompletableFuture<>();
                    result.completeExceptionally(t);
                    return result;
                }
            }

            return myClient;
        }
    }

    @Override
    public void dispose() {
        destroyClientIfExists();
    }

    private void destroyClientIfExists() {
        synchronized (myClientLock) {
            if (myClient == null)
                return;

            myClient.thenAccept(ReactiveTfvcClientHost::terminate);
            myClient = null;
        }
    }

    private CompletableFuture<ReactiveTfvcClientHost> createNewClientAsync() throws ExecutionException {
        Path clientPath = Paths.get(
                Objects.requireNonNull(PluginManager.getPlugin(IdeaHelper.PLUGIN_ID)).getPath().getAbsolutePath(),
                "backend",
                "bin",
                SystemInfo.isWindows ? "backend.bat" : "backend");
        ReactiveTfvcClientHost client = ReactiveTfvcClientHost.create(myProject, clientPath);
        return client.startAsync().thenApply(unused -> client);
    }
}
