// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs

import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.warn
import com.microsoft.tfs.core.TFSTeamProjectCollection
import com.microsoft.tfs.core.clients.versioncontrol.*
import com.microsoft.tfs.core.clients.versioncontrol.events.NewPendingChangeListener
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorListener
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent
import com.microsoft.tfs.core.clients.versioncontrol.events.UndonePendingChangeListener
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.*
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec
import com.microsoft.tfs.core.httpclient.Credentials
import com.microsoft.tfs.model.host.TfsDeleteResult
import com.microsoft.tfs.model.host.TfsItemInfo
import com.microsoft.tfs.model.host.TfsLocalPath
import com.microsoft.tfs.model.host.TfsPath
import com.microsoft.tfs.sdk.*
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
            it.eventEngine.addNonFatalErrorListener { event ->
                logger.warn { event.message }
            }
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

    fun getLocalItemsInfo(paths: List<TfsLocalPath>): List<TfsItemInfo> {
        val infos = ArrayList<TfsItemInfo>(paths.size)
        enumeratePathsWithWorkspace(paths) { workspace, workspacePaths ->
            val downloadType = if (workspace.isLocal) GetItemsOptions.LOCAL_ONLY else GetItemsOptions.NONE
            val itemSpecs = workspacePaths.mapToArray { it.toCanonicalPathItemSpec(RecursionType.NONE) }
            val extendedItems = workspace.getExtendedItems(itemSpecs, DeletedState.ANY, ItemType.ANY, downloadType)
                .asSequence()
                .flatMap { it.asSequence() }

            for (extendedItem in extendedItems) {
                infos.add(extendedItem.toTfsItemInfo())
            }
        }

        return infos
    }

    fun invalidatePaths(paths: List<TfsLocalPath>) {
        pathWatcherFactory.pathsInvalidated.fire(paths.map { Paths.get(it.path) })
    }

    fun deletePathsRecursively(paths: List<TfsPath>): TfsDeleteResult {
        val eventEngine = client.eventEngine
        val deletedEvents = mutableListOf<PendingChangeEvent>()
        val itemNotExistsFailures = mutableListOf<Failure>()
        val otherFailures = mutableListOf<Failure>()

        val changeListener = NewPendingChangeListener { event ->
            if (event.pendingChange.changeType.contains(ChangeType.DELETE)) {
                deletedEvents.add(event)
            }
        }

        val errorListener = NonFatalErrorListener { event ->
            event.failure?.let {
                when (event.failure.code) {
                    FailureCodes.ITEM_NOT_FOUND_EXCEPTION -> itemNotExistsFailures.add(it)
                    else -> otherFailures.add(it)
                }
            }
        }

        eventEngine.withNewPendingChangeListener(changeListener) {
            eventEngine.withNonFatalErrorListener(errorListener) {
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
        }

        val deletedPaths = deletedEvents.map { TfsLocalPath(it.pendingChange.localItem) }
        val errorMessages = otherFailures.map { it.toString() }
        val notFoundPaths = itemNotExistsFailures.map { TfsLocalPath(it.localItem) }

        return TfsDeleteResult(deletedPaths, notFoundPaths, errorMessages)
    }

    fun undoLocalChanges(paths: List<TfsPath>): List<TfsLocalPath> {
        val undonePaths = mutableListOf<TfsLocalPath>()
        val listener = UndonePendingChangeListener { undonePaths.add(TfsLocalPath(it.pendingChange.localItem)) }
        client.eventEngine.withUndonePendingChangeListener(listener) {
            enumeratePathsWithWorkspace(paths) { workspace, workspacePaths ->
                val count = workspace.undo(workspacePaths.mapToArray { it.toCanonicalPathItemSpec(RecursionType.NONE) })
                logger.info { "Undo result = $count" }
            }
        }

        return undonePaths
    }
}
