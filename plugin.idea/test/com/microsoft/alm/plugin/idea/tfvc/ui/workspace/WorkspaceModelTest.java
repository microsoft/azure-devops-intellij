// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.workspace;

import com.intellij.notification.NotificationListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.ui.common.mocks.MockObserver;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.operations.OperationExecutor;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OperationExecutor.class, ServerContextManager.class, CommandUtils.class,
        VcsHelper.class, VcsNotifier.class})
public class WorkspaceModelTest extends IdeaAbstractTest {
    final String name = "name1";
    final String server = "http://server1";
    final String comment = "this is a comment";
    final String computer = "computer1";
    final List<Workspace.Mapping> mappings = Collections.singletonList(new Workspace.Mapping("$/", "/path/", false));
    final String owner = "owner1";
    OperationExecutor executor;
    ServerContextManager serverContextManager;
    Project mockProject;
    Project mockProject2;
    ServerContext authenticatedContext;
    RepositoryContext repositoryContext;
    RepositoryContext repositoryContext_noProject;
    VcsNotifier mockVcsNotifier;

    @Before
    public void mockObjects() {
        mockProject = mock(Project.class);
        executor = mock(OperationExecutor.class);
        PowerMockito.mockStatic(OperationExecutor.class);
        when(OperationExecutor.getInstance()).thenReturn(executor);

        authenticatedContext = mock(ServerContext.class);
        when(authenticatedContext.getTeamProjectReference()).thenReturn(new TeamProjectReference());

        serverContextManager = mock(ServerContextManager.class);
        when(serverContextManager.get(anyString())).thenReturn(authenticatedContext);
        when(serverContextManager.createContextFromTfvcServerUrl(anyString(), anyString(), anyBoolean()))
                .thenReturn(authenticatedContext);
        when(serverContextManager.createContextFromTfvcServerUrl(anyString(), Matchers.eq(""), anyBoolean()))
                .thenReturn(null);

        PowerMockito.mockStatic(ServerContextManager.class);
        when(ServerContextManager.getInstance()).thenReturn(serverContextManager);

        final Workspace workspace = new Workspace(server, name, computer, owner, comment, mappings, Workspace.Location.SERVER);
        PowerMockito.mockStatic(CommandUtils.class);
        when(CommandUtils.getWorkspace(Matchers.any(ServerContext.class), anyString())).thenReturn(workspace);
        when(CommandUtils.getWorkspace(Matchers.any(ServerContext.class), Matchers.any(Project.class))).thenReturn(workspace);
        when(CommandUtils.updateWorkspace(Matchers.any(ServerContext.class), Matchers.any(Workspace.class), Matchers.any(Workspace.class))).thenReturn("");
        when(CommandUtils.syncWorkspace(Matchers.any(ServerContext.class), anyString())).thenReturn(null);

        repositoryContext = RepositoryContext.createTfvcContext("/path", name, "project1", server);
        repositoryContext_noProject = RepositoryContext.createTfvcContext("/path", name, "", server);
        PowerMockito.mockStatic(VcsHelper.class);
        when(VcsHelper.getRepositoryContext(mockProject)).thenReturn(repositoryContext);
        when(VcsHelper.getRepositoryContext(mockProject2)).thenReturn(repositoryContext_noProject);

        mockVcsNotifier = mock(VcsNotifier.class);
        PowerMockito.mockStatic(VcsNotifier.class);
        when(VcsNotifier.getInstance(Matchers.any(Project.class))).thenReturn(mockVcsNotifier);
    }


    @Test
    public void testConstructor() {
        final WorkspaceModel m = new WorkspaceModel();
        Assert.assertEquals(null, m.getComment());
        Assert.assertEquals(null, m.getComputer());
        Assert.assertEquals(null, m.getName());
        Assert.assertEquals(null, m.getOwner());
        Assert.assertEquals(Collections.emptyList(), m.getMappings());
        Assert.assertEquals(null, m.getServer());
    }

    @Test
    public void testComment() {
        final WorkspaceModel m = new WorkspaceModel();
        final MockObserver observer = new MockObserver(m);
        Assert.assertEquals(null, m.getComment());

        // Set it to a value and make sure the event is fired and the value changes
        m.setComment(comment);
        Assert.assertEquals(comment, m.getComment());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_COMMENT);

        // Set it to the same value and make sure the event is NOT fired
        m.setComment(comment);
        Assert.assertEquals(comment, m.getComment());
        observer.assertAndClearLastUpdate(null, null);

        // Set it back to null and make sure the event is fired
        m.setComment(null);
        Assert.assertEquals(null, m.getComment());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_COMMENT);
    }

    @Test
    public void testComputer() {
        final WorkspaceModel m = new WorkspaceModel();
        final MockObserver observer = new MockObserver(m);
        Assert.assertEquals(null, m.getComputer());

        m.setComputer(computer);
        Assert.assertEquals(computer, m.getComputer());

        // Set it to a value and make sure the event is fired and the value changes
        m.setComputer(computer);
        Assert.assertEquals(computer, m.getComputer());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_COMPUTER);

        // Set it to the same value and make sure the event is NOT fired
        m.setComputer(computer);
        Assert.assertEquals(computer, m.getComputer());
        observer.assertAndClearLastUpdate(null, null);

        // Set it back to null and make sure the event is fired
        m.setComputer(null);
        Assert.assertEquals(null, m.getComputer());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_COMPUTER);
    }

    @Test
    public void testLoading() {
        final WorkspaceModel m = new WorkspaceModel();
        final MockObserver observer = new MockObserver(m);
        Assert.assertEquals(false, m.isLoading());

        // Set it to a value and make sure the event is fired and the value changes
        m.setLoading(true);
        Assert.assertEquals(true, m.isLoading());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_LOADING);

        // Set it to the same value and make sure the event is STILL fired
        // Loading flag is special, we fire the event every time the setter is called
        m.setLoading(true);
        Assert.assertEquals(true, m.isLoading());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_LOADING);

        // Set it back to null and make sure the event is fired
        m.setLoading(false);
        Assert.assertEquals(false, m.isLoading());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_LOADING);
    }

    @Test
    public void testName() {
        final WorkspaceModel m = new WorkspaceModel();
        final MockObserver observer = new MockObserver(m);
        Assert.assertEquals(null, m.getName());

        // Set it to a value and make sure the event is fired and the value changes
        m.setName(name);
        Assert.assertEquals(name, m.getName());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_NAME);

        // Set it to the same value and make sure the event is NOT fired
        m.setName(name);
        Assert.assertEquals(name, m.getName());
        observer.assertAndClearLastUpdate(null, null);

        // Set it back to null and make sure the event is fired
        m.setName(null);
        Assert.assertEquals(null, m.getName());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_NAME);
    }

    @Test
    public void testOwner() {
        final WorkspaceModel m = new WorkspaceModel();
        final MockObserver observer = new MockObserver(m);
        Assert.assertEquals(null, m.getOwner());

        // Set it to a value and make sure the event is fired and the value changes
        m.setOwner(owner);
        Assert.assertEquals(owner, m.getOwner());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_OWNER);

        // Set it to the same value and make sure the event is NOT fired
        m.setOwner(owner);
        Assert.assertEquals(owner, m.getOwner());
        observer.assertAndClearLastUpdate(null, null);

        // Set it back to null and make sure the event is fired
        m.setOwner(null);
        Assert.assertEquals(null, m.getOwner());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_OWNER);
    }

    @Test
    public void testServer() {
        final WorkspaceModel m = new WorkspaceModel();
        final MockObserver observer = new MockObserver(m);
        Assert.assertEquals(null, m.getServer());

        // Set it to a value and make sure the event is fired and the value changes
        m.setServer(server);
        Assert.assertEquals(server, m.getServer());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_SERVER);

        // Set it to the same value and make sure the event is NOT fired
        m.setServer(server);
        Assert.assertEquals(server, m.getServer());
        observer.assertAndClearLastUpdate(null, null);

        // Set it back to null and make sure the event is fired
        m.setServer(null);
        Assert.assertEquals(null, m.getServer());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_SERVER);
    }

    @Test
    public void testMappings() {
        final WorkspaceModel m = new WorkspaceModel();
        final MockObserver observer = new MockObserver(m);
        Assert.assertEquals(Collections.emptyList(), m.getMappings());

        // Set it to a value and make sure the event is fired and the value changes
        m.setMappings(mappings);
        Assert.assertEquals(mappings, m.getMappings());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_MAPPINGS);

        // Set it to the same value and make sure the event is NOT fired
        m.setMappings(mappings);
        Assert.assertEquals(mappings, m.getMappings());
        observer.assertAndClearLastUpdate(null, null);

        // Set it back to empty and make sure the event is fired
        m.setMappings(new ArrayList<Workspace.Mapping>());
        Assert.assertEquals(Collections.emptyList(), m.getMappings());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_MAPPINGS);
    }

    @Test
    public void testLocation() {
        final WorkspaceModel m = new WorkspaceModel();
        final MockObserver observer = new MockObserver(m);
        Assert.assertEquals(null, m.getLocation());

        // Set it to a value and make sure the event is fired and the value changes
        m.setLocation(Workspace.Location.LOCAL);
        Assert.assertEquals(Workspace.Location.LOCAL, m.getLocation());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_LOCATION);

        // Set it to the same value and make sure the event is NOT fired
        m.setLocation(Workspace.Location.LOCAL);
        Assert.assertEquals(Workspace.Location.LOCAL, m.getLocation());
        observer.assertAndClearLastUpdate(null, null);

        // Set it back to server and make sure the event is fired
        m.setLocation(Workspace.Location.SERVER);
        Assert.assertEquals(Workspace.Location.SERVER, m.getLocation());
        observer.assertAndClearLastUpdate(m, WorkspaceModel.PROP_LOCATION);
    }

    @Test
    public void testValidate() {
        final WorkspaceModel m = new WorkspaceModel();
        Assert.assertEquals(TfPluginBundle.KEY_WORKSPACE_DIALOG_ERRORS_NAME_EMPTY, m.validate().getValidationMessageKey());
        m.setName(name);
        Assert.assertEquals(TfPluginBundle.KEY_WORKSPACE_DIALOG_ERRORS_MAPPINGS_EMPTY, m.validate().getValidationMessageKey());
        m.setMappings(mappings);
        Assert.assertEquals(ModelValidationInfo.NO_ERRORS, m.validate());
    }

    @Test
    public void testLoadWorkspace_withProject() {
        final WorkspaceModel m = new WorkspaceModel();
        m.loadWorkspace(mockProject);

        // make sure the runnable task was submitted
        ArgumentCaptor<Runnable> taskArgument = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submitOperationTask(taskArgument.capture());

        // Make sure we starting loading
        Assert.assertEquals(true, m.isLoading());

        // Now force the runnable to run
        taskArgument.getValue().run();

        // Check the values in model now to make sure the match the workspace we created
        Assert.assertEquals(comment, m.getComment());
        Assert.assertEquals(computer, m.getComputer());
        Assert.assertEquals(name, m.getName());
        Assert.assertEquals(owner, m.getOwner());
        Assert.assertEquals(server, m.getServer());
        Assert.assertEquals(mappings, m.getMappings());
        Assert.assertEquals(Workspace.Location.SERVER, m.getLocation());

        // Make sure loading got turned off
        Assert.assertEquals(false, m.isLoading());
    }

    @Test(expected = RuntimeException.class)
    public void testLoadWorkspace_withInvalidProject() {
        final WorkspaceModel m = new WorkspaceModel();
        final Project localMockProject = mock(Project.class);
        m.loadWorkspace(localMockProject);

        // make sure the runnable task was submitted
        ArgumentCaptor<Runnable> taskArgument = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submitOperationTask(taskArgument.capture());

        // Make sure we starting loading
        Assert.assertEquals(true, m.isLoading());

        // Now force the runnable to run
        taskArgument.getValue().run();
    }

    @Test(expected = RuntimeException.class)
    public void testLoadWorkspace_withNullRepo() {
        final WorkspaceModel m = new WorkspaceModel();
        final Project localMockProject = mock(Project.class);
        when(VcsHelper.getRepositoryContext(localMockProject)).thenReturn(null);
        m.loadWorkspace(localMockProject);

        // make sure the runnable task was submitted
        ArgumentCaptor<Runnable> taskArgument = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submitOperationTask(taskArgument.capture());

        // Make sure we starting loading
        Assert.assertEquals(true, m.isLoading());

        // Now force the runnable to run
        taskArgument.getValue().run();
    }

    @Test(expected = RuntimeException.class)
    public void testLoadWorkspace_withNoRepoUrl() {
        final WorkspaceModel m = new WorkspaceModel();
        final Project localMockProject = mock(Project.class);
        final RepositoryContext mockRepositoryContext = mock(RepositoryContext.class);
        when(repositoryContext.getUrl()).thenReturn(StringUtils.EMPTY);
        when(repositoryContext.getTeamProjectName()).thenReturn("TeamProjectName");
        when(VcsHelper.getRepositoryContext(localMockProject)).thenReturn(mockRepositoryContext);
        m.loadWorkspace(localMockProject);

        // make sure the runnable task was submitted
        ArgumentCaptor<Runnable> taskArgument = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submitOperationTask(taskArgument.capture());

        // Make sure we starting loading
        Assert.assertEquals(true, m.isLoading());

        // Now force the runnable to run
        taskArgument.getValue().run();
    }

    @Test(expected = RuntimeException.class)
    public void testLoadWorkspace_withNoTeamProjectName() {
        final WorkspaceModel m = new WorkspaceModel();
        final Project localMockProject = mock(Project.class);
        final RepositoryContext mockRepositoryContext = mock(RepositoryContext.class);
        when(mockRepositoryContext.getUrl()).thenReturn("URL");
        when(mockRepositoryContext.getTeamProjectName()).thenReturn(StringUtils.EMPTY);
        when(VcsHelper.getRepositoryContext(localMockProject)).thenReturn(mockRepositoryContext);
        m.loadWorkspace(localMockProject);

        // make sure the runnable task was submitted
        ArgumentCaptor<Runnable> taskArgument = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submitOperationTask(taskArgument.capture());

        // Make sure we starting loading
        Assert.assertEquals(true, m.isLoading());

        // Now force the runnable to run
        taskArgument.getValue().run();
    }

    @Test(expected = NotAuthorizedException.class)
    public void testLoadWorkspace_NoRepoContextFound() {
        final WorkspaceModel m = new WorkspaceModel();
        // mockProject2 will return a repository context that does not have a project
        // this will in turn call a null server context to be returned
        when(serverContextManager.createContextFromTfvcServerUrl("bad url", "bad team name", true))
                .thenReturn(null);
        final Project localMockProject = mock(Project.class);
        final RepositoryContext mockRepositoryContext = mock(RepositoryContext.class);
        when(mockRepositoryContext.getUrl()).thenReturn("bad url");
        when(mockRepositoryContext.getTeamProjectName()).thenReturn("bad team name");
        when(VcsHelper.getRepositoryContext(localMockProject)).thenReturn(mockRepositoryContext);
        m.loadWorkspace(localMockProject);

        // make sure the runnable task was submitted
        ArgumentCaptor<Runnable> taskArgument = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submitOperationTask(taskArgument.capture());

        // Make sure we starting loading
        Assert.assertEquals(true, m.isLoading());

        // Now force the runnable to run
        taskArgument.getValue().run();
    }

    @Test
    public void testLoadWorkspace_noProject() {
        final WorkspaceModel m = new WorkspaceModel();
        m.loadWorkspace(repositoryContext, name);

        // make sure the runnable task was submitted
        ArgumentCaptor<Runnable> taskArgument = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submitOperationTask(taskArgument.capture());

        // Make sure we starting loading
        Assert.assertEquals(true, m.isLoading());

        // Now force the runnable to run
        taskArgument.getValue().run();

        // Check the values in model now to make sure the match the workspace we created
        Assert.assertEquals(comment, m.getComment());
        Assert.assertEquals(computer, m.getComputer());
        Assert.assertEquals(name, m.getName());
        Assert.assertEquals(owner, m.getOwner());
        Assert.assertEquals(server, m.getServer());
        Assert.assertEquals(mappings, m.getMappings());
        Assert.assertEquals(Workspace.Location.SERVER, m.getLocation());

        // Make sure loading got turned off
        Assert.assertEquals(false, m.isLoading());
    }

    @Test
    public void testLoadWorkspace_withWorkspace() {
        Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getComment()).thenReturn(comment);
        when(mockWorkspace.getComputer()).thenReturn(computer);
        when(mockWorkspace.getName()).thenReturn(name);
        when(mockWorkspace.getOwner()).thenReturn(owner);
        when(mockWorkspace.getServer()).thenReturn(server);
        when(mockWorkspace.getMappings()).thenReturn(mappings);
        when(mockWorkspace.getLocation()).thenReturn(Workspace.Location.SERVER);

        final WorkspaceModel m = new WorkspaceModel();
        m.loadWorkspace(authenticatedContext, mockWorkspace);

        // make sure the runnable task was submitted
        ArgumentCaptor<Runnable> taskArgument = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submitOperationTask(taskArgument.capture());

        // Make sure we starting loading
        Assert.assertEquals(true, m.isLoading());

        // Now force the runnable to run
        taskArgument.getValue().run();

        // Check the values in model now to make sure the match the workspace we created
        Assert.assertEquals(comment, m.getComment());
        Assert.assertEquals(computer, m.getComputer());
        Assert.assertEquals(name, m.getName());
        Assert.assertEquals(owner, m.getOwner());
        Assert.assertEquals(server, m.getServer());
        Assert.assertEquals(mappings, m.getMappings());
        Assert.assertEquals(Workspace.Location.SERVER, m.getLocation());

        // Make sure loading got turned off
        Assert.assertEquals(false, m.isLoading());
    }

    @Test
    public void testSaveWorkspaceInternal() {
        final WorkspaceModel m = new WorkspaceModel();
        final Workspace newWorkspace = new Workspace(server, name, computer, owner, comment, mappings);
        final Workspace oldWorkspace = new Workspace(server, name + "old", computer, owner, comment, mappings);
        m.saveWorkspaceInternal(authenticatedContext, oldWorkspace, newWorkspace, null, mockProject, "/path", true, null);
        verify(mockVcsNotifier).notifyImportantInfo(anyString(), anyString(), Matchers.any(NotificationListener.class));
    }
}
