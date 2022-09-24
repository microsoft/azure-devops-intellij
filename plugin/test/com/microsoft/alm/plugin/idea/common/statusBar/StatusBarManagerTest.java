// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.statusBar;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.events.ServerEventManager;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.utils.EventContextHelper;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.operations.BuildStatusLookupOperation;
import com.microsoft.alm.plugin.operations.OperationFactory;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StatusBarManagerTest extends IdeaAbstractTest {

    @Mock
    public WindowManager windowManager;
    @Mock
    public StatusBar statusBar;
    @Mock
    public ProjectManager projectManager;
    @Mock
    public Project project;
    @Mock
    public GitRepository gitRepository;
    @Mock
    public ApplicationNamesInfo applicationNamesInfo;

    // Mocked via subclass below
    public MyBuildStatusLookupOperation buildStatusLookupOperation;

    @Mock
    private MockedStatic<WindowManager> windowManagerStatic;

    @Mock
    private MockedStatic<ProjectManager> projectManagerStatic;

    @Mock
    private MockedStatic<VcsHelper> vcsHelper;

    @Mock
    private MockedStatic<GitBranchUtil> gitBranchUtil;

    @Mock
    private MockedStatic<ApplicationNamesInfo> applicationNamesInfoStatic;

    @Mock
    private MockedStatic<OperationFactory> operationFactory;

    @Before
    public void setupLocalTests() {
        buildStatusLookupOperation = new MyBuildStatusLookupOperation();

        windowManagerStatic.when(WindowManager::getInstance).thenReturn(windowManager);
        when(windowManager.getStatusBar(any(Project.class))).thenReturn(statusBar);
        when(statusBar.getWidget(anyString()))
                .thenReturn(null) // First time return null
                .thenReturn(new BuildWidget()); // All other calls should return something other than null
        doNothing().when(statusBar).updateWidget(anyString());

        projectManagerStatic.when(ProjectManager::getInstance).thenReturn(projectManager);
        when(projectManager.getOpenProjects()).thenReturn(new Project[]{project});

        vcsHelper.when(() -> VcsHelper.getRepositoryContext(any(Project.class)))
                .thenReturn(
                        RepositoryContext.createGitContext(
                                "/root/one",
                                "repo1",
                                "branch1",
                                URI.create("http://repoUrl1")));

        gitBranchUtil.when(() -> GitBranchUtil.getCurrentRepository(any(Project.class))).thenReturn(gitRepository);
        gitBranchUtil.when(() -> GitBranchUtil.getDisplayableBranchText(any(GitRepository.class)))
                .thenReturn("branch");

        when(applicationNamesInfo.getProductName()).thenReturn("IDEA");
        applicationNamesInfoStatic.when(ApplicationNamesInfo::getInstance).thenReturn(applicationNamesInfo);

        operationFactory.when(
                () -> OperationFactory.createBuildStatusLookupOperation(any(RepositoryContext.class), anyBoolean()))
                .thenReturn(buildStatusLookupOperation);
    }

    @Test
    public void testSetupStatusBar() {
        StatusBarManager.setupStatusBar();
        verify(statusBar, VerificationModeFactory.times(0)).addWidget(any(BuildWidget.class), any(Project.class));
    }

    @Test
    public void testProjectOpenedEvent_NotRider() {
        StatusBarManager.setupStatusBar();
        Map<String, Object> map = EventContextHelper.createContext(EventContextHelper.SENDER_PROJECT_OPENED);
        EventContextHelper.setProject(map, project);
        ServerEventManager.getInstance().triggerAllEvents(map);
        verify(statusBar, VerificationModeFactory.times(1)).addWidget(any(BuildWidget.class), eq(project));
        buildStatusLookupOperation.onLookupStarted();
        buildStatusLookupOperation.onLookupResults(new BuildStatusLookupOperation.BuildStatusResults(
                new ServerContextBuilder().uri("https://test.visualstudio.com/").type(ServerContext.Type.VSO).build(),
                new ArrayList<BuildStatusLookupOperation.BuildStatusRecord>()));
        verify(statusBar, VerificationModeFactory.times(1)).updateWidget(anyString());
    }

    @Test
    public void testProjectOpenedEvent_RiderVsts() {
        when(applicationNamesInfo.getProductName()).thenReturn(IdeaHelper.RIDER_PRODUCT_NAME);
        vcsHelper.when(() -> VcsHelper.isVstsRepo(project)).thenReturn(true);

        StatusBarManager.setupStatusBar();
        Map<String, Object> map = EventContextHelper.createContext(EventContextHelper.SENDER_PROJECT_OPENED);
        EventContextHelper.setProject(map, project);
        ServerEventManager.getInstance().triggerAllEvents(map);
        verify(statusBar, VerificationModeFactory.times(1)).addWidget(any(BuildWidget.class), eq(project));
        buildStatusLookupOperation.onLookupStarted();
        buildStatusLookupOperation.onLookupResults(new BuildStatusLookupOperation.BuildStatusResults(
                new ServerContextBuilder().uri("https://test.visualstudio.com/").type(ServerContext.Type.VSO).build(),
                new ArrayList<BuildStatusLookupOperation.BuildStatusRecord>()));
        verify(statusBar, VerificationModeFactory.times(1)).updateWidget(anyString());
    }

    @Test
    public void testProjectOpenedEvent_RiderNotVsts() {
        when(applicationNamesInfo.getProductName()).thenReturn(IdeaHelper.RIDER_PRODUCT_NAME);
        vcsHelper.when(() -> VcsHelper.isVstsRepo(project)).thenReturn(false);
        when(statusBar.getWidget(anyString())).thenReturn(new BuildWidget());

        StatusBarManager.setupStatusBar();
        Map<String, Object> map = EventContextHelper.createContext(EventContextHelper.SENDER_PROJECT_OPENED);
        EventContextHelper.setProject(map, project);
        ServerEventManager.getInstance().triggerAllEvents(map);
        verify(statusBar, VerificationModeFactory.times(0)).addWidget(any(BuildWidget.class), eq(project));
        verify(statusBar, VerificationModeFactory.times(1)).removeWidget(any(String.class));
    }

    @Test
    public void testRepoChangedEvent() {
        StatusBarManager.setupStatusBar();
        Map<String, Object> map = EventContextHelper.createContext(EventContextHelper.SENDER_REPO_CHANGED);
        EventContextHelper.setProject(map, project);
        ServerEventManager.getInstance().triggerAllEvents(map);
        verify(statusBar, VerificationModeFactory.times(1)).addWidget(any(BuildWidget.class), eq(project));
        buildStatusLookupOperation.onLookupStarted();
        buildStatusLookupOperation.onLookupResults(new BuildStatusLookupOperation.BuildStatusResults(
                new ServerContextBuilder().uri("https://test.visualstudio.com/").type(ServerContext.Type.VSO).build(),
                new ArrayList<BuildStatusLookupOperation.BuildStatusRecord>()));
        verify(statusBar, VerificationModeFactory.times(1)).updateWidget(anyString());
    }

    @Test
    public void testProjectClosingEvent_noPreviousEvents() {
        StatusBarManager.setupStatusBar();
        Map<String, Object> map = EventContextHelper.createContext(EventContextHelper.SENDER_PROJECT_CLOSING);
        EventContextHelper.setProject(map, project);
        ServerEventManager.getInstance().triggerAllEvents(map);
        verify(statusBar, VerificationModeFactory.times(0)).addWidget(any(BuildWidget.class), eq(project));
        verify(statusBar, VerificationModeFactory.times(0)).removeWidget(anyString());
    }

    @Test
    public void testRepoChangedEvent_afterProjectOpened() {
        StatusBarManager.setupStatusBar();
        Map<String, Object> map = EventContextHelper.createContext(EventContextHelper.SENDER_PROJECT_OPENED);
        EventContextHelper.setProject(map, project);
        ServerEventManager.getInstance().triggerAllEvents(map);
        verify(statusBar, VerificationModeFactory.times(1)).addWidget(any(BuildWidget.class), eq(project));
        buildStatusLookupOperation.onLookupStarted();
        buildStatusLookupOperation.onLookupResults(new BuildStatusLookupOperation.BuildStatusResults(
                new ServerContextBuilder().uri("https://test.visualstudio.com/").type(ServerContext.Type.VSO).build(),
                new ArrayList<BuildStatusLookupOperation.BuildStatusRecord>()));
        verify(statusBar, VerificationModeFactory.times(1)).updateWidget(anyString());

        // Now close the project
        Map<String, Object> map2 = EventContextHelper.createContext(EventContextHelper.SENDER_PROJECT_CLOSING);
        EventContextHelper.setProject(map2, project);
        ServerEventManager.getInstance().triggerAllEvents(map2);
        verify(statusBar, VerificationModeFactory.times(1)).removeWidget(anyString());
    }

    @Test
    public void testUpdateStatusBar() {
        StatusBarManager.setupStatusBar();
        // An unknown sender should cause a call to UpdateStatusBar()
        Map<String, Object> map = EventContextHelper.createContext("TestSender");
        EventContextHelper.setProject(map, project);
        ServerEventManager.getInstance().triggerAllEvents(map);
        verify(statusBar, VerificationModeFactory.times(1)).addWidget(any(BuildWidget.class), eq(project));
    }

    private class MyBuildStatusLookupOperation extends BuildStatusLookupOperation {

        protected MyBuildStatusLookupOperation() {
            super(
                    RepositoryContext.createGitContext(
                            "/root/one",
                            "gitrepo",
                            "branch",
                            URI.create("http://gitRemoteUrl")),
                    false);
        }

        @Override
        public void doWorkAsync(Inputs inputs) {
            // Do nothing
        }

        @Override
        public void doWork(Inputs inputs) {
            // Do nothing
        }

        @Override
        public void onLookupStarted() {
            super.onLookupStarted();
        }

        @Override
        public void onLookupCompleted() {
            super.onLookupCompleted();
        }

        public void onLookupResults(BuildStatusLookupOperation.BuildStatusResults results) {
            super.onLookupResults(results);
        }
    }
}

