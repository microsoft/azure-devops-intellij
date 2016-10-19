// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2;

import com.intellij.notification.NotificationsManager;
import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import git4idea.GitVcs;
import git4idea.annotate.GitAnnotationProvider;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitHttpAuthService;
import git4idea.commands.GitHttpAuthenticator;
import git4idea.commands.GitImpl;
import git4idea.config.GitSharedSettings;
import git4idea.config.GitVcsApplicationSettings;
import git4idea.config.GitVcsSettings;
import git4idea.diff.GitDiffProvider;
import git4idea.history.GitHistoryProvider;
import git4idea.i18n.GitBundle;
import git4idea.rollback.GitRollbackEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectManager.class, LocalFileSystem.class, ServiceManager.class, ProgressManager.class,
        GitVcsSettings.class, GitVcs.class, NotificationsConfigurationImpl.class, GitBundle.class,
        NotificationsManager.class, GitVcsApplicationSettings.class, EditorColorsManager.class,
        EncodingManager.class, VsoAuthenticationProvider.class})
// PowerMock and the javax.net.ssl.SSLContext class don't play well together. If you mock any static classes
// you have to PowerMockIgnore("javax.net.ssl.*") to avoid exceptions being thrown by SSLContext
@PowerMockIgnore({"javax.net.ssl.*", "javax.security.*"})
public abstract class L2Test extends IdeaAbstractTest {
    // Setup all the appropriate mocks so that we can run UI code
    @Mock
    WindowManager windowManager;
    @Mock
    StatusBar statusBar;
    @Mock
    ProjectManager projectManager;
    @Mock
    Project project;
    @Mock
    MyLocalFileSystem localFileSystem;
    @Mock
    ProgressManager progressManager;
    @Mock
    ProjectLevelVcsManager projectLevelVcsManager;
    @Mock
    NotificationsConfigurationImpl notificationsConfiguration;
    @Mock
    GitBundle gitBundle;
    @Mock
    NotificationsManager notificationsManager;
    @Mock
    EditorColorsManager editorColorsManager;
    @Mock
    GitHttpAuthService gitHttpAuthService;
    @Mock
    GitHttpAuthenticator gitHttpAuthenticator;
    @Mock
    EncodingManager encodingManager;

    GitVcsSettings gitVcsSettings;
    GitVcs gitVcs;
    ProgressIndicator progressIndicator;

    // Context variables
    String serverUrl;
    String teamProject;
    String user;
    String pass;
    String repoUrl;
    String legacyRepoUrl;
    String tfExe;

    public String getServerUrl() {
        return serverUrl;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public String getTeamProject() {
        return teamProject;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getLegacyRepoUrl() {
        return legacyRepoUrl;
    }

    private AuthenticationInfo getAuthenticationInfo() {
        final AuthenticationInfo authenticationInfo = new AuthenticationInfo(user, pass, serverUrl, user);
        return authenticationInfo;
    }

    private void loadContext() {
        user = System.getenv("MSVSTS_INTELLIJ_VSO_USER");
        pass = System.getenv("MSVSTS_INTELLIJ_VSO_PASS");
        serverUrl = System.getenv("MSVSTS_INTELLIJ_VSO_SERVER_URL");
        teamProject = System.getenv("MSVSTS_INTELLIJ_VSO_TEAM_PROJECT");
        repoUrl = System.getenv("MSVSTS_INTELLIJ_VSO_GIT_REPO_URL");
        legacyRepoUrl = System.getenv("MSVSTS_INTELLIJ_VSO_LEGACY_GIT_REPO_URL");
        tfExe = System.getenv("MSVSTS_INTELLIJ_TF_EXE");

        Assert.assertFalse("You must provide a username, password, serverUrl, teamProject, repoUrl, and legacyRepoUrl " +
                        "for the L2 tests through the following environment variables: " +
                        "MSVSTS_INTELLIJ_VSO_USER, MSVSTS_INTELLIJ_VSO_PASS, MSVSTS_INTELLIJ_VSO_SERVER_URL, " +
                        "MSVSTS_INTELLIJ_VSO_TEAM_PROJECT, MSVSTS_INTELLIJ_VSO_GIT_REPO_URL, " +
                        "MSVSTS_INTELLIJ_VSO_LEGACY_GIT_REPO_URL, MSVSTS_INTELLIJ_TF_EXE",
                StringUtils.isEmpty(user) ||
                        StringUtils.isEmpty(pass) ||
                        StringUtils.isEmpty(serverUrl) ||
                        StringUtils.isEmpty(teamProject) ||
                        StringUtils.isEmpty(repoUrl) ||
                        StringUtils.isEmpty(legacyRepoUrl) ||
                        StringUtils.isEmpty(tfExe));

        // Examples
        //serverUrl = "https://xplatalm.visualstudio.com/";
        //user = "jpricket@microsoft.com";
        //pass = "PAT_GENERATED_BY SERVER";
        //teamProject = "L2.IntelliJ";
        //repoUrl = "https://xplatalm.visualstudio.com/_git/L2.IntelliJ";
        //legacyRepoUrl = "https://xplatalm.visualstudio.com/defaultcollection/_git/L2.IntelliJ";
        //tfExe = "d:\\tmp\\bin\\TEE-CLC-14.0.4\\tf.cmd";
    }

    @Before
    public void setupLocalTests() throws IOException {
        loadContext();

        PluginServiceProvider.getInstance().getPropertyService().setProperty(PropertyService.PROP_TF_HOME, tfExe);

        final VsoAuthenticationProvider authenticationProvider = Mockito.mock(VsoAuthenticationProvider.class);
        PowerMockito.mockStatic(VsoAuthenticationProvider.class);
        when(VsoAuthenticationProvider.getInstance()).thenReturn(authenticationProvider);
        when(authenticationProvider.getAuthenticationInfo()).thenReturn(getAuthenticationInfo());
        // We return false, the first time so that the constructors don't start loading stuff
        // But we return true there after.
        when(authenticationProvider.isAuthenticated()).thenReturn(true);

        //TODO Try to use the IntelliJ test framework to avoid mocking so much
        // When I first tried to use these classes they didn't help at all :(
        //final TestFixtureBuilder<IdeaProjectTestFixture> lightFixtureBuilder = IdeaTestFixtureFactory.getFixtureFactory().createLightFixtureBuilder();
        //IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(lightFixtureBuilder.getFixture()).setUp();

        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(EncodingManager.class);
        when(EncodingManager.getInstance()).thenReturn(encodingManager);

        EditorColorsScheme editorColorsScheme = Mockito.mock(EditorColorsScheme.class);
        PowerMockito.mockStatic(EditorColorsManager.class);
        when(EditorColorsManager.getInstance()).thenReturn(editorColorsManager);
        when(editorColorsManager.getGlobalScheme()).thenReturn(editorColorsScheme);
        when(editorColorsScheme.getAttributes(Matchers.any(TextAttributesKey.class))).thenReturn(new TextAttributes());

        PowerMockito.mockStatic(NotificationsManager.class);
        when(NotificationsManager.getNotificationsManager()).thenReturn(notificationsManager);

        PowerMockito.mockStatic(ProjectManager.class);
        when(ProjectManager.getInstance()).thenReturn(projectManager);
        when(projectManager.getOpenProjects()).thenReturn(new Project[]{project});
        when(projectManager.getDefaultProject()).thenReturn(project);

        PowerMockito.mockStatic(LocalFileSystem.class);
        when(LocalFileSystem.getInstance()).thenReturn(localFileSystem);
        when(localFileSystem.findFileByIoFile(Matchers.any(File.class))).thenCallRealMethod();

        Git gitService = new GitImpl();
        PowerMockito.mockStatic(ServiceManager.class);
        when(ServiceManager.getService(Git.class)).thenReturn(gitService);
        when(ServiceManager.getService(GitHttpAuthService.class)).thenReturn(gitHttpAuthService);
        when(gitHttpAuthService.getScriptPath()).thenReturn(File.createTempFile("Test", "Test", null));
        when(gitHttpAuthService.createAuthenticator(Matchers.any(Project.class), Matchers.any(GitCommand.class), anyString())).thenReturn(gitHttpAuthenticator);

        progressIndicator = new MyProgressIndicator();

        PowerMockito.mockStatic(ProgressManager.class);
        when(ProgressManager.getInstance()).thenReturn(progressManager);
        doNothing().when(progressManager).run(Matchers.any(Task.class));
        when(progressManager.getProgressIndicator()).thenReturn(progressIndicator);

        gitVcsSettings = new GitVcsSettings(null);
        PowerMockito.mockStatic(GitVcsSettings.class);
        when(GitVcsSettings.getInstance(Matchers.any(Project.class))).thenReturn(gitVcsSettings);

        PowerMockito.mockStatic(NotificationsConfigurationImpl.class);

        PowerMockito.mockStatic(GitBundle.class);
        when(GitBundle.getString(anyString())).thenReturn("locedString");
        when(GitBundle.message(anyString(), anyVararg())).thenReturn("locedString");

        GitVcsApplicationSettings applicationSettings = new GitVcsApplicationSettings();

        PowerMockito.mockStatic(GitVcsApplicationSettings.class);
        when(GitVcsApplicationSettings.getInstance()).thenReturn(applicationSettings);

        GitAnnotationProvider annotationProvider = new GitAnnotationProvider(project);
        GitDiffProvider diffProvider = new GitDiffProvider(project);
        GitHistoryProvider gitHistoryProvider = new GitHistoryProvider(project);
        GitRollbackEnvironment rollbackEnvironment = new GitRollbackEnvironment(project);
        GitSharedSettings sharedSettings = new GitSharedSettings();
        gitVcs = new GitVcs(project, gitService, projectLevelVcsManager, annotationProvider,
                diffProvider, gitHistoryProvider, rollbackEnvironment,
                applicationSettings, gitVcsSettings, sharedSettings);
        PowerMockito.mockStatic(GitVcs.class);
        when(GitVcs.getInstance(Matchers.any(Project.class))).thenReturn(gitVcs);

        //TODO setup server context with auth info without prompting during the test
        //ServerContextManager.getInstance().getAuthenticatedContext("https://xplatalm.visualstudio.com/", true);
    }

    public void runProgressManagerTask() {
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(progressManager).run(taskCaptor.capture());
        Task task = taskCaptor.getValue();
        task.run(progressIndicator);
    }

    public static File createTempDirectory()
            throws IOException {
        final File temp;

        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

        if (!temp.delete()) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!temp.mkdir()) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return temp;
    }

    private class MyProgressIndicator implements ProgressIndicator {
        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void setText(String text) {
        }

        @Override
        public String getText() {
            return null;
        }

        @Override
        public void setText2(String text) {
        }

        @Override
        public String getText2() {
            return null;
        }

        @Override
        public double getFraction() {
            return 0;
        }

        @Override
        public void setFraction(double fraction) {
        }

        @Override
        public void pushState() {
        }

        @Override
        public void popState() {
        }

        @Override
        public void startNonCancelableSection() {
        }

        @Override
        public void finishNonCancelableSection() {
        }

        @Override
        public boolean isModal() {
            return false;
        }

        @NotNull
        @Override
        public ModalityState getModalityState() {
            return null;
        }

        @Override
        public void setModalityProgress(ProgressIndicator modalityProgress) {
        }

        @Override
        public boolean isIndeterminate() {
            return false;
        }

        @Override
        public void setIndeterminate(boolean indeterminate) {
        }

        @Override
        public void checkCanceled() throws ProcessCanceledException {
        }

        @Override
        public boolean isPopupWasShown() {
            return false;
        }

        @Override
        public boolean isShowing() {
            return false;
        }
    }

    private abstract class MyLocalFileSystem extends LocalFileSystem {
        @Nullable
        @Override
        public VirtualFile findFileByIoFile(final File file) {
            return new VirtualFile() {
                @NotNull
                @Override
                public String getName() {
                    return file.getName();
                }

                @NotNull
                @Override
                public VirtualFileSystem getFileSystem() {
                    return null;
                }

                @NotNull
                @Override
                public String getPath() {
                    return file.getPath();
                }

                @Override
                public boolean isWritable() {
                    return file.canWrite();
                }

                @Override
                public boolean isDirectory() {
                    return file.isDirectory();
                }

                @Override
                public boolean isValid() {
                    return file.exists();
                }

                @Override
                public VirtualFile getParent() {
                    return null;
                }

                @Override
                public VirtualFile[] getChildren() {
                    return new VirtualFile[0];
                }

                @NotNull
                @Override
                public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
                    return null;
                }

                @NotNull
                @Override
                public byte[] contentsToByteArray() throws IOException {
                    return new byte[0];
                }

                @Override
                public long getTimeStamp() {
                    return file.lastModified();
                }

                @Override
                public long getLength() {
                    return file.getTotalSpace();
                }

                @Override
                public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return null;
                }
            };
        }
    }
}
