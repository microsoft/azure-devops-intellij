// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vcs.FileStatusFactory;
import com.intellij.openapi.vcs.FileStatusFactoryImpl;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.lenient;

public class MockedIdeaApplicationTest extends IdeaAbstractTest {
    @Mock
    protected Application mockApplication;

    @Mock
    private MockedStatic<ApplicationManager> applicationManagerStatic;

    @Before
    public final void setUpMocks() {
        //noinspection ResultOfMethodCallIgnored
        applicationManagerStatic.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        lenient().when(mockApplication.isUnitTestMode()).thenReturn(true);

        // This is required to get FileStatus items in certain tests, since they access an application-bound service in
        // enum item constructor.
        lenient().when(mockApplication.getService(FileStatusFactory.class)).thenReturn(new FileStatusFactoryImpl());
    }
}
