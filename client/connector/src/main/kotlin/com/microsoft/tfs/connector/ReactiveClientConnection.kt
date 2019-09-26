package com.microsoft.tfs.connector

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.onTermination
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.adviseOnce
import com.jetbrains.rd.util.reactive.whenTrue
import com.microsoft.tfs.model.connector.*
import kotlinx.coroutines.CancellationException
import java.util.concurrent.CompletableFuture

class ReactiveClientConnection(private val scheduler: IScheduler) {
    private val lifetimeDefinition = LifetimeDefinition()
    val lifetime = lifetimeDefinition.lifetime
    private val socket = SocketWire.Server(
        lifetime,
        scheduler,
        null).apply {
        // Handle disconnection:
        connected.change.advise(lifetime) { connected ->
            if (!connected) lifetimeDefinition.terminate()
        }
    }
    private val protocol = Protocol(
        Serializers(),
        Identities(IdKind.Server),
        scheduler,
        socket,
        lifetime
    )

    lateinit var model: TfsRoot

    val port
        get() = socket.port

    fun terminate() = lifetimeDefinition.terminate()

    fun startAsync(): CompletableFuture<Void> =
        queueFutureAsync { lt ->
            model = TfsRoot.create(lifetime, protocol).apply {
                connected.whenTrue(lt) {
                    complete(null)
                }
            }
        }

    fun getVersionAsync(): CompletableFuture<VersionNumber> =
        queueFutureAsync { lt ->
            model.version.advise(lt) {
                complete(it)
            }
        }

    fun healthCheckAsync(): CompletableFuture<String?> =
        queueFutureAsync { lt ->
            model.healthCheck.start(Unit).pipeTo(lt, this)
        }

    fun getOrCreateWorkspace(definition: TfsWorkspaceDefinition): TfsWorkspace =
        model.workspaces[definition] ?: TfsWorkspace().apply { model.workspaces[definition] = this }

    fun waitForReadyAsync(workspace: TfsWorkspace): CompletableFuture<Void> =
        queueFutureAsync {
            workspace.isReady.whenTrue(lifetime) { complete(null) }
        }

    fun getPendingChangesAsync(
        workspace: TfsWorkspace,
        paths: List<TfsLocalPath>): CompletableFuture<List<TfsPendingChange>> =
        queueFutureAsync { lt ->
            workspace.getPendingChanges.start(paths).pipeTo(lt, this)
        }

    private fun <T> queueFutureAsync(action: CompletableFuture<T>.(Lifetime) -> Unit): CompletableFuture<T> {
        val lifetime = lifetime.createNested()
        val future = CompletableFuture<T>().whenComplete { _, _ -> lifetime.terminate() }
        lifetime.onTermination { future.cancel(false) }
        scheduler.queue {
            future.action(lifetime)
        }
        return future
    }

    private fun <T> IRdTask<T>.pipeTo(lt: Lifetime, future: CompletableFuture<T>) {
        result.adviseOnce(lt) {
            try {
                future.complete(it.unwrap())
            } catch (ex: CancellationException) {
                future.cancel(false)
            } catch (ex: Throwable) {
                future.completeExceptionally(ex)
            }
        }
    }
}