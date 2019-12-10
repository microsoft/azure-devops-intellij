// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs.watcher

import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.reactive.Signal
import com.jetbrains.rd.util.trace
import com.microsoft.tfs.Logging
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcher
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcherReport
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher
import java.nio.file.Path

class ExternallyControlledPathWatcher(
    parentLifetime: Lifetime,
    private val pathsInvalidated: Signal<List<Path>>,
    private val workspaceWatcher: WorkspaceWatcher,
    private val pathToWatch: Path
) : PathWatcher {

    companion object {
        private val logger = Logging.getLogger<ExternallyControlledPathWatcher>()
    }

    init {
        logger.info { "Path watcher created for path $pathToWatch" }
    }

    private val lock = Any()
    private val changedPaths = mutableListOf<Path>()
    private var isFullyInvalidated = false

    private val sessionLifetimes = SequentialLifetimes(parentLifetime)
    private var currentSessionLifetime = LifetimeDefinition.Terminated

    override fun getPath(): String = pathToWatch.toString()

    override fun hasChanged(): Boolean = synchronized(lock) {
        changedPaths.size > 0
    }

    override fun setClean() {
        synchronized(lock) {
            changedPaths.clear()
        }
    }

    override fun startWatching() {
        currentSessionLifetime = sessionLifetimes.next()
        pathsInvalidated.advise(currentSessionLifetime, ::invalidatePaths)
    }

    override fun stopWatching() {
        currentSessionLifetime.terminate()
    }

    override fun isWatching(): Boolean = currentSessionLifetime.isAlive

    override fun poll(): PathWatcherReport {
        val fullyInvalidated: Boolean
        val changes: List<Path>
        synchronized(lock) {
            changes = changedPaths.toList()
            changedPaths.clear()

            fullyInvalidated = isFullyInvalidated
            isFullyInvalidated = false
        }

        return PathWatcherReport(false).apply {
            for (changedPath in changes) {
                addChangedPath(changedPath.toString())
            }
            if (fullyInvalidated) {
                fullyInvalidate()
            }

            logger.trace { "Returning PathWatcherReport (fullyInvalidated = ${this.fullyInvalidated}, nothingChanged = $isNothingChanged) with paths:\n" + changedPaths.joinToString("\n") }
        }
    }

    private fun invalidatePaths(paths: List<Path>) {
        val pathsToInvalidate = paths.filter { it.startsWith(pathToWatch) }
        if (pathsToInvalidate.isNotEmpty()) {
            synchronized(lock) {
                for (path in pathsToInvalidate) {
                    if (path == pathToWatch)
                        isFullyInvalidated = true
                    changedPaths.add(path)
                    logger.trace { "Emitting changed path signal: $path" }
                }
            }
            workspaceWatcher.pathChanged(this)
        }
    }
}