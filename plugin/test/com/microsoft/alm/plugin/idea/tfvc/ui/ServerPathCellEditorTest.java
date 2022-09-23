// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Messages.class})
public class ServerPathCellEditorTest {

    @Mock
    private Project mockProject;
    @Mock
    private ServerContext mockContext;
    @Mock
    private TeamProjectReference mockTeamProjectReference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Messages.class);
    }

    @Test
    public void testGetServerPath_PathPresent() {
        ServerPathCellEditor editor = new ServerPathCellEditor("title", mockProject, mockContext);
        ServerPathCellEditor spy = spy(editor);
        doReturn("$/root/dir/file").when(spy).getCellEditorValue();
        assertEquals("$/root/dir/file", spy.getServerPath());
    }

    @Test
    public void testCreateBrowserDialog_NullContext() {
        ServerPathCellEditor editor = new ServerPathCellEditor("title", mockProject, null);
        ServerPathCellEditor spy = spy(editor);
        doReturn(StringUtils.EMPTY).when(spy).getCellEditorValue();
        assertEquals(StringUtils.EMPTY, spy.getServerPath());
    }

    @Test
    public void testCreateBrowserDialog_NullTeamProjectReference() {
        when(mockContext.getTeamProjectReference()).thenReturn(null);
        ServerPathCellEditor editor = new ServerPathCellEditor("title", mockProject, mockContext);
        ServerPathCellEditor spy = spy(editor);
        doReturn(StringUtils.EMPTY).when(spy).getCellEditorValue();
        assertEquals(StringUtils.EMPTY, spy.getServerPath());
    }

    @Test
    public void testCreateBrowserDialog_PathFromContext() {
        when(mockTeamProjectReference.getName()).thenReturn("projectRoot");
        when(mockContext.getTeamProjectReference()).thenReturn(mockTeamProjectReference);
        ServerPathCellEditor editor = new ServerPathCellEditor("title", mockProject, mockContext);
        ServerPathCellEditor spy = spy(editor);
        doReturn(StringUtils.EMPTY).when(spy).getCellEditorValue();
        assertEquals("$/projectRoot", spy.getServerPath());
    }

    @Test
    public void testCreateBrowserDialog_NoPath() {
        ServerPathCellEditor editor = new ServerPathCellEditor("title", mockProject, mockContext);
        ServerPathCellEditor spy = spy(editor);
        doReturn(StringUtils.EMPTY).when(spy).getServerPath();

        spy.createBrowserDialog();
        verifyStatic(Messages.class, times(1));
        Messages.showErrorDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_SERVER_TREE_NO_ROOT_MSG)),
                eq(TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_TFVC_SERVER_TREE_NO_ROOT_TITLE)));
    }
}
