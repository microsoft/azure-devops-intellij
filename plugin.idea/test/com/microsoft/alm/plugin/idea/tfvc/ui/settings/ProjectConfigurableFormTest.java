// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.settings;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PluginServiceProvider.class, TfTool.class})
@PowerMockIgnore("javax.swing.*")
public class ProjectConfigurableFormTest {
    ProjectConfigurableForm form;

    @Mock
    Project mockProject;

    @Mock
    PluginServiceProvider mockPluginServiceProvider;

    @Mock
    PropertyService mockPropertyService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(PluginServiceProvider.class, TfTool.class);
        when(mockPluginServiceProvider.getPropertyService()).thenReturn(mockPropertyService);
        when(PluginServiceProvider.getInstance()).thenReturn(mockPluginServiceProvider);
        form = new ProjectConfigurableForm(mockProject);
    }

    @Test
    public void testLoad_TfCmdSaved() {
        when(mockPropertyService.getProperty(PropertyService.PROP_TF_HOME)).thenReturn("/path/to/saved/cmd/tf");

        form.load();
        assertEquals("/path/to/saved/cmd/tf", form.getCurrentExecutablePath());
    }

    @Test
    public void testLoad_TfDetected() {
        when(mockPropertyService.getProperty(PropertyService.PROP_TF_HOME)).thenReturn(StringUtils.EMPTY);
        when(TfTool.tryDetectTf()).thenReturn("/path/to/detected/cmd/tf");

        form.load();
        assertEquals("/path/to/detected/cmd/tf", form.getCurrentExecutablePath());
    }

    @Test
    public void testLoad_TfNotFound() {
        when(mockPropertyService.getProperty(PropertyService.PROP_TF_HOME)).thenReturn(StringUtils.EMPTY);
        when(TfTool.tryDetectTf()).thenReturn(null);

        form.load();
        assertEquals(StringUtils.EMPTY, form.getCurrentExecutablePath());
    }
}
