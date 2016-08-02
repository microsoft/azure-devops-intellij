// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.vcsimport;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.forms.LoginForm;
import com.microsoft.alm.plugin.idea.common.ui.controls.UserAccountPanel;
import com.microsoft.alm.plugin.idea.git.ui.vcsimport.mocks.MockImportPage;
import com.microsoft.alm.plugin.idea.git.ui.vcsimport.mocks.MockImportPageModel;
import org.junit.Assert;
import org.junit.Test;

import java.awt.event.ActionEvent;

public class ImportPageControllerTest extends IdeaAbstractTest {
    @Test
    public void testActionPerformed() {
        final MockImportPageModel pageModel = new MockImportPageModel(null, false);
        final MockImportPage page = new MockImportPage();
        final ImportPageController pageController = new ImportPageController(null, pageModel, page);

        //trigger create new account
        pageController.actionPerformed(new ActionEvent(this, 1, LoginForm.CMD_CREATE_ACCOUNT));
        Assert.assertEquals(ImportPageModel.URL_CREATE_ACCOUNT, pageModel.getUrlVisited());

        //trigger sign in
        pageController.actionPerformed(new ActionEvent(this, 1, LoginForm.CMD_SIGN_IN));
        Assert.assertTrue(pageModel.isLoadTeamProjectsCalled());
        Assert.assertTrue(pageModel.isConnected());
        pageModel.clearInternals();

        //trigger sign out
        pageController.actionPerformed(new ActionEvent(this, 1, UserAccountPanel.CMD_SIGN_OUT));
        Assert.assertFalse("Sign out from tfs import page did not take user back to login page",
                pageModel.isConnected());
        pageModel.clearInternals();

        //team project filter changed
        page.setTeamProjectFilter("filter");
        pageController.actionPerformed(new ActionEvent(this, 1, ImportForm.CMD_PROJECT_FILTER_CHANGED));
        Assert.assertEquals("filter", pageModel.getTeamProjectFilter());
        pageModel.clearInternals();

        //refresh team projects table
        Assert.assertEquals(0, pageModel.getTableModel().getRowCount());
        pageController.actionPerformed(new ActionEvent(this, 1, ImportForm.CMD_REFRESH));
        Assert.assertTrue("Team projects were not reloaded on refresh",
                pageModel.isLoadTeamProjectsCalled());
        pageModel.clearInternals();
    }
}
