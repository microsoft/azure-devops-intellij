// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import com.sun.jna.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetAddress;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Platform.class, SystemHelper.class})
public class SystemHelperTest {
    private static final String WINDOWS_PATH = "C:\\test\\path\\dir";
    private static final String UNIX_PATH = "C:/test/path/dir";

    @Before
    public void setup() {
        PowerMockito.mockStatic(Platform.class, InetAddress.class);
    }

    @Test
    public void testGetUnixPath_Windows() {
        when(Platform.isWindows()).thenReturn(true);
        assertEquals(UNIX_PATH, SystemHelper.getUnixPath(WINDOWS_PATH));
    }

    @Test
    public void testGetUnixPath_Unix() {
        when(Platform.isWindows()).thenReturn(false);
        assertEquals(UNIX_PATH, SystemHelper.getUnixPath(UNIX_PATH));
    }

    @Test
    public void testGetComputerNameShort() throws Exception {
        InetAddress mockInetAddress = mock(InetAddress.class);
        when(mockInetAddress.getHostName()).thenReturn("ws-100.domain.net");
        when(InetAddress.getLocalHost()).thenReturn(mockInetAddress);

        assertEquals("ws-100", SystemHelper.getComputerNameShort());
    }
}
