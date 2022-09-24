// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.starters;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.settings.TeamServicesSettingsService;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationStarterBaseTest extends IdeaAbstractTest {
    public final String URI_ARG = "vsoi://checkout/?url=https://laa018-test.visualstudio.com/DefaultCollection/_git/TestProject&EncFormat=UTF8";
    public final String URI_MINUS_PREFIX = "checkout/?url=https://laa018-test.visualstudio.com/DefaultCollection/_git/TestProject&EncFormat=UTF8";
    public final String VALID_GIT_URL = "https://organization.visualstudio.com/DefaultCollection/_git/TestProject";
    public final String SUB_COMMAND = "checkout";

    public List<String> processCommandArgs = Collections.emptyList();
    public String processUriArgs = StringUtils.EMPTY;

    @Mock
    public FileDocumentManager mockFileDocumentManager;

    @Mock
    public Application mockApplication;

    public TeamServicesSettingsService teamServicesSettingsService = new TeamServicesSettingsService();

    public ApplicationStarterBase starterBase = new ApplicationStarterBase() {
        @Override
        public String getUsageMessage() {
            return null;
        }

        @Override
        protected void processCommand(final List<String> args) throws RuntimeException {
            processCommandArgs = args;
        }

        @Override
        protected void processUri(final String uri) throws RuntimeException  {
            processUriArgs = uri;
        }
    };

    @Mock
    private MockedStatic<FileDocumentManager> fileDocumentManager;

    @Mock
    private MockedStatic<ApplicationManager> applicationManager;

    @Mock
    private MockedStatic<TeamServicesSettingsService> teamServicesSettingsServiceStatic;

    @Before
    public void setupLocalTests() {
        fileDocumentManager.when(FileDocumentManager::getInstance).thenReturn(mockFileDocumentManager);
        applicationManager.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        teamServicesSettingsServiceStatic.when(TeamServicesSettingsService::getInstance)
                .thenReturn(teamServicesSettingsService);

        processCommandArgs = Collections.emptyList();
        processUriArgs  = StringUtils.EMPTY;
    }

    @Test
    public void testCheckArgumentsUriHappy() {
        String[] args = {ApplicationStarterBase.VSTS_COMMAND, URI_ARG};
        Assert.assertTrue(starterBase.checkArguments(args));
    }

    @Test
    public void testCheckArgumentsCommandsHappy() {
        String[] args = {ApplicationStarterBase.VSTS_COMMAND, SUB_COMMAND, VALID_GIT_URL};
        Assert.assertTrue(starterBase.checkArguments(args));
    }

    @Test
    public void testCheckArgumentsIncorrectCommand() {
        String[] args = {"vstsssss", SUB_COMMAND, VALID_GIT_URL};
        Assert.assertFalse(starterBase.checkArguments(args));
    }

    @Test
    public void testCheckArgumentsIncorrectArgsNumber() {
        String[] argsFew = {ApplicationStarterBase.VSTS_COMMAND};
        Assert.assertFalse(starterBase.checkArguments(argsFew));
    }

    @Test
    public void testMainUriArgs() {
        String[] args = {ApplicationStarterBase.VSTS_COMMAND, URI_ARG};
        starterBase.main(args);
        Assert.assertEquals(URI_MINUS_PREFIX, processUriArgs);
        Assert.assertEquals(Collections.emptyList(), processCommandArgs);
    }

    @Test
    public void testMainCommandArgs() {
        String[] args = {ApplicationStarterBase.VSTS_COMMAND, SUB_COMMAND, VALID_GIT_URL};
        List<String> expectedParams = Arrays.asList(SUB_COMMAND, VALID_GIT_URL);
        starterBase.main(args);
        Assert.assertEquals(StringUtils.EMPTY, processUriArgs);
        Assert.assertEquals(expectedParams, processCommandArgs);
    }
}
