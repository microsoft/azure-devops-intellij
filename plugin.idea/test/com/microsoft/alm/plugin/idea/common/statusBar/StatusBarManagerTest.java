// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.statusBar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.events.ServerEventManager;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.utils.EventContextHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.operations.BuildStatusLookupOperation;
import com.microsoft.alm.plugin.operations.OperationFactory;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WindowManager.class, ProjectManager.class, GitBranchUtil.class, OperationFactory.class, VcsHelper.class})
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
    public GitBranchUtil gitBranchUtil;
    @Mock
    public GitRepository gitRepository;
    @Mock
    public OperationFactory operationFactory;

    // Mocked via subclass below
    public MyBuildStatusLookupOperation buildStatusLookupOperation;

    @Before
    public void setupLocalTests() {
        MockitoAnnotations.initMocks(this);
        buildStatusLookupOperation = new MyBuildStatusLookupOperation();

        PowerMockito.mockStatic(WindowManager.class);
        when(WindowManager.getInstance()).thenReturn(windowManager);
        when(windowManager.getStatusBar(any(Project.class))).thenReturn(statusBar);
        when(statusBar.getWidget(anyString()))
                .thenReturn(null) // First time return null
                .thenReturn(new BuildWidget()); // All other calls should return something other than null
        doNothing().when(statusBar).addWidget(any(StatusBarWidget.class));
        doNothing().when(statusBar).updateWidget(anyString());

        PowerMockito.mockStatic(ProjectManager.class);
        when(ProjectManager.getInstance()).thenReturn(projectManager);
        when(projectManager.getOpenProjects()).thenReturn(new Project[]{project});

        PowerMockito.mockStatic(VcsHelper.class);
        when(VcsHelper.getRepositoryContext(any(Project.class))).thenReturn(RepositoryContext.createGitContext("/root/one", "repo1", "branch1", "repoUrl1"));

        PowerMockito.mockStatic(GitBranchUtil.class);
        when(GitBranchUtil.getCurrentRepository(any(Project.class))).thenReturn(gitRepository);
        when(GitBranchUtil.getDisplayableBranchText(any(GitRepository.class))).thenReturn("branch");

        when(gitRepository.getRemotes()).thenReturn(Collections.singletonList(
                new GitRemote("origin", Collections.singletonList("https://test.visualstudio.com/"),
                        Collections.singletonList("https://test.visualstudio.com/"),
                        Collections.singletonList("https://test.visualstudio.com/"),
                        Collections.singletonList("https://test.visualstudio.com/"))));

        PowerMockito.mockStatic(OperationFactory.class);
        when(OperationFactory.createBuildStatusLookupOperation(any(RepositoryContext.class), anyBoolean())).thenReturn(buildStatusLookupOperation);
    }

    @Test
    public void testSetupStatusBar() {
        StatusBarManager.setupStatusBar();
        verify(statusBar, VerificationModeFactory.times(0)).addWidget(any(BuildWidget.class), any(Project.class));
    }

    @Test
    public void testProjectOpenedEvent() {
        StatusBarManager.setupStatusBar();
        Map<String, Object> map = EventContextHelper.createContext(EventContextHelper.SENDER_PROJECT_OPENED);
        EventContextHelper.setProject(map, project);
        ServerEventManager.getInstance().triggerAllEvents(map);
        verify(statusBar, VerificationModeFactory.times(1)).addWidget(any(BuildWidget.class), Matchers.eq(project));
        buildStatusLookupOperation.onLookupStarted();
        buildStatusLookupOperation.onLookupResults(new BuildStatusLookupOperation.BuildStatusResults(
                new ServerContextBuilder().uri("https://test.visualstudio.com/").type(ServerContext.Type.VSO).build(),
                new ArrayList<BuildStatusLookupOperation.BuildStatusRecord>()));
        verify(statusBar, VerificationModeFactory.times(1)).updateWidget(anyString());
    }

    @Test
    public void testRepoChangedEvent() {
        StatusBarManager.setupStatusBar();
        Map<String, Object> map = EventContextHelper.createContext(EventContextHelper.SENDER_REPO_CHANGED);
        EventContextHelper.setProject(map, project);
        ServerEventManager.getInstance().triggerAllEvents(map);
        verify(statusBar, VerificationModeFactory.times(1)).addWidget(any(BuildWidget.class), Matchers.eq(project));
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
        verify(statusBar, VerificationModeFactory.times(0)).addWidget(any(BuildWidget.class), Matchers.eq(project));
        verify(statusBar, VerificationModeFactory.times(0)).removeWidget(anyString());
    }

    @Test
    public void testRepoChangedEvent_afterProjectOpened() {
        StatusBarManager.setupStatusBar();
        Map<String, Object> map = EventContextHelper.createContext(EventContextHelper.SENDER_PROJECT_OPENED);
        EventContextHelper.setProject(map, project);
        ServerEventManager.getInstance().triggerAllEvents(map);
        verify(statusBar, VerificationModeFactory.times(1)).addWidget(any(BuildWidget.class), Matchers.eq(project));
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
        verify(statusBar, VerificationModeFactory.times(1)).addWidget(any(BuildWidget.class), Matchers.eq(project));
    }

    private class MyBuildStatusLookupOperation extends BuildStatusLookupOperation {

        protected MyBuildStatusLookupOperation() {
            super(RepositoryContext.createGitContext("/root/one", "gitrepo", "branch", "gitRemoteUrl"), false);
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

