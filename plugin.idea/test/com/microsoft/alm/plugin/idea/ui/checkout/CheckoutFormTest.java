// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.checkout;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import org.junit.Ignore;
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
        final CheckoutForm form = new CheckoutForm(true);
        assertFalse(form.getUserAccountPanel().isWindowsAccount());
    }

    @Test
    public void initTfs() {
        final CheckoutForm form = new CheckoutForm(false);
        assertTrue(form.getUserAccountPanel().isWindowsAccount());
    }

    @Test
    public void showBusySpinner() {
        final CheckoutForm form = new CheckoutForm(true);
        form.setLoading(true);
        assertTrue(form.getBusySpinner().isVisible());
        form.setLoading(false);
        assertFalse(form.getBusySpinner().isVisible());
    }

    @Test
    @Ignore("TODO: these tests are failing because of a dependency issue in swt")
    public void parentDirectory() {
        final CheckoutForm form = new CheckoutForm(false);
        form.setParentDirectory("C:/My Path ");
        assertEquals("C:/My Path", form.getParentDirectory()); //Verify getter trims the path
    }

    @Test
    @Ignore("TODO: these tests are failing because of a dependency issue in swt")
    public void directoryName() {
        final CheckoutForm form = new CheckoutForm(true);
        form.setDirectoryName(" My Directory");
        assertEquals("My Directory", form.getDirectoryName()); //Verify getter trims the name
    }

    @Test
    @Ignore("TODO: these tests are failing because of a dependency issue in swt")
    public void repositoryFilter() {
        final CheckoutForm form = new CheckoutForm(false);
        form.setRepositoryFilter("My repo ");
        assertEquals("My repo ", form.getRepositoryFilter()); //Verify getter does not trim the filter
    }
}
