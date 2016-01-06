// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.simplecheckout;

import com.google.common.io.Files;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PluginServiceProvider.class, LocalFileSystem.class})
public class SimpleCheckoutModelTest extends IdeaAbstractTest {
    public static final String GIT_URL = "https://account.visualstudio.com/DefaultCollection/_git/TestProject";
    public final String INVALID_GIT_URL = "https://account.visualstudio.com/TestProject";

    @Mock
    public PluginServiceProvider pluginServiceProvider;
    @Mock
    public LocalFileSystem localFileSystem;
    @Mock
    public Project mockProject;
    @Mock
    public CheckoutProvider.Listener mockListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(PluginServiceProvider.class);
        PowerMockito.mockStatic(LocalFileSystem.class);
    }

    @Test
    public void testConstructor_Happy() {
        SimpleCheckoutModel model = modelCreationAndMocking(SimpleCheckoutModel.DEFAULT_SOURCE_PATH, GIT_URL);
        Assert.assertEquals(SimpleCheckoutModel.DEFAULT_SOURCE_PATH, model.getParentDirectory());
        Assert.assertEquals("TestProject", model.getDirectoryName());
    }

    @Test
    public void testConstructor_NoParentDirectory() {
        SimpleCheckoutModel model = modelCreationAndMocking(null, GIT_URL);
        Assert.assertEquals(SimpleCheckoutModel.DEFAULT_SOURCE_PATH, model.getParentDirectory());
    }

    @Test
    public void testConstructor_NoDirectoryName() {
        SimpleCheckoutModel model = modelCreationAndMocking(SimpleCheckoutModel.DEFAULT_SOURCE_PATH, INVALID_GIT_URL);
        Assert.assertEquals(StringUtils.EMPTY, model.getDirectoryName());
    }

    @Test
    public void testValidate_Happy() {
        SimpleCheckoutModel model = modelCreationAndMocking(SimpleCheckoutModel.DEFAULT_SOURCE_PATH, GIT_URL);
        ModelValidationInfo actualValidationInfo = model.validate();
        Assert.assertNull(actualValidationInfo);
    }

    @Test
    public void testValidate_ParentDirectoryEmpty() {
        SimpleCheckoutModel model = modelCreationAndMocking(SimpleCheckoutModel.DEFAULT_SOURCE_PATH, GIT_URL);
        model.setParentDirectory(StringUtils.EMPTY);
        ModelValidationInfo actualValidationInfo = model.validate();
        ModelValidationInfo expectedValidationInfo = ModelValidationInfo.createWithResource(SimpleCheckoutModel.PROP_PARENT_DIR,
                TfPluginBundle.KEY_CHECKOUT_DIALOG_ERRORS_PARENT_DIR_EMPTY);
        Assert.assertEquals(expectedValidationInfo.getValidationMessage(), actualValidationInfo.getValidationMessage());
        Assert.assertEquals(expectedValidationInfo.getValidationSource(), actualValidationInfo.getValidationSource());
    }

    @Test
    public void testValidate_ParentDirectoryNotFound() {
        SimpleCheckoutModel model = modelCreationAndMocking(SimpleCheckoutModel.DEFAULT_SOURCE_PATH, GIT_URL);
        model.setParentDirectory("/not/a/real/directory");
        ModelValidationInfo actualValidationInfo = model.validate();
        ModelValidationInfo expectedValidationInfo = ModelValidationInfo.createWithResource(SimpleCheckoutModel.PROP_PARENT_DIR,
                TfPluginBundle.KEY_CHECKOUT_DIALOG_ERRORS_PARENT_DIR_NOT_FOUND);
        Assert.assertEquals(expectedValidationInfo.getValidationMessage(), actualValidationInfo.getValidationMessage());
        Assert.assertEquals(expectedValidationInfo.getValidationSource(), actualValidationInfo.getValidationSource());
    }

    @Test
    public void testValidate_DirectoryNameEmpty() {
        SimpleCheckoutModel model = modelCreationAndMocking(SimpleCheckoutModel.DEFAULT_SOURCE_PATH, GIT_URL);
        model.setDirectoryName(StringUtils.EMPTY);
        ModelValidationInfo actualValidationInfo = model.validate();
        ModelValidationInfo expectedValidationInfo = ModelValidationInfo.createWithResource(SimpleCheckoutModel.PROP_DIRECTORY_NAME,
                TfPluginBundle.KEY_CHECKOUT_DIALOG_ERRORS_DIR_NAME_EMPTY);
        Assert.assertEquals(expectedValidationInfo.getValidationMessage(), actualValidationInfo.getValidationMessage());
        Assert.assertEquals(expectedValidationInfo.getValidationSource(), actualValidationInfo.getValidationSource());
    }

    @Test
    public void testValidate_DestinationExists() throws IOException {
        File tempDir = Files.createTempDir();
        SimpleCheckoutModel model = modelCreationAndMocking(SimpleCheckoutModel.DEFAULT_SOURCE_PATH, GIT_URL);
        model.setDirectoryName(tempDir.getName());
        model.setParentDirectory(tempDir.getParent());
        ModelValidationInfo actualValidationInfo = model.validate();
        ModelValidationInfo expectedValidationInfo = ModelValidationInfo.createWithResource(SimpleCheckoutModel.PROP_DIRECTORY_NAME,
                TfPluginBundle.KEY_CHECKOUT_DIALOG_ERRORS_DESTINATION_EXISTS, tempDir.getName());
        Assert.assertEquals(expectedValidationInfo.getValidationMessage(), actualValidationInfo.getValidationMessage());
        Assert.assertEquals(expectedValidationInfo.getValidationSource(), actualValidationInfo.getValidationSource());
    }

    public SimpleCheckoutModel modelCreationAndMocking(String repoRoot, String gitUrl) {
        PropertyService propertyService = mock(PropertyService.class);
        when(propertyService.getProperty(PropertyService.PROP_REPO_ROOT)).thenReturn(repoRoot);
        when(PluginServiceProvider.getInstance()).thenReturn(pluginServiceProvider);
        when(pluginServiceProvider.getPropertyService()).thenReturn(propertyService);

        return new SimpleCheckoutModel(mockProject, mockListener, gitUrl);
    }
}
