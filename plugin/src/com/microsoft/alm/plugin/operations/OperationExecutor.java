// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OperationExecutor {
    private static final Logger logger = LoggerFactory.getLogger(OperationExecutor.class);
    final int MAX_THREADS = 2;
    final int CORE_THREADS = MAX_THREADS;
    final int THREAD_RECOVERY_TIMEOUT_SECONDS = 5;
    final BlockingQueue<Runnable> queue = new PriorityBlockingQueue<Runnable>();
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
}
