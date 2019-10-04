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

    lateinit var model: TfsModel

    val port
        get() = socket.port

    fun terminate() = lifetimeDefinition.terminate()

    fun startAsync(): CompletableFuture<Void> =
        queueFutureAsync { lt ->
            model = TfsModel.create(lifetime, protocol).apply {
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

    fun getOrCreateCollectionAsync(definition: TfsCollectionDefinition): CompletableFuture<TfsCollection> =
        queueFutureAsync {
            complete(model.collections[definition] ?: TfsCollection().apply { model.collections[definition] = this })
        }

    fun waitForReadyAsync(collection: TfsCollection): CompletableFuture<Void> =
        queueFutureAsync {
            collection.isReady.whenTrue(lifetime) { complete(null) }
        }

    fun getPendingChangesAsync(
        collection: TfsCollection,
        paths: List<TfsLocalPath>): CompletableFuture<List<TfsPendingChange>> =
        queueFutureAsync { lt ->
            collection.getPendingChanges.start(paths).pipeTo(lt, this)
        }

    fun invalidatePathAsync(collection: TfsCollection, path: TfsLocalPath): CompletableFuture<Void> =
        queueFutureAsync { lt ->
            collection.invalidatePath.start(path).pipeToVoid(lt, this)
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

    private fun <T> IRdTask<T>.pipeToVoid(lt: Lifetime, future: CompletableFuture<Void>) {
        result.adviseOnce(lt) {
            try {
                it.unwrap()
                future.complete(null)
            } catch (ex: CancellationException) {
                future.cancel(false)
            } catch (ex: Throwable) {
                future.completeExceptionally(ex)
            }
        }
    }
}