// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationListener;
import com.microsoft.alm.plugin.authentication.AuthenticationProvider;
import com.microsoft.alm.plugin.exceptions.ProfileDoesNotExistException;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.operations.AccountLookupOperation;
import com.microsoft.alm.plugin.operations.OperationFactory;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectManager.class, OperationFactory.class})
public class LookupHelperTests extends IdeaAbstractTest {

    @Mock
    public ProjectManager projectManager;
    @Mock
    public Project project;
    @Mock
    public OperationFactory operationFactory;
    // Mocked via subclass below
    public MyAccountLookupOperation accountLookupOperation;

    @Before
    public void setupLocalTest() {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(ProjectManager.class);
        when(ProjectManager.getInstance()).thenReturn(projectManager);
        when(projectManager.getDefaultProject()).thenReturn(project);

        accountLookupOperation = new MyAccountLookupOperation();
        PowerMockito.mockStatic(OperationFactory.class);
        PowerMockito.when(OperationFactory.createAccountLookupOperation()).thenReturn(accountLookupOperation);
    }

    @Test
    public void testAuthenticateAndLoadTfsContexts_nullServerUrl() {
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn(null);
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        LookupHelper.authenticateAndLoadTfsContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, ServerContextLookupOperation.ContextScope.REPOSITORY);

        verify(loginPageModel).addError(Mockito.any(ModelValidationInfo.class));
        verify(loginPageModel).setConnected(false);
    }

    @Test
    public void testAuthenticateAndLoadTfsContexts_emptyServerUrl() {
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn("");
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        LookupHelper.authenticateAndLoadTfsContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, ServerContextLookupOperation.ContextScope.REPOSITORY);

        verify(loginPageModel).addError(Mockito.any(ModelValidationInfo.class));
        verify(loginPageModel).setConnected(false);
    }

    @Test
    public void testAuthenticateAndLoadTfsContexts_invalidServerUrl() {
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn("not_valid_url");
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        LookupHelper.authenticateAndLoadTfsContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, ServerContextLookupOperation.ContextScope.REPOSITORY);

        verify(loginPageModel).addError(Mockito.any(ModelValidationInfo.class));
        verify(loginPageModel).setConnected(false);
    }

    @Test
    public void testAuthenticateAndLoadTfsContexts_authenticated() {
        final ServerContextLookupOperation.ContextScope scope = ServerContextLookupOperation.ContextScope.REPOSITORY;
        final String serverUrl = "http://server:8080/tfs";
        final AuthenticationInfo authInfo = new AuthenticationInfo("user1", "pass1", serverUrl, "userOne");
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn(serverUrl);
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        when(authenticationProvider.isAuthenticated()).thenReturn(true);
        when(authenticationProvider.getAuthenticationInfo()).thenReturn(authInfo);
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        LookupHelper.authenticateAndLoadTfsContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, scope);

        verify(loginPageModel).setConnected(true);
        verify(lookupPageModel).setLoading(true);
        verify(lookupListener).loadContexts(anyList(), Matchers.eq(scope));
    }

    @Test
    public void testAuthenticateAndLoadTfsContexts_notAuthenticated() {
        final ServerContextLookupOperation.ContextScope scope = ServerContextLookupOperation.ContextScope.REPOSITORY;
        final String serverUrl = "http://server:8080/tfs";
        final AuthenticationInfo authInfo = new AuthenticationInfo("user1", "pass1", serverUrl, "userOne");
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn(serverUrl);
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        when(authenticationProvider.isAuthenticated()).thenReturn(false);
        when(authenticationProvider.getAuthenticationInfo()).thenReturn(authInfo);
        doNothing().when(authenticationProvider).authenticateAsync(Matchers.anyString(), Matchers.any(AuthenticationListener.class));
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        ArgumentCaptor<AuthenticationListener> captor = ArgumentCaptor.forClass(AuthenticationListener.class);
        LookupHelper.authenticateAndLoadTfsContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, scope);
        verify(authenticationProvider).authenticateAsync(Matchers.anyString(), captor.capture());
        final AuthenticationListener listener = captor.getValue();
        // Finish the code path by calling the listener methods
        listener.authenticating();
        verify(loginPageModel).setAuthenticating(true);
        listener.authenticated(authInfo, null);
        verify(loginPageModel).setAuthenticating(false);

        verify(loginPageModel).addError(Mockito.any(ModelValidationInfo.class));
        verify(loginPageModel).signOut();
    }

    @Test
    public void testAuthenticateAndLoadTfsContexts_errorInAuthenticated() {
        final ServerContextLookupOperation.ContextScope scope = ServerContextLookupOperation.ContextScope.REPOSITORY;
        final String serverUrl = "http://server:8080/tfs";
        final AuthenticationInfo authInfo = new AuthenticationInfo("user1", "pass1", serverUrl, "userOne");
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn(serverUrl);
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        when(authenticationProvider.isAuthenticated()).thenReturn(false);
        when(authenticationProvider.getAuthenticationInfo()).thenReturn(authInfo);
        doNothing().when(authenticationProvider).authenticateAsync(Matchers.anyString(), Matchers.any(AuthenticationListener.class));
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        ArgumentCaptor<AuthenticationListener> captor = ArgumentCaptor.forClass(AuthenticationListener.class);
        LookupHelper.authenticateAndLoadTfsContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, scope);
        verify(authenticationProvider).authenticateAsync(Matchers.anyString(), captor.capture());
        final AuthenticationListener listener = captor.getValue();
        // Finish the code path by calling the listener methods
        listener.authenticating();
        verify(loginPageModel).setAuthenticating(true);
        listener.authenticated(authInfo, new Exception("Thrown to cause exception"));
        verify(loginPageModel).setAuthenticating(false);

        verify(loginPageModel).addError(Mockito.any(ModelValidationInfo.class));
        verify(loginPageModel).signOut();
    }

    @Test
    public void testAuthenticateAndLoadTfsContexts_profileDoesNotExist() {
        final ServerContextLookupOperation.ContextScope scope = ServerContextLookupOperation.ContextScope.REPOSITORY;
        final String serverUrl = "http://server:8080/tfs";
        final AuthenticationInfo authInfo = new AuthenticationInfo("user1", "pass1", serverUrl, "userOne");
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn(serverUrl);
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        when(authenticationProvider.isAuthenticated()).thenReturn(false);
        when(authenticationProvider.getAuthenticationInfo()).thenReturn(authInfo);
        doNothing().when(authenticationProvider).authenticateAsync(Matchers.anyString(), Matchers.any(AuthenticationListener.class));
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        ArgumentCaptor<AuthenticationListener> captor = ArgumentCaptor.forClass(AuthenticationListener.class);
        LookupHelper.authenticateAndLoadTfsContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, scope);
        verify(authenticationProvider).authenticateAsync(Matchers.anyString(), captor.capture());
        final AuthenticationListener listener = captor.getValue();
        // Finish the code path by calling the listener methods
        listener.authenticating();
        verify(loginPageModel).setAuthenticating(true);
        listener.authenticated(authInfo, new ProfileDoesNotExistException("test exception", null));
        verify(loginPageModel).setAuthenticating(false);

        verify(loginPageModel).addError(Mockito.any(ModelValidationInfo.class));
        verify(loginPageModel).signOut();
    }

    @Test
    public void testAuthenticateAndLoadVsoContexts_specifiedAccountUrl() {
        final ServerContextLookupOperation.ContextScope scope = ServerContextLookupOperation.ContextScope.REPOSITORY;
        final String serverUrl = "http://test.visualstudio.com";
        final AuthenticationInfo authInfo = new AuthenticationInfo("user1", "pass1", serverUrl, "userOne");
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn(serverUrl);
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        when(authenticationProvider.isAuthenticated()).thenReturn(true);
        when(authenticationProvider.getAuthenticationInfo()).thenReturn(authInfo);
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        LookupHelper.authenticateAndLoadVsoContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, scope);

        verify(loginPageModel).setConnected(true);
        verify(lookupPageModel).setLoading(true);
        verify(lookupListener).loadContexts(anyList(), Matchers.eq(scope));
    }

    @Test
    public void testAuthenticateAndLoadVsoContexts_authenticated() {
        final ServerContextLookupOperation.ContextScope scope = ServerContextLookupOperation.ContextScope.REPOSITORY;
        final String serverUrl = "http://server:8080/tfs";
        final AuthenticationInfo authInfo = new AuthenticationInfo("user1", "pass1", serverUrl, "userOne");
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn(serverUrl);
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        when(authenticationProvider.isAuthenticated()).thenReturn(true);
        when(authenticationProvider.getAuthenticationInfo()).thenReturn(authInfo);
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        LookupHelper.authenticateAndLoadVsoContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, scope);
        accountLookupOperation.onLookupResults(new AccountLookupOperation.AccountLookupResults());

        verify(loginPageModel).setConnected(true);
        verify(lookupPageModel).setLoading(true);
        verify(lookupListener).loadContexts(anyList(), Matchers.eq(scope));
    }

    @Test
    public void testAuthenticateAndLoadVsoContexts_notAuthenticated() {
        final ServerContextLookupOperation.ContextScope scope = ServerContextLookupOperation.ContextScope.REPOSITORY;
        final String serverUrl = "http://server:8080/tfs";
        final AuthenticationInfo authInfo = new AuthenticationInfo("user1", "pass1", serverUrl, "userOne");
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn(serverUrl);
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        when(authenticationProvider.isAuthenticated()).thenReturn(false);
        when(authenticationProvider.getAuthenticationInfo()).thenReturn(authInfo);
        doNothing().when(authenticationProvider).authenticateAsync(Matchers.anyString(), Matchers.any(AuthenticationListener.class));
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        ArgumentCaptor<AuthenticationListener> captor = ArgumentCaptor.forClass(AuthenticationListener.class);
        LookupHelper.authenticateAndLoadVsoContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, scope);
        verify(authenticationProvider).authenticateAsync(Matchers.anyString(), captor.capture());
        final AuthenticationListener listener = captor.getValue();
        // Finish the code path by calling the listener methods
        listener.authenticating();
        verify(loginPageModel).setAuthenticating(true);
        listener.authenticated(authInfo, null);
        verify(loginPageModel).setAuthenticating(false);

        verify(loginPageModel).addError(Mockito.any(ModelValidationInfo.class));
        verify(loginPageModel).signOut();
    }

    @Test
    public void testAuthenticateAndLoadVsoContexts_errorInAuthenticated() {
        final ServerContextLookupOperation.ContextScope scope = ServerContextLookupOperation.ContextScope.REPOSITORY;
        final String serverUrl = "http://server:8080/tfs";
        final AuthenticationInfo authInfo = new AuthenticationInfo("user1", "pass1", serverUrl, "userOne");
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn(serverUrl);
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        when(authenticationProvider.isAuthenticated()).thenReturn(false);
        when(authenticationProvider.getAuthenticationInfo()).thenReturn(authInfo);
        doNothing().when(authenticationProvider).authenticateAsync(Matchers.anyString(), Matchers.any(AuthenticationListener.class));
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        ArgumentCaptor<AuthenticationListener> captor = ArgumentCaptor.forClass(AuthenticationListener.class);
        LookupHelper.authenticateAndLoadVsoContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, scope);
        verify(authenticationProvider).authenticateAsync(Matchers.anyString(), captor.capture());
        final AuthenticationListener listener = captor.getValue();
        // Finish the code path by calling the listener methods
        listener.authenticating();
        verify(loginPageModel).setAuthenticating(true);
        listener.authenticated(authInfo, new Exception("Thrown to cause exception"));
        verify(loginPageModel).setAuthenticating(false);

        verify(loginPageModel).addError(Mockito.any(ModelValidationInfo.class));
        verify(loginPageModel).signOut();
    }

    @Test
    public void testAuthenticateAndLoadVsoContexts_profileDoesNotExist() {
        final ServerContextLookupOperation.ContextScope scope = ServerContextLookupOperation.ContextScope.REPOSITORY;
        final String serverUrl = "http://server:8080/tfs";
        final AuthenticationInfo authInfo = new AuthenticationInfo("user1", "pass1", serverUrl, "userOne");
        final LoginPageModel loginPageModel = Mockito.mock(LoginPageModel.class);
        when(loginPageModel.getServerName()).thenReturn(serverUrl);
        final ServerContextLookupPageModel lookupPageModel = Mockito.mock(ServerContextLookupPageModel.class);
        final AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        when(authenticationProvider.isAuthenticated()).thenReturn(false);
        when(authenticationProvider.getAuthenticationInfo()).thenReturn(authInfo);
        doNothing().when(authenticationProvider).authenticateAsync(Matchers.anyString(), Matchers.any(AuthenticationListener.class));
        final ServerContextLookupListener lookupListener = Mockito.mock(ServerContextLookupListener.class);

        ArgumentCaptor<AuthenticationListener> captor = ArgumentCaptor.forClass(AuthenticationListener.class);
        LookupHelper.authenticateAndLoadVsoContexts(loginPageModel, lookupPageModel,
                authenticationProvider, lookupListener, scope);
        verify(authenticationProvider).authenticateAsync(Matchers.anyString(), captor.capture());
        final AuthenticationListener listener = captor.getValue();
        // Finish the code path by calling the listener methods
        listener.authenticating();
        verify(loginPageModel).setAuthenticating(true);
        listener.authenticated(authInfo, new ProfileDoesNotExistException("test exception", null));
        verify(loginPageModel).setAuthenticating(false);

        verify(loginPageModel).addError(Mockito.any(ModelValidationInfo.class));
        verify(loginPageModel).signOut();
    }

    private class MyAccountLookupOperation extends AccountLookupOperation {
        @Override
        public void doWorkAsync(Inputs inputs) {
            // Do nothing
        }

        @Override
        public void doWork(Inputs inputs) {
            // Do nothing
        }

        @Override
        public void onLookupStarted() {
            super.onLookupStarted();
        }

        @Override
        public void onLookupCompleted() {
            super.onLookupCompleted();
        }

        @Override
        public void onLookupResults(Results results) {
            super.onLookupResults(results);
        }
    }
}
