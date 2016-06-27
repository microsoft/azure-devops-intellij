// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import com.sun.jna.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Platform.class)
public class SystemHelperTest {
    private static final String WINDOWS_PATH = "C:\\test\\path\\dir";
    private static final String UNIX_PATH = "C:/test/path/dir";

    @Before
    public void setup() {
        PowerMockito.mockStatic(Platform.class);
    }

    @Test
    public void testGetUnixPath_Windows() {
        Mockito.when(Platform.isWindows()).thenReturn(true);
        assertEquals(UNIX_PATH, SystemHelper.getUnixPath(WINDOWS_PATH));
    }

    @Test
    public void testGetUnixPath_Unix() {
        Mockito.when(Platform.isWindows()).thenReturn(false);
        assertEquals(UNIX_PATH, SystemHelper.getUnixPath(UNIX_PATH));
    }
}
