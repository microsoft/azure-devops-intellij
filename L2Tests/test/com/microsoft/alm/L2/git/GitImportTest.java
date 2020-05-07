// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.git;

import com.intellij.openapi.util.io.FileUtil;
import com.microsoft.alm.L2.L2Test;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.git.ui.vcsimport.ImportModel;
import com.microsoft.alm.plugin.idea.git.ui.vcsimport.VsoImportPageModel;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import sun.security.util.Debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.UUID;


public class GitImportTest extends L2Test {

    public static final String GIT_FOLDER = ".git";
    public static final String README_FILE = "readme.md";

    @Override
    protected void tearDown() throws Exception {
        File gitDirectory = new File(myProjectPath, GIT_FOLDER);
        FileUtils.deleteDirectory(gitDirectory);

        super.tearDown();
    }

    @Test(timeout = 60000)
    public void testImport_VSO() throws Exception {
        Debug.println("Start", null);
        final ImportModel importModel = new ImportModel(myProject, null, null, false);
        final String repoName = generateUniqueName("testRepo");

        final File projectPath = new File(myProjectPath);

        // Add a file to the folder
        final String uniqueString = UUID.randomUUID().toString();
        Debug.println("uniqueString", uniqueString);
        final File readme = new File(projectPath, README_FILE);
        FileUtil.writeToFile(readme, uniqueString);
        Debug.println("readme path", readme.getAbsolutePath());

        // Mock the select files dialog that the model calls into
        mockSelectFilesDialog(Collections.singletonList(readme));

        // Get the model and set fields appropriately
        VsoImportPageModel model = (VsoImportPageModel) importModel.getVsoImportPageModel();
        // To avoid the test loading all the accounts for the user, we set the account server we care about
        model.setServerName(getServerUrl());
        model.setUserName(getUser());
        model.setRepositoryName(repoName);

        // Load the projects and choose the right one
        model.loadTeamProjects();
        while (model.isAuthenticating() || model.isLoading()) {
            Thread.sleep(100);
        }

        // Now we need to find ours and select it
        ServerContext selectedContext = null;
        final ServerContextTableModel table = model.getTableModel();
        int index = -1;
        for (int i = 0; i < table.getRowCount(); i++) {
            final ServerContext context = table.getServerContext(i);
            if (context.getTeamProjectReference().getName().equalsIgnoreCase(getTeamProject())) {
                index = i;
                selectedContext = context;
                break;
            }
        }

        // verify that we found it
        Assert.assertTrue(index >= 0);
        // select it
        model.getTableSelectionModel().setSelectionInterval(index, index);

        // delete the repo if it already exists
        removeServerGitRepository(selectedContext, repoName);

        // clone it
        model.importIntoRepository();

        // verify that it got imported
        final GitRepository repo = getServerGitRepository(selectedContext, repoName);
        Assert.assertNotNull(repo);
        final GitHttpClient gitClient = selectedContext.getGitHttpClient();
        final InputStream contentStream = gitClient.getItemContent(repo.getId(), README_FILE, null);
        final BufferedReader bufferedContent = new BufferedReader(new InputStreamReader(contentStream));
        final String content = bufferedContent.readLine();
        Assert.assertEquals(uniqueString, content);
        bufferedContent.close();
        contentStream.close();
        selectedContext.dispose();
    }
}
