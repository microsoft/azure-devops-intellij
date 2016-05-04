// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.alm.sourcecontrol.webapi.model.GitPullRequestSearchCriteria;
import com.microsoft.alm.sourcecontrol.webapi.model.PullRequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

public class PullRequestLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(PullRequestLookupOperation.class);

    public enum PullRequestScope {
        REQUESTED_BY_ME,
        ASSIGNED_TO_ME,
        ALL
    }

    private final String gitRemoteUrl;
    private final PullRequestLookupResults requestedByMeResults = new PullRequestLookupResults(PullRequestScope.REQUESTED_BY_ME);
    private final PullRequestLookupResults assignedToMeResults = new PullRequestLookupResults(PullRequestScope.ASSIGNED_TO_ME);

    public class PullRequestLookupResults extends ResultsImpl {
        private final List<GitPullRequest> pullRequests = new ArrayList<GitPullRequest>();
        private final PullRequestScope scope;

        public PullRequestLookupResults(final PullRequestScope scope) {
            this.scope = scope;
        }

        public List<GitPullRequest> getPullRequests() {
            return Collections.unmodifiableList(pullRequests);
        }

        public PullRequestScope getScope() {
            return scope;
        }
    }

    public PullRequestLookupOperation(final String gitRemoteUrl) {
        assert gitRemoteUrl != null;
        this.gitRemoteUrl = gitRemoteUrl;
    }

    public void doWork(final Inputs inputs) {
        onLookupStarted();

        final List<ServerContext> authenticatedContexts = new ArrayList<ServerContext>();
        final List<Future> authTasks = new ArrayList<Future>();
        try {
            authTasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                @Override
                public void run() {
                    // Get the authenticated context for the gitRemoteUrl
                    // This should be done on a background thread so as not to block UI or hang the IDE
                    // Get the context before doing the server calls to reduce possibility of using an outdated context with expired credentials
                    final ServerContext context = ServerContextManager.getInstance().getAuthenticatedContext(gitRemoteUrl, false);
                    if (context != null) {
                        authenticatedContexts.add(context);
                    }
                }
            }));
            OperationExecutor.getInstance().wait(authTasks);
        } catch (Throwable t) {
            logger.warn("doWork: failed to get authenticated server context", t);
            terminate(new NotAuthorizedException(gitRemoteUrl));
        }

        if (authenticatedContexts == null || authenticatedContexts.size() != 1) {
            //no context was found, user might have cancelled
            terminate(new NotAuthorizedException(gitRemoteUrl));
        }

        final ServerContext context = authenticatedContexts.get(0);
        final List<Future> lookupTasks = new ArrayList<Future>();
        try {
            lookupTasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                @Override
                public void run() {
                    doLookup(context, PullRequestScope.REQUESTED_BY_ME);
                }
            }));
            lookupTasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                @Override
                public void run() {
                    doLookup(context, PullRequestScope.ASSIGNED_TO_ME);
                }
            }));
            OperationExecutor.getInstance().wait(lookupTasks);
            onLookupCompleted();
        } catch (Throwable t) {
            logger.warn("doWork: failed with an exception", t);
            terminate(t);
        }

    }

    protected void doLookup(final ServerContext context, final PullRequestScope scope) {
        try {
            final GitHttpClient gitHttpClient = context.getGitHttpClient();
            final PullRequestLookupResults results = scope == PullRequestScope.REQUESTED_BY_ME ? requestedByMeResults : assignedToMeResults;

            //setup criteria for the query
            final GitPullRequestSearchCriteria criteria = new GitPullRequestSearchCriteria();
            criteria.setRepositoryId(context.getGitRepository().getId());
            criteria.setStatus(PullRequestStatus.ACTIVE);
            criteria.setIncludeLinks(false);
            if (scope == PullRequestScope.REQUESTED_BY_ME) {
                criteria.setCreatorId(context.getUserId());
            } else {
                criteria.setReviewerId(context.getUserId());
            }

            //query server and add results
            final List<GitPullRequest> pullRequests = gitHttpClient.getPullRequests(context.getGitRepository().getId(), criteria, 256, 0, 101);
            logger.debug("doLookup: Found {} pull requests {} on repo {}", pullRequests.size(), scope.toString(), context.getGitRepository().getRemoteUrl());
            results.pullRequests.addAll(pullRequests);
            super.onLookupResults(results);

        } catch (Throwable t) {
            logger.warn("doLookup: failed with an exception", t);
            terminate(t);
        }
    }

    @Override
    protected void terminate(final Throwable t) {
        super.terminate(t);

        final PullRequestLookupResults results = new PullRequestLookupResults(PullRequestScope.ALL);
        results.error = t;
        onLookupResults(results);
        onLookupCompleted();
    }
}
