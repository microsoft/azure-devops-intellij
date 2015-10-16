// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Basic tests for the import form
 */
public class ImportFormTest extends IdeaAbstractTest {

    @Test
    public void initVso() {
        final ImportForm form = new ImportForm(true);
        assertFalse(form.getUserAccountPanel().isWindowsAccount());
    }

    @Test
    public void initTfs() {
        final ImportForm form = new ImportForm(false);
        assertTrue(form.getUserAccountPanel().isWindowsAccount());
    }

    @Test
    public void showBusySpinner() {
        final ImportForm form = new ImportForm(true);
        form.setLoading(true);
        assertTrue(form.getBusySpinner().isVisible());
        form.setLoading(false);
        assertFalse(form.getBusySpinner().isVisible());
    }

    @Test
    @Ignore("TODO: these tests are failing because of a dependency issue in swt")
    public void repositoryName() {
        final ImportForm form = new ImportForm(false);
        form.setRepositoryName(" MyNewRepo ");
        assertEquals("MyNewRepo", form.getRepositoryName()); //Verify getter trims the path
    }

    @Test
    @Ignore("TODO: these tests are failing because of a dependency issue in swt")
    public void projectFilter() {
        final ImportForm form = new ImportForm(false);
        form.setTeamProjectFilter("My project ");
        assertEquals("My project ", form.getTeamProjectFilter()); //Verify getter does not trim the filter
    }
}
