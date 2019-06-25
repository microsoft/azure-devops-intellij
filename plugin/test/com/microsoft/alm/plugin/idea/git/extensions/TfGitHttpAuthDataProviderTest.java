package com.microsoft.alm.plugin.idea.git.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.util.AuthData;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.settings.TeamServicesSecrets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TeamServicesSecrets.class})
public class TfGitHttpAuthDataProviderTest extends IdeaAbstractTest {
    private final AuthenticationInfo authenticationInfo = new AuthenticationInfo(
            "userName",
            "password",
            "serverUri",
            "userNameForDisplay");

    private TfGitHttpAuthDataProvider authDataProvider;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(TeamServicesSecrets.class);

        authDataProvider = new TfGitHttpAuthDataProvider();
        ServerContext context = Mockito.mock(ServerContext.class);
        when(context.getKey()).thenReturn(ServerContext.getKey("https://dev.azure.com/username"));
        when(context.getAuthenticationInfo()).thenReturn(authenticationInfo);
        ServerContextManager.getInstance().add(context);
    }

    @Test
    public void httpAuthShouldWorkOnUrlThatDoesNotRequireConversion() {
        AuthData authData = authDataProvider.getAuthData("https://dev.azure.com/username");

        assertNotNull(authData);
        assertEquals(authenticationInfo.getUserName(), authData.getLogin());
        assertEquals(authenticationInfo.getPassword(), authData.getPassword());
    }

    @Test
    public void httpAuthShouldConvertUrlProperly() {
        AuthData authData = authDataProvider.getAuthData("https://username@dev.azure.com/");

        assertNotNull(authData);
        assertEquals(authenticationInfo.getUserName(), authData.getLogin());
        assertEquals(authenticationInfo.getPassword(), authData.getPassword());
    }

    @Test
    public void usernameShouldBeCombinedWithAnUrl() {
        TfGitHttpAuthDataProvider mockedAuthDataProvider = Mockito.mock(TfGitHttpAuthDataProvider.class);
        when(mockedAuthDataProvider.getAuthData(any(Project.class), any(String.class), any(String.class))).thenCallRealMethod();

        Project project = Mockito.mock(Project.class);

        mockedAuthDataProvider.getAuthData(project, "https://dev.azure.com", "username");

        verify(mockedAuthDataProvider, times(1)).getAuthData("https://username@dev.azure.com");
    }
}
