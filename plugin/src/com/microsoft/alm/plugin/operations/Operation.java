// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import org.slf4j.Logger;

import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

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

    public static class CredInputsImpl implements Inputs {
        protected boolean promptForCreds = true;

        public void setPromptForCreds(final boolean promptForCreds) {
            this.promptForCreds = promptForCreds;
        }

        public boolean getPromptForCreds() {
            return promptForCreds;
        }
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

    /**
     * Use this method to get a properly authenticated context based on the repositoryContext given.
     *
     * @param repositoryContext
     * @return
     */
    protected static ServerContext getServerContext(final RepositoryContext repositoryContext, final boolean forcePrompt, final boolean allowPrompt, final Logger logger) {
        final ServerContext serverContext;
        logger.info(String.format("getServerContext: url=%s", repositoryContext.getUrl()));
        logger.info(String.format("getServerContext: forcePrompt=%s", forcePrompt));
        logger.info(String.format("getServerContext: allowPrompt=%s", allowPrompt));

        // Get the authenticated context for the Url
        // This should be done on a background thread so as not to block UI or hang the IDE
        // Get the context before doing the server calls to reduce possibility of using an outdated context with expired credentials
        if (ServerContextManager.getInstance().get(repositoryContext.getUrl()) != null && forcePrompt) {
            logger.info("getServerContext: context found. updating auth info");
            // The context already exists, but the credentials may be out of date, so we need to update the auth info
            ServerContextManager.getInstance().updateAuthenticationInfo(repositoryContext.getUrl());
        }

        // Create the context from the appropriate url and repo type
        // Note that this will simply return the existing context if one exists.
        if (repositoryContext.getType() == RepositoryContext.Type.GIT) {
            logger.info("getServerContext: creating GIT context");
            if (allowPrompt) {
                final List<ServerContext> authenticatedContexts = new ArrayList<ServerContext>();
                final List<Future> authTasks = new ArrayList<Future>();
                //TODO: get rid of the calls that create more background tasks unless they run in parallel
                try {
                    authTasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                        @Override
                        public void run() {
                            // Get the authenticated context for the gitRemoteUrl
                            // This should be done on a background thread so as not to block UI or hang the IDE
                            // Get the context before doing the server calls to reduce possibility of using an outdated context with expired credentials
                            final ServerContext context = ServerContextManager.getInstance().getUpdatedContext(repositoryContext.getUrl(), false);
                            if (context != null) {
                                authenticatedContexts.add(context);
                            }
                        }
                    }));
                    OperationExecutor.getInstance().wait(authTasks);
                } catch (Throwable t) {
                    logger.warn("getServerContext: failed to get authenticated server context", t);
                    return null;
                }

                if (authenticatedContexts == null || authenticatedContexts.size() != 1) {
                    logger.warn("getServerContext: Context not found");
                    //no context was found, user might have cancelled
                    return null;
                } else {
                    serverContext = authenticatedContexts.get(0);
                }
            } else {
                serverContext = ServerContextManager.getInstance().createContextFromGitRemoteUrl(repositoryContext.getUrl(), allowPrompt);
            }
        } else {
            logger.info("getServerContext: creating TFVC context");
            serverContext = ServerContextManager.getInstance().createContextFromTfvcServerUrl(
                    repositoryContext.getUrl(), repositoryContext.getTeamProjectName(), allowPrompt);
        }

        if (serverContext == null) {
            logger.warn("getServerContext: failed to get authenticated server context");
            throw new NotAuthorizedException(repositoryContext.getUrl());
        }

        return serverContext;
    }
}
