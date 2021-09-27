// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.Before;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.mockito.Mockito.when;

@PrepareForTest({ApplicationManager.class})
public class MockedIdeaApplicationTest extends IdeaAbstractTest {
    @Mock
    protected Application mockApplication;

    @Before
    public final void setUpMocks() {
        PowerMockito.mockStatic(ApplicationManager.class);
        when(ApplicationManager.getApplication()).thenReturn(mockApplication);
        when(mockApplication.isUnitTestMode()).thenReturn(true);
    }
}
