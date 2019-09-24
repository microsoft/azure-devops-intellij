package com.microsoft.tfs.connector

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.impl.startAndAdviseSuccess
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.reactive.whenTrue
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.microsoft.tfs.model.connector.TfsRoot
import com.microsoft.tfs.model.connector.VersionNumber
import java.util.concurrent.CompletableFuture

class ReactiveClientConnection {
    private val lifetimeDefinition = LifetimeDefinition()
    val lifetime = lifetimeDefinition.lifetime
    private val socket = SocketWire.Server(
        lifetime,
        SingleThreadScheduler(lifetime, "com.microsoft.tfs.connector.ReactiveClientConnection.socket"),
        null).apply {
        // Handle disconnection:
        connected.change.advise(lifetime) { connected ->
            if (!connected) lifetimeDefinition.terminate()
        }
    }
    private val protocol = Protocol(
        Serializers(),
        Identities(IdKind.Server),
        SingleThreadScheduler(lifetime, "com.microsoft.tfs.connector.ReactiveClientConnection.protocol.scheduler"),
        socket,
        lifetime
    )

    val model = TfsRoot.create(lifetime, protocol)

    val port
        get() = socket.port

    fun startAsync(): CompletableFuture<Void> {
        val startLifetime = lifetime.createNested()
        val future = startLifetime.createFuture<Void>()
        socket.connected.whenTrue(startLifetime.lifetime) {
            future.complete(null)
            startLifetime.terminate()
        }
        return future
    }

    fun getVersionAsync(): CompletableFuture<VersionNumber> {
        val ld = lifetime.createNested()
        val future = ld.lifetime.createFuture<VersionNumber>()
        model.version.advise(ld.lifetime) {
            future.complete(it)
            ld.terminate()
        }
        return future
    }

    fun healthCheckAsync(): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        model.healthCheck.startAndAdviseSuccess(Unit) { future.complete(it) }
        return future
    }

    fun terminate() = lifetimeDefinition.terminate()

    fun <T> Lifetime.createFuture(): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        onTermination { future.cancel(false) }
        return future
    }
}