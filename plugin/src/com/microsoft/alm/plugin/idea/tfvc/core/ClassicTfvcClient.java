// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.FindWorkspaceCommand;
import com.microsoft.alm.plugin.external.exceptions.ToolBadExitCodeException;
import com.microsoft.alm.plugin.external.exceptions.WorkspaceCouldNotBeDeterminedException;
import com.microsoft.alm.plugin.external.models.ExtendedItemInfo;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.EULADialog;
import com.microsoft.tfs.model.connector.TfsDetailedWorkspaceInfo;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfsPath;
import com.microsoft.tfs.model.connector.TfsServerPath;
import com.microsoft.tfs.model.connector.TfsWorkspaceInfo;
import com.microsoft.tfs.model.connector.TfvcCheckoutResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ClassicTfvcClient implements TfvcClient {

    private static final Logger ourLogger = Logger.getInstance(ClassicTfvcClient.class);

    public static ClassicTfvcClient getInstance() {
        return ServiceManager.getService(ClassicTfvcClient.class);
    }

    @NotNull
    @Override
    public CompletionStage<List<PendingChange>> getStatusForFilesAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess) {
        return CompletableFuture.completedFuture(getStatusForFiles(project, serverContext, pathsToProcess));
    }

    @NotNull
    @Override
    public List<PendingChange> getStatusForFiles(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess) {
        return EULADialog.executeWithGuard(
                project,
                () -> CommandUtils.getStatusForFiles(project, serverContext, pathsToProcess));
    }

    @NotNull
    @Override
    public CompletionStage<Void> getLocalItemsInfoAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ItemInfo> onItemReceived) {
        return getExtendedItemsInfoAsync(project, serverContext, pathsToProcess, onItemReceived::accept);
    }

    @Override
    public void getLocalItemsInfo(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ItemInfo> onItemReceived) {
        getExtendedItemsInfo(project, serverContext, pathsToProcess, onItemReceived::accept);
    }

    @NotNull
    @Override
    public CompletionStage<Void> getExtendedItemsInfoAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ExtendedItemInfo> onItemReceived) {
        getExtendedItemsInfo(project, serverContext, pathsToProcess, onItemReceived);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void getExtendedItemsInfo(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ExtendedItemInfo> onItemReceived) {
        List<ExtendedItemInfo> itemInfos = CommandUtils.getItemInfos(serverContext, pathsToProcess);
        itemInfos.forEach(onItemReceived);
    }

    @NotNull
    @Override
    public CompletionStage<List<Path>> addFilesAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<Path> files) {
        return CompletableFuture.completedFuture(addFiles(project, serverContext, files));
    }

    @NotNull
    @Override
    public List<Path> addFiles(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<Path> files) {
        List<String> pathStrings = files.stream().map(Path::toString).collect(Collectors.toList());
        return CommandUtils.addFiles(serverContext, pathStrings).stream().map(Paths::get).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public CompletionStage<TfvcDeleteResult> deleteFilesRecursivelyAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items) {
        return CompletableFuture.completedFuture(deleteFilesRecursively(project, serverContext, items));
    }

    @NotNull
    public Optional<String> getWorkspace(TfsPath path) {
        if (path instanceof TfsLocalPath)
            return Optional.empty();
        else if (path instanceof TfsServerPath)
            return Optional.of(((TfsServerPath) path).getWorkspace());
        else
            throw new RuntimeException("Unknown path type: " + path);
    }

    @Override
    @NotNull
    public TfvcDeleteResult deleteFilesRecursively(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items) {
        Map<Optional<String>, List<TfsPath>> itemsByWorkspace = items.stream()
                .collect(Collectors.groupingBy(this::getWorkspace));
        TfvcDeleteResult result = new TfvcDeleteResult();
        for (Map.Entry<Optional<String>, List<TfsPath>> workspaceItems : itemsByWorkspace.entrySet()) {
            Optional<String> workspace = workspaceItems.getKey();
            List<String> itemsInWorkspace = workspaceItems.getValue().stream()
                    .map(TfsFileUtil::getPathItem)
                    .collect(Collectors.toList());
            result = result.mergeWith(CommandUtils.deleteFiles(
                    serverContext,
                    itemsInWorkspace,
                    workspace.orElse(null),
                    true));
        }

        return result;
    }

    @NotNull
    @Override
    public CompletionStage<List<TfsLocalPath>> undoLocalChangesAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items) {
        undoLocalChanges(project, serverContext, items);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public List<TfsLocalPath> undoLocalChanges(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items) {
        List<String> itemPaths = items.stream().map(TfsFileUtil::getPathItem).collect(Collectors.toList());
        List<String> undonePaths = CommandUtils.undoLocalFiles(serverContext, itemPaths);
        return undonePaths.stream().map(TfsLocalPath::new).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public CompletionStage<TfvcCheckoutResult> checkoutForEditAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<Path> filePaths,
            boolean recursive) {
        return CompletableFuture.completedFuture(checkoutForEdit(project, serverContext, filePaths, recursive));
    }

    @NotNull
    @Override
    public TfvcCheckoutResult checkoutForEdit(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull List<Path> filePaths,
            boolean recursive) {
        return CommandUtils.checkoutFilesForEdit(serverContext, filePaths, recursive);
    }

    @NotNull
    @Override
    public CompletionStage<Boolean> renameFileAsync(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull Path oldFile,
            @NotNull Path newFile) {
        renameFile(project, serverContext, oldFile, newFile);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean renameFile(
            @Nullable Project project,
            @NotNull ServerContext serverContext,
            @NotNull Path oldFile,
            @NotNull Path newFile) {
        try {
            CommandUtils.renameFile(serverContext, oldFile.toString(), newFile.toString());
            return true;
        } catch (ToolBadExitCodeException ex) {
            ourLogger.error(ex);
            return false;
        }
    }

    @Nullable
    @Override
    public TfsWorkspaceInfo getBasicWorkspaceInfo(@Nullable Project project, @NotNull Path workspacePath) {
        try {
            return new FindWorkspaceCommand(workspacePath.toString(), null, true).runSynchronously();
        } catch (WorkspaceCouldNotBeDeterminedException ex) {
            return null;
        }
    }

    @NotNull
    @Override
    public CompletionStage<TfsWorkspaceInfo> getBasicWorkspaceInfoAsync(
            @Nullable Project project,
            @NotNull Path workspacePath) {
        return CompletableFuture.completedFuture(getBasicWorkspaceInfo(project, workspacePath));
    }

    @Nullable
    @Override
    public TfsDetailedWorkspaceInfo getDetailedWorkspaceInfo(
            @Nullable Project project,
            @NotNull AuthenticationInfo authenticationInfo,
            @NotNull Path workspacePath) {
        try {
            TfsWorkspaceInfo workspace = new FindWorkspaceCommand(
                    workspacePath.toString(),
                    authenticationInfo,
                    true).runSynchronously();
            return (TfsDetailedWorkspaceInfo) workspace;
        } catch (WorkspaceCouldNotBeDeterminedException ex) {
            return null;
        }
    }

    @NotNull
    @Override
    public CompletionStage<TfsDetailedWorkspaceInfo> getDetailedWorkspaceInfoAsync(
            @Nullable Project project,
            @NotNull AuthenticationInfo authenticationInfo,
            @NotNull Path workspacePath) {
        return CompletableFuture.completedFuture(getDetailedWorkspaceInfo(project, authenticationInfo, workspacePath));
    }
}
