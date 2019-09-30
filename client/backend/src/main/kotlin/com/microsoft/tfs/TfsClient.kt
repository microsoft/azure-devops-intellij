package com.microsoft.tfs

import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.tfs.core.TFSTeamProjectCollection
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient
import com.microsoft.tfs.core.clients.versioncontrol.Workstation
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider
import com.microsoft.tfs.core.httpclient.Credentials
import com.microsoft.tfs.watcher.ExternallyControlledPathWatcherFactory
import java.nio.file.Path

class TfsClient(lifetime: Lifetime, path: Path, credentials: Credentials) {
    private val client: VersionControlClient
    private val workspace: Workspace
    private val pathWatcherFactory = ExternallyControlledPathWatcherFactory(lifetime)
    init {
        val workstation = Workstation.getCurrent(DefaultPersistenceStoreProvider.INSTANCE)
        val workspaceInfo = workstation.getLocalWorkspaceInfo(path.toString())
        val serverUri = workspaceInfo.serverURI

        val collection = TFSTeamProjectCollection(serverUri, credentials)
        client = collection.versionControlClient.also {
            it.pathWatcherFactory = pathWatcherFactory
        }
        workspace = client.getLocalWorkspace(path.toString(), true)
    }

    private val workspaceName
        get() = workspace.name

    private val workspaceOwner
        get() = workspace.ownerName

    fun status(paths: List<Path>): Array<PendingSet> {
        val itemSpecs = ItemSpec.fromStrings(paths.map { LocalPath.canonicalize(it.toString()) }.toTypedArray(), RecursionType.FULL)
        return client.queryPendingSets(itemSpecs, false, workspaceName, workspaceOwner, true)
    }

    fun invalidatePath(path: Path) {
        pathWatcherFactory.pathInvalidated.fire(path)
    }
}