// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.checkout;

import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.IdeaLightweightTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.checkout.mocks.MockCheckoutPageModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.common.ui.common.mocks.MockObserver;
import com.microsoft.alm.plugin.idea.common.ui.common.mocks.MockServerContext;
import com.microsoft.alm.plugin.idea.git.ui.checkout.GitCheckoutModel;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;
import org.junit.Assert;

import java.io.File;
import java.net.URI;
import java.util.Observable;

public class CheckoutPageModelTest extends IdeaLightweightTest {
    /**
     * This is just a really basic test of the constructor(s)
     * It checks all the variants of the constructor(s)
     * It checks the values of the properties right after construction
     */
    public void testConstructor() {

        // all combinations should succeed
        CheckoutPageModel pm1 = new MockCheckoutPageModel(null, ServerContextTableModel.VSO_GIT_REPO_COLUMNS);
        CheckoutPageModel pm2 = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.TFS_GIT_REPO_COLUMNS);
        CheckoutPageModel pm3 = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), new ServerContextTableModel.Column[0]);

        // Make sure default values are set correctly
        CheckoutPageModel pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.VSO_GIT_REPO_COLUMNS);
        Assert.assertEquals(CheckoutPageModel.DEFAULT_SOURCE_PATH, pm.getParentDirectory());
        Assert.assertTrue(pm.getTableModel() != null);
        Assert.assertTrue(pm.getTableSelectionModel() != null);
    }

    /**
     * This test makes sure that all setters on the page model notify the observer if and only if the value changes.
     */
    public void testObservable() {
        final CheckoutModel m = new CheckoutModel(null, null, new GitCheckoutModel());
        CheckoutPageModel pm = new MockCheckoutPageModel(m, ServerContextTableModel.VSO_GIT_REPO_COLUMNS);
        MockObserver observer = new MockObserver((Observable) pm);
        MockObserver observer1 = new MockObserver(m);

        // Change connected and make sure that we get notified
        pm.setConnected(true);
        observer.assertAndClearLastUpdate((Observable) pm, CheckoutPageModel.PROP_CONNECTED);
        Assert.assertEquals(true, pm.isConnected());
        // Set connected to the exact same value and make sure we don't get notified
        pm.setConnected(true);
        observer.assertAndClearLastUpdate(null, null);
        Assert.assertEquals(true, pm.isConnected());

        // Change directoryName and make sure that we get notified
        String value = "newDirName";
        pm.setDirectoryName(value);
        observer.assertAndClearLastUpdate((Observable) pm, CheckoutPageModel.PROP_DIRECTORY_NAME);
        Assert.assertEquals(value, pm.getDirectoryName());
        // Set directoryName to the exact same value and make sure we don't get notified
        pm.setDirectoryName(value);
        observer.assertAndClearLastUpdate(null, null);
        Assert.assertEquals(value, pm.getDirectoryName());

        // Change Loading and make sure that we get notified
        pm.setLoading(true);
        observer.assertAndClearLastUpdate((Observable) pm, CheckoutPageModel.PROP_LOADING);
        Assert.assertEquals(true, pm.isLoading());
        // Set Loading to the exact same value and make sure we don't get notified
        pm.setLoading(true);
        observer.assertAndClearLastUpdate(null, null);
        Assert.assertEquals(true, pm.isLoading());

        // Change Authenticating and make sure that we get notified
        pm.setAuthenticating(true);
        observer.assertAndClearLastUpdate((Observable) pm, CheckoutPageModel.PROP_AUTHENTICATING);
        Assert.assertEquals(true, pm.isAuthenticating());
        // Set Loading to the exact same value and make sure we don't get notified
        pm.setAuthenticating(true);
        observer.assertAndClearLastUpdate(null, null);
        Assert.assertEquals(true, pm.isAuthenticating());

        // Change parent dir and make sure that we get notified
        value = "newParentDir";
        pm.setParentDirectory(value);
        observer.assertAndClearLastUpdate((Observable) pm, CheckoutPageModel.PROP_PARENT_DIR);
        Assert.assertEquals(value, pm.getParentDirectory());
        // Set parent dir to the exact same value and make sure we don't get notified
        pm.setParentDirectory(value);
        observer.assertAndClearLastUpdate(null, null);
        Assert.assertEquals(value, pm.getParentDirectory());

        // Change RepositoryFilter and make sure that we get notified
        value = "newRepoFilter";
        pm.setRepositoryFilter(value);
        observer.assertAndClearLastUpdate((Observable) pm, CheckoutPageModel.PROP_REPO_FILTER);
        Assert.assertEquals(value, pm.getRepositoryFilter());
        // Set RepositoryFilter to the exact same value and make sure we don't get notified
        pm.setRepositoryFilter(value);
        observer.assertAndClearLastUpdate(null, null);
        Assert.assertEquals(value, pm.getRepositoryFilter());

        // Change ServerName and make sure that we get notified
        value = "http://newServerName:8080/tfs";
        pm.setServerName(value);
        observer.assertAndClearLastUpdate((Observable) pm, CheckoutPageModel.PROP_SERVER_NAME);
        Assert.assertEquals(value, pm.getServerName());
        // Set ServerName to the exact same value and make sure we don't get notified
        pm.setServerName(value);
        observer.assertAndClearLastUpdate(null, null);
        Assert.assertEquals(value, pm.getServerName());

        // Change userName and make sure that we get notified
        value = "newUserName";
        pm.setUserName(value);
        observer.assertAndClearLastUpdate((Observable) pm, CheckoutPageModel.PROP_USER_NAME);
        Assert.assertEquals(value, pm.getUserName());
        // Set userName to the exact same value and make sure we don't get notified
        pm.setUserName(value);
        observer.assertAndClearLastUpdate(null, null);
        Assert.assertEquals(value, pm.getUserName());

        // Add an error and make sure that we get notified
        value = "an error occurred";
        pm.addError(ModelValidationInfo.createWithMessage(value));
        observer1.assertAndClearLastUpdate(m, CheckoutModel.PROP_ERRORS);
        Assert.assertEquals(value, pm.getErrors().get(0).getValidationMessage());
        Assert.assertEquals(true, pm.hasErrors());

        // Clear errors and make sure that we get notified
        pm.clearErrors();
        observer1.assertAndClearLastUpdate(m, CheckoutModel.PROP_ERRORS);
        Assert.assertEquals(0, pm.getErrors().size());
        Assert.assertEquals(false, pm.hasErrors());
        // Clear errors again and make sure we don't get notified
        pm.clearErrors();
        observer.assertAndClearLastUpdate(null, null);
        Assert.assertEquals(0, pm.getErrors().size());
        Assert.assertEquals(false, pm.hasErrors());
    }

    /**
     * This test makes sure that properties are validated and in the correct order
     */
    public void testValidate() {
        final CheckoutModel m = new CheckoutModel(null, null, new GitCheckoutModel());
        CheckoutPageModel pm = new MockCheckoutPageModel(m, ServerContextTableModel.VSO_GIT_REPO_COLUMNS);

        // The model is empty right now. So, all properties are missing.
        // Will call validate and fill in each property as we go
        ModelValidationInfo validationInfo = pm.validate();
        Assert.assertTrue(validationInfo != null);
        Assert.assertEquals(CheckoutPageModel.PROP_CONNECTED, validationInfo.getValidationSource());
        pm.setConnected(true);

        // clear the Parent dir that is defaulted for us
        pm.setParentDirectory("");

        // Get the root folder
        final String root = System.getProperty("user.dir");

        validationInfo = pm.validate();
        Assert.assertTrue(validationInfo != null);
        Assert.assertEquals(CheckoutPageModel.PROP_PARENT_DIR, validationInfo.getValidationSource());
        pm.setParentDirectory(root + File.separator + "doesntExist");

        validationInfo = pm.validate();
        Assert.assertTrue(validationInfo != null);
        Assert.assertEquals(CheckoutPageModel.PROP_PARENT_DIR, validationInfo.getValidationSource());
        pm.setParentDirectory(root);

        validationInfo = pm.validate();
        Assert.assertTrue(validationInfo != null);
        Assert.assertEquals(CheckoutPageModel.PROP_REPO_TABLE, validationInfo.getValidationSource());
        // Add repo1 to the table and select it
        ServerContext context = getServerContext("https://server.com", "coll", "proj", "repo1", "remoteUrl");
        ((MockCheckoutPageModel) pm).addContext(context);
        Assert.assertEquals(1, pm.getTableModel().getRowCount());
        pm.getTableSelectionModel().setSelectionInterval(0, 0);

        // all fields are filled in...
        validationInfo = pm.validate();
        Assert.assertTrue(validationInfo == null);

        // now clear the default dirname so we can check that validation
        pm.setDirectoryName("");
        validationInfo = pm.validate();
        Assert.assertTrue(validationInfo != null);
        Assert.assertEquals(CheckoutPageModel.PROP_DIRECTORY_NAME, validationInfo.getValidationSource());

        // finally, fill in the dirname with an existing folder name
        final File rootFolder = new File(root);
        final String existingDirName = getFirstDir(rootFolder);
        pm.setDirectoryName(existingDirName);
        validationInfo = pm.validate();
        Assert.assertTrue(validationInfo != null);
        Assert.assertEquals(CheckoutPageModel.PROP_DIRECTORY_NAME, validationInfo.getValidationSource());
    }

    private ServerContext getServerContext(String serverName, String collectionName, String projectName, String repoName, String repoUrl) {
        TeamProjectCollectionReference collection = new TeamProjectCollectionReference();
        collection.setName(collectionName);
        TeamProjectReference teamProject1 = new TeamProjectReference();
        teamProject1.setName(projectName);
        GitRepository repo1 = new GitRepository();
        repo1.setName(repoName);
        repo1.setProjectReference(teamProject1);
        repo1.setRemoteUrl(repoUrl);
        return new MockServerContext(ServerContext.Type.TFS, null, URI.create(serverName), collection, teamProject1, repo1);
    }

    private String getFirstDir(final File root) {
        for (File f : root.listFiles()) {
            if (f.isDirectory()) {
                return f.getName();
            }
        }

        return null;
    }

    /**
     * This checks that the columns in the table are correct for both VSO and TFS table models
     */
    public void testGetColumnName() {
        // First test the VSO columns
        CheckoutPageModel pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.VSO_GIT_REPO_COLUMNS);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_SERVER_CONTEXT_TABLE_REPO_COLUMN), pm.getTableModel().getColumnName(0));
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_SERVER_CONTEXT_TABLE_PROJECT_COLUMN), pm.getTableModel().getColumnName(1));
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_SERVER_CONTEXT_TABLE_ACCOUNT_COLUMN), pm.getTableModel().getColumnName(2));

        // Now check the TFS columns
        pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.TFS_GIT_REPO_COLUMNS);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_SERVER_CONTEXT_TABLE_REPO_COLUMN), pm.getTableModel().getColumnName(0));
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_SERVER_CONTEXT_TABLE_PROJECT_COLUMN), pm.getTableModel().getColumnName(1));
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_SERVER_CONTEXT_TABLE_COLLECTION_COLUMN), pm.getTableModel().getColumnName(2));
    }

    /**
     * This is just a simple check to make sure that the gotoLink method handles null and empty string.
     */
    public void testGotoLink() {
        CheckoutPageModel pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.VSO_GIT_REPO_COLUMNS);
        pm.gotoLink(null);
        pm.gotoLink("");
    }

    /**
     * This test makes sure that the methods associated with errors behave correctly.
     */
    public void testErrors() {
        CheckoutPageModel pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.VSO_GIT_REPO_COLUMNS);
        Assert.assertEquals(0, pm.getErrors().size());
        Assert.assertEquals(false, pm.hasErrors());

        String error1 = "error1";
        pm.addError(ModelValidationInfo.createWithMessage(error1));
        Assert.assertEquals(error1, pm.getErrors().get(0).getValidationMessage());
        Assert.assertEquals(1, pm.getErrors().size());
        Assert.assertEquals(true, pm.hasErrors());

        String error2 = "error2";
        pm.addError(ModelValidationInfo.createWithMessage(error2));
        Assert.assertEquals(error1, pm.getErrors().get(0).getValidationMessage());
        Assert.assertEquals(error2, pm.getErrors().get(1).getValidationMessage());
        Assert.assertEquals(2, pm.getErrors().size());
        Assert.assertEquals(true, pm.hasErrors());

        String error3 = "error3";
        pm.addError(ModelValidationInfo.createWithMessage(error3));
        Assert.assertEquals(error1, pm.getErrors().get(0).getValidationMessage());
        Assert.assertEquals(error2, pm.getErrors().get(1).getValidationMessage());
        Assert.assertEquals(error3, pm.getErrors().get(2).getValidationMessage());
        Assert.assertEquals(3, pm.getErrors().size());
        Assert.assertEquals(true, pm.hasErrors());

        // Make sure that the list returned is immutable
        try {
            pm.getErrors().add(ModelValidationInfo.createWithMessage("foo"));
            Assert.fail("Error list returned from model is modifiable.");
        } catch (UnsupportedOperationException ex) {
            //list is immutable as expected
        }

        pm.clearErrors();
        Assert.assertEquals(0, pm.getErrors().size());
        Assert.assertEquals(false, pm.hasErrors());
    }

    /**
     * This tests the behavior of the ServerName setter and getter.
     * By default the setter will actually change the value passed in if it is in a simplified form.
     */
    public void testServerName() {
        CheckoutPageModel pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.VSO_GIT_REPO_COLUMNS);
        Assert.assertEquals("", pm.getServerName());

        // full server url
        String value = "http://newServerName/tfs";
        pm.setServerName(value);
        Assert.assertEquals(value, pm.getServerName());

        // just server name
        value = "newServerName";
        pm.setServerName(value);
        Assert.assertEquals(String.format(CheckoutPageModel.DEFAULT_SERVER_FORMAT, value), pm.getServerName());
    }

    /**
     * This test makes sure that the table model returns the expected values when in VSO page mode.
     */
    public void testTableModel_VSO() {
        final String serverName = "http://server/tfs";
        final String collName = "collection1";
        final String projectName1 = "project1";
        final String projectName2 = "project2";

        // Construct the model and verify the number of columns
        CheckoutPageModel pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.VSO_GIT_REPO_COLUMNS);
        Assert.assertEquals(3, pm.getTableModel().getColumnCount());

        // Create the mock context and other objects
        ServerContext context1 = getServerContext(serverName, collName, projectName1, "repo1", "");
        ServerContext context2 = getServerContext(serverName, collName, projectName2, "repo2", "");

        // Add repo1 to the table
        ((MockCheckoutPageModel) pm).addContext(context1);
        Assert.assertEquals(1, pm.getTableModel().getRowCount());
        // Add repo2 to the table
        ((MockCheckoutPageModel) pm).addContext(context2);
        Assert.assertEquals(2, pm.getTableModel().getRowCount());

        // Verify that the rows have the correct data
        final String hostName = context1.getUri().getHost();
        Assert.assertEquals("repo1", pm.getTableModel().getValueAt(0, 0));
        Assert.assertEquals(projectName1, pm.getTableModel().getValueAt(0, 1));
        Assert.assertEquals(hostName, pm.getTableModel().getValueAt(0, 2));
        Assert.assertEquals("repo2", pm.getTableModel().getValueAt(1, 0));
        Assert.assertEquals(projectName2, pm.getTableModel().getValueAt(1, 1));
        Assert.assertEquals(hostName, pm.getTableModel().getValueAt(1, 2));

        // Clear the table and verify that it is empty
        ((MockCheckoutPageModel) pm).clearContexts();
        Assert.assertEquals(0, pm.getTableModel().getRowCount());
    }

    /**
     * This test makes sure that the table model returns the expected values when in TFS page mode.
     */
    public void testTableModel_TFS() {
        final String serverName = "http://server/tfs";
        final String collName = "collection1";
        final String projectName1 = "project1";
        final String projectName2 = "project2";

        // Construct the model and verify the number of columns
        CheckoutPageModel pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.TFS_GIT_REPO_COLUMNS);
        Assert.assertEquals(3, pm.getTableModel().getColumnCount());

        // Create the mock context and other objects
        ServerContext context1 = getServerContext(serverName, collName, projectName1, "repo1", "");
        ServerContext context2 = getServerContext(serverName, collName, projectName2, "repo2", "");

        // Add repo1 to the table
        ((MockCheckoutPageModel) pm).addContext(context1);
        Assert.assertEquals(1, pm.getTableModel().getRowCount());
        // Add repo2 to the table
        ((MockCheckoutPageModel) pm).addContext(context2);
        Assert.assertEquals(2, pm.getTableModel().getRowCount());

        // Verify that the rows have the correct data
        Assert.assertEquals("repo1", pm.getTableModel().getValueAt(0, 0));
        Assert.assertEquals(projectName1, pm.getTableModel().getValueAt(0, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(0, 2));
        Assert.assertEquals("repo2", pm.getTableModel().getValueAt(1, 0));
        Assert.assertEquals(projectName2, pm.getTableModel().getValueAt(1, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(1, 2));

        // Clear the table and verify that it is empty
        ((MockCheckoutPageModel) pm).clearContexts();
        Assert.assertEquals(0, pm.getTableModel().getRowCount());
    }

    /**
     * This test makes sure that the table model properly keeps track of selection.
     */
    public void testTableModel_selection() {
        final String serverName = "http://server/tfs";
        final String collName = "collection1";
        final String projectName1 = "project1";
        final String projectName2 = "project2";
        final String repoName1 = "repo1";
        final String repoUrl1 = "repoUrl1";
        final String repoName2 = "repo2";
        final String repoUrl2 = "repoUrl2";

        // Construct the model and verify the number of columns
        CheckoutPageModel pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.TFS_GIT_REPO_COLUMNS);
        Assert.assertEquals(3, pm.getTableModel().getColumnCount());

        // Create the mock context and other objects
        ServerContext context1 = getServerContext(serverName, collName, projectName1, repoName1, repoUrl1);
        ServerContext context2 = getServerContext(serverName, collName, projectName2, repoName2, repoUrl2);

        // Add repo1 to the table
        ((MockCheckoutPageModel) pm).addContext(context1);
        Assert.assertEquals(1, pm.getTableModel().getRowCount());
        // Add repo2 to the table
        ((MockCheckoutPageModel) pm).addContext(context2);
        Assert.assertEquals(2, pm.getTableModel().getRowCount());

        // Verify that the rows have the correct data
        Assert.assertEquals(repoName1, pm.getTableModel().getValueAt(0, 0));
        Assert.assertEquals(projectName1, pm.getTableModel().getValueAt(0, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(0, 2));
        Assert.assertEquals(repoName2, pm.getTableModel().getValueAt(1, 0));
        Assert.assertEquals(projectName2, pm.getTableModel().getValueAt(1, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(1, 2));

        // Select the first repo and verify
        pm.getTableSelectionModel().setSelectionInterval(0, 0);
        Assert.assertEquals(repoUrl1, ((MockCheckoutPageModel) pm).getSelectedContext().getGitRepository().getRemoteUrl());

        // Select the second repo and verify
        pm.getTableSelectionModel().setSelectionInterval(1, 1);
        Assert.assertEquals(repoUrl2, ((MockCheckoutPageModel) pm).getSelectedContext().getGitRepository().getRemoteUrl());

        // Clear the table and verify that it is empty
        ((MockCheckoutPageModel) pm).clearContexts();
        Assert.assertEquals(0, pm.getTableModel().getRowCount());
    }

    /**
     * This test makes sure that the table model keeps the repos sorted by name as they are added.
     */
    public void testTableModel_initialSort() {
        final String serverName = "http://server/tfs";
        final String collName = "collection1";
        final String projectName1 = "project1";
        final String projectName2 = "project2";
        final String repoName1 = "repo1";
        final String repoName2 = "repo2";
        final String repoName3 = "repo3";
        final String repoName4 = "repo4";

        // Construct the model and verify the number of columns
        CheckoutPageModel pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.VSO_GIT_REPO_COLUMNS);
        Assert.assertEquals(3, pm.getTableModel().getColumnCount());

        // Create the mock context and other objects
        ServerContext context1 = getServerContext(serverName, collName, projectName1, repoName1, "");
        ServerContext context2 = getServerContext(serverName, collName, projectName2, repoName2, "");
        ServerContext context3 = getServerContext(serverName, collName, projectName1, repoName3, "");
        ServerContext context4 = getServerContext(serverName, collName, projectName2, repoName4, "");

        // Add repos to the table out of order (2,1,4,3)
        ((MockCheckoutPageModel) pm).addContext(context2);
        ((MockCheckoutPageModel) pm).addContext(context1);
        ((MockCheckoutPageModel) pm).addContext(context4);
        ((MockCheckoutPageModel) pm).addContext(context3);

        // Verify that the rows are sorted
        Assert.assertEquals(4, pm.getTableModel().getRowCount());
        Assert.assertEquals(repoName1, pm.getTableModel().getValueAt(0, 0));
        Assert.assertEquals(repoName2, pm.getTableModel().getValueAt(1, 0));
        Assert.assertEquals(repoName3, pm.getTableModel().getValueAt(2, 0));
        Assert.assertEquals(repoName4, pm.getTableModel().getValueAt(3, 0));

        // Clear the table and verify that it is empty
        ((MockCheckoutPageModel) pm).clearContexts();
        Assert.assertEquals(0, pm.getTableModel().getRowCount());
    }

    /**
     * This test makes sure that the table model responds correctly to the repository filter being changed.
     */
    public void testTableModel_filtering() {
        final String serverName = "http://server/tfs";
        final String collName = "collection1";
        final String projectName1 = "project1";
        final String projectName2 = "project2";
        final String repoName1 = "repo1";
        final String repoName2 = "repo2";

        // Construct the model and verify the number of columns
        CheckoutPageModel pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.TFS_GIT_REPO_COLUMNS);
        Assert.assertEquals(3, pm.getTableModel().getColumnCount());

        // Create the mock context and other objects
        ServerContext context1 = getServerContext(serverName, collName, projectName1, repoName1, "");
        ServerContext context2 = getServerContext(serverName, collName, projectName2, repoName2, "");

        // Add repo1 to the table
        ((MockCheckoutPageModel) pm).addContext(context1);
        Assert.assertEquals(1, pm.getTableModel().getRowCount());
        // Add repo2 to the table
        ((MockCheckoutPageModel) pm).addContext(context2);
        Assert.assertEquals(2, pm.getTableModel().getRowCount());

        // Verify that the rows have the correct data
        Assert.assertEquals(repoName1, pm.getTableModel().getValueAt(0, 0));
        Assert.assertEquals(projectName1, pm.getTableModel().getValueAt(0, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(0, 2));
        Assert.assertEquals(repoName2, pm.getTableModel().getValueAt(1, 0));
        Assert.assertEquals(projectName2, pm.getTableModel().getValueAt(1, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(1, 2));

        // Add a filter for repo1 and verify the data
        pm.setRepositoryFilter(repoName1);
        Assert.assertEquals(1, pm.getTableModel().getRowCount());
        Assert.assertEquals(repoName1, pm.getTableModel().getValueAt(0, 0));
        Assert.assertEquals(projectName1, pm.getTableModel().getValueAt(0, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(0, 2));

        // Add a filter for REPO1 and verify the data
        pm.setRepositoryFilter(repoName1.toUpperCase());
        Assert.assertEquals(1, pm.getTableModel().getRowCount());
        Assert.assertEquals(repoName1, pm.getTableModel().getValueAt(0, 0));
        Assert.assertEquals(projectName1, pm.getTableModel().getValueAt(0, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(0, 2));

        // Add a filter for repo2 and verify the data
        pm.setRepositoryFilter(repoName2);
        Assert.assertEquals(1, pm.getTableModel().getRowCount());
        Assert.assertEquals(repoName2, pm.getTableModel().getValueAt(0, 0));
        Assert.assertEquals(projectName2, pm.getTableModel().getValueAt(0, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(0, 2));

        // Add a filter for "no_matches" and verify the data
        pm.setRepositoryFilter("no_matches");
        Assert.assertEquals(0, pm.getTableModel().getRowCount());

        // Clear the filter and check the rows again
        pm.setRepositoryFilter("");
        Assert.assertEquals(2, pm.getTableModel().getRowCount());
        Assert.assertEquals(repoName1, pm.getTableModel().getValueAt(0, 0));
        Assert.assertEquals(projectName1, pm.getTableModel().getValueAt(0, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(0, 2));
        Assert.assertEquals(repoName2, pm.getTableModel().getValueAt(1, 0));
        Assert.assertEquals(projectName2, pm.getTableModel().getValueAt(1, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(1, 2));

        // Make sure null doesn't cause any problems
        pm.setRepositoryFilter(null);
        Assert.assertEquals(2, pm.getTableModel().getRowCount());

        // Clear the table and verify that it is empty
        ((MockCheckoutPageModel) pm).clearContexts();
        Assert.assertEquals(0, pm.getTableModel().getRowCount());
    }

    /**
     * This test makes sure that the table model responds correctly to the repository filter being set
     * before the rows are added.
     */
    public void testTableModel_filterFirst() {
        final String serverName = "http://server/tfs";
        final String collName = "collection1";
        final String projectName1 = "project1";
        final String projectName2 = "project2";
        final String repoName1 = "repo1";
        final String repoName2 = "repo2";

        // Construct the model and verify the number of columns
        MockCheckoutPageModel pm = new MockCheckoutPageModel(new CheckoutModel(null, null, new GitCheckoutModel()), ServerContextTableModel.TFS_GIT_REPO_COLUMNS);
        Assert.assertEquals(3, pm.getTableModel().getColumnCount());

        // Create the mock context and other objects
        ServerContext context1 = getServerContext(serverName, collName, projectName1, repoName1, "");
        ServerContext context2 = getServerContext(serverName, collName, projectName2, repoName2, "");

        // Add a filter for repo1 and verify the data
        pm.setRepositoryFilter(repoName1);

        // Add repo1 and repo2 to the table (repo2 is filtered out)
        pm.addContext(context1);
        pm.addContext(context2);

        // Verify that the rows have the correct data
        Assert.assertEquals(1, pm.getTableModel().getRowCount());
        Assert.assertEquals(repoName1, pm.getTableModel().getValueAt(0, 0));
        Assert.assertEquals(projectName1, pm.getTableModel().getValueAt(0, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(0, 2));

        // Clear the filter and check the rows again
        pm.setRepositoryFilter("");
        Assert.assertEquals(2, pm.getTableModel().getRowCount());
        Assert.assertEquals(repoName1, pm.getTableModel().getValueAt(0, 0));
        Assert.assertEquals(projectName1, pm.getTableModel().getValueAt(0, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(0, 2));
        Assert.assertEquals(repoName2, pm.getTableModel().getValueAt(1, 0));
        Assert.assertEquals(projectName2, pm.getTableModel().getValueAt(1, 1));
        Assert.assertEquals(collName, pm.getTableModel().getValueAt(1, 2));

        // Clear the table and verify that it is empty
        pm.clearContexts();
        Assert.assertEquals(0, pm.getTableModel().getRowCount());
    }

}
