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
    private val changedPaths = mutableSetOf<Path>()
    private var isFullyInvalidated = true

    private val sessionLifetimes = SequentialLifetimes(parentLifetime)
    private var currentSessionLifetime = LifetimeDefinition.Terminated

    override fun getPath(): String = pathToWatch.toString()

    override fun hasChanged(): Boolean = synchronized(lock) {
        changedPaths.size > 0
    }

    override fun setClean() {
        synchronized(lock) {
            isFullyInvalidated = false
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
            if (fullyInvalidated) {
                fullyInvalidate()
            } else {
                for (changedPath in changes) {
                    addChangedPath(changedPath.toString())
                }
            }

            logger.trace {
                "Returning PathWatcherReport (fullyInvalidated = ${this.fullyInvalidated}, nothingChanged = $isNothingChanged) with paths:\n" +
                        changedPaths.joinToString("\n")
            }
        }
    }

    private fun invalidatePaths(paths: List<Path>) {
        var shouldFullyInvalidate = false
        val pathsToInvalidate = mutableListOf<Path>()
        for (path in paths) {
            if (pathToWatch.startsWith(path)) {
                logger.info { "Fully invalidating watcher for path $pathToWatch because path $path is its parent" }
                shouldFullyInvalidate = true
                break
            } else if (path.toFile().isDirectory) {
                // Path watchers in TFS SDK don't currently support recursive invalidation, so we could either enumerate
                // all the files as invalidated ourselves, or report a full invalidation (even if it's too greedy).
                //
                // For now, we'll always report full invalidation: in most cases (or maybe even all of them?), IDEA asks
                // for recursive directory invalidation when a user has pressed a "Refresh" button manually or something
                // major happened, like a check-in.
                logger.info {
                    "Fully invalidating watcher for path $pathToWatch because path $path points to a directory"
                }
                shouldFullyInvalidate = true
                break
            } else if (path.startsWith(pathToWatch))
                pathsToInvalidate.add(path)
        }

        if (pathsToInvalidate.isNotEmpty() || shouldFullyInvalidate) {
            synchronized(lock) {
                if (shouldFullyInvalidate) {
                    isFullyInvalidated = true
                    changedPaths.clear()
                } else {
                    for (path in pathsToInvalidate) {
                        changedPaths.add(path)
                    }
                }
            }

            logger.trace { "Emitting changed path signal: $pathToWatch" }
            workspaceWatcher.pathChanged(this)
        }
    }
}