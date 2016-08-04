// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.build.webapi.BuildHttpClient;
import com.microsoft.alm.build.webapi.model.Build;
import com.microsoft.alm.build.webapi.model.BuildQueryOrder;
import com.microsoft.alm.build.webapi.model.BuildRepository;
import com.microsoft.alm.build.webapi.model.BuildResult;
import com.microsoft.alm.build.webapi.model.BuildStatus;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class BuildStatusLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(BuildStatusLookupOperation.class);

    private final String gitRemoteUrl;
    private final String branch;
    private final boolean forcePrompt;

    public static class BuildStatusRecord {
        private final String repositoryId;
        private final String branch;
        private final boolean successful;
        private final int buildId;
        private final int definitionId;
        private final String buildName;
        private final Date finishTime;

        public BuildStatusRecord(final Build build) {
            this.repositoryId = build.getRepository().getId();
            this.branch = build.getSourceBranch();
            this.successful = build.getResult() == BuildResult.SUCCEEDED;
            this.buildId = build.getId();
            this.definitionId = build.getDefinition().getId();
            this.buildName = build.getBuildNumber();
            this.finishTime = build.getFinishTime();
        }

        public String getRepositoryId() {
            return repositoryId;
        }

        public String getBranch() {
            return branch;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public int getBuildId() {
            return buildId;
        }

        public String getBuildName() {
            return buildName;
        }

        public int getDefinitionId() {
            return definitionId;
        }

        public Date getFinishTime() {
            return finishTime;
        }

    }

    public static class BuildStatusResults extends ResultsImpl {
        private final ServerContext context;
        private final List<BuildStatusRecord> builds;

        public BuildStatusResults(final ServerContext context, final List<BuildStatusRecord> builds) {
            logger.info("Creating build status results.");
            logger.info("   builds = " + builds);

            this.context = context;
            this.builds = builds;
        }

        public ServerContext getContext() {
            return context;
        }

        public boolean isBuildFound() {
            return builds != null && builds.size() > 0;
        }

        public List<BuildStatusRecord> getBuilds() {
            if (isBuildFound()) {
                return Collections.unmodifiableList(builds);
            } else {
                return Collections.emptyList();
            }
        }

        // This method gets the build that should be displayed on the status bar.
        // If more than one build was found for the branch, this is the most specific one available.
        public BuildStatusRecord getBuildForDisplay() {
            // The last build in the list should be the one to return
            if (builds.size() > 0) {
                return builds.get(builds.size() - 1);
            }

            return null;
        }

        // This method gets the build that contains the repository status.
        // If more than one build was found for the branch, this is the least specific one available.
        public BuildStatusRecord getRepositoryStatus() {
            // The first build in the list should be the one to return
            if (builds.size() > 0) {
                return builds.get(0);
            }

            return null;
        }
    }

    /**
     * Use OperationFactory to create one of these operation classes.
     */
    protected BuildStatusLookupOperation(final String gitRemoteUrl, final String branch, final boolean forcePrompt) {
        logger.info("BuildStatusLookupOperation created.");
        ArgumentHelper.checkNotEmptyString(gitRemoteUrl);
        ArgumentHelper.checkNotEmptyString(branch);
        this.gitRemoteUrl = gitRemoteUrl;
        this.branch = branch;
        this.forcePrompt = forcePrompt;
    }

    @Override
    public void doWork(final Inputs inputs) {
        logger.info("BuildStatusLookupOperation.doWork()");
        onLookupStarted();

        try {
            // Create a default result to return if something goes wrong
            final List<BuildStatusRecord> buildStatusRecords = new ArrayList<BuildStatusRecord>(2);
            final BuildStatusResults results;
            Build latestBuildForRepository = null;
            Build matchingBuild = null;

            // Check to see if we should remove the context from the manager
            if (ServerContextManager.getInstance().get(gitRemoteUrl) != null && forcePrompt) {
                // The context already exists, but the user has requested to "Sign In", so we need to update the auth info
                ServerContextManager.getInstance().updateAuthenticationInfo(gitRemoteUrl);
            }

            // Lookup the context that goes with this remoteUrl
            // If no match exists simply return the default results
            final ServerContext context = ServerContextManager.getInstance().createContextFromGitRemoteUrl(gitRemoteUrl, forcePrompt);
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
                            // TODO: Get the constant refs/heads/master from someplace common or query for the default branch from the server
                            // Branch names are case sensitive
                            if (StringUtils.equals(b.getSourceBranch(), "refs/heads/master")) {
                                if (latestBuildForRepository == null) {
                                    // Found the master branch for the repo, so save that off
                                    logger.info("Latest build found for repo for the master branch.");
                                    latestBuildForRepository = b;
                                }
                            } else if (StringUtils.equals(b.getSourceBranch(), branch)) {
                                if (matchingBuild == null) {
                                    // The repo and branch match the build exactly, so save that off
                                    logger.info("Matching build found for repo and branch.");
                                    matchingBuild = b;
                                }
                            }

                            if (latestBuildForRepository != null && matchingBuild != null) {
                                // We found both builds
                                break;
                            }
                        }
                    }

                    // Create the results
                    if (latestBuildForRepository != null) {
                        // Add the repository build to the status records list first
                        buildStatusRecords.add(new BuildStatusRecord(latestBuildForRepository));
                    }
                    if (matchingBuild != null) {
                        // Add the matching build to the status records list last
                        buildStatusRecords.add(new BuildStatusRecord(matchingBuild));
                    }
                    results = new BuildStatusResults(context, buildStatusRecords);
                } else {
                    // No builds were found for this project
                    results = new BuildStatusResults(context, null);
                }
            } else {
                results = new BuildStatusResults(null, null);
            }

            logger.info("Returning results.");
            onLookupResults(results);
            onLookupCompleted();
        } catch (Throwable t) {
            logger.warn("doWork: failed with an exception", t);
            terminate(t);
        }
    }

    @Override
    protected void terminate(final Throwable t) {
        super.terminate(t);

        final BuildStatusResults results = new BuildStatusResults(null, null);
        results.error = t;
        onLookupResults(results);
        onLookupCompleted();
    }
}
