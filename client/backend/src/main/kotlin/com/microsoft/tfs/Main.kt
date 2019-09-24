package com.microsoft.tfs

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.microsoft.tfs.model.host.TfsRoot
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val port = args.getOrNull(0)?.toIntOrNull()
    if (port == null) {
        printUsage()
        exitProcess(1)
    }

    runRdClient(port)
}

private fun printUsage() {
    println("Reactive TFS Client ver. 1.0")
    println("Command-line options:")
    println("  <portNumber>: port to connect to")
}

private fun runRdClient(portNumber: Int) {
    val appLifetimeDefinition = LifetimeDefinition()
    val appLifetime = appLifetimeDefinition.lifetime
    val scheduler = SingleThreadScheduler(appLifetime, "com.microsoft.tfs.MainScheduler")
    val socket = SocketWire.Client(appLifetime, scheduler, portNumber, "com.microsoft.tfs.MainSocketClient")
    val protocol = Protocol(Serializers(), Identities(IdKind.Client), scheduler, socket, appLifetime)
    val model = TfsRoot.create(appLifetime, protocol)

    model.shutdown.advise(appLifetime) { appLifetimeDefinition.terminate() }
    // TODO: Terminate if wasn't able to connect
}