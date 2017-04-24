// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.settings;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthTypes;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.swing.Icon;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServerContextManager.class, Messages.class, ProgressManager.class, AuthHelper.class})
public class TeamServicesSettingsModelTest extends IdeaAbstractTest {
    private TeamServicesSettingsModel teamServicesSettingsModel;

    @Mock
    private Project mockProject;
    @Mock
    private ServerContextManager mockServerContextManager;
    @Mock
    private ServerContext mockServerContext_GitRepo;
    @Mock
    private ServerContext mockServerContext_TfvcRepo;
    @Mock
    private ServerContext mockServerContext_NoRepo;
    @Mock
    private ServerContext mockServerContext_TfsLastUsedUrl;
    @Mock
    private GitRepository mockRepo1;
    @Mock
    private GitRepository mockRepo_TfsLastUsedUrl;
    @Mock
    private TeamProjectReference mockProfRef2;
    @Mock
    private ProgressManager mockProgressManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(ServerContextManager.class, Messages.class, ProgressManager.class, AuthHelper.class);

        when(mockRepo1.getName()).thenReturn("repo1");
        when(mockProfRef2.getName()).thenReturn("repo2");
        when(mockRepo_TfsLastUsedUrl.getName()).thenReturn("mockRepo_TfsLastUsedUrl");
        when(mockServerContext_GitRepo.getKey()).thenReturn("mockServerContext_GitRepo");
        when(mockServerContext_GitRepo.getGitRepository()).thenReturn(mockRepo1);
        when(mockServerContext_GitRepo.getUri()).thenReturn(URI.create("https://repo1.visualstudio.com"));
        when(mockServerContext_TfvcRepo.getKey()).thenReturn("mockServerContext_TfvcRepo");
        when(mockServerContext_TfvcRepo.getGitRepository()).thenReturn(null);
        when(mockServerContext_TfvcRepo.getTeamProjectReference()).thenReturn(mockProfRef2);
        when(mockServerContext_TfvcRepo.getUri()).thenReturn(URI.create("https://repo2.visualstudio.com"));
        when(mockServerContext_NoRepo.getKey()).thenReturn("mockServerContext_NoRepo");
        when(mockServerContext_NoRepo.getGitRepository()).thenReturn(null);
        when(mockServerContext_TfsLastUsedUrl.getKey()).thenReturn("mockServerContext_TfsLastUsedUrl");
        when(mockServerContext_TfsLastUsedUrl.getGitRepository()).thenReturn(mockRepo_TfsLastUsedUrl);
        when(mockServerContext_TfsLastUsedUrl.getUri()).thenReturn(URI.create(TfsAuthenticationProvider.TFS_LAST_USED_URL));
        when(mockServerContextManager.getAllServerContexts()).thenReturn(ImmutableList.of(mockServerContext_GitRepo, mockServerContext_TfvcRepo, mockServerContext_NoRepo, mockServerContext_TfsLastUsedUrl));
        when(ServerContextManager.getInstance()).thenReturn(mockServerContextManager);
        when(ProgressManager.getInstance()).thenReturn(mockProgressManager);

        teamServicesSettingsModel = new TeamServicesSettingsModel(mockProject);
    }

    @Test
    public void testIsModifiedContexts_True() {
        teamServicesSettingsModel.setDeleteContexts(ImmutableList.of(mockServerContext_GitRepo));

        assertTrue(teamServicesSettingsModel.isModified());
        assertEquals(1, teamServicesSettingsModel.getDeleteContexts().size());
    }

    @Test
    public void testIsModifiedContexts_False() {
        teamServicesSettingsModel.setDeleteContexts(Collections.EMPTY_LIST);

        assertFalse(teamServicesSettingsModel.isModified());
        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
    }

    @Test
    public void testIsModifiedAuthType_True() {
        when(AuthHelper.getAuthTypeInSettingsFile()).thenReturn(AuthTypes.DEVICE_FLOW.name());
        teamServicesSettingsModel.setUpdatedAuthType(AuthTypes.CREDS);

        assertTrue(teamServicesSettingsModel.isModified());
    }

    @Test
    public void testIsModifiedAuthType_False() {
        when(AuthHelper.getAuthTypeInSettingsFile()).thenReturn(AuthTypes.DEVICE_FLOW.name());

        assertFalse(teamServicesSettingsModel.isModified());
    }

    @Test
    public void testLoadSettings() {
        when(AuthHelper.getAuthTypeInSettingsFile()).thenReturn(AuthTypes.DEVICE_FLOW.name());

        // running it twice to test we clear contexts
        teamServicesSettingsModel.loadSettings();
        assertEquals(AuthTypes.DEVICE_FLOW, teamServicesSettingsModel.getOriginalAuthType());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        assertFalse(teamServicesSettingsModel.isModified());

        teamServicesSettingsModel.loadSettings();
        assertEquals(AuthTypes.DEVICE_FLOW, teamServicesSettingsModel.getOriginalAuthType());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        assertFalse(teamServicesSettingsModel.isModified());
    }

    @Test
    public void testApply() {
        when(AuthHelper.getAuthTypeInSettingsFile()).thenReturn(AuthTypes.DEVICE_FLOW.name());
        teamServicesSettingsModel.setDeleteContexts(ImmutableList.of(mockServerContext_GitRepo, mockServerContext_TfvcRepo));
        teamServicesSettingsModel.setUpdatedAuthType(AuthTypes.CREDS);
        teamServicesSettingsModel.apply();

        assertEquals(0, teamServicesSettingsModel.getTableModel().getRowCount());
        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(AuthTypes.CREDS, teamServicesSettingsModel.getOriginalAuthType());
        verifyStatic(times(1));
        mockServerContextManager.remove("mockServerContext_GitRepo");
        mockServerContextManager.remove("mockServerContext_TfvcRepo");
        AuthHelper.setDeviceFlowEnvVariableFalse();
    }

    @Test
    public void testReset() {
        when(AuthHelper.getAuthTypeInSettingsFile()).thenReturn(AuthTypes.DEVICE_FLOW.name());
        teamServicesSettingsModel.setUpdatedAuthType(AuthTypes.CREDS);
        teamServicesSettingsModel.setDeleteContexts(ImmutableList.of(mockServerContext_GitRepo, mockServerContext_TfvcRepo));
        teamServicesSettingsModel.reset();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        assertFalse(teamServicesSettingsModel.isModified());
    }

    @Test
    public void testDeletePasswords_OK() {
        teamServicesSettingsModel.getTableSelectionModel().setSelectionInterval(0, 0);
        when(Messages.showYesNoDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.YES);
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext_GitRepo, mockServerContext_TfvcRepo)));
        teamServicesSettingsModel.deletePasswords();

        assertEquals(1, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(1, teamServicesSettingsModel.getTableModel().getRowCount());
        assertEquals(mockServerContext_GitRepo, teamServicesSettingsModel.getTableModel().getServerContext(0));
        verifyStatic(times(1));
        Messages.showYesNoDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG)), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE)), any(Icon.class));
    }

    @Test
    public void testDeletePasswords_Cancel() {
        teamServicesSettingsModel.getTableSelectionModel().setSelectionInterval(0, 0);
        when(Messages.showYesNoDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.CANCEL);
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext_GitRepo, mockServerContext_TfvcRepo)));
        teamServicesSettingsModel.deletePasswords();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        assertEquals(mockServerContext_TfvcRepo, teamServicesSettingsModel.getTableModel().getServerContext(0));
        assertEquals(mockServerContext_GitRepo, teamServicesSettingsModel.getTableModel().getServerContext(1));
        verifyStatic(times(1));
        Messages.showYesNoDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG)), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE)), any(Icon.class));
    }

    @Test
    public void testDeletePasswords_NoRowSelected() {
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext_GitRepo, mockServerContext_TfvcRepo)));
        teamServicesSettingsModel.deletePasswords();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        assertEquals(mockServerContext_TfvcRepo, teamServicesSettingsModel.getTableModel().getServerContext(0));
        assertEquals(mockServerContext_GitRepo, teamServicesSettingsModel.getTableModel().getServerContext(1));
        verifyStatic(times(1));
        Messages.showWarningDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE));
    }

    @Test
    public void testUpdatePasswords_OK() {
        teamServicesSettingsModel.getTableSelectionModel().setSelectionInterval(0, 0);
        when(Messages.showYesNoDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.YES);
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext_GitRepo, mockServerContext_TfvcRepo)));
        teamServicesSettingsModel.updatePasswords();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        verifyStatic(times(1));
        Messages.showYesNoDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG)), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE)), any(Icon.class));
    }

    @Test
    public void testUpdatePasswords_Cancel() {
        teamServicesSettingsModel.getTableSelectionModel().setSelectionInterval(0, 0);
        when(Messages.showYesNoDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE), Messages.getQuestionIcon())).thenReturn(Messages.CANCEL);
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext_GitRepo, mockServerContext_TfvcRepo)));
        teamServicesSettingsModel.updatePasswords();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        assertEquals(mockServerContext_TfvcRepo, teamServicesSettingsModel.getTableModel().getServerContext(0));
        assertEquals(mockServerContext_GitRepo, teamServicesSettingsModel.getTableModel().getServerContext(1));
        verifyStatic(times(1));
        Messages.showYesNoDialog(eq(mockProject), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG)), eq(TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE)), any(Icon.class));
    }

    @Test
    public void testUpdatePasswords_NoRowSelected() {
        teamServicesSettingsModel.getTableModel().addServerContexts(new ArrayList<ServerContext>(ImmutableList.of(mockServerContext_GitRepo, mockServerContext_TfvcRepo)));
        teamServicesSettingsModel.updatePasswords();

        assertEquals(0, teamServicesSettingsModel.getDeleteContexts().size());
        assertEquals(2, teamServicesSettingsModel.getTableModel().getRowCount());
        verifyStatic(times(1));
        Messages.showWarningDialog(mockProject, TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED), TfPluginBundle.message(TfPluginBundle.KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE));
    }
}
