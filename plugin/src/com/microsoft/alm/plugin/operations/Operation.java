// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This is an abstract Operation class to use as a base class for other operations.
 */
public abstract class Operation {
    public final static Inputs EMPTY_INPUTS = null;

    public enum State {NOT_STARTED, STARTED, CANCELLED, COMPLETED}

    public interface Listener {
        void notifyLookupStarted();

        void notifyLookupCompleted();

        void notifyLookupResults(Results results);
    }

    public interface Results {
        Throwable getError();

        boolean hasError();

        boolean isCancelled();
    }

    public static class ResultsImpl implements Results {
        protected boolean isCancelled = false;
        protected Throwable error = null;

        @Override
        public Throwable getError() {
            return error;
        }

        @Override
        public boolean hasError() {
            return error != null;
        }

        @Override
        public boolean isCancelled() {
            return isCancelled;
        }
    }

    public interface Inputs {
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private final UUID id;
    private State state;

    // This constructor is protected to make sure users don't create one directly
    protected Operation() {
        id = UUID.randomUUID();
        state = State.NOT_STARTED;
    }

    public UUID getId() {
        return id;
    }

    public State getState() {
        return state;
    }

    public boolean isFinished() {
        return state == State.COMPLETED || state == State.CANCELLED;
    }

    public boolean isCancelled() {
        return state == State.CANCELLED;
    }

    public void addListener(final Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(final Listener listener) {
        listeners.remove(listener);
    }

    public void doWorkAsync(final Inputs inputs) {
        OperationExecutor.getInstance().executeAsync(this, inputs);
    }

    public abstract void doWork(final Inputs inputs);

    public void cancel() {
        state = State.CANCELLED;
    }

    protected void terminate(final Throwable throwable) {
        state = State.COMPLETED;
    }

    protected void onLookupStarted() {
        state = State.STARTED;
        for (final Listener listener : listeners) {
            listener.notifyLookupStarted();
        }
    }

    protected void onLookupCompleted() {
        if (state != State.CANCELLED) {
            state = State.COMPLETED;
        }

        for (final Listener listener : listeners) {
            listener.notifyLookupCompleted();
        }
    }

    protected void onLookupResults(final Results results) {
        for (final Listener listener : listeners) {
            listener.notifyLookupResults(results);
        }
    }
}
