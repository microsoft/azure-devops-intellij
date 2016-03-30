// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.utils;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileNotFoundException;

public class IdeaHelperTest {
    public File mockExecutable = Mockito.mock(File.class);

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
}