// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.connection;

import com.microsoft.alm.plugin.AbstractTest;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.ui.common.mocks.MockServerContext;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.apache.http.auth.NTCredentials;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ServerContextLookupOperationTest extends AbstractTest {

    private long MAX_TIMEOUT_NANO = 5L * 60L * 1000L * 1000L * 1000L; // five minutes

    private boolean debug = true;
    private boolean compareAsyncWithSync = false;

    @Rule
    public TestName testName = new TestName();

    // setup in #init
    private List<ServerContext> serverContextList;

    private class MyListener implements ServerContextLookupOperation.Listener {
        int startEvents = 0;
        int completeEvents = 0;
        int cancelEvents = 0;

        long startNotificationTime;
        long completeNotificationTime;

        class ResultEvent {
            long eventTime;
            ServerContext result;
        }

        List<ResultEvent> results = new ArrayList<ResultEvent>();

        final String name;

        public MyListener() {
            name = testName.getMethodName();
        }

        @Override
        public void notifyLookupStarted() {
            startNotificationTime = System.nanoTime();
            startEvents++;
            if (debug) {
                System.out.println("lookup Started (" + name + ")\n  for context count:" + serverContextList.size());
            }
        }

        @Override
        public void notifyLookupCompleted() {
            completeNotificationTime = System.nanoTime();
            completeEvents++;
            if (debug) {
                long totalTimeNano = completeNotificationTime - startNotificationTime;
                long milliTime = totalTimeNano / 1000000L;
                System.out.println("lookup Completed (" + name + ")\n  found:" + results.size() + "\n  time (milliseconds):" + milliTime);
            }
        }

        @Override
        public void notifyLookupCanceled() {
            cancelEvents++;
            if (debug) {
                System.out.println("lookup Canceled (" + name + ")");
            }
        }

        @Override
        public void notifyLookupResults(List<ServerContext> serverContexts) {
            long now = System.nanoTime();
            for (ServerContext serverContext : serverContexts) {
                ResultEvent resultEvent = new ResultEvent();
                resultEvent.eventTime = now;
                resultEvent.result = serverContext;
                results.add(resultEvent);
                Assert.assertNotNull(serverContext.getGitRepository());
                Assert.assertNotNull(serverContext.getGitRepository().getRemoteUrl());
                if (debug) {
                    System.out.println(serverContext.getGitRepository().getRemoteUrl());
                    System.out.println("lookedUp: " + serverContext);
                }
            }
        }
    }

    @Before
    public void init() throws Exception {
        serverContextList = new ArrayList<ServerContext>();

        Properties systemProperties = System.getProperties();

        for (int i = 0; ; i++) {
            String userId = systemProperties.getProperty("userId" + i);
            if (userId == null) {
                break;
            }
            String password = systemProperties.getProperty("password" + i);
            if (password == null) {
                break;
            }
            String url = systemProperties.getProperty("url" + i);
            if (url == null) {
                break;
            }
            URI uri = new URI(url);

            ServerContext serverContext;
            if (url.endsWith("visualstudio.com")) {
                final AuthenticationInfo authenticationInfo = new AuthenticationInfo(userId, password, uri.toString(), userId);
                serverContext = new MockServerContext(ServerContext.Type.VSO_DEPLOYMENT, authenticationInfo, uri, null, null, null);
            } else {
                final AuthenticationInfo authenticationInfo = AuthHelper.createAuthenticationInfo(url, new NTCredentials(userId + ":" + password));
                serverContext = new MockServerContext(ServerContext.Type.TFS, authenticationInfo, uri, null, null, null);
            }

            serverContextList.add(serverContext);
        }
        Assert.assertFalse(serverContextList.isEmpty());
    }

    //@Test
    public void testGitRepositoryLookupSync() {
        ServerContextLookupOperation gitRepositoryLookupOperation = new ServerContextLookupOperation(serverContextList, ServerContextLookupOperation.ContextScope.REPOSITORY);
        MyListener myListener = new MyListener();
        gitRepositoryLookupOperation.addListener(myListener);

        gitRepositoryLookupOperation.lookupContextsSync();
        Assert.assertTrue(gitRepositoryLookupOperation.isComplete());

        List<ServerContext> syncResults = gitRepositoryLookupOperation.getResults();

        verifyEventsMatchResults(myListener, syncResults);
    }

    //@Test
    public void testGitRepositoryLookupSyncX10() {
        expandTfsContextList(10);
        testGitRepositoryLookupSync();
    }

    private void verifyEventsMatchResults(MyListener myListener, List<ServerContext> serverContexts) {
        //one start
        Assert.assertEquals(1, myListener.startEvents);
        //one complete
        Assert.assertEquals(1, myListener.completeEvents);
        //start comes before complete
        Assert.assertTrue(myListener.startNotificationTime < myListener.completeNotificationTime);

        //no cancel
        Assert.assertEquals(0, myListener.cancelEvents);

        // TODO need to do some more work here, because this will vary depending on what accounts
        // for now, just ensure something was found
        Assert.assertNotEquals(0, myListener.results.size());

        // ensure we got one event for each result
        Assert.assertNotNull(serverContexts);
        Assert.assertEquals(myListener.results.size(), serverContexts.size());
        for (MyListener.ResultEvent resultEvent : myListener.results) {
            Assert.assertTrue(serverContexts.contains(resultEvent.result));
            Assert.assertTrue(resultEvent.eventTime > myListener.startNotificationTime);
            Assert.assertTrue(resultEvent.eventTime < myListener.completeNotificationTime);
        }
    }

    private void expandTfsContextList(final int multiple) {
        List<ServerContext> baseList = serverContextList;
        serverContextList = new ArrayList<ServerContext>(baseList.size() * multiple);
        for (int i = 0; i < multiple; i++) {
            for (ServerContext context : baseList) {
                serverContextList.add(context);
            }
        }
    }

    //@Test
    public void testGitRepositoryLookupASync() {
        testGitRepositoryLookupASync(compareAsyncWithSync);
    }

    public void testGitRepositoryLookupASync(final boolean compareWithSync) {
        ServerContextLookupOperation asyncGitRepositoryLookupOperation = new ServerContextLookupOperation(serverContextList, ServerContextLookupOperation.ContextScope.REPOSITORY);
        MyListener myListener = new MyListener();
        asyncGitRepositoryLookupOperation.addListener(myListener);

        asyncGitRepositoryLookupOperation.lookupContextsAsync();

        //TODO need some more robust multi threaded testing here
        //for now just make sure it doesn't finish right away but does finish later
        Assert.assertEquals(0, myListener.completeEvents);
        //and should not have any results
        Assert.assertTrue(asyncGitRepositoryLookupOperation.getResults().isEmpty());

        //wait for it to finish -- but time out if it is inordinately long
        long start = System.nanoTime();
        while (asyncGitRepositoryLookupOperation.isRunning()) {
            long now = System.nanoTime();
            if (start + MAX_TIMEOUT_NANO < now) {
                Assert.fail("timed out");
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        Assert.assertTrue(asyncGitRepositoryLookupOperation.isComplete());

        //ok, now that is done, verify
        List<ServerContext> asyncResults = asyncGitRepositoryLookupOperation.getResults();
        verifyEventsMatchResults(myListener, asyncResults);
        // also compare against sync
        if (compareWithSync) {
            // run this test first in sync fashion, then compare results to async
            ServerContextLookupOperation syncGitRepositoryLookupOperation = new ServerContextLookupOperation(serverContextList, ServerContextLookupOperation.ContextScope.REPOSITORY);
            syncGitRepositoryLookupOperation.lookupContextsSync();
            List<ServerContext> syncResults = syncGitRepositoryLookupOperation.getResults();
            verifyResultsMatch(syncResults, asyncResults);
        }
    }

    //@Test
    public void testGitRepositoryLookupASyncX10() {
        expandTfsContextList(10);
        testGitRepositoryLookupASync(false);
    }


    private void verifyResultsMatch(List<ServerContext> aResults, List<ServerContext> bResults) {
        Assert.assertNotNull(aResults);
        Assert.assertNotNull(bResults);
        Assert.assertEquals(aResults.size(), bResults.size());
//        for (ServerContextLookupOperation.ServerContextItemStore aResult : aResults) {
//            Assert.assertTrue(bResults.contains(aResult));
//        }
    }


}
