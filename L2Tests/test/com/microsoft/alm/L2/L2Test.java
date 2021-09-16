// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.TestLoggerFactory;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ThrowableRunnable;
import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.MockAuthenticationProvider;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.MockRepositoryContextManager;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.RepositoryContextManager;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.events.ServerPollingManager;
import com.microsoft.alm.plugin.external.reactive.ReactiveTfvcClientHolder;
import com.microsoft.alm.plugin.idea.common.settings.MockTeamServicesSecrets;
import com.microsoft.alm.plugin.idea.common.settings.ServerContextState;
import com.microsoft.alm.plugin.idea.common.settings.SettingsState;
import com.microsoft.alm.plugin.idea.common.settings.TeamServicesSecrets;
import com.microsoft.alm.plugin.idea.common.settings.TeamServicesSettingsService;
import com.microsoft.alm.plugin.idea.common.ui.common.IdeaFileSelector;
import com.microsoft.alm.plugin.idea.common.ui.common.MockFileSelector;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.EULADialog;
import com.microsoft.alm.plugin.operations.OperationExecutor;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.commands.Git;
import git4idea.commands.GitHandler;
import git4idea.config.GitVcsSettings;
import git4idea.repo.GitRepositoryManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(JUnit38ClassRunner.class)
public abstract class L2Test extends UsefulTestCase {
    protected static final String ENV_UNIQUE_SUFFIX = "MSVSTS_INTELLIJ_UNIQUE_SUFFIX";
    static {
        Logger.setFactory(TestLoggerFactory.class);
    }

    @Rule
    public TestName name = new TestName();

    public L2Test() {
        // Set the name of the test in the base class
        setName(name.getMethodName());
    }

    // Context variables
    String serverUrl;
    String teamProject;
    String user;
    String pass;
    String repoUrl;
    String legacyRepoUrl;
    String tfExe;

    protected static final Logger LOG = Logger.getInstance(L2Test.class);

    protected Project myProject;
    protected VirtualFile myProjectRoot;
    protected String myProjectPath;
    protected GitRepositoryManager myGitRepositoryManager;
    protected GitVcsSettings myGitSettings;
    protected Git myGit;
    protected GitVcs myVcs;

    protected IdeaProjectTestFixture myProjectFixture;
    private String myTestStartedIndicator;

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

    /**
     * Returns name unique enough for test purposes. Adds value of {@link L2Test#ENV_UNIQUE_SUFFIX} to the passed
     * prefix.
     */
    protected String generateUniqueName(String prefix) {
        String suffix = System.getenv(ENV_UNIQUE_SUFFIX);
        if (StringUtils.isEmpty(suffix))
            return prefix + "." + SystemHelper.getComputerName();

        return prefix + "." + suffix;
    }

    private AuthenticationInfo getAuthenticationInfo() {
        final AuthenticationInfo authenticationInfo = new AuthenticationInfo(user, pass, serverUrl, user);
        return authenticationInfo;
    }

    public void mockRepositoryContextForProject(final String serverUriString) {
        URI serverUri = URI.create(serverUriString);
        RepositoryContext mockContext = RepositoryContext.createGitContext("/root/one", "repo1", "branch1", serverUri);
        MockRepositoryContextManager contextManager =
                (MockRepositoryContextManager) RepositoryContextManager.getInstance();
        contextManager.useContext(mockContext);
    }

    private void loadContext() {
        user = System.getenv("MSVSTS_INTELLIJ_VSO_USER");
        pass = System.getenv("MSVSTS_INTELLIJ_VSO_PASS");
        serverUrl = System.getenv("MSVSTS_INTELLIJ_VSO_SERVER_URL");
        teamProject = System.getenv("MSVSTS_INTELLIJ_VSO_TEAM_PROJECT");
        repoUrl = System.getenv("MSVSTS_INTELLIJ_VSO_GIT_REPO_URL");
        legacyRepoUrl = System.getenv("MSVSTS_INTELLIJ_VSO_LEGACY_GIT_REPO_URL");
        tfExe = System.getenv("MSVSTS_INTELLIJ_TF_EXE");

        final String message = "You must provide %s for the L2 tests through the following environment variable: %s (see Readme.md for more information)";
        Assert.assertFalse(String.format(message, "user", "MSVSTS_INTELLIJ_VSO_USER"), StringUtils.isEmpty(user));
        Assert.assertFalse(String.format(message, "pass", "MSVSTS_INTELLIJ_VSO_PASS"), StringUtils.isEmpty(pass));
        Assert.assertFalse(String.format(message, "serverUrl", "MSVSTS_INTELLIJ_VSO_SERVER_URL"), StringUtils.isEmpty(serverUrl));
        Assert.assertFalse(String.format(message, "teamProject", "MSVSTS_INTELLIJ_VSO_TEAM_PROJECT"), StringUtils.isEmpty(teamProject));
        Assert.assertFalse(String.format(message, "repoUrl", "MSVSTS_INTELLIJ_VSO_GIT_REPO_URL"), StringUtils.isEmpty(repoUrl));
        Assert.assertFalse(String.format(message, "legacyRepoUrl", "MSVSTS_INTELLIJ_VSO_LEGACY_GIT_REPO_URL"), StringUtils.isEmpty(legacyRepoUrl));
        Assert.assertFalse(String.format(message, "tfExe", "MSVSTS_INTELLIJ_TF_EXE"), StringUtils.isEmpty(tfExe));
    }

    protected void initializeTfEnvironment() {
        // Make sure that we can find the location of tf command line
        PluginServiceProvider.getInstance().getPropertyService().setProperty(PropertyService.PROP_TF_HOME, tfExe);
        AuthenticationInfo info = getAuthenticationInfo();

        // Make sure that the authentication info we found above is used
        MockAuthenticationProvider authenticationProvider =
                (MockAuthenticationProvider) VsoAuthenticationProvider.getInstance();
        authenticationProvider.setAuthenticationInfo(serverUrl, info);

        MockTeamServicesSecrets secrets = (MockTeamServicesSecrets) TeamServicesSecrets.getInstance();
        secrets.forceUseAuthenticationInfo(info);
        secrets.setIgnoreWrites(true);
    }

    protected void mockTeamServicesSettingsService() {
        // create a context to use as the "saved context" but remove it like you're starting from scratch
        final ServerContext context = ServerContextManager.getInstance().getUpdatedContext(getRepoUrl(), false);
        ServerContextManager.getInstance().remove(context.getKey());
        assertTrue(ServerContextManager.getInstance().getAllServerContexts().isEmpty());

        // create the needed objects to restore a previous state
        final ServerContextState completeContextStateActual = new ServerContextState(context);
        final ServerContextState appContextState = new ServerContextState();
        appContextState.type = ServerContext.Type.VSO_DEPLOYMENT;
        appContextState.uri = VsoAuthenticationProvider.VSO_AUTH_URL;
        appContextState.serverUri = VsoAuthenticationProvider.VSO_AUTH_URL;
        final ServerContextState completeContextStateExtra = new ServerContextState(context);
        completeContextStateExtra.uri = context.getUri().toString().concat("-Extra");
        completeContextStateExtra.serverUri = context.getServerUri().toString().concat("-Extra");
        final SettingsState settingsState = new SettingsState();
        settingsState.serverContexts = new ServerContextState[]{completeContextStateActual, appContextState, completeContextStateExtra};
        final TeamServicesSettingsService settingsService = new TeamServicesSettingsService();
        settingsService.loadState(settingsState);

        // mimic restoring the state since it's already happened
        for (final ServerContext storedContext : settingsService.restoreServerContexts()) {
            ServerContextManager.getInstance().add(storedContext, false);
        }
        assertEquals(3, ServerContextManager.getInstance().getAllServerContexts().size());
    }

    /**
     * This method can be called by test methods to mock the JetBrains SelectFilesDialog so that it returns the files
     * passed in here.
     *
     * @param filesToSelect are the file you want the dialog to return for the test
     */
    protected void mockSelectFilesDialog(final List<File> filesToSelect) {
        final List<VirtualFile> virtualFiles = new ArrayList<VirtualFile>();
        for (File f : filesToSelect) {
            final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(f);
            Assert.assertNotNull(vf);
            virtualFiles.add(vf);
        }

        MockFileSelector fileSelector = (MockFileSelector) IdeaFileSelector.getInstance();
        fileSelector.forceReturnSelectedFiles(virtualFiles);
    }

    @Override
    protected void setUp() throws Exception {
        loadContext();

        super.setUp();
        enableDebugLogging();

        try {
            myProjectFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getTestName(true)).getFixture();
            myProjectFixture.setUp();

            // Use the context info loaded earlier to setup the environment for TF work
            initializeTfEnvironment();

            myProject = myProjectFixture.getProject();
            myProjectPath = myProject.getBasePath();

            myGitSettings = GitVcsSettings.getInstance(myProject);
            myGitRepositoryManager = GitUtil.getRepositoryManager(myProject);
            myGit = ServiceManager.getService(Git.class);
            myVcs = ObjectUtils.assertNotNull(GitVcs.getInstance(myProject));
            myVcs.doActivate();

            EULADialog.acceptClientEula();
            EULADialog.acceptSdkEula();
            ServerPollingManager.getInstance().startPolling();
        } catch (Exception e) {
            tearDown();
            throw e;
        }
    }

    @Override
    @NotNull
    public String getTestName(boolean lowercaseFirstLetter) {
        String name = super.getTestName(lowercaseFirstLetter);
        name = StringUtil.shortenTextWithEllipsis(name.trim().replace(" ", "_"), 12, 6, "_");
        if (name.startsWith("_")) {
            name = name.substring(1);
        }
        return name;
    }

    private void disposeRegisteredServerContexts() {
        // remove and dispose all the contexts for the next test
        for (final ServerContext context : ServerContextManager.getInstance().getAllServerContexts()) {
            ServerContextManager.getInstance().remove(context.getKey());
            context.dispose();
        }
    }

    private void disposeJerseyPool() throws InterruptedException {
        // Since there's no API to dispose Jersey thread pool, let's just give it a bit more time to shutdown the pool.
        Thread.sleep(1000);
    }

    private void disposeReactiveClient() {
        ReactiveTfvcClientHolder.getInstance().dispose();
    }

    @Override
    protected void tearDown() throws Exception {
        disposeRegisteredServerContexts();
        disposeJerseyPool();
        disposeReactiveClient();
        try {
            /*if (myDialogManager != null) {
                myDialogManager.cleanup();
            }
            if (myVcsNotifier != null) {
                myVcsNotifier.cleanup();
            }*/
            ServerPollingManager.getInstance().stopPolling();
            OperationExecutor.getInstance().shutdown();
            if (myProjectFixture != null) {
                EdtTestUtil.runInEdtAndWait(new ThrowableRunnable<Throwable>() {
                    @Override
                    public void run() throws Throwable {
                        myProjectFixture.tearDown();
                    }
                });

            }
        } finally {
            try {
                String tempTestIndicator = myTestStartedIndicator;
                clearFields(this);
                myTestStartedIndicator = tempTestIndicator;
            } finally {
                super.tearDown();
            }
        }
    }

    private void enableDebugLogging() {
        List<String> commonCategories = new ArrayList<String>(Arrays.asList("#" + L2Test.class.getName(),
                "#" + GitHandler.class.getName(),
                GitHandler.class.getName()));
        commonCategories.addAll(getDebugLogCategories());
        //TestLoggerFactory.enableDebugLogging(myTestRootDisposable, ArrayUtil.toStringArray(commonCategories));
        myTestStartedIndicator = createTestStartedIndicator();
        LOG.info(myTestStartedIndicator);
    }

    /**
     * Test methods can call this method to get the Server GitRepository object given a project level server context and
     * the repository name.
     *
     * @param projectLevelContext
     * @param repositoryName
     * @return
     */
    protected GitRepository getServerGitRepository(final ServerContext projectLevelContext, final String repositoryName) {
        final GitHttpClient gitClient = projectLevelContext.getGitHttpClient();
        final List<GitRepository> repos = gitClient.getRepositories(projectLevelContext.getTeamProjectReference().getName());
        if (repos != null && repos.size() > 0) {
            for (GitRepository repo : repos) {
                if (repo.getName().equalsIgnoreCase(repositoryName)) {
                    return repo;
                }
            }
        }

        return null;
    }

    /**
     * Test methods can call this method to remove an existing repository from the server.
     * This is usually done at the beginning of a test that will be creating the repository.
     *
     * @param projectLevelContext
     * @param repositoryName
     */
    protected void removeServerGitRepository(final ServerContext projectLevelContext, final String repositoryName) {
        final GitHttpClient gitClient = projectLevelContext.getGitHttpClient();
        final GitRepository foundRepo = getServerGitRepository(projectLevelContext, repositoryName);
        if (foundRepo != null) {
            gitClient.deleteRepository(foundRepo.getId());
        }
    }

    @NotNull
    protected Collection<String> getDebugLogCategories() {
        return Collections.emptyList();
    }

    @SuppressWarnings("UnstableApiUsage") // TODO: Replace with defaultRunBare(ThrowableRunnable<Throwable>) after update to IDEA 2020.3
    @Override
    protected void defaultRunBare() throws Throwable {
        try {
            super.defaultRunBare();
        } catch (Throwable throwable) {
            if (myTestStartedIndicator != null) {
                TestLoggerFactory.dumpLogToStdout(myTestStartedIndicator);
            }
            throw throwable;
        }
    }

    @NotNull
    private String createTestStartedIndicator() {
        return "Starting " + getClass().getName() + "." + getTestName(false) + Math.random();
    }

    public static File createTempDirectory()
            throws IOException {
        final File temp;

        temp = File.createTempFile("temp", Long.toString(System.nanoTime())).getCanonicalFile();

        if (!temp.delete()) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!temp.mkdir()) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return temp;
    }
}
