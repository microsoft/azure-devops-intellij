// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.checkout;

import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Basic tests for the checkout form
 */
public class CheckoutFormTest extends IdeaAbstractTest {
    @Test
    public void initVso() {
        final CheckoutForm form = new CheckoutForm(true, RepositoryContext.Type.GIT);
        assertFalse(form.getUserAccountPanel().isWindowsAccount());
    }

    @Test
    public void initTfs() {
        final CheckoutForm form = new CheckoutForm(false, RepositoryContext.Type.GIT);
        assertTrue(form.getUserAccountPanel().isWindowsAccount());
    }

    @Test
    public void showBusySpinner() {
        final CheckoutForm form = new CheckoutForm(true, RepositoryContext.Type.GIT);
        form.setLoading(true);
        assertTrue(form.getBusySpinner().isVisible());
        form.setLoading(false);
        assertFalse(form.getBusySpinner().isVisible());
    }

    @Test
    public void parentDirectory() {
        final CheckoutForm form = new CheckoutForm(false, RepositoryContext.Type.GIT);
        form.setParentDirectory("C:/My Path ");
        assertEquals("C:/My Path", form.getParentDirectory()); //Verify getter trims the path
    }

    @Test
    public void directoryName() {
        final CheckoutForm form = new CheckoutForm(true, RepositoryContext.Type.GIT);
        form.setDirectoryName(" My Directory");
        assertEquals("My Directory", form.getDirectoryName()); //Verify getter trims the name
    }

    @Test
    public void repositoryFilter() {
        final CheckoutForm form = new CheckoutForm(false, RepositoryContext.Type.GIT);
        form.setRepositoryFilter("My repo ");
        assertEquals("My repo ", form.getRepositoryFilter()); //Verify getter does not trim the filter
    }
}
