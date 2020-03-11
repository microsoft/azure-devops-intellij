// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.warn
import com.microsoft.tfs.core.TFSTeamProjectCollection
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec
import com.microsoft.tfs.core.httpclient.Credentials
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
}
