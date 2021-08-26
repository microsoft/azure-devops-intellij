// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials
import com.microsoft.tfs.model.host.*
import org.apache.log4j.Level
import java.nio.file.Paths
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    println("Reactive TFS Client version 1.0")

    val port = args.getOrNull(0)?.toIntOrNull()
    if (port == null) {
        printUsage()
        exitProcess(1)
    }

    initializeApp(args)
    runRdClient(port)
}

private fun printUsage() {
    println("Usage:")
    println("  <binary> <portNumber> [logDirectory]")
    println()
    println("- <portNumber>: port to connect to")
    println("- [logDirectory]: path to the log directory (log file will be created automatically)")
    println("- [logLevel]: log level (either ALL, DEBUG, INFO, WARN, ERROR, FATAL, OFF, or TRACE), INFO by default")
}

private fun initializeApp(args: Array<String>) {
    Locale.setDefault(Locale.US)
    val logDirectory = if (args.size > 1) Paths.get(args[1]) else null
    val logLevel = if (args.size > 2) Level.toLevel(args[2]) else Level.INFO
    Logging.initialize(logDirectory, logLevel)
}

private fun runRdClient(portNumber: Int) {
    val logger = Logging.getLogger("Main")
    logger.info { "Application initializing" }

    val appLifetime = LifetimeDefinition()
    val scheduler = SingleThreadScheduler(appLifetime, "com.microsoft.tfs.MainScheduler")
    val socket = SocketWire.Client(appLifetime, scheduler, portNumber, "com.microsoft.tfs.MainSocketClient")
    startSocketWatchdog(appLifetime, socket, logger)

    val protocol = Protocol(Serializers(), Identities(IdKind.Client), scheduler, socket, appLifetime)
    scheduler.queue {
        val model = TfsModel.create(appLifetime, protocol)
        model.shutdown.advise(appLifetime) {
            logger.info { "Shutting down per request" }
            appLifetime.terminate()
        }

        initializeModel(appLifetime, model)
    }

    logger.info { "Application initialized, waiting termination" }
    waitTermination(appLifetime)
}

private fun startSocketWatchdog(ld: LifetimeDefinition, socket: SocketWire.Client, logger: Logger) {
    socket.connected.change.advise(ld) { connected ->
        if (!connected) {
            logger.info { "Socket has been disconnected, terminating" }
            ld.terminate()
        }
    }
}

private fun waitTermination(lifetime: Lifetime, msBetweenChecks: Long = 1000L) {
    while (lifetime.isAlive) {
        Thread.sleep(msBetweenChecks)
    }
}

private fun initializeModel(lifetime: Lifetime, model: TfsModel) {
    val logger = Logging.getLogger("Workspace")

    model.collections.view(lifetime, ::initializeCollection)
    model.getBasicWorkspaceInfo.set { workspacePath ->
        logger.info { "Searching basic workspace info for path \"$workspacePath\"." }
        val result = TfsClient.getBasicWorkspaceInfo(workspacePath)
        logger.info { "Workspace for $workspacePath is ${if (result == null) "not " else ""}detected." }
        result
    }
    model.getDetailedWorkspaceInfo.set { request ->
        logger.info { "Searching detailed workspace info for path \"${request.workspacePath}\"." }
        val result = TfsClient.getDetailedWorkspaceInfo(request.workspacePath, request.credentials)
        logger.info { "Workspace for ${request.workspacePath} is ${if (result == null) "not " else ""}detected." }
        result
    }
}

private fun initializeCollection(lifetime: Lifetime, definition: TfsCollectionDefinition, collection: TfsCollection) {
    val logger = Logging.getLogger("Collection")
    logger.info { "Initializing collection for ${definition.serverUri}" }

    val credentials = definition.credentials.run { UsernamePasswordCredentials(login, password.contents) }
    val client = TfsClient(lifetime, definition.serverUri, credentials)

    collection.getPendingChanges.set { paths ->
        logger.info { "Calculating pending changes for ${paths.size} paths" }
        val result = client.status(paths).flatMap(::toPendingChanges).toList()
        logger.info { "${result.size} changes detected" }
        logger.info { "First 10 changes: " + result.take(10).joinToString { it.serverItem } }
        result
    }

    fun logPaths(title: String, paths: List<TfsPath>) {
        logger.info { "Performing $title operation on ${paths.size} paths, first 10: ${paths.take(10).joinToString()}" }
    }

    collection.getLocalItemsInfo.set { paths ->
        if (paths.isEmpty()) return@set emptyList()

        logPaths("Get Local Items Info", paths)
        client.getLocalItemsInfo(paths)
    }

    collection.getExtendedLocalItemsInfo.set { paths ->
        if (paths.isEmpty()) return@set emptyList()

        logPaths("Get Local Items Info", paths)
        client.getExtendedLocalItemsInfo(paths)
    }

    collection.invalidatePaths.set { paths ->
        if (paths.isEmpty()) return@set

        logPaths("Invalidate", paths)
        client.invalidatePaths(paths)
    }

    collection.addFiles.set { paths ->
        if (paths.isEmpty()) return@set emptyList()

        logPaths("Add", paths)
        client.addFiles(paths)
    }

    collection.deleteFilesRecursively.set { paths ->
        if (paths.isEmpty()) return@set TfsDeleteResult(emptyList(), emptyList(), emptyList())

        logPaths("Recursive Delete", paths)
        client.deletePathsRecursively(paths)
    }

    collection.undoLocalChanges.set { paths ->
        if (paths.isEmpty()) return@set emptyList()

        logPaths("Undo", paths)
        client.undoLocalChanges(paths)
    }

    collection.checkoutFilesForEdit.set { parameters ->
        logPaths("Checkout (recursive: ${parameters.recursive})", parameters.filePaths)
        client.checkoutFilesForEdit(parameters.filePaths, parameters.recursive)
    }

    collection.renameFile.set { (oldPath, newPath) ->
        logger.info { "Performing Rename operation on \"${oldPath.path}\" to \"${newPath.path}\"" }
        client.renameFile(oldPath, newPath)
    }

    client.workspaces.advise(lifetime) { workspaces ->
        val paths = workspaces.flatMap { it.mappedPaths.map(::TfsLocalPath) }
        collection.mappedPaths.set(paths)
    }

    collection.isReady.set(true)
}