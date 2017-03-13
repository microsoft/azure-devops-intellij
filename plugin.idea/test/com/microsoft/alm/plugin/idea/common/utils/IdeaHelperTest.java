// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.utils;

import com.intellij.openapi.application.ApplicationNamesInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ApplicationNamesInfo.class})
public class IdeaHelperTest {
    public File mockExecutable = Mockito.mock(File.class);

    @Mock
    private ApplicationNamesInfo mockApplicationNamesInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(ApplicationNamesInfo.class);
        when(ApplicationNamesInfo.getInstance()).thenReturn(mockApplicationNamesInfo);
    }

    @Test
    public void testSetAppletPermission_PermissionsSet() throws Exception {
        Mockito.when(mockExecutable.exists()).thenReturn(true);
        Mockito.when(mockExecutable.canExecute()).thenReturn(true);
        IdeaHelper.setExecutablePermissions(mockExecutable);
        Mockito.verify(mockExecutable, Mockito.never()).setExecutable(Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void testSetAppletPermission_PermissionsNotSet() throws Exception {
        Mockito.when(mockExecutable.exists()).thenReturn(true);
        Mockito.when(mockExecutable.canExecute()).thenReturn(false);
        IdeaHelper.setExecutablePermissions(mockExecutable);
        Mockito.verify(mockExecutable, Mockito.times(1)).setExecutable(true, false);
    }

    @Test(expected = FileNotFoundException.class)
    public void testSetAppletPermission_FileNotFound() throws Exception {
        Mockito.when(mockExecutable.exists()).thenReturn(false);
        IdeaHelper.setExecutablePermissions(mockExecutable);
    }

    @Test
    public void testIsRider_True() {
        when(mockApplicationNamesInfo.getProductName()).thenReturn(IdeaHelper.RIDER_PRODUCT_NAME);
        assertTrue(IdeaHelper.isRider());
    }

    @Test
    public void testIsRider_False() {
        when(mockApplicationNamesInfo.getProductName()).thenReturn("IDEA");
        assertFalse(IdeaHelper.isRider());
    }
}