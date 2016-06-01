// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.openapi.ui.JBMenuItem;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabForm;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VcsWorkItemsFormTest {
    VcsWorkItemsForm underTest;

    @Before
    public void setUp() {
        underTest = new VcsWorkItemsForm();
    }

    @Test
    public void testCreateCustomView() {
        underTest.createCustomView();
        assertNotNull(underTest.getWorkItemsTable());
    }

    @Test
    public void testGetMenuItems() {
        List<JBMenuItem> menuItemList = underTest.getMenuItems(null);
        assertEquals(2, menuItemList.size());
        assertEquals(TabForm.CMD_OPEN_SELECTED_ITEM_IN_BROWSER, menuItemList.get(0).getActionCommand());
        assertEquals(VcsWorkItemsForm.CMD_CREATE_BRANCH, menuItemList.get(1).getActionCommand());
    }
}
