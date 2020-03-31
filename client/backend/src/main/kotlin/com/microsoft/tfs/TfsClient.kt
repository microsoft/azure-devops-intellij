// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs

import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.warn
import com.microsoft.tfs.core.TFSTeamProjectCollection
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient
import com.microsoft.tfs.core.clients.versioncontrol.events.UndonePendingChangeListener
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.*
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec
import com.microsoft.tfs.core.httpclient.Credentials
import com.microsoft.tfs.model.host.TfsItemInfo
import com.microsoft.tfs.model.host.TfsLocalPath
import com.microsoft.tfs.model.host.TfsPath
import com.microsoft.tfs.sdk.isPathMapped
import com.microsoft.tfs.sdk.tryGetWorkspace
import com.microsoft.tfs.watcher.ExternallyControlledPathWatcherFactory
import java.net.URI
import java.nio.file.Paths

class TfsClient(lifetime: Lifetime, serverUri: URI, credentials: Credentials) {
    companion object {
        private val logger = Logging.getLogger<TfsClient>()
    }

    private val client: VersionControlClient
    private val pathWatcherFactory = ExternallyControlledPathWatcherFactory(lifetime)
    init {
        val collection = TFSTeamProjectCollection(serverUri, credentials)
        lifetime.onTermination { collection.close() }

        client = collection.versionControlClient.also {
            it.pathWatcherFactory = pathWatcherFactory
        }
    }

    val workspaces = Property<List<Workspace>>(listOf())
    private fun getWorkspaceFor(path: TfsPath): Workspace? {
        for (workspace in workspaces.value) {
            if (workspace.isPathMapped(path)) {
                return workspace
            }
        }

        return client.tryGetWorkspace(path)?.also {
            workspaces.value += it
        }
    }

    private fun enumeratePathsWithWorkspace(paths: Iterable<TfsPath>, action: (Workspace, List<TfsPath>) -> Unit) {
        for ((workspace, workspacePathList) in paths.asSequence().groupBy(::getWorkspaceFor)) {
            if (workspace == null) {
                logger.warn { "Could not determine workspace for paths: " + paths.joinToString() }
                continue
            }

            action(workspace, workspacePathList)
        }
    }

    fun status(paths: List<TfsPath>): List<PendingSet> {
        val results = mutableListOf<PendingSet>()
        enumeratePathsWithWorkspace(paths) { workspace, workspacePaths ->
            val workspaceName = workspace.name
            val workspaceOwner = workspace.ownerName

            val itemSpecs = ItemSpec.fromStrings(
                workspacePaths.mapToArray { it.toCanonicalPathString() },
                RecursionType.FULL
            )
            val pendingSets = client.queryPendingSets(itemSpecs, false, workspaceName, workspaceOwner, true)
            results.addAll(pendingSets)
        }

        return results
    }

    fun getItemsInfo(paths: List<TfsLocalPath>): List<TfsItemInfo> {
        val infos = ArrayList<TfsItemInfo>(paths.size)
        enumeratePathsWithWorkspace(paths) { workspace, workspacePaths ->
            val downloadType = if (workspace.isLocal) GetItemsOptions.LOCAL_ONLY else GetItemsOptions.NONE
            val itemSpecs = workspacePaths.mapToArray { it.toCanonicalPathItemSpec(RecursionType.NONE) }
            val itemSets = client.getItems(
                itemSpecs,
                LatestVersionSpec.INSTANCE,
                DeletedState.ANY,
                ItemType.ANY,
                downloadType
            )
            val items = itemSets.asSequence().flatMap { it.items.asSequence() }.map { it.itemID to it }.toMap()
            val extendedItems = workspace.getExtendedItems(itemSpecs, DeletedState.ANY, ItemType.ANY, downloadType)
                .asSequence()
                .flatMap { it.asSequence() }
                .map { it.itemID to it }
                .toMap()

            val keySet = items.keys + extendedItems.keys
            for (itemId in keySet) {
                val item = items[itemId]
                val extendedItem = extendedItems[itemId]
                infos.add(toTfsItemInfo(item, extendedItem))
            }
        }

        return infos
    }

    fun invalidatePaths(paths: List<TfsLocalPath>) {
        pathWatcherFactory.pathsInvalidated.fire(paths.map { Paths.get(it.path) })
    }

    fun deletePathsRecursively(paths: List<TfsPath>) {
        enumeratePathsWithWorkspace(paths) { workspace, workspacePaths ->
            workspace.pendDelete(
                workspacePaths.mapToArray { it.toCanonicalPathString() },
                RecursionType.FULL,
                LockLevel.UNCHANGED,
                GetOptions.NONE,
                PendChangesOptions.NONE
            )
        }
    }

    fun undoLocalChanges(paths: List<TfsPath>): List<TfsLocalPath> {
        val eventEngine = client.eventEngine
        val undonePaths = mutableListOf<TfsLocalPath>()
        val listener = UndonePendingChangeListener { undonePaths.add(TfsLocalPath(it.pendingChange.localItem)) }
        eventEngine.addUndonePendingChangeListener(listener)
        try {
            enumeratePathsWithWorkspace(paths) { workspace, workspacePaths ->
                val count = workspace.undo(workspacePaths.mapToArray { it.toCanonicalPathItemSpec(RecursionType.NONE) })
                logger.info { "Undo result = $count" }
            }
        } finally {
            eventEngine.removeUndonePendingChangeListener(listener)
        }
        return undonePaths
    }
}
