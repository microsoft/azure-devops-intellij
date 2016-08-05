// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.alm.build.webapi.BuildHttpClient;
import com.microsoft.alm.build.webapi.model.Build;
import com.microsoft.alm.build.webapi.model.BuildQueryOrder;
import com.microsoft.alm.build.webapi.model.BuildReason;
import com.microsoft.alm.build.webapi.model.BuildRepository;
import com.microsoft.alm.build.webapi.model.BuildResult;
import com.microsoft.alm.build.webapi.model.BuildStatus;
import com.microsoft.alm.build.webapi.model.DefinitionReference;
import com.microsoft.alm.build.webapi.model.DefinitionType;
import com.microsoft.alm.build.webapi.model.QueryDeletedOption;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServerContextManager.class})
public class BuildStatusLookupOperationTest extends AbstractTest {
    private ServerContextManager serverContextManager;

    private void setupLocalTests(GitRepository gitRepository, List<Build> builds) {
        MockitoAnnotations.initMocks(this);

        BuildHttpClient buildHttpClient = Mockito.mock(BuildHttpClient.class);
        when(buildHttpClient.getBuilds(any(UUID.class), any(List.class), any(List.class),
                anyString(), any(Date.class), any(Date.class), anyString(), any(BuildReason.class),
                eq(BuildStatus.COMPLETED), any(BuildResult.class), any(List.class), any(List.class),
                any(DefinitionType.class), eq(100), anyString(), anyInt(), any(QueryDeletedOption.class),
                eq(BuildQueryOrder.FINISH_TIME_DESCENDING))).thenReturn(builds);

        AuthenticationInfo authInfo = new AuthenticationInfo("user", "pass", "serverURI", "user");
        ServerContext authenticatedContext = Mockito.mock(ServerContext.class);
        when(authenticatedContext.getBuildHttpClient()).thenReturn(buildHttpClient);
        when(authenticatedContext.getTeamProjectReference()).thenReturn(new TeamProjectReference());
        when(authenticatedContext.getGitRepository()).thenReturn(gitRepository);

        serverContextManager = Mockito.mock(ServerContextManager.class);
        when(serverContextManager.get(anyString())).thenReturn(authenticatedContext);
        when(serverContextManager.createContextFromGitRemoteUrl(anyString(), anyBoolean())).thenReturn(authenticatedContext);

        PowerMockito.mockStatic(ServerContextManager.class);
        when(ServerContextManager.getInstance()).thenReturn(serverContextManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullRepo() {
        BuildStatusLookupOperation operation = new BuildStatusLookupOperation(null, false);
    }

    @Test
    public void testConstructor_goodInputs() {
        BuildStatusLookupOperation operation1 = new BuildStatusLookupOperation(
                RepositoryContext.createGitContext("repoName", "branch", "url"), false);
    }

    @Test
    public void testDoWork_noMatch() throws InterruptedException, ExecutionException, TimeoutException {
        TestData data = new TestData();
        data.addBuild(4, data.currentRepo, data.otherBranch);   // branch doesn't match
        data.addBuild(3, data.otherRepo, data.currentBranch);   // repo doesn't match
        data.addBuild(1, data.otherRepo, data.otherBranch);     // nothing matches

        setupLocalTests(data.currentRepo, data.builds);

        BuildStatusLookupOperation operation = new BuildStatusLookupOperation(
                data.getRepositoryContext(), false);
        data.setupListener(operation);

        operation.doWork(null);
        Assert.assertTrue(data.startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(data.completedCalled.get(1, TimeUnit.SECONDS));
        BuildStatusLookupOperation.BuildStatusResults results = data.buildResults.get(1, TimeUnit.SECONDS);
        Assert.assertNull(results.getError());
        Assert.assertEquals(0, results.getBuilds().size());
    }

    @Test
    public void testDoWork_1Match() throws InterruptedException, ExecutionException, TimeoutException {
        TestData data = new TestData();
        data.addBuild(4, data.currentRepo, data.otherBranch);   // branch doesn't match
        data.addBuild(3, data.otherRepo, data.currentBranch);   // repo doesn't match
        data.addBuild(2, data.currentRepo, data.currentBranch); // MATCH
        data.addBuild(1, data.otherRepo, data.otherBranch);     // nothing matches

        setupLocalTests(data.currentRepo, data.builds);

        BuildStatusLookupOperation operation = new BuildStatusLookupOperation(
                data.getRepositoryContext(), false);
        data.setupListener(operation);

        operation.doWork(null);
        Assert.assertTrue(data.startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(data.completedCalled.get(1, TimeUnit.SECONDS));
        BuildStatusLookupOperation.BuildStatusResults results = data.buildResults.get(1, TimeUnit.SECONDS);
        Assert.assertNull(results.getError());
        Assert.assertEquals(1, results.getBuilds().size());
        // Verify the right build was returned
        Assert.assertEquals(2, results.getBuilds().get(0).getBuildId());
        Assert.assertEquals(data.currentRepo.getId().toString(), results.getBuilds().get(0).getRepositoryId());
        Assert.assertEquals(data.currentBranch, results.getBuilds().get(0).getBranch());
    }

    @Test
    public void testDoWork_2Matches() throws InterruptedException, ExecutionException, TimeoutException {
        TestData data = new TestData();
        data.addBuild(8, data.currentRepo, data.otherBranch);   // branch doesn't match
        data.addBuild(7, data.otherRepo, data.currentBranch);   // repo doesn't match
        data.addBuild(6, data.currentRepo, data.currentBranch); // MATCH on exact branch
        data.addBuild(5, data.currentRepo, TestData.MASTER);    // MATCH on Repo and master
        data.addBuild(4, data.otherRepo, data.otherBranch);     // nothing matches

        setupLocalTests(data.currentRepo, data.builds);

        BuildStatusLookupOperation operation = new BuildStatusLookupOperation(
                data.getRepositoryContext(), false);
        data.setupListener(operation);

        operation.doWork(null);
        Assert.assertTrue(data.startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(data.completedCalled.get(1, TimeUnit.SECONDS));
        BuildStatusLookupOperation.BuildStatusResults results = data.buildResults.get(1, TimeUnit.SECONDS);
        Assert.assertNull(results.getError());
        Assert.assertEquals(2, results.getBuilds().size());
        // Verify that the first build in the list is for master
        Assert.assertEquals(5, results.getBuilds().get(0).getBuildId());
        Assert.assertEquals(data.currentRepo.getId().toString(), results.getBuilds().get(0).getRepositoryId());
        Assert.assertEquals(TestData.MASTER, results.getBuilds().get(0).getBranch());
        // Verify that the last build in the list is for branch1
        Assert.assertEquals(6, results.getBuilds().get(1).getBuildId());
        Assert.assertEquals(data.currentRepo.getId().toString(), results.getBuilds().get(1).getRepositoryId());
        Assert.assertEquals(data.currentBranch, results.getBuilds().get(1).getBranch());
    }

    @Test
    public void testDoWork_matchOnMasterOnly() throws InterruptedException, ExecutionException, TimeoutException {
        TestData data = new TestData();
        data.addBuild(8, data.currentRepo, data.otherBranch);  // branch doesn't match
        data.addBuild(7, data.otherRepo, data.currentBranch);  // repo doesn't match
        data.addBuild(5, data.currentRepo, TestData.MASTER);   // MATCH on Repo and master
        data.addBuild(4, data.otherRepo, data.otherBranch);    // nothing matches

        setupLocalTests(data.currentRepo, data.builds);

        BuildStatusLookupOperation operation = new BuildStatusLookupOperation(
                data.getRepositoryContext(), false);
        data.setupListener(operation);

        operation.doWork(null);
        Assert.assertTrue(data.startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(data.completedCalled.get(1, TimeUnit.SECONDS));
        BuildStatusLookupOperation.BuildStatusResults results = data.buildResults.get(1, TimeUnit.SECONDS);
        Assert.assertNull(results.getError());
        Assert.assertEquals(1, results.getBuilds().size());
        // Verify that the first build in the list is for master
        Assert.assertEquals(5, results.getBuilds().get(0).getBuildId());
        Assert.assertEquals(data.currentRepo.getId().toString(), results.getBuilds().get(0).getRepositoryId());
        Assert.assertEquals(TestData.MASTER, results.getBuilds().get(0).getBranch());
    }

    @Test
    public void testDoWork_moreMatches() throws InterruptedException, ExecutionException, TimeoutException {
        TestData data = new TestData();
        data.addBuild(8, data.currentRepo, data.otherBranch);  // branch doesn't match
        data.addBuild(7, data.otherRepo, data.currentBranch);  // repo doesn't match
        data.addBuild(6, data.currentRepo, TestData.MASTER); // MATCH on Repo and master
        data.addBuild(5, data.currentRepo, data.currentBranch); // MATCH on exact branch
        data.addBuild(4, data.otherRepo, data.otherBranch);    // nothing matches
        data.addBuild(3, data.currentRepo, TestData.MASTER); // OLD MATCH on Repo and master
        data.addBuild(2, data.currentRepo, data.currentBranch); // OLD MATCH on exact branch
        data.addBuild(1, data.currentRepo, data.currentBranch); // OLD MATCH on exact branch

        setupLocalTests(data.currentRepo, data.builds);

        BuildStatusLookupOperation operation = new BuildStatusLookupOperation(
                data.getRepositoryContext(), false);
        data.setupListener(operation);
        operation.doWork(null);
        Assert.assertTrue(data.startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(data.completedCalled.get(1, TimeUnit.SECONDS));
        BuildStatusLookupOperation.BuildStatusResults results = data.buildResults.get(1, TimeUnit.SECONDS);
        Assert.assertNull(results.getError());
        Assert.assertEquals(2, results.getBuilds().size());
        // Verify that the first build in the list is for master
        Assert.assertEquals(6, results.getBuilds().get(0).getBuildId());
        Assert.assertEquals(data.currentRepo.getId().toString(), results.getBuilds().get(0).getRepositoryId());
        Assert.assertEquals(TestData.MASTER, results.getBuilds().get(0).getBranch());
        // Verify that the last build in the list is for branch1
        Assert.assertEquals(5, results.getBuilds().get(1).getBuildId());
        Assert.assertEquals(data.currentRepo.getId().toString(), results.getBuilds().get(1).getRepositoryId());
        Assert.assertEquals(data.currentBranch, results.getBuilds().get(1).getBranch());
    }

    @Test
    public void testDoWork_failure() throws InterruptedException, ExecutionException, TimeoutException {
        TestData data = new TestData();
        data.addBuild(2, data.currentRepo, data.currentBranch);
        setupLocalTests(data.currentRepo, null);

        BuildStatusLookupOperation operation = new BuildStatusLookupOperation(
                data.getRepositoryContext(), false);
        data.setupListener(operation);
        operation.doWork(null); // This should cause a null point exception during the operation
        Assert.assertTrue(data.startedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(data.completedCalled.get(1, TimeUnit.SECONDS));
        Assert.assertEquals(NullPointerException.class, data.buildResults.get(1, TimeUnit.SECONDS).getError().getClass());
    }

    private class TestData {
        public static final String MASTER = "refs/heads/master";

        public final GitRepository currentRepo;
        public final String currentBranch;
        public final GitRepository otherRepo;
        public final String otherBranch;
        public final List<Build> builds;
        public final SettableFuture<Boolean> startedCalled;
        public final SettableFuture<Boolean> completedCalled;
        public final SettableFuture<BuildStatusLookupOperation.BuildStatusResults> buildResults;

        public TestData() {
            this.currentRepo = getRepo("repo1");
            this.currentBranch = "branch1";
            this.otherRepo = getRepo("repo2");
            this.otherBranch = "branch2";
            this.builds = new ArrayList<Build>();
            this.startedCalled = SettableFuture.create();
            this.completedCalled = SettableFuture.create();
            this.buildResults = SettableFuture.create();
        }

        public void addBuild(int buildId, GitRepository repo, String branch) {
            builds.add(getBuild(buildId, repo, branch));
        }

        public void setupListener(BuildStatusLookupOperation operation) {
            operation.addListener(new Operation.Listener() {
                public void notifyLookupStarted() {
                    startedCalled.set(true);
                }

                public void notifyLookupCompleted() {
                    completedCalled.set(true);
                }

                @Override
                public void notifyLookupResults(Operation.Results lookupResults) {
                    buildResults.set((BuildStatusLookupOperation.BuildStatusResults) lookupResults);
                }
            });
        }

        public RepositoryContext getRepositoryContext() {
            return RepositoryContext.createGitContext(currentRepo.getName(), currentBranch, currentRepo.getRemoteUrl());
        }
    }

    private GitRepository getRepo(String name) {
        GitRepository gitRepository = new GitRepository();
        gitRepository.setId(UUID.randomUUID());
        gitRepository.setName(name);
        gitRepository.setRemoteUrl("http://server:8080/tfs/project/_git/" + name);
        return gitRepository;
    }

    private Build getBuild(int id, GitRepository gitRepository, String branch) {
        BuildRepository repo = new BuildRepository();
        repo.setId(gitRepository.getId().toString());
        repo.setName(gitRepository.getName());
        repo.setUrl(URI.create(gitRepository.getRemoteUrl()));

        Build build = new Build();
        build.setId(id);
        build.setBuildNumber("number" + id);
        build.setDefinition(new DefinitionReference());
        build.getDefinition().setId(1);
        build.getDefinition().setName("definition1");
        build.setResult(BuildResult.SUCCEEDED);
        build.setRepository(repo);
        build.setSourceBranch(branch);
        return build;
    }
}
