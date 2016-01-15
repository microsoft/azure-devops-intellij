// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.teamfoundation.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequestSearchCriteria;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.PullRequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

public class PullRequestLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(PullRequestLookupOperation.class);

    public enum PullRequestScope {
        REQUESTED_BY_ME,
        ASSIGNED_TO_ME
    }

    private final ServerContext context;
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

    public PullRequestLookupOperation(final ServerContext context) {
        assert context != null;
        this.context = context;
    }

    public void doWork(final Inputs inputs) {
        onLookupStarted();

        final List<Future> tasks = new ArrayList<Future>();

        try {
            tasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                @Override
                public void run() {
                    doLookup(context, PullRequestScope.REQUESTED_BY_ME);
                }
            }));

            tasks.add(OperationExecutor.getInstance().submitOperationTask(new Runnable() {
                @Override
                public void run() {
                    doLookup(context, PullRequestScope.ASSIGNED_TO_ME);
                }
            }));

            OperationExecutor.getInstance().wait(tasks);
            onLookupCompleted();
        } catch (Throwable t) {
            logger.warn("doWork: failed with an exception", t);
            terminate(t);
        }

    }

    protected void doLookup(final ServerContext context, final PullRequestScope scope) {
        final GitHttpClient gitHttpClient = context.getGitHttpClient();
        final GitPullRequestSearchCriteria criteria = new GitPullRequestSearchCriteria();
        criteria.setRepositoryId(context.getGitRepository().getId());
        criteria.setStatus(PullRequestStatus.ACTIVE);
        criteria.setIncludeLinks(false);

        //TODO: update queries for getting pull requests correctly
        if (scope == PullRequestScope.REQUESTED_BY_ME) {
            final List<GitPullRequest> requestedByMe = gitHttpClient.getPullRequests(context.getGitRepository().getId(), criteria, 256, 0, 101);
            requestedByMeResults.pullRequests.addAll(requestedByMe);
            super.onLookupResults(requestedByMeResults);
        } else if (scope == PullRequestScope.ASSIGNED_TO_ME) {
            final List<GitPullRequest> assignedToMe = gitHttpClient.getPullRequests(context.getGitRepository().getId(), null, 256, 0, 101);
            assignedToMeResults.pullRequests.addAll(assignedToMe);
            super.onLookupResults(assignedToMeResults);
        }
    }
}
