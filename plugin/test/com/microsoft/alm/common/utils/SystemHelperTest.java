// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import com.sun.jna.Platform;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(MockitoJUnitRunner.class)
public class SystemHelperTest {
    private static final String WINDOWS_PATH = "C:\\test\\path\\dir";
    private static final String UNIX_PATH = "C:/test/path/dir";

    @Mock
    private MockedStatic<Platform> platform;

    @Test
    public void testGetUnixPath_Windows() {
        platform.when(Platform::isWindows).thenReturn(true);
        assertEquals(UNIX_PATH, SystemHelper.getUnixPath(WINDOWS_PATH));
    }

    @Test
    public void testGetUnixPath_Unix() {
        platform.when(Platform::isWindows).thenReturn(false);
        assertEquals(UNIX_PATH, SystemHelper.getUnixPath(UNIX_PATH));
    }
}
