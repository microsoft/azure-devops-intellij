// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.statusBar;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.build.webapi.model.Build;
import com.microsoft.alm.build.webapi.model.BuildDefinition;
import com.microsoft.alm.build.webapi.model.BuildRepository;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.operations.BuildStatusLookupOperation;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.JMenuItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildPopupTest extends IdeaAbstractTest {
    @Test
    public void testConstructor_noBuilds() {
        Project project = mock(Project.class);
        BuildStatusModel model = BuildStatusModel.create(project,
                new BuildStatusLookupOperation.BuildStatusResults(
                        new ServerContextBuilder().uri("https://test.visualstudio.com/").type(ServerContext.Type.VSO).build(),
                        new ArrayList<BuildStatusLookupOperation.BuildStatusRecord>()));
        BuildPopup popup = new BuildPopup(model);
        Assert.assertEquals(3, popup.getSubElements().length);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_VIEW_BUILDS_PAGE),
                ((JMenuItem) popup.getSubElements()[0]).getText());
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_REFRESH),
                ((JMenuItem) popup.getSubElements()[1]).getText());
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE),
                ((JMenuItem) popup.getSubElements()[2]).getText());
    }

    @Test
    public void testConstructor_oneBuild() {
        BuildDefinition definition = mock(BuildDefinition.class);
        Build build = mock(Build.class);
        when(build.getDefinition()).thenReturn(definition);
        BuildRepository repo = new BuildRepository();
        when(build.getRepository()).thenReturn(repo);
        Project project = mock(Project.class);
        BuildStatusModel model = BuildStatusModel.create(project,
                new BuildStatusLookupOperation.BuildStatusResults(
                        new ServerContextBuilder().uri("https://test.visualstudio.com/").type(ServerContext.Type.VSO).build(),
                        Collections.singletonList(new BuildStatusLookupOperation.BuildStatusRecord(build))));
        BuildPopup popup = new BuildPopup(model);
        Assert.assertEquals(4, popup.getSubElements().length);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_VIEW_DETAILS, "null"),
                ((JMenuItem) popup.getSubElements()[0]).getText());
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_QUEUE_BUILD),
                ((JMenuItem) popup.getSubElements()[1]).getText());
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_REFRESH),
                ((JMenuItem) popup.getSubElements()[2]).getText());
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE),
                ((JMenuItem) popup.getSubElements()[3]).getText());
    }

    @Test
    public void testConstructor_twoBuilds() {
        BuildDefinition definition = mock(BuildDefinition.class);
        Build build = mock(Build.class);
        when(build.getDefinition()).thenReturn(definition);
        BuildRepository repo = new BuildRepository();
        when(build.getRepository()).thenReturn(repo);
        Project project = mock(Project.class);

        List<BuildStatusLookupOperation.BuildStatusRecord> buildStatusRecords = new ArrayList<BuildStatusLookupOperation.BuildStatusRecord>();
        buildStatusRecords.add(new BuildStatusLookupOperation.BuildStatusRecord(build));
        buildStatusRecords.add(new BuildStatusLookupOperation.BuildStatusRecord(build));

        BuildStatusModel model = BuildStatusModel.create(project,
                new BuildStatusLookupOperation.BuildStatusResults(
                        new ServerContextBuilder().uri("https://test.visualstudio.com/").type(ServerContext.Type.VSO).build(),
                        buildStatusRecords));
        BuildPopup popup = new BuildPopup(model);
        Assert.assertEquals(5, popup.getSubElements().length);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_VIEW_DETAILS, "null"),
                ((JMenuItem) popup.getSubElements()[0]).getText());
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_VIEW_DETAILS, "null"),
                ((JMenuItem) popup.getSubElements()[1]).getText());
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_QUEUE_BUILD),
                ((JMenuItem) popup.getSubElements()[2]).getText());
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_REFRESH),
                ((JMenuItem) popup.getSubElements()[3]).getText());
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE),
                ((JMenuItem) popup.getSubElements()[4]).getText());
    }

    @Test
    public void testConstructor_notSignedIn() {
        Project project = mock(Project.class);

        BuildStatusModel model = BuildStatusModel.create(project,
                new BuildStatusLookupOperation.BuildStatusResults(
                        null,
                        new ArrayList<BuildStatusLookupOperation.BuildStatusRecord>()));
        BuildPopup popup = new BuildPopup(model);
        Assert.assertEquals(2, popup.getSubElements().length);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_STATUSBAR_BUILD_POPUP_SIGN_IN),
                ((JMenuItem) popup.getSubElements()[0]).getText());
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE),
                ((JMenuItem) popup.getSubElements()[1]).getText());
    }
}
