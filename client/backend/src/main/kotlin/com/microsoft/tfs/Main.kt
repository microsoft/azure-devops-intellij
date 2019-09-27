package com.microsoft.tfs

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.trace
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials
import com.microsoft.tfs.jni.loader.NativeLoader
import com.microsoft.tfs.model.host.TfsRoot
import com.microsoft.tfs.model.host.TfsWorkspace
import com.microsoft.tfs.model.host.TfsWorkspaceDefinition
import com.microsoft.tfs.model.host.VersionNumber
import org.apache.log4j.Level
import java.nio.file.Paths
import kotlin.system.exitProcess

val protocolVersion = VersionNumber(1, 0)

fun main(args: Array<String>) {
    println("Reactive TFS Client version 1.0")
    println("Protocol version: ${protocolVersion.major}.${protocolVersion.minor}")

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
    val logDirectory = if (args.size > 1) Paths.get(args[1]) else null
    val logLevel = if (args.size > 2) Level.toLevel(args[2]) else Level.INFO
    Logging.initialize(logDirectory, logLevel)

    if (System.getProperty(NativeLoader.NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY) == null) {
        val possibleNativeLibsPath = Paths.get("nativeLibs")
        if (possibleNativeLibsPath.toFile().isDirectory)
            System.setProperty(NativeLoader.NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY, possibleNativeLibsPath.toString())
    }

    Logging.getLogger("Main").info {
        "${NativeLoader.NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY}: " +
            System.getProperty(NativeLoader.NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY)
    }
}

private fun runRdClient(portNumber: Int) {
    val logger = Logging.getLogger("Main")
    logger.info { "Application initializing" }

    val appLifetimeDefinition = LifetimeDefinition()
    startConsoleWatchdog(appLifetimeDefinition)

    val appLifetime = appLifetimeDefinition.lifetime
    val scheduler = SingleThreadScheduler(appLifetime, "com.microsoft.tfs.MainScheduler")
    val socket = SocketWire.Client(appLifetime, scheduler, portNumber, "com.microsoft.tfs.MainSocketClient")
    val protocol = Protocol(Serializers(), Identities(IdKind.Client), scheduler, socket, appLifetime)
    scheduler.queue {
        val model = TfsRoot.create(appLifetime, protocol)
        model.shutdown.advise(appLifetime) {
            logger.info { "Shutting down per request" }
            appLifetimeDefinition.terminate()
        }
        model.version.set(protocolVersion)
        model.healthCheck.set { _ ->
            val result = healthCheck()
            logger.info { "Health check performed, result: $result" }
            result
        }

        model.workspaces.view(appLifetime) { _, definition, workspace -> initializeWorkspace(definition, workspace) }
    }

    logger.info { "Application initialized, waiting termination" }
    waitTermination(appLifetime)
}

/**
 * Will terminate the lifetime when [System.out.checkError] returns true.
 */
private fun startConsoleWatchdog(ld: LifetimeDefinition, msBetweenChecks: Long = 1000L) = Thread {
    while (ld.isAlive) {
        if (System.out.checkError()) {
            Logging.getLogger("ConsoleWatchdog").info { "System.out.checkError returned true, terminating" }
            ld.terminate()
        }
        Thread.sleep(msBetweenChecks)
    }
}.start()

private fun waitTermination(lifetime: Lifetime, msBetweenChecks: Long = 1000L) {
    while (lifetime.isAlive) {
        Thread.sleep(msBetweenChecks)
    }
}

private fun healthCheck(): String? = try {
    // Accessing the DefaultPersistenceStoreProvider will trigger loading native library and throw an exception in
    // case the libraries aren't set up properly
    val provider = DefaultPersistenceStoreProvider.INSTANCE
    if (provider == null)
        "Cannot create DefaultPersistenceStoreProvider"
    else null
} catch (t: Throwable) {
    Logging.getLogger("Main").error(t)
    t.message
}

private fun initializeWorkspace(definition: TfsWorkspaceDefinition, workspace: TfsWorkspace) {
    val logger = Logging.getLogger("Workspace")
    logger.info { "Initializing workspace for ${definition.localPath}" }

    val credentials = definition.credentials.run { UsernamePasswordCredentials(login, password) }
    val client = TfsClient(definition.localPath.toJavaPath(), credentials)
    workspace.isReady.set(true)

    workspace.getPendingChanges.set { paths ->
        logger.trace { "Calculating pending changes for ${paths.size} paths" }
        val result = client.status(paths.map { it.toJavaPath() }).flatMap(::toPendingChanges).toList()
        logger.trace { "${result.size} changes detected" }
        logger.trace { "First 10 changes: " + result.take(10).joinToString { it.serverItem } }
        result
    }
}