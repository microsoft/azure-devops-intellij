// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.build.webapi.BuildHttpClient;
import com.microsoft.alm.build.webapi.model.Build;
import com.microsoft.alm.build.webapi.model.BuildQueryOrder;
import com.microsoft.alm.build.webapi.model.BuildRepository;
import com.microsoft.alm.build.webapi.model.BuildResult;
import com.microsoft.alm.build.webapi.model.BuildStatus;
import com.microsoft.alm.client.utils.StringUtil;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class BuildStatusLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(BuildStatusLookupOperation.class);

    private final String gitRemoteUrl;
    private final String branch;

    public class BuildStatusResults extends ResultsImpl {
        private final ServerContext context;
        private final boolean buildFound;
        private final boolean successful;
        private final String buildName;
        private final Date finishTime;

        public BuildStatusResults(final ServerContext context, final boolean buildFound, final boolean successful, final String buildName, final Date finishTime) {
            logger.info("Creating build status results.");
            logger.info("   buildFound = " + buildFound);
            logger.info("   successful = " + successful);
            logger.info("   buildName = " + buildName);
            logger.info("   finishTime = " + finishTime);

            this.context = context;
            this.buildFound = buildFound;
            this.successful = successful;
            this.buildName = buildName;
            this.finishTime = finishTime;
        }

        public ServerContext getContext() {
            return context;
        }

        public boolean isBuildFound() {
            return buildFound;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getBuildName() {
            return buildName;
        }

        public Date getFinishTime() {
            return finishTime;
        }
    }

    public BuildStatusLookupOperation(final String gitRemoteUrl, final String branch) {
        logger.info("BuildStatusLookupOperation created.");
        if (StringUtil.isNullOrEmpty(gitRemoteUrl)) throw new IllegalArgumentException("gitRemoteUrl");
        if (StringUtil.isNullOrEmpty(branch)) throw new IllegalArgumentException("branch");
        this.gitRemoteUrl = gitRemoteUrl;
        this.branch = branch;
    }

    @Override
    public void doWork(final Inputs inputs) {
        logger.info("BuildStatusLookupOperation.doWork()");
        onLookupStarted();

        // Create a default result to return if something goes wrong
        BuildStatusResults results = new BuildStatusResults(null, false, false, null, null);
        Build latestBuildForRepository = null;
        Build matchingBuild = null;

        // Lookup the context that goes with this remoteUrl
        // If no match exists simply return the default results
        final ServerContext context = ServerContextManager.getInstance().createContextFromRemoteUrl(gitRemoteUrl, false);
        if (context != null && context.getGitRepository() != null) {
            // Using the build REST client we will get the last 100 builds for this team project.
            // We will go through those builds and try to find one that matches our repo and branch.
            // If we can't find a perfect match, we will keep the first one that matches our repo.
            // TODO: The latest REST API allows you to filter the builds based on repo and branch, but the Java SDK
            // TODO: is not up to date with that version yet. We should change this code to use that method as soon
            // TODO: as we can.
            final BuildHttpClient buildClient = context.getBuildHttpClient();
            final List<Build> builds = buildClient.getBuilds(context.getTeamProjectReference().getId(), null, null, null, null, null, null, null, BuildStatus.COMPLETED, null, null, null, null, 100, null, null, null, BuildQueryOrder.FINISH_TIME_DESCENDING);
            if (builds.size() > 0) {
                for (final Build b : builds) {
                    // Get the repo and branch for the build and compare them to ours
                    final BuildRepository repo = b.getRepository();
                    if (repo != null && StringUtils.equalsIgnoreCase(context.getGitRepository().getId().toString(), repo.getId())) {
                        // save off this build since the repo matches
                        // TODO: Get the constant refs/heads/master from someplace common
                        if (latestBuildForRepository == null && StringUtils.equals(b.getSourceBranch(), "refs/heads/master")) {
                            logger.info("Latest build found for repo for the master branch.");
                            latestBuildForRepository = b;
                        }

                        // Branch names are case sensitive
                        if (StringUtils.equals(b.getSourceBranch(), branch)) {
                            // The repo and branch match the build exactly
                            logger.info("Matching build found for repo and branch.");
                            matchingBuild = b;
                            break;
                        }
                    }
                }
                // Create the results from the
                final Build buildToReturn = matchingBuild != null ? matchingBuild : latestBuildForRepository;
                if (buildToReturn != null) {
                    results = new BuildStatusResults(context, true, buildToReturn.getResult() == BuildResult.SUCCEEDED,
                            buildToReturn.getBuildNumber(), buildToReturn.getFinishTime());
                }
            } else {
                // No builds were found for this project
                results = new BuildStatusResults(context, false, false, null, null);
            }
        }

        logger.info("Returning results.");
        onLookupResults(results);
        onLookupCompleted();
    }

    @Override
    protected void terminate(final Throwable t) {
        logger.error(t.getMessage(), t);
        super.terminate(t);

        final BuildStatusResults results = new BuildStatusResults(null, false, false, null, null);
        results.error = t;
        onLookupResults(results);
        onLookupCompleted();
    }
}
