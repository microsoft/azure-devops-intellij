// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OperationExecutor {
    private static final Logger logger = LoggerFactory.getLogger(OperationExecutor.class);
    final int THREAD_RECOVERY_TIMEOUT_SECONDS = 5;
    // For now we are limiting ourselves to 5 threads (single threaded is way too slow)
    final int MAX_THREADS = 5;
    // No need for Core threads and Max threads to be different
    final int CORE_THREADS = MAX_THREADS;
    // The number of items that can be in the Queue needs to be bigger than the number of threads (10x is somewhat arbitrary)
    final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(MAX_THREADS * 10);
    final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_THREADS, MAX_THREADS, THREAD_RECOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS, queue);

    private static class Holder {
        public final static OperationExecutor INSTANCE = new OperationExecutor();
    }

    public static OperationExecutor getInstance() {
        return Holder.INSTANCE;
    }

    public UUID executeAsync(final Operation operation, final Operation.Inputs inputs) {
        execute(operation, inputs);
        return operation.getId();
    }

    public int getQueueSize() {
        return queue.size();
    }

    private synchronized void execute(final Operation operation, final Operation.Inputs inputs) {
        try {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.doWork(inputs);
                }
            });
        } catch (Throwable t) {
            logger.warn("Operation failed", t);
            if (!operation.isFinished()) {
                operation.terminate(t);
            }
        }
    }

    public Future submitOperationTask(Runnable task) {
        return threadPoolExecutor.submit(task);
    }

    public void wait(List<Future> futures) throws ExecutionException {
        for (Future f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                logger.warn("Thread interrupted", e);
            }
        }
    }
}
