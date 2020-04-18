// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.vcsimport;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.IdeaLightweightTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.ui.common.PageModel;
import com.microsoft.alm.plugin.idea.common.ui.common.helpers.ServerContextHelper;
import com.microsoft.alm.plugin.idea.common.ui.common.mocks.MockObserver;
import com.microsoft.alm.plugin.idea.git.ui.vcsimport.mocks.MockImportPageModel;
import org.junit.Assert;

import java.util.Observable;

public class ImportPageModelTest extends IdeaLightweightTest {

    /**
     * Basic test for the constructors
     * It checks the values of properties are initialized correctly
     */
    public void testConstructor() {
        //VSO
        final ImportPageModel pageModelVso = new MockImportPageModel(null, false);
        Assert.assertEquals("1st column on the team project table not setup correctly",
                TfPluginBundle.message(TfPluginBundle.KEY_SERVER_CONTEXT_TABLE_PROJECT_COLUMN), pageModelVso.getTableModel().getColumnName(0));
        Assert.assertEquals("2nd column on the team project table not setup correctly",
                TfPluginBundle.message(TfPluginBundle.KEY_SERVER_CONTEXT_TABLE_ACCOUNT_COLUMN), pageModelVso.getTableModel().getColumnName(1));


        //TFS
        final ImportPageModel pageModelTfs = new MockImportPageModel(null, true);
        Assert.assertEquals("1st column on the team project table not setup correctly",
                TfPluginBundle.message(TfPluginBundle.KEY_SERVER_CONTEXT_TABLE_PROJECT_COLUMN), pageModelVso.getTableModel().getColumnName(0));
        Assert.assertEquals("2nd column on the team project table not setup correctly",
                TfPluginBundle.message(TfPluginBundle.KEY_SERVER_CONTEXT_TABLE_COLLECTION_COLUMN), pageModelTfs.getTableModel().getColumnName(1));

        //Verify defaults
        final ImportPageModel pageModel = new MockImportPageModel(new ImportModel(null), false);
        Assert.assertEquals("Repository name not empty as expected", "",
                pageModel.getRepositoryName());
        Assert.assertEquals("Team projects filter not empty as expected", "",
                pageModel.getTeamProjectFilter());
        Assert.assertNotNull("Team projects table model not initialized",
                pageModel.getTableModel());
        Assert.assertNotNull("Team projects table selection model not initialized",
                pageModel.getTableSelectionModel());

    }

    public void testObservable() {
        final ImportModel model = new ImportModel(null);
        final MockObserver observerModel = new MockObserver(model);
        final ImportPageModel pageModel = new MockImportPageModel(model, false);
        final MockObserver observerPageModel = new MockObserver((Observable) pageModel);

        //Verify connected property
        pageModel.setConnected(true);
        observerPageModel.assertAndClearLastUpdate((Observable) pageModel, ImportPageModel.PROP_CONNECTED);
        Assert.assertTrue("Import page mode is not connected as expected",
                pageModel.isConnected());
        //Verify observer is not notified when connected property does not change
        pageModel.setConnected(true);
        observerPageModel.assertAndClearLastUpdate(null, null);
        Assert.assertTrue("Import page mode is not connected as expected",
                pageModel.isConnected());

        //Verify loading property
        pageModel.setLoading(true);
        observerPageModel.assertAndClearLastUpdate((Observable) pageModel, ImportPageModel.PROP_LOADING);
        Assert.assertTrue("Import page mode is not loading as expected",
                pageModel.isLoading());
        //Verify observer is not notified when loading property does not change
        pageModel.setLoading(true);
        observerPageModel.assertAndClearLastUpdate(null, null);
        Assert.assertTrue("Import page mode is not loading as expected",
                pageModel.isLoading());

        //Verify authenticating property
        pageModel.setAuthenticating(true);
        observerPageModel.assertAndClearLastUpdate((Observable) pageModel, ImportPageModel.PROP_AUTHENTICATING);
        Assert.assertTrue("Import page mode is not authenticating as expected",
                pageModel.isAuthenticating());
        //Verify observer is not notified when authenticating property does not change
        pageModel.setAuthenticating(true);
        observerPageModel.assertAndClearLastUpdate(null, null);
        Assert.assertTrue("Import page mode is not authenticating as expected",
                pageModel.isAuthenticating());

        //Change new repository name
        pageModel.setRepositoryName("Repo To Create");
        observerPageModel.assertAndClearLastUpdate((Observable) pageModel, ImportPageModel.PROP_REPO_NAME);
        Assert.assertEquals("Repository name not as expected",
                "Repo To Create", pageModel.getRepositoryName());
        //Verify observer is not notified when repo name does not change
        pageModel.setRepositoryName("Repo To Create");
        observerPageModel.assertAndClearLastUpdate(null, null);
        Assert.assertEquals("Repository name not as expected",
                "Repo To Create", pageModel.getRepositoryName());

        //Change team project filter
        pageModel.setTeamProjectFilter("Myproject");
        observerPageModel.assertAndClearLastUpdate((Observable) pageModel, ImportPageModel.PROP_PROJECT_FILTER);
        Assert.assertEquals("Teamproject filter not as expected",
                "Myproject", pageModel.getTeamProjectFilter());
        //Verify observer is not notified when team project filter does not change
        pageModel.setTeamProjectFilter("Myproject");
        observerPageModel.assertAndClearLastUpdate(null, null);
        Assert.assertEquals("Teamproject filter not as expected",
                "Myproject", pageModel.getTeamProjectFilter());

        //Change server name
        pageModel.setServerName("http://Myserver");
        observerPageModel.assertAndClearLastUpdate((Observable) pageModel, ImportPageModel.PROP_SERVER_NAME);
        Assert.assertEquals("server name not as expected",
                "http://Myserver", pageModel.getServerName());
        //Verify observer is not notified when server name does not change
        pageModel.setServerName("http://Myserver");
        observerPageModel.assertAndClearLastUpdate(null, null);
        Assert.assertEquals("server name not as expected",
                "http://Myserver", pageModel.getServerName());

        //Change user name
        pageModel.setUserName("My user");
        observerPageModel.assertAndClearLastUpdate((Observable) pageModel, ImportPageModel.PROP_USER_NAME);
        Assert.assertEquals("user name not as expected",
                "My user", pageModel.getUserName());
        //Verify observer is not notified when user name does not change
        pageModel.setUserName("My user");
        observerPageModel.assertAndClearLastUpdate(null, null);
        Assert.assertEquals("user name not as expected",
                "My user", pageModel.getUserName());

        //add an error, verify model observer is notified
        final ModelValidationInfo error = ModelValidationInfo.createWithMessage("error on page model");
        pageModel.addError(error);
        observerModel.assertAndClearLastUpdate(model, PageModel.PROP_ERRORS);
        Assert.assertTrue("Page model does not show errors as expected",
                pageModel.hasErrors());
        Assert.assertEquals("Error on page model not as expected",
                error, pageModel.getErrors().get(0));

        //clear errors
        pageModel.clearErrors();
        observerModel.assertAndClearLastUpdate(model, PageModel.PROP_ERRORS);
        Assert.assertEquals("Unexpected errors found on the import model",
                0, pageModel.getErrors().size());
        //verify clearing errors again does not notify observer
        pageModel.clearErrors();
        observerModel.assertAndClearLastUpdate(null, null);
        Assert.assertEquals("Unexpected errors found on the import model",
                0, pageModel.getErrors().size());

    }

    /**
     * This test verifies properties on the pageModel are validation in correct order
     */
    public void testValidate() {
        final ImportModel model = new ImportModel(null);
        final ImportPageModel pageModel = new MockImportPageModel(model, false);

        //model is empty, so all properties are missing
        //we will add each property and validate as we go
        ModelValidationInfo info = pageModel.validate();
        Assert.assertNotNull("No validation errors found on empty import page model",
                info);

        //verify error when user is not logged in or connected
        Assert.assertEquals(ImportPageModel.PROP_CONNECTED, info.getValidationSource());
        Assert.assertEquals(TfPluginBundle.KEY_LOGIN_FORM_ERRORS_NOT_CONNECTED, info.getValidationMessageKey());

        pageModel.setConnected(true);

        //verify team project is selected
        info = pageModel.validate();
        Assert.assertNotNull(info);
        Assert.assertEquals(ImportPageModel.PROP_PROJECT_TABLE, info.getValidationSource());
        Assert.assertEquals(TfPluginBundle.KEY_IMPORT_DIALOG_ERRORS_PROJECT_NOT_SELECTED, info.getValidationMessageKey());

        //add a team project to the table model
        final ServerContext context = ServerContextHelper.getNewServerContext("mytest", true);
        ((MockImportPageModel) pageModel).addContext(context);
        Assert.assertEquals("Team project was not added to the table model",
                1, pageModel.getTableModel().getRowCount());
        pageModel.getTableSelectionModel().setSelectionInterval(0, 0);

        //verify error when repository name is not set
        info = pageModel.validate();
        Assert.assertNotNull(info);
        Assert.assertEquals(ImportPageModel.PROP_REPO_NAME, info.getValidationSource());
        Assert.assertEquals(TfPluginBundle.KEY_IMPORT_DIALOG_ERRORS_REPO_NAME_EMPTY, info.getValidationMessageKey());

        pageModel.setRepositoryName("new");

        //verify no errors after repo name is set
        info = pageModel.validate();
        Assert.assertNull("Unexpected validation errors on import dialog",
                info);

        //clear repository name to verify error shows up again
        pageModel.setRepositoryName(null);
        info = pageModel.validate();
        Assert.assertNotNull(info);
        Assert.assertEquals(ImportPageModel.PROP_REPO_NAME, info.getValidationSource());
        Assert.assertEquals(TfPluginBundle.KEY_IMPORT_DIALOG_ERRORS_REPO_NAME_EMPTY, info.getValidationMessageKey());
    }

    /**
     * Verifies the goto link feature used for create an account in VSO handles null and empty strings
     */
    public void testGotoLink() {
        ImportPageModel pageModel = new MockImportPageModel(new ImportModel(null), false);
        pageModel.gotoLink(null);
        pageModel.gotoLink("");
    }

    /**
     * This test verifies adding, getting and clearing errors on the page model
     */
    public void testErrors() {
        //verify no errors on new page model
        final ImportPageModel pageModel = new MockImportPageModel(new ImportModel(null), false);
        Assert.assertEquals("Unexpected errors on new import page model",
                0, pageModel.getErrors().size());
        Assert.assertFalse(pageModel.hasErrors());

        //add error
        final ModelValidationInfo error1 = ModelValidationInfo.createWithMessage("first error");
        pageModel.addError(error1);
        Assert.assertEquals("Error on page model not as expected",
                error1, pageModel.getErrors().get(0));
        Assert.assertEquals(1, pageModel.getErrors().size());
        Assert.assertTrue(pageModel.hasErrors());

        //add another error
        final ModelValidationInfo error2 = ModelValidationInfo.createWithMessage("second error");
        pageModel.addError(error2);
        Assert.assertEquals("Error on page model not as expected",
                error1, pageModel.getErrors().get(0));
        Assert.assertEquals("Error on page model not as expected",
                error2, pageModel.getErrors().get(1));
        Assert.assertEquals(2, pageModel.getErrors().size());
        Assert.assertTrue(pageModel.hasErrors());

        //verify error list is immutable
        try {
            pageModel.getErrors().add(ModelValidationInfo.createWithMessage("error"));
            Assert.fail("Error list returned from page model is not immutable");
        } catch (UnsupportedOperationException uoe) {
            //list cannot be modified
        }

        pageModel.clearErrors();
        Assert.assertEquals("Errors on page model not cleared",
                0, pageModel.getErrors().size());
        Assert.assertFalse(pageModel.hasErrors());
    }

    /**
     * This test verifies the behavior of the server name getter and setter for Tfs
     */
    public void testTfsServerName() {
        final ImportPageModel pageModel = new MockImportPageModel(new ImportModel(null), true); //tfs page model
        Assert.assertEquals("Server name on new page model is not empty", "", pageModel.getServerName());

        //set full server url
        pageModel.setServerName("http://myserver:8080/tfs");
        Assert.assertEquals("http://myserver:8080/tfs", pageModel.getServerName());

        //set just server host name
        pageModel.setServerName("myserverhost");
        Assert.assertEquals("Server url is not set correctly when only server name is specified",
                String.format(ImportPageModel.DEFAULT_SERVER_FORMAT, "myserverhost"), pageModel.getServerName());
    }

    /**
     * This test verifies the table model returns expected values in VSO page mode
     */
    public void testVsoTableModel() {
        //initialize and verify number of columns
        final ImportPageModel pageModel = new MockImportPageModel(new ImportModel(null), false);
        Assert.assertEquals("Number of columns in team projects table not as expected",
                2, pageModel.getTableModel().getColumnCount());
        Assert.assertEquals("Number of rows in team projects table not as expected",
                0, pageModel.getTableModel().getRowCount());


        //add mock objects to table
        final ServerContext context1 = ServerContextHelper.getNewServerContext("project1", true);
        ((MockImportPageModel) pageModel).addContext(context1);
        final ServerContext context2 = ServerContextHelper.getNewServerContext("project2", true);
        ((MockImportPageModel) pageModel).addContext(context2);

        //verify table rows have correct data
        Assert.assertEquals("Team project name not as expected",
                "project1", pageModel.getTableModel().getValueAt(0, 0));
        Assert.assertEquals("Account name not as expected",
                "project1.visualstudio.com", pageModel.getTableModel().getValueAt(0, 1));
        Assert.assertEquals("Team project name not as expected",
                "project2", pageModel.getTableModel().getValueAt(1, 0));
        Assert.assertEquals("Account name not as expected",
                "project2.visualstudio.com", pageModel.getTableModel().getValueAt(1, 1));

        //clear projects and verify table model is empty
        ((MockImportPageModel) pageModel).clearContexts();
        Assert.assertEquals("Team projects found in table model after they were cleared",
                0, pageModel.getTableModel().getRowCount());

    }

    /**
     * This test verifies the table model returns expected values in TFS page mode
     */
    public void testTfsTableModel() {
        //initialize and verify number of columns
        final ImportPageModel pageModel = new MockImportPageModel(new ImportModel(null), true);
        Assert.assertEquals("Number of columns in team projects table not as expected",
                2, pageModel.getTableModel().getColumnCount());
        Assert.assertEquals("Number of rows in team projects table not as expected",
                0, pageModel.getTableModel().getRowCount());


        //add mock objects to table
        final ServerContext context1 = ServerContextHelper.getNewServerContext("project1", false);
        ((MockImportPageModel) pageModel).addContext(context1);
        final ServerContext context2 = ServerContextHelper.getNewServerContext("project2", false);
        ((MockImportPageModel) pageModel).addContext(context2);

        //verify table rows have correct data
        Assert.assertEquals("Team project name not as expected",
                "project1", pageModel.getTableModel().getValueAt(0, 0));
        Assert.assertEquals("Account name not as expected",
                "Collection_project1", pageModel.getTableModel().getValueAt(0, 1));
        Assert.assertEquals("Team project name not as expected",
                "project2", pageModel.getTableModel().getValueAt(1, 0));
        Assert.assertEquals("Account name not as expected",
                "Collection_project2", pageModel.getTableModel().getValueAt(1, 1));

        //clear projects and verify table model is empty
        ((MockImportPageModel) pageModel).clearContexts();
        Assert.assertEquals("Team projects found in table model after they were cleared",
                0, pageModel.getTableModel().getRowCount());
    }

    public void testTableModelSelection() {
        final ImportPageModel pageModel = new MockImportPageModel(new ImportModel(null), false);

        //add mock objects to table
        final ServerContext context1 = ServerContextHelper.getNewServerContext("project1", true);
        ((MockImportPageModel) pageModel).addContext(context1);
        final ServerContext context2 = ServerContextHelper.getNewServerContext("project2", true);
        ((MockImportPageModel) pageModel).addContext(context2);

        //select 2nd team project and verify
        pageModel.getTableSelectionModel().setSelectionInterval(1, 1);
        Assert.assertEquals("Selected team project not as expected",
                context2, ((MockImportPageModel) pageModel).getSelectedContext());

        //select 1st team project and verify
        pageModel.getTableSelectionModel().setSelectionInterval(0, 0);
        Assert.assertEquals("Selected team project not as expected",
                context1, ((MockImportPageModel) pageModel).getSelectedContext());

        pageModel.getTableSelectionModel().clearSelection();
        Assert.assertNull("Selection in team projects table not cleared",
                ((MockImportPageModel) pageModel).getSelectedContext());
    }

    public void testTableModelFiltering() {
        final ImportPageModel pageModel = new MockImportPageModel(new ImportModel(null), false);

        //add mock objects to table
        final ServerContext context1 = ServerContextHelper.getNewServerContext("hello", true);
        ((MockImportPageModel) pageModel).addContext(context1);
        final ServerContext context2 = ServerContextHelper.getNewServerContext("world", true);
        ((MockImportPageModel) pageModel).addContext(context2);

        //filter for hello
        pageModel.setTeamProjectFilter("hel");
        Assert.assertEquals("Number of filtered team projects not as expected",
                1, pageModel.getTableModel().getRowCount());
        Assert.assertEquals("Filtered team project not as expected",
                "hello", pageModel.getTableModel().getValueAt(0, 0));

        //filter for world
        pageModel.setTeamProjectFilter("orld");
        Assert.assertEquals("Number of filtered team projects not as expected",
                1, pageModel.getTableModel().getRowCount());
        Assert.assertEquals("Filtered team project not as expected",
                "world", pageModel.getTableModel().getValueAt(0, 0));

        //filter for no match
        pageModel.setTeamProjectFilter("nomatches");
        Assert.assertEquals("Number of filtered team projects not as expected",
                0, pageModel.getTableModel().getRowCount());

        //clear filter and verify all team projects are shown
        pageModel.setTeamProjectFilter("");
        Assert.assertEquals("Number of filtered team projects not as expected",
                2, pageModel.getTableModel().getRowCount());
        Assert.assertEquals("hello", pageModel.getTableModel().getValueAt(0, 0));
        Assert.assertEquals("world", pageModel.getTableModel().getValueAt(1, 0));

        //verify null filter works
        pageModel.setTeamProjectFilter(null);
        Assert.assertEquals("Number of filtered team projects not as expected",
                2, pageModel.getTableModel().getRowCount());

    }

    /**
     * This test verifies the table model handles the case where repository filter is set before rows are added to the table
     */
    public void testTableModelFilterFirst() {
        final ImportPageModel pageModel = new MockImportPageModel(new ImportModel(null), true);

        //set filter
        pageModel.setTeamProjectFilter("world");

        //add mock objects to table
        final ServerContext context1 = ServerContextHelper.getNewServerContext("hello", true);
        ((MockImportPageModel) pageModel).addContext(context1);
        final ServerContext context2 = ServerContextHelper.getNewServerContext("world", true);
        ((MockImportPageModel) pageModel).addContext(context2);

        //verify row for world is shown
        Assert.assertEquals("Number of filtered team projects not as expected",
                1, pageModel.getTableModel().getRowCount());
        Assert.assertEquals("Filtered team project not as expected",
                "world", pageModel.getTableModel().getValueAt(0, 0));

        //clear filter and verify all team projects are shown
        pageModel.setTeamProjectFilter(null);
        Assert.assertEquals("Number of filtered team projects not as expected",
                2, pageModel.getTableModel().getRowCount());
        Assert.assertEquals("hello", pageModel.getTableModel().getValueAt(0, 0));
        Assert.assertEquals("world", pageModel.getTableModel().getValueAt(1, 0));
    }


}
