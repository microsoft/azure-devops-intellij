// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.vcsimport;

import com.intellij.openapi.util.Disposer;
import com.microsoft.alm.plugin.idea.DisposableTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Basic tests for the import form
 */
public class ImportFormTest extends DisposableTest {

    private ImportForm newImportForm(boolean vsoSelected) {
        ImportForm form = new ImportForm(vsoSelected);
        Disposer.register(testDisposable, form);
        return form;
    }

    @Test
    public void initVso() {
        final ImportForm form = newImportForm(true);
        assertFalse(form.getUserAccountPanel().isWindowsAccount());
    }

    @Test
    public void initTfs() {
        final ImportForm form = newImportForm(false);
        assertTrue(form.getUserAccountPanel().isWindowsAccount());
    }

    @Test
    public void showBusySpinner() {
        final ImportForm form = newImportForm(true);
        form.setLoading(true);
        assertTrue(form.getBusySpinner().isVisible());
        form.setLoading(false);
        assertFalse(form.getBusySpinner().isVisible());
    }

    @Test
    public void repositoryName() {
        final ImportForm form = newImportForm(false);
        form.setRepositoryName(" MyNewRepo ");
        assertEquals("MyNewRepo", form.getRepositoryName()); //Verify getter trims the path
    }

    @Test
    public void projectFilter() {
        final ImportForm form = newImportForm(false);
        form.setTeamProjectFilter("My project ");
        assertEquals("My project ", form.getTeamProjectFilter()); //Verify getter does not trim the filter
    }
}
