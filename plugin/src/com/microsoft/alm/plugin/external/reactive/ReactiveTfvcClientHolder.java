// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.reactive;

import com.intellij.execution.ExecutionException;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.microsoft.alm.plugin.external.commands.ToolEulaNotAcceptedException;
import com.microsoft.alm.plugin.idea.common.settings.SettingsChangedNotifier;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.EULADialog;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.LicenseKind;
import com.microsoft.alm.plugin.services.PropertyService;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ReactiveTfvcClientHolder implements Disposable {

    public static ReactiveTfvcClientHolder getInstance() {
        return ServiceManager.getService(ReactiveTfvcClientHolder.class);
    }

    public static Path getClientBackendPath() {
        return Paths.get(
                Objects.requireNonNull(PluginManager.getPlugin(IdeaHelper.PLUGIN_ID)).getPath().getAbsolutePath(),
                "backend");
    }

    private final Object myClientLock = new Object();
    private CompletableFuture<ReactiveTfvcClientHost> myClient;

    public ReactiveTfvcClientHolder() {
        ApplicationManager.getApplication().getMessageBus()
                .connect(this)
                .subscribe(SettingsChangedNotifier.SETTINGS_CHANGED_TOPIC, propertyKey -> {
                    if (propertyKey.equals(PropertyService.PROP_TFVC_USE_REACTIVE_CLIENT)
                        || propertyKey.equals(PropertyService.PROP_REACTIVE_CLIENT_MEMORY)) {
                        destroyClientIfExists();
                    }
                });
    }

    public CompletionStage<ReactiveTfvcClientHost> getClient(@Nullable Project project) {
        ensureEulaAccepted(project);

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

    private void ensureEulaAccepted(@Nullable Project project) {
        PropertyService propertyService = PropertyService.getInstance();
        String eulaAccepted = propertyService.getProperty(PropertyService.PROP_TF_SDK_EULA_ACCEPTED);
        if (!"true".equalsIgnoreCase(eulaAccepted)) {
            if (!EULADialog.isEulaDialogAllowed()) {
                throw new ToolEulaNotAcceptedException(
                        LicenseKind.TfsSdk,
                        "EULA acceptance is required to use the reactive TF client");
            }

            ApplicationManager.getApplication().invokeAndWait(() -> {
                if (!EULADialog.forTfsSdk(project).showAndGet())
                    throw new ToolEulaNotAcceptedException(
                            LicenseKind.TfsSdk,
                            "EULA acceptance is required to use the reactive TF client");
            }, ModalityState.any()); // EULA should be shown even if there's a modal dialog (e.g. a commit one)
        }
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
        Path clientPath = getClientBackendPath()
                .resolve("bin")
                .resolve(SystemInfo.isWindows ? "backend.bat" : "backend");
        ReactiveTfvcClientHost client = ReactiveTfvcClientHost.create(this, clientPath);
        return client.startAsync().thenApply(unused -> client).toCompletableFuture();
    }
}
