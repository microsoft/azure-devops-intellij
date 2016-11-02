// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.TestLoggerFactory;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.util.ObjectUtils;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import git4idea.GitPlatformFacade;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.commands.Git;
import git4idea.commands.GitHandler;
import git4idea.config.GitVcsSettings;
import git4idea.repo.GitRepositoryManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sun.security.ssl.Debug;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VsoAuthenticationProvider.class})
// PowerMock and the javax.net.ssl.SSLContext class don't play well together. If you mock any static classes
// you have to PowerMockIgnore("javax.net.ssl.*") to avoid exceptions being thrown by SSLContext
@PowerMockIgnore({"javax.net.ssl.*", "javax.swing.*", "javax.security.*"})
public abstract class L2Test extends UsefulTestCase {
    static {
        Logger.setFactory(TestLoggerFactory.class);
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
    protected GitPlatformFacade myPlatformFacade;
    protected Git myGit;
    protected GitVcs myVcs;

    private IdeaProjectTestFixture myProjectFixture;
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

        // Make sure that the authentication info we found above is used
        final VsoAuthenticationProvider authenticationProvider = Mockito.mock(VsoAuthenticationProvider.class);
        PowerMockito.mockStatic(VsoAuthenticationProvider.class);
        when(VsoAuthenticationProvider.getInstance()).thenReturn(authenticationProvider);
        when(authenticationProvider.getAuthenticationInfo()).thenReturn(getAuthenticationInfo());
        when(authenticationProvider.isAuthenticated()).thenReturn(true);
    }

    @Before
    public void verifyEnvironment() {
        if (!"true".equalsIgnoreCase(System.getenv("MSVSTS_INTELLIJ_RUN_L2_TESTS"))) {
            Debug.println("***** SKIP ******", "Skipping this test because MSVSTS_INTELLIJ_RUN_L2_TESTS is not set to 'true'.");
            Assume.assumeTrue(false);
        }

        // Load context info from the environment
        loadContext();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        enableDebugLogging();

        try {
            myProjectFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getTestName(true)).getFixture();
            PluginManagerCore.loadDescriptors(null, new ArrayList<String>());
            myProjectFixture.setUp();

            // Use the context info loaded earlier to setup the environment for TF work
            initializeTfEnvironment();

            myProject = myProjectFixture.getProject();
            myProjectRoot = myProject.getBaseDir();
            myProjectPath = myProjectRoot.getPath();

            myGitSettings = GitVcsSettings.getInstance(myProject);
            myGitRepositoryManager = GitUtil.getRepositoryManager(myProject);
            myPlatformFacade = ServiceManager.getService(myProject, GitPlatformFacade.class);
            myGit = ServiceManager.getService(myProject, Git.class);
            myVcs = ObjectUtils.assertNotNull(GitVcs.getInstance(myProject));
            myVcs.doActivate();

            addSilently();
            removeSilently();
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

    @Override
    protected void tearDown() throws Exception {
        try {
            /*if (myDialogManager != null) {
                myDialogManager.cleanup();
            }
            if (myVcsNotifier != null) {
                myVcsNotifier.cleanup();
            }*/
            if (myProjectFixture != null) {
                //myProjectFixture.tearDown();
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

    @NotNull
    protected Collection<String> getDebugLogCategories() {
        return Collections.emptyList();
    }

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

    protected void doActionSilently(final VcsConfiguration.StandardConfirmation op) {
        //AbstractVcsTestCase.setStandardConfirmation(myProject, GitVcs.NAME, op, VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);
    }

    protected void updateChangeListManager() {
        ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
        VcsDirtyScopeManager.getInstance(myProject).markEverythingDirty();
        changeListManager.ensureUpToDate(false);
    }

    protected void addSilently() {
        doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    }

    protected void removeSilently() {
        doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
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
}