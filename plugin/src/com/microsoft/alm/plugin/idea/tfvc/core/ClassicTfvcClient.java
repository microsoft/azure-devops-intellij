// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.exceptions.ToolBadExitCodeException;
import com.microsoft.alm.plugin.external.models.ExtendedItemInfo;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.EULADialog;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfsPath;
import com.microsoft.tfs.model.connector.TfsServerPath;
import com.microsoft.tfs.model.connector.TfvcCheckoutResult;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    private final Project myProject;

    public ClassicTfvcClient(@NotNull Project project) {
        myProject = project;
    }

    @NotNull
    @Override
    public CompletionStage<List<PendingChange>> getStatusForFilesAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess) {
        return CompletableFuture.completedFuture(getStatusForFiles(serverContext, pathsToProcess));
    }

    @NotNull
    @Override
    public List<PendingChange> getStatusForFiles(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess) {
        return EULADialog.executeWithGuard(
                myProject,
                () -> CommandUtils.getStatusForFiles(myProject, serverContext, pathsToProcess));
    }

    @NotNull
    @Override
    public CompletionStage<Void> getLocalItemsInfoAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ItemInfo> onItemReceived) {
        return getExtendedItemsInfoAsync(serverContext, pathsToProcess, onItemReceived::accept);
    }

    @Override
    public void getLocalItemsInfo(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ItemInfo> onItemReceived) {
        getExtendedItemsInfo(serverContext, pathsToProcess, onItemReceived::accept);
    }

    @NotNull
    @Override
    public CompletionStage<Void> getExtendedItemsInfoAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ExtendedItemInfo> onItemReceived) {
        getExtendedItemsInfo(serverContext, pathsToProcess, onItemReceived);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void getExtendedItemsInfo(
            @NotNull ServerContext serverContext,
            @NotNull List<String> pathsToProcess,
            @NotNull Consumer<ExtendedItemInfo> onItemReceived) {
        List<ExtendedItemInfo> itemInfos = CommandUtils.getItemInfos(serverContext, pathsToProcess);
        itemInfos.forEach(onItemReceived);
    }

    @NotNull
    @Override
    public CompletionStage<List<Path>> addFilesAsync(@NotNull ServerContext serverContext, @NotNull List<Path> files) {
        return CompletableFuture.completedFuture(addFiles(serverContext, files));
    }

    @NotNull
    @Override
    public List<Path> addFiles(@NotNull ServerContext serverContext, @NotNull List<Path> files) {
        List<String> pathStrings = files.stream().map(Path::toString).collect(Collectors.toList());
        return CommandUtils.addFiles(serverContext, pathStrings).stream().map(Paths::get).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public CompletionStage<TfvcDeleteResult> deleteFilesRecursivelyAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items) {
        return CompletableFuture.completedFuture(deleteFilesRecursively(serverContext, items));
    }

    @NotNull
    private static Optional<String> getWorkspace(TfsPath path) {
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
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items) {
        Map<Optional<String>, List<TfsPath>> itemsByWorkspace = items.stream()
                .collect(Collectors.groupingBy(ClassicTfvcClient::getWorkspace));
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
            @NotNull ServerContext serverContext,
            @NotNull List<TfsPath> items) {
        undoLocalChanges(serverContext, items);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public List<TfsLocalPath> undoLocalChanges(@NotNull ServerContext serverContext, @NotNull List<TfsPath> items) {
        List<String> itemPaths = items.stream().map(TfsFileUtil::getPathItem).collect(Collectors.toList());
        List<String> undonePaths = CommandUtils.undoLocalFiles(serverContext, itemPaths);
        return undonePaths.stream().map(TfsLocalPath::new).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public CompletionStage<TfvcCheckoutResult> checkoutForEditAsync(
            @NotNull ServerContext serverContext,
            @NotNull List<Path> filePaths,
            boolean recursive) {
        return CompletableFuture.completedFuture(checkoutForEdit(serverContext, filePaths, recursive));
    }

    @NotNull
    @Override
    public TfvcCheckoutResult checkoutForEdit(
            @NotNull ServerContext serverContext,
            @NotNull List<Path> filePaths,
            boolean recursive) {
        return CommandUtils.checkoutFilesForEdit(serverContext, filePaths, recursive);
    }

    @NotNull
    @Override
    public CompletionStage<Boolean> renameFileAsync(
            @NotNull ServerContext serverContext,
            @NotNull Path oldFile,
            @NotNull Path newFile) {
        renameFile(serverContext, oldFile, newFile);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean renameFile(@NotNull ServerContext serverContext, @NotNull Path oldFile, @NotNull Path newFile) {
        try {
            CommandUtils.renameFile(serverContext, oldFile.toString(), newFile.toString());
            return true;
        } catch (ToolBadExitCodeException ex) {
            ourLogger.error(ex);
            return false;
        }
    }
}
