package com.microsoft.tfs.watcher

import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.reactive.Signal
import com.microsoft.tfs.Logging
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcher
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.PathWatcherReport
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher
import java.nio.file.Path

class ExternallyControlledPathWatcher(
    parentLifetime: Lifetime,
    private val pathInvalidated: Signal<Path>,
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
        pathInvalidated.advise(currentSessionLifetime, ::invalidatePath)
    }

    override fun stopWatching() {
        currentSessionLifetime.terminate()
    }

    override fun isWatching(): Boolean = currentSessionLifetime.isAlive

    override fun poll(): PathWatcherReport {
        val changes = synchronized(lock) {
            val copy = changedPaths.toList()
            changedPaths.clear()
            copy
        }
        return PathWatcherReport(false).apply {
            for (changedPath in changes) {
                addChangedPath(changedPath.toString())
            }
        }
    }

    private fun invalidatePath(path: Path) {
        if (path.startsWith(pathToWatch)) {
            synchronized(lock) {
                changedPaths.add(path)
            }
            workspaceWatcher.pathChanged(this)
        }
    }
}