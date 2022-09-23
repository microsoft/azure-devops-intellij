// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VcsUtil.class, Presentation.class, Messages.class, BrowserUtil.class})
public class AnnotateActionTest extends IdeaAbstractTest {
    private AnnotateAction annotateAction;
    private final URI serverURI = URI.create("http://organization.visualstudio.com/");

    @Mock
    private AnActionEvent mockAnActionEvent;
    @Mock
    private Presentation mockPresentation;
    @Mock
    private VirtualFile mockVirtualFile;
    @Mock
    private Project mockProject;
    @Mock
    private SingleItemAction.SingleItemActionContext mockActionContext;
    @Mock
    private ServerContext mockServerContext;
    @Mock
    private TeamProjectReference mockTeamProjectReference;
    @Mock
    private ItemInfo mockItemInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(VcsUtil.class, Messages.class, BrowserUtil.class);

        when(mockAnActionEvent.getPresentation()).thenReturn(mockPresentation);
        when(VcsUtil.getOneVirtualFile(mockAnActionEvent)).thenReturn(mockVirtualFile);
        when(mockActionContext.getProject()).thenReturn(mockProject);
        when(mockServerContext.getUri()).thenReturn(serverURI);
        annotateAction = new AnnotateAction();
    }

    @Test
    public void testDoUpdate_NullFile() {
        when(VcsUtil.getOneVirtualFile(mockAnActionEvent)).thenReturn(null);
        annotateAction.update(mockAnActionEvent);

        verify(mockPresentation, times(1)).setEnabled(false);
    }

    @Test
    public void testDoUpdate_Directory() {
        when(mockVirtualFile.isDirectory()).thenReturn(false);
        annotateAction.update(mockAnActionEvent);

        verify(mockPresentation, times(1)).setEnabled(false);
    }

    @Test
    public void testExecute_NullContext() {
        when(mockActionContext.getServerContext()).thenReturn(null);
        annotateAction.execute(mockActionContext);

        verifyStatic(Messages.class, times(1));
        Messages.showErrorDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_ANNOTATE_ERROR_MSG),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_ANNOTATE_ERROR_TITLE));
        verifyStatic(BrowserUtil.class, times(0));
        BrowserUtil.browse(any(URI.class));
    }

    @Test
    public void testExecute_NullTeamProject() {
        when(mockServerContext.getTeamProjectReference()).thenReturn(null);
        when(mockActionContext.getServerContext()).thenReturn(mockServerContext);
        annotateAction.execute(mockActionContext);

        verifyStatic(Messages.class, times(1));
        Messages.showErrorDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_ANNOTATE_ERROR_MSG),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_ANNOTATE_ERROR_TITLE));
        verifyStatic(BrowserUtil.class, times(0));
        BrowserUtil.browse(any(URI.class));
    }

    @Test
    public void testExecute_NullItem() {
        when(mockServerContext.getTeamProjectReference()).thenReturn(mockTeamProjectReference);
        when(mockActionContext.getServerContext()).thenReturn(mockServerContext);
        when(mockActionContext.getItem()).thenReturn(null);
        annotateAction.execute(mockActionContext);

        verifyStatic(Messages.class, times(1));
        Messages.showErrorDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_ANNOTATE_ERROR_MSG),
                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_ANNOTATE_ERROR_TITLE));
        verifyStatic(BrowserUtil.class, times(0));
        BrowserUtil.browse(any(URI.class));
    }

    @Test
    public void testExecute_Success() {
        ArgumentCaptor<URI> argCapture = ArgumentCaptor.forClass(URI.class);
        when(mockTeamProjectReference.getName()).thenReturn("TeamName");
        when(mockItemInfo.getServerItem()).thenReturn("$/path/to/file.txt");
        when(mockServerContext.getTeamProjectReference()).thenReturn(mockTeamProjectReference);
        when(mockActionContext.getServerContext()).thenReturn(mockServerContext);
        when(mockActionContext.getItem()).thenReturn(mockItemInfo);
        annotateAction.execute(mockActionContext);

        verifyStatic(Messages.class, times(0));
        Messages.showErrorDialog(any(Project.class), anyString(), anyString());
        verifyStatic(BrowserUtil.class, times(1));
        BrowserUtil.browse(argCapture.capture());
        assertEquals(serverURI.toString() +
                        "TeamName/_versionControl/?path=%24%2Fpath%2Fto%2Ffile.txt&_a=contents&annotate=true&hideComments=true",
                argCapture.getValue().toString());
    }
}
