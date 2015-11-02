// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.soap.CatalogService;
import com.microsoft.teamfoundation.core.webapi.CoreHttpClient;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerContextLookupOperation {
    public enum ContextScope {REPOSITORY, PROJECT}

    enum State {NEW, RUNNING, COMPLETED, CANCELED}

    public interface Listener {
        void notifyLookupStarted();

        void notifyLookupCompleted();

        void notifyLookupCanceled();

        void notifyLookupResults(List<ServerContext> serverContexts);
    }

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private State state;
    private final List<ServerContext> contextList;
    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private final ContextScope resultScope;

    public ServerContextLookupOperation(final List<ServerContext> contextList, final ContextScope resultScope) {
        assert contextList != null;
        assert !contextList.isEmpty();

        this.contextList = new ArrayList<ServerContext>(contextList.size());
        this.contextList.addAll(contextList);

        state = State.NEW;
        this.resultScope = resultScope;
    }

    private Lookup lookup;

    private class Lookup {
        //TODO these should be moved elsewhere and shared
        final int MAX_THREADS = 10;
        final int CORE_THREADS = Math.min(contextList.size(), MAX_THREADS);
        final int THREAD_RECOVERY_TIMEOUT_SECONDS = 5;
        final BlockingQueue<Runnable> queue;
        final ThreadPoolExecutor threadPoolExecutor;

        final boolean runAsync;
        int scheduledJobCount = 0;
        private List<ServerContext> serverContexts = new ArrayList<ServerContext>();

        public Lookup(final boolean runAsync) {
            this.runAsync = runAsync;
            if (runAsync) {
                queue = new PriorityBlockingQueue<Runnable>();
                threadPoolExecutor = new ThreadPoolExecutor(CORE_THREADS, MAX_THREADS, THREAD_RECOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS, queue);
            } else {
                queue = null;
                threadPoolExecutor = null;
            }
        }

        private class WrappedRunnable implements Runnable, Comparable {

            private final Runnable realRunnable;

            public WrappedRunnable(final Runnable realRunnable) {
                this.realRunnable = realRunnable;
            }

            @Override
            public void run() {
                try {
                    realRunnable.run();
                } catch (Throwable t) {
                    // TODO should we actually cancel the entire job, or does it make sense to try to recover?
                    cancel();
                    throw new RuntimeException(t);
                } finally {
                    boolean completed = false;
                    try {
                        writeLock.lock();
                        if (!isCanceled()) {
                            scheduledJobCount--;
                            if (scheduledJobCount == 0) {
                                completed = true;
                                state = State.COMPLETED;
                            }
                        }
                    } finally {
                        //downgrade the lock prior to notifying
                        writeLock.unlock();

                        if (completed) {
                            onLookupCompleted();
                        }
                    }
                }
            }

            @Override
            public int compareTo(final Object o) {
                //TODO implement priorities?
                return 0;
            }
        }

        private void execute(final Runnable runnable) {
            final WrappedRunnable wrappedRunnable = new WrappedRunnable(runnable);
            try {
                writeLock.lock();
                if (isCanceled()) {
                    return;
                }
                scheduledJobCount++;
            } finally {
                writeLock.unlock();
            }
            try {
                if (runAsync) {
                    threadPoolExecutor.execute(wrappedRunnable);
                } else {
                    wrappedRunnable.run();
                }
            } catch (Throwable t) {
                // TODO should we actually cancel the entire job, or does it make sense to try to recover?
                cancel();
                throw new RuntimeException(t);
            }
        }

        private void addResults(final List<ServerContext> results) {
            if (isCanceled()) {
                return;
            }
            try {
                writeLock.lock();
                serverContexts.addAll(results);
            } finally {
                //downgrade the lock prior to notifying
                writeLock.unlock();
                onLookupResults(results);
            }
        }

        private void notifyStarting() {
            if (isCanceled()) {
                return;
            }
            onLookupStarted();
        }
    }

    protected void onLookupStarted() {
        try {
            readLock.lock();
            for (final Listener listener : listeners) {
                listener.notifyLookupStarted();
            }
        } finally {
            readLock.unlock();
        }
    }

    protected void onLookupResults(List<ServerContext> results) {
        try {
            readLock.lock();
            for (final Listener listener : listeners) {
                listener.notifyLookupResults(results);
            }
        } finally {
            readLock.unlock();
        }
    }

    protected void onLookupCompleted() {
        try {
            readLock.lock();
            for (final Listener listener : listeners) {
                listener.notifyLookupCompleted();
            }
        } finally {
            readLock.unlock();
        }
    }

    protected void onLookupCanceled() {
        try {
            // downgrade the lock prior to notifying listeners
            readLock.lock();
            writeLock.unlock();
            for (final Listener listener : listeners) {
                listener.notifyLookupCanceled();
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Runs asynchronously
     */
    public void lookupContextsAsync() {
        doLookupContexts(true);
    }

    /**
     * Runs synchronously
     */
    public void lookupContextsSync() {
        doLookupContexts(false);
    }

    private void doLookupContexts(final boolean runAsync) {
        try {
            writeLock.lock();
            if (state != State.NEW) {
                throw new IllegalStateException("This operation has already been executed");
            }
            state = State.RUNNING;
        } finally {
            writeLock.unlock();
        }

        final Runnable rootRunnable = new Runnable() {
            public void run() {
                if (isCanceled()) {
                    return;
                }
                for (final ServerContext context : contextList) {
                    if (context.getType() == ServerContext.Type.TFS) {
                        doSoapCollectionLookup(context);
                    } else { // VSO_DEPLOYMENT || VSO
                        //testing only
                        //AuthenticationManager.getInstance().getSessionTokenInfo(context.getAuthenticationResult(), context.getAccountId(), context.getUri().toString(), true);
                        doRestCollectionLookup(context);
                    }
                }
            }
        };

        lookup = new Lookup(runAsync);
        lookup.notifyStarting();
        lookup.execute(rootRunnable);
    }

    private void doRestCollectionLookup(final ServerContext context) {
        final Runnable projectCollectionsRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCanceled()) {
                    return;
                }
                final CoreHttpClient rootClient = new CoreHttpClient(context.getClient(), context.getUri());
                final List<TeamProjectCollectionReference> collections = rootClient.getProjectCollections(null, null);
                doLookup(context, collections);

            }
        };
        lookup.execute(projectCollectionsRunnable);
    }

    private void doSoapCollectionLookup(final ServerContext context) {
        final Runnable projectCollectionsRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCanceled()) {
                    return;
                }

                CatalogService catalogService = context.getSoapServices().getCatalogService();
                final List<TeamProjectCollectionReference> collections = catalogService.getProjectCollections();
                doLookup(context, collections);
            }
        };
        lookup.execute(projectCollectionsRunnable);
    }

    private void doLookup(final ServerContext context, final List<TeamProjectCollectionReference> collections) {
        for (final TeamProjectCollectionReference teamProjectCollectionReference : collections) {
            final Runnable projectLookupRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isCanceled()) {
                        return;
                    }

                    // --------- resultScope == ContextScope.PROJECT -------
                    // Ideally, we would be using the following client to get the list of projects
                    // But getProjects doesn't allow us to filter to just Git Team Projects, so we get the list of repos and filter to unique projects
                    // -----------------------------------------------------
                    //final CoreHttpClient client = new CoreHttpClient(context.getClient(), collectionURI);
                    //final List<TeamProjectReference> projects = client.getProjects();
                    // -----------------------------------------------------

                    final URI collectionURI = URI.create(context.getUri().toString() + "/" + teamProjectCollectionReference.getName());
                    final GitHttpClient gitClient = new GitHttpClient(context.getClient(), collectionURI);
                    final List<GitRepository> gitRepositories = gitClient.getRepositories();
                    if (isCanceled()) {
                        return;
                    }

                    final List<ServerContext> serverContexts = new ArrayList<ServerContext>(gitRepositories.size());
                    final Set<UUID> includedContexts = new HashSet<UUID>(gitRepositories.size());

                    for (final GitRepository gitRepository : gitRepositories) {
                        // If we are just looking for projects, only get the unique ones
                        if (resultScope == ContextScope.PROJECT) {
                            final UUID key = gitRepository.getProjectReference().getId();
                            if (includedContexts.contains(key)) {
                                continue;
                            } else {
                                includedContexts.add(key);
                            }
                        }

                        ServerContext gitServerContext = new ServerContextBuilder(context)
                                .repository(gitRepository)
                                .teamProject(gitRepository.getProjectReference())
                                .collection(teamProjectCollectionReference)
                                .build();
                        serverContexts.add(gitServerContext);
                    }
                    lookup.addResults(serverContexts);
                }
            };
            lookup.execute(projectLookupRunnable);
        }
    }


    public void cancel() {
        try {
            writeLock.lock();
            if (state == State.CANCELED) {
                return; // already canceled
            } else if (state == State.COMPLETED) {
                return; // already completed
            }
            state = State.CANCELED;
        } finally {
            onLookupCanceled();
        }
    }

    public void addListener(final Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(final Listener listener) {
        listeners.remove(listener);
    }

    private boolean checkState(final State stateToCheck) {
        try {
            readLock.lock();
            return state == stateToCheck;
        } finally {
            readLock.unlock();
        }
    }

    public boolean isNew() {
        return checkState(State.NEW);
    }

    public boolean isRunning() {
        return checkState(State.RUNNING);
    }

    public boolean isComplete() {
        return checkState(State.COMPLETED);
    }

    public boolean isCanceled() {
        return checkState(State.CANCELED);
    }

    /**
     * This will return an empty list while the query is running
     *
     * @return the resulting lookup results
     */
    public List<ServerContext> getResults() {
        try {
            readLock.lock();
            if (state == State.NEW || state == State.RUNNING) {
                return Collections.<ServerContext>emptyList();
            }
            return lookup.serverContexts;
        } finally {
            readLock.unlock();
        }
    }
}
