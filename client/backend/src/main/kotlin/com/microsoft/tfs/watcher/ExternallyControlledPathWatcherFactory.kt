package com.microsoft.tfs.watcher

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Signal
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcher
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcherFactory
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher
import java.nio.file.Path
import java.nio.file.Paths

class ExternallyControlledPathWatcherFactory(private val lifetime: Lifetime) : PathWatcherFactory {
    val pathsInvalidated = Signal<List<Path>>()
    override fun newPathWatcher(path: String, watcher: WorkspaceWatcher): PathWatcher =
        ExternallyControlledPathWatcher(lifetime, pathsInvalidated, watcher, Paths.get(path))
}