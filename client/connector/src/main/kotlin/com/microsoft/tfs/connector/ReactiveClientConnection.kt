// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

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
import java.util.concurrent.CompletionStage

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

    fun startAsync(): CompletionStage<Void> =
        queueFutureAsync { lt ->
            model = TfsModel.create(lifetime, protocol).apply {
                connected.whenTrue(lt) {
                    complete(null)
                }
            }
        }

    fun getOrCreateCollectionAsync(definition: TfsCollectionDefinition): CompletionStage<TfsCollection> =
        queueFutureAsync {
            complete(model.collections[definition] ?: TfsCollection().apply { model.collections[definition] = this })
        }

    fun waitForReadyAsync(collection: TfsCollection): CompletionStage<Void> =
        queueFutureAsync {
            collection.isReady.whenTrue(lifetime) { complete(null) }
        }

    fun getPendingChangesAsync(
        collection: TfsCollection,
        paths: List<TfsPath>): CompletionStage<List<TfsPendingChange>> =
        queueFutureAsync { lt ->
            collection.getPendingChanges.start(paths).pipeTo(lt, this)
        }

    fun invalidatePathsAsync(collection: TfsCollection, paths: List<TfsLocalPath>): CompletionStage<Void> =
        queueFutureAsync { lt ->
            collection.invalidatePaths.start(paths).pipeToVoid(lt, this)
        }

    fun deleteFilesRecursivelyAsync(collection: TfsCollection, paths: List<TfsPath>): CompletionStage<Void> =
        queueFutureAsync { lt ->
            collection.deleteFilesRecursively.start(paths).pipeToVoid(lt, this)
        }

    fun undoLocalChangesAsync(collection: TfsCollection, paths: List<TfsPath>): CompletionStage<List<TfsLocalPath>> =
        queueFutureAsync { lt ->
            collection.undoLocalChanges.start(paths).pipeTo(lt, this)
        }

    private fun <T> queueFutureAsync(action: CompletableFuture<T>.(Lifetime) -> Unit): CompletionStage<T> {
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