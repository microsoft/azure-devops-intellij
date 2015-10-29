// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This is an abstract Operation class to use as a base class for other operations.
 */
public abstract class Operation {
    public final static LookupInputs EMPTY_INPUTS = null;

    public interface Listener {
        void notifyLookupStarted();

        void notifyLookupCompleted();

        void notifyLookupResults(LookupResults results);
    }

    public interface LookupResults {
        Throwable getError();

        boolean hasError();

        boolean isCanceled();
    }

    public interface LookupInputs {
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

    protected Operation() {
        // This constructor is protected to make sure users don't create one directly
    }

    public void addListener(final Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(final Listener listener) {
        listeners.remove(listener);
    }

    public abstract void doLookup(LookupInputs inputs);

    protected void onLookupStarted() {
        for (final Listener listener : listeners) {
            listener.notifyLookupStarted();
        }
    }

    protected void onLookupCompleted() {
        for (final Listener listener : listeners) {
            listener.notifyLookupCompleted();
        }
    }

    protected void onLookupResults(LookupResults results) {
        for (final Listener listener : listeners) {
            listener.notifyLookupResults(results);
        }
    }
}
